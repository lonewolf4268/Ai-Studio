import { GoogleGenAI } from '@google/genai';
const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
async function run() {
  try {
    console.log('Sending...');
    const stream = await ai.models.generateContent({
      model: 'gemini-3.7-flash',
      contents: 'Hello',
    });
    console.log('Result:', stream.text);
  } catch (e) {
    console.error('ERROR:', e);
  }
}
run();
