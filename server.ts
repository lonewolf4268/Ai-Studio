import express from 'express';
import path from 'path';
import { GoogleGenAI } from '@google/genai';
import { createServer as createViteServer } from 'vite';

const app = express();
const PORT = 3000;

app.use(express.json({ limit: '20mb' }));
app.use(express.urlencoded({ extended: true, limit: '20mb' }));

// Lazy GoogleGenAI client helper
function getGeminiClient(): GoogleGenAI {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    throw new Error('GEMINI_API_KEY environment variable is not set. Please provide an API key.');
  }
  return new GoogleGenAI({
    apiKey,
    httpOptions: {
      headers: {
        'User-Agent': 'aistudio-build',
      },
    },
  });
}

// Health check endpoint
app.get('/api/health', (_req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Chat endpoint with streaming support via Server-Sent Events (SSE)
app.post('/api/chat', async (req, res) => {
  try {
    const { history, prompt, image } = req.body;

    // Build contextual prompt from chat history or direct prompt
    let formattedPrompt = '';
    if (Array.isArray(history) && history.length > 0) {
      formattedPrompt = history
        .map((msg: { sender: string; message: string }) => `${msg.sender}: ${msg.message}`)
        .join('\n');
    } else if (typeof prompt === 'string') {
      formattedPrompt = prompt;
    } else {
      return res.status(400).json({ error: 'Either history or prompt must be provided.' });
    }

    const ai = getGeminiClient();

    // Set headers for SSE streaming
    res.setHeader('Content-Type', 'text/event-stream');
    res.setHeader('Cache-Control', 'no-cache');
    res.setHeader('Connection', 'keep-alive');
    res.flushHeaders?.();

    let streamResponse;
    if (image && typeof image.data === 'string' && typeof image.mimeType === 'string') {
      // Multimodal request with image and prompt
      const imagePart = {
        inlineData: {
          mimeType: image.mimeType,
          data: image.data.replace(/^data:[^;]+;base64,/, ''),
        },
      };
      const textPart = {
        text: formattedPrompt || 'Describe this image or transcribe the text.',
      };

      streamResponse = await ai.models.generateContentStream({
        model: 'gemini-2.5-flash',
        contents: {
          parts: [imagePart, textPart],
        },
      });
    } else {
      // Text-only request
      streamResponse = await ai.models.generateContentStream({
        model: 'gemini-2.5-flash',
        contents: formattedPrompt,
      });
    }

    for await (const chunk of streamResponse) {
      const text = chunk.text || '';
      if (text) {
        res.write(`data: ${JSON.stringify({ text })}\n\n`);
      }
    }

    res.write('data: [DONE]\n\n');
    return res.end();
  } catch (error: any) {
    console.error('Gemini API error:', error);
    let errorMessage = error?.message || 'An error occurred while generating a response.';
    if (errorMessage.includes('RESOURCE_EXHAUSTED') || errorMessage.includes('quota') || errorMessage.includes('exceeded')) {
      errorMessage = 'Gemini API quota or rate limit exceeded. Please check your plan and billing details at https://ai.google.dev/gemini-api/docs/rate-limits.';
    }
    if (!res.headersSent) {
      return res.status(500).json({ error: errorMessage });
    } else {
      res.write(`data: ${JSON.stringify({ error: errorMessage })}\n\n`);
      return res.end();
    }
  }
});

app.post('/api/suggestions', async (req, res) => {
  try {
    const { history } = req.body;
    if (!Array.isArray(history) || history.length === 0) {
      return res.status(400).json({ error: 'History must be provided.' });
    }

    const formattedPrompt = history
      .map((msg: { sender: string; message: string }) => `${msg.sender}: ${msg.message}`)
      .join('\n') + '\n\nBased on the conversation above, generate exactly 3 short, engaging follow-up questions or prompts the user could ask next to continue the conversation. Respond strictly with a JSON array of strings, nothing else. Maximum 10 words per prompt.';

    const ai = getGeminiClient();
    const response = await ai.models.generateContent({
      model: 'gemini-2.5-flash',
      contents: formattedPrompt,
      config: {
        responseMimeType: 'application/json',
      }
    });

    const text = response.text || '[]';
    let suggestions = [];
    try {
      suggestions = JSON.parse(text);
    } catch (e) {
      suggestions = [];
    }

    if (!Array.isArray(suggestions)) {
      suggestions = [];
    }

    res.json({ suggestions: suggestions.slice(0, 3) });
  } catch (error: any) {
    console.error('Suggestions API error:', error);
    res.status(500).json({ error: 'Failed to generate suggestions' });
  }
});

async function startServer() {
  if (process.env.NODE_ENV !== 'production') {
    const vite = await createViteServer({
      server: { middlewareMode: true, host: '0.0.0.0', port: PORT },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), 'dist');
    app.use(express.static(distPath));
    app.get('*', (_req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Server running on http://0.0.0.0:${PORT}`);
  });
}

startServer();
