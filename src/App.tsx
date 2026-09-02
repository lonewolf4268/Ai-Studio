import React, { useState, useEffect, useRef } from 'react';
import { Header } from './components/Header';
import { MessageItem } from './components/MessageItem';
import { ChatInput } from './components/ChatInput';
import { ChatMessage, ChatSession } from './types';
import { recognizeTextFromImage, fileToBase64 } from './utils/ocr';
import { Bot, Loader2, MessageSquare, Plus, Trash2, X, Pencil, ArrowUp, Search, BookOpen, Pin, ChevronRight, Folder, Check, Code, PencilLine } from 'lucide-react';

function formatRelativeTime(timestamp: number): string {
  const diffInMs = Date.now() - timestamp;
  const diffInMins = Math.floor(diffInMs / (1000 * 60));
  const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60));
  const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));

  if (diffInMins < 1) return 'Just now';
  if (diffInMins < 60) return `${diffInMins}m ago`;
  if (diffInHours < 24) return `${diffInHours}h ago`;
  if (diffInDays < 7) return `${diffInDays}d ago`;
  return new Date(timestamp).toLocaleDateString();
}

export const App: React.FC = () => {
  // Chat sessions state
  const [sessions, setSessions] = useState<ChatSession[]>(() => {
    try {
      const saved = localStorage.getItem('ai_studio_sessions');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) {
          return parsed;
        }
      }
    } catch (e) {
      console.error('Failed to load sessions', e);
    }
    const defaultSession: ChatSession = {
      id: `session-${Date.now()}`,
      title: 'New Chat',
      messages: [],
      createdAt: Date.now(),
    };
    return [defaultSession];
  });

  const [currentSessionId, setCurrentSessionId] = useState<string>(() => {
    return sessions[0]?.id || 'session-1';
  });

  const [editingSessionId, setEditingSessionId] = useState<string | null>(null);
  const [editingTitleValue, setEditingTitleValue] = useState<string>('');
  const [editingCategoryValue, setEditingCategoryValue] = useState<string>('General');
  const [editingTagsValue, setEditingTagsValue] = useState<string>('');
  const [sessionsFilterQuery, setSessionsFilterQuery] = useState<string>('');
  const [selectedCategoryFilter, setSelectedCategoryFilter] = useState<string>('All');
  const [autoScroll, setAutoScroll] = useState<boolean>(true);
  const [isTemplatesOpen, setIsTemplatesOpen] = useState<boolean>(false);
  const [inputMessageText, setInputMessageText] = useState<string>('');

  // Sidebar responsive desktop view
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState<boolean>(() => {
    const saved = localStorage.getItem('ai_studio_sidebar_collapsed');
    return saved === 'true';
  });

  useEffect(() => {
    localStorage.setItem('ai_studio_sidebar_collapsed', String(isSidebarCollapsed));
  }, [isSidebarCollapsed]);

  interface PromptTemplate {
    id: string;
    title: string;
    prompt: string;
  }

  const [promptTemplates, setPromptTemplates] = useState<PromptTemplate[]>(() => {
    try {
      const saved = localStorage.getItem('ai_studio_templates');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) return parsed;
      }
    } catch (e) {}
    return [
      { id: 't1', title: 'Code Refactoring', prompt: 'Please refactor the following code for cleanliness, performance, and best practices:\n\n' },
      { id: 't2', title: 'Bug Fixer', prompt: 'Analyze this code snippet, identify any bugs or edge case failures, and provide a corrected version:\n\n' },
      { id: 't3', title: 'Summarizer', prompt: 'Provide a concise, bulleted summary of the following text:\n\n' },
      { id: 't4', title: 'Explain Like I\'m 5', prompt: 'Explain the following concept or technical topic in simple terms with an everyday analogy:\n\n' },
    ];
  });

  useEffect(() => {
    try {
      localStorage.setItem('ai_studio_templates', JSON.stringify(promptTemplates));
    } catch (e) {}
  }, [promptTemplates]);

  const [newTemplateTitle, setNewTemplateTitle] = useState('');
  const [newTemplatePrompt, setNewTemplatePrompt] = useState('');
  const [editingTemplateId, setEditingTemplateId] = useState<string | null>(null);
  const [editTitleVal, setEditTitleVal] = useState('');
  const [editPromptVal, setEditPromptVal] = useState('');
  const [showAddTemplateForm, setShowAddTemplateForm] = useState(false);

  const currentSession = sessions.find((s) => s.id === currentSessionId) || sessions[0];
  const messages = currentSession?.messages || [];

  const setMessages = (updater: ChatMessage[] | ((prev: ChatMessage[]) => ChatMessage[])) => {
    setSessions((prevSessions) => {
      return prevSessions.map((session) => {
        if (session.id === currentSessionId) {
          const newMessages = typeof updater === 'function' ? updater(session.messages) : updater;
          // Auto-update title based on first user message if title is 'New Chat'
          let title = session.title;
          if (title === 'New Chat') {
            const firstUserMsg = newMessages.find((m) => m.sender === 'You');
            if (firstUserMsg) {
              title = firstUserMsg.message.slice(0, 32) + (firstUserMsg.message.length > 32 ? '...' : '');
            }
          }
          return { ...session, messages: newMessages, title, updatedAt: Date.now() };
        }
        return session;
      });
    });
  };

  // Persist sessions
  useEffect(() => {
    try {
      localStorage.setItem('ai_studio_sessions', JSON.stringify(sessions));
    } catch (e) {
      console.error('Failed to save sessions', e);
    }
  }, [sessions]);

  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isOcrProcessing, setIsOcrProcessing] = useState<boolean>(false);
  const [isMobileDrawerOpen, setIsMobileDrawerOpen] = useState<boolean>(false);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedImage, setSelectedImage] = useState<{
    file: File;
    previewUrl: string;
    ocrText?: string;
  } | null>(null);

  const [showBackToTop, setShowBackToTop] = useState<boolean>(false);
  const streamingTargetTextRef = useRef<Record<string, string>>({});

  // Typewriter smooth streaming effect
  useEffect(() => {
    const interval = setInterval(() => {
      let updated = false;
      const nextMessages = messages.map((msg) => {
        const target = streamingTargetTextRef.current[msg.id];
        if (target !== undefined && msg.message !== target) {
          updated = true;
          const diff = target.length - msg.message.length;
          const step = Math.max(1, Math.min(diff, Math.ceil(diff / 3), 8));
          const nextSubstr = target.slice(0, msg.message.length + step);
          return { ...msg, message: nextSubstr };
        }
        return msg;
      });

      if (updated) {
        setSessions((prevSessions) =>
          prevSessions.map((session) =>
            session.id === currentSessionId ? { ...session, messages: nextMessages } : session
          )
        );
      }
    }, 25);

    return () => clearInterval(interval);
  }, [messages, currentSessionId]);

  // Dark mode state with localStorage persistence
  const [isDarkMode, setIsDarkMode] = useState<boolean>(() => {
    const saved = localStorage.getItem('ai_studio_theme');
    if (saved) return saved === 'dark';
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  });

  useEffect(() => {
    localStorage.setItem('ai_studio_theme', isDarkMode ? 'dark' : 'light');
    if (isDarkMode) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [isDarkMode]);

  const toggleDarkMode = () => {
    setIsDarkMode((prev) => !prev);
  };

  const handleExportTranscript = (format: 'md' | 'txt') => {
    if (messages.length === 0) return;
    const dateStr = new Date().toLocaleString();
    let content = '';

    if (format === 'md') {
      content = `# AI Studio Chat Transcript\nExported on: ${dateStr}\n\n---\n\n`;
      messages.forEach((m) => {
        if (m.sender === 'App') {
          content += `> **System Error**: ${m.message}\n\n`;
        } else {
          content += `### ${m.sender} (${m.timestamp})\n\n${m.message}\n\n`;
        }
      });
    } else {
      content = `AI STUDIO CHAT TRANSCRIPT\nExported on: ${dateStr}\n====================================\n\n`;
      messages.forEach((m) => {
        content += `[${m.timestamp}] ${m.sender}: ${m.message}\n\n`;
      });
    }

    const blob = new Blob([content], { type: format === 'md' ? 'text/markdown' : 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `ai-studio-transcript-${Date.now()}.${format}`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const isInitialGreetingSent = useRef<Record<string, boolean>>({});

  // Auto-scroll to bottom of chat
  const scrollToBottom = () => {
    if (autoScroll) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isLoading, isOcrProcessing]);

  // Initial greeting for new sessions if empty
  useEffect(() => {
    if (messages.length > 0 || isInitialGreetingSent.current[currentSessionId]) return;
    isInitialGreetingSent.current[currentSessionId] = true;

    const welcomeMessages = [
      'Hello! I am your AI assistant. Send me a message or attach an image to get started!',
      'Hi there! How can I help you today? Feel free to ask questions or analyze images.',
      "Welcome! I'm ready to assist you with writing, coding, summaries, and more.",
      'Hello! What would you like to explore or create together today?',
    ];

    const randomMessage = welcomeMessages[Math.floor(Math.random() * welcomeMessages.length)];

    setMessages([
      {
        id: 'init-ai',
        sender: 'Ai',
        message: randomMessage,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      },
    ]);
  }, [currentSessionId]);

  // Call Google AI server endpoint with streaming support
  const askGoogleAi = async (
    fullHistory: ChatMessage[],
    imageData?: { data: string; mimeType: string },
    directPrompt?: string
  ) => {
    setIsLoading(true);
    const aiMessageId = `ai-${Date.now()}`;
    const timestamp = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    // Add placeholder AI message for real-time streaming updates
    setMessages((prev) => [
      ...prev,
      {
        id: aiMessageId,
        sender: 'Ai',
        message: '',
        timestamp,
      },
    ]);

    try {
      const payload: any = {};
      
      if (imageData) {
        payload.image = imageData;
        payload.prompt = directPrompt || 'Describe this image or explain the contents.';
      } else {
        payload.history = fullHistory
          .filter((m) => m.sender === 'You' || m.sender === 'Ai')
          .map((m) => ({ sender: m.sender, message: m.message }));
      }

      const response = await fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.error || 'Failed to get response from AI');
      }

      if (!response.body) {
        throw new Error('Response body is missing.');
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let accumulatedText = '';
      let buffer = '';

      while (true) {
        const { value, done } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          const trimmed = line.trim();
          if (trimmed.startsWith('data:')) {
            const dataStr = trimmed.replace('data:', '').trim();
            if (dataStr === '[DONE]') {
              break;
            }
            try {
              const parsed = JSON.parse(dataStr);
              if (parsed.error) {
                throw new Error(parsed.error);
              }
              if (parsed.text) {
                accumulatedText += parsed.text;
                streamingTargetTextRef.current[aiMessageId] = accumulatedText;
              }
            } catch (e) {
              // Ignore non-json or malformed chunk lines
            }
          }
        }
      }

      if (!accumulatedText) {
        streamingTargetTextRef.current[aiMessageId] = 'No response generated.';
      }

      // Fetch quick-follow-up suggestions
      try {
        const suggRes = await fetch('/api/suggestions', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            history: fullHistory.concat({
              id: aiMessageId,
              sender: 'Ai',
              message: accumulatedText,
              timestamp
            })
          }),
        });
        if (suggRes.ok) {
          const { suggestions } = await suggRes.json();
          if (suggestions && Array.isArray(suggestions) && suggestions.length > 0) {
            setMessages((prev) => 
              prev.map(m => m.id === aiMessageId ? { ...m, suggestions } : m)
            );
          }
        }
      } catch (suggErr) {
        console.error('Failed to fetch suggestions:', suggErr);
      }
    } catch (err: any) {
      console.error('AI query error:', err);
      setMessages((prev) => {
        const filtered = prev.filter((m) => m.id !== aiMessageId);
        return [
          ...filtered,
          {
            id: `app-err-${Date.now()}`,
            sender: 'App',
            message: err.message || 'An error occurred while connecting to AI Studio.',
            timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          },
        ];
      });
    } finally {
      setIsLoading(false);
    }
  };

  // Handle image upload & OCR processing matching Android processImage
  const handleImageSelected = async (file: File) => {
    const previewUrl = URL.createObjectURL(file);
    setSelectedImage({ file, previewUrl });
    setIsOcrProcessing(true);

    try {
      const extracted = await recognizeTextFromImage(file);
      if (extracted && extracted.trim().length > 0) {
        setSelectedImage({ file, previewUrl, ocrText: extracted });
      } else {
        setSelectedImage({ file, previewUrl, ocrText: '' });
      }
    } catch (err) {
      console.warn('Client OCR notice:', err);
      setSelectedImage({ file, previewUrl });
    } finally {
      setIsOcrProcessing(false);
    }
  };

  // Handle message sending
  const handleSendMessage = async (userText: string) => {
    let finalUserText = userText;
    let imagePayload: { data: string; mimeType: string } | undefined;
    let imagePreviewUrl: string | undefined;
    let ocrInfo: string | undefined;

    if (selectedImage) {
      imagePreviewUrl = selectedImage.previewUrl;
      ocrInfo = selectedImage.ocrText;

      try {
        const { base64, mimeType } = await fileToBase64(selectedImage.file);
        imagePayload = { data: base64, mimeType };
      } catch (e) {
        console.error('Failed to convert image to base64', e);
      }

      if (!finalUserText.trim() && selectedImage.ocrText) {
        finalUserText = selectedImage.ocrText;
      } else if (!finalUserText.trim()) {
        finalUserText = 'Please examine this image and provide insights.';
      }
    }

    const newUserMessage: ChatMessage = {
      id: `user-${Date.now()}`,
      sender: 'You',
      message: finalUserText,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      imageUri: imagePreviewUrl,
      extractedText: ocrInfo,
    };

    // Clear old suggestions from previous messages to keep UI clean
    const historyWithoutOldSuggestions = messages.map(m => ({ ...m, suggestions: undefined }));
    const updatedHistory = [...historyWithoutOldSuggestions, newUserMessage];
    
    setMessages(updatedHistory);
    setSelectedImage(null);

    await askGoogleAi(updatedHistory, imagePayload, finalUserText);
  };

  const handleClearChat = () => {
    setMessages([]);
    setSelectedImage(null);
  };

  const handleReaction = (messageId: string, reaction: 'thumbs-up' | 'thumbs-down' | undefined) => {
    setMessages((prev) =>
      prev.map((m) => (m.id === messageId ? { ...m, reaction } : m))
    );
  };

  const handleDeleteMessageItem = (messageId: string) => {
    setMessages((prev) => prev.filter((m) => m.id !== messageId));
  };

  const handleRetryMessage = (messageId: string) => {
    const msg = messages.find((m) => m.id === messageId);
    if (!msg) return;

    let textToRetry = msg.message;
    if (msg.sender === 'App') {
      const msgIndex = messages.findIndex((m) => m.id === messageId);
      const prevUserMsg = [...messages]
        .slice(0, msgIndex)
        .reverse()
        .find((m) => m.sender === 'You');
      if (prevUserMsg) {
        textToRetry = prevUserMsg.message;
      }
    }
    handleSendMessage(textToRetry);
  };

  const handleNewChat = () => {
    const newSession: ChatSession = {
      id: `session-${Date.now()}`,
      title: 'New Chat',
      messages: [],
      createdAt: Date.now(),
      updatedAt: Date.now(),
      category: selectedCategoryFilter !== 'All' ? selectedCategoryFilter : 'General',
    };
    setSessions((prev) => [newSession, ...prev]);
    setCurrentSessionId(newSession.id);
    setIsMobileDrawerOpen(false);
  };

  const handleDeleteSession = (sessionId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (sessions.length <= 1) {
      handleClearChat();
      return;
    }
    const updated = sessions.filter((s) => s.id !== sessionId);
    setSessions(updated);
    if (currentSessionId === sessionId) {
      setCurrentSessionId(updated[0].id);
    }
  };

  const handleStartRename = (session: ChatSession, e: React.MouseEvent) => {
    e.stopPropagation();
    setEditingSessionId(session.id);
    setEditingTitleValue(session.title);
    setEditingCategoryValue(session.category || 'General');
    setEditingTagsValue((session.tags || []).join(', '));
  };

  const handleSaveRename = (sessionId: string, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    if (editingTitleValue.trim()) {
      const parsedTags = editingTagsValue.split(',').map(t => t.trim()).filter(Boolean);
      setSessions((prev) =>
        prev.map((s) => (s.id === sessionId ? { ...s, title: editingTitleValue.trim(), category: editingCategoryValue, tags: parsedTags } : s))
      );
    }
    setEditingSessionId(null);
  };

  const handleScroll = (e: React.UIEvent<HTMLDivElement>) => {
    const target = e.currentTarget;
    const scrollBottom = target.scrollHeight - target.scrollTop - target.clientHeight;
    if (scrollBottom > 280) {
      setShowBackToTop(true);
    } else {
      setShowBackToTop(false);
    }
  };

  // Filter messages by search query if any
  const filteredMessages = searchQuery.trim()
    ? messages.filter((m) => m.message.toLowerCase().includes(searchQuery.toLowerCase()))
    : messages;

  const getSidebarCategoryCount = (categoryName: string) => {
    return sessions.filter(s => categoryName === 'All' || (s.category || 'General') === categoryName).length;
  };

  // Helper renderer for Sidebar content (DRY)
  const renderSidebarContent = () => (
    <div className="h-full flex flex-col bg-slate-50 dark:bg-slate-900 border-r border-slate-200/80 dark:border-slate-800">
      {/* Title & Brand */}
      <div className="p-4 border-b border-slate-200/60 dark:border-slate-800 flex items-center justify-between">
        <div className="flex items-center space-x-2.5">
          <div className="w-8 h-8 rounded-lg bg-blue-600 flex items-center justify-center text-white shadow-3xs">
            <Bot className="w-4.5 h-4.5" />
          </div>
          <div>
            <span className="font-bold text-xs tracking-tight text-slate-900 dark:text-slate-100 block">AI Workspace</span>
            <span className="text-[10px] text-slate-400 dark:text-slate-500 font-medium">Session History</span>
          </div>
        </div>
        <button
          onClick={() => {
            setIsMobileDrawerOpen(false);
            setIsSidebarCollapsed(true);
          }}
          className="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-200/40 dark:hover:bg-slate-800 rounded-md transition-colors lg:hidden"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      {/* Primary Actions */}
      <div className="p-3">
        <button
          onClick={handleNewChat}
          className="w-full py-2.5 px-4 bg-slate-900 hover:bg-slate-800 dark:bg-slate-100 dark:hover:bg-white text-white dark:text-slate-900 rounded-xl text-xs font-semibold flex items-center justify-center space-x-2 shadow-2xs hover:scale-102 active:scale-98 transition-all"
        >
          <Plus className="w-4 h-4 stroke-[2.5px]" />
          <span>New Chat Session</span>
        </button>
      </div>

      {/* Filter Filters */}
      <div className="px-3 pb-3 space-y-2.5 border-b border-slate-200/55 dark:border-slate-800/60">
        {/* Search */}
        <div className="flex items-center bg-white dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl px-2.5 py-1.5 space-x-2">
          <Search className="w-3.5 h-3.5 text-slate-400 shrink-0" />
          <input
            type="text"
            value={sessionsFilterQuery}
            onChange={(e) => setSessionsFilterQuery(e.target.value)}
            placeholder="Search chat titles..."
            className="bg-transparent text-xs text-slate-800 dark:text-slate-200 outline-none w-full font-medium"
          />
          {sessionsFilterQuery && (
            <button onClick={() => setSessionsFilterQuery('')} className="text-slate-400 hover:text-slate-600 shrink-0">
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>

        {/* Categories Pills */}
        <div className="space-y-1">
          <div className="flex items-center space-x-1.5 px-1 pb-1">
            <Folder className="w-3 h-3 text-slate-400" />
            <span className="text-[10px] text-slate-400 dark:text-slate-500 font-bold uppercase tracking-wider">Categories</span>
          </div>
          <div className="flex flex-wrap gap-1">
            {['All', 'General', 'Work', 'Coding', 'Personal'].map((cat) => {
              const isActive = selectedCategoryFilter === cat;
              const count = getSidebarCategoryCount(cat);
              return (
                <button
                  key={cat}
                  onClick={() => setSelectedCategoryFilter(cat)}
                  className={`px-2 py-1 rounded-lg text-[10px] font-bold transition-all flex items-center space-x-1 ${
                    isActive
                      ? 'bg-slate-900 text-slate-100 dark:bg-slate-100 dark:text-slate-900 shadow-3xs'
                      : 'bg-white hover:bg-slate-100 dark:bg-slate-950 dark:hover:bg-slate-800 border border-slate-200/50 dark:border-slate-800/80 text-slate-600 dark:text-slate-400'
                  }`}
                >
                  <span>{cat}</span>
                  <span className={`px-1 py-0.2 text-[8px] rounded font-bold ${isActive ? 'bg-slate-700 text-slate-200 dark:bg-slate-200 dark:text-slate-700' : 'bg-slate-100 dark:bg-slate-900 text-slate-400'}`}>{count}</span>
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Sessions list */}
      <div className="flex-1 overflow-y-auto px-2.5 py-3 space-y-1 scrollbar-thin">
        {sessions
          .filter((s) => {
            const query = sessionsFilterQuery.toLowerCase();
            const matchesQuery = s.title.toLowerCase().includes(query) || (s.tags || []).some(t => t.toLowerCase().includes(query));
            const sessionCategory = s.category || 'General';
            const matchesCategory = selectedCategoryFilter === 'All' || sessionCategory === selectedCategoryFilter;
            return matchesQuery && matchesCategory;
          })
          .sort((a, b) => {
            if (a.pinned && !b.pinned) return -1;
            if (!a.pinned && b.pinned) return 1;
            return b.createdAt - a.createdAt;
          })
          .map((session) => {
            const isEditing = editingSessionId === session.id;
            const isSelected = session.id === currentSessionId;
            return (
              <div
                key={session.id}
                onClick={() => {
                  if (!isEditing) {
                    setCurrentSessionId(session.id);
                    setIsMobileDrawerOpen(false);
                  }
                }}
                className={`group relative flex flex-col p-2.5 rounded-xl cursor-pointer border transition-all ${
                  isSelected
                    ? 'bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 shadow-3xs'
                    : 'bg-transparent border-transparent hover:bg-slate-200/40 dark:hover:bg-slate-800/50 text-slate-700 dark:text-slate-300'
                }`}
              >
                <div className="flex items-start justify-between min-w-0 w-full">
                  <div className="flex items-start space-x-2.5 min-w-0 flex-1">
                    <MessageSquare className={`w-4 h-4 shrink-0 mt-0.5 ${isSelected ? 'text-blue-500' : 'text-slate-400'}`} />
                    
                    {isEditing ? (
                      <div className="flex flex-col space-y-2 flex-1 p-1" onClick={(e) => e.stopPropagation()}>
                        <input
                          type="text"
                          value={editingTitleValue}
                          onChange={(e) => setEditingTitleValue(e.target.value)}
                          autoFocus
                          placeholder="Chat title"
                          className="bg-white dark:bg-slate-900 border border-blue-400 dark:border-blue-500 rounded-lg px-2 py-1 text-xs text-slate-900 dark:text-slate-100 w-full outline-none"
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') handleSaveRename(session.id);
                            if (e.key === 'Escape') setEditingSessionId(null);
                          }}
                        />
                        <input
                          type="text"
                          value={editingTagsValue}
                          onChange={(e) => setEditingTagsValue(e.target.value)}
                          placeholder="Tags (comma separated)"
                          className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-lg px-2 py-1 text-[11px] text-slate-800 dark:text-slate-200 w-full outline-none"
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') handleSaveRename(session.id);
                            if (e.key === 'Escape') setEditingSessionId(null);
                          }}
                        />
                        <div className="flex items-center justify-between gap-1.5">
                          <select
                            value={editingCategoryValue}
                            onChange={(e) => setEditingCategoryValue(e.target.value)}
                            className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-lg px-1.5 py-1 text-[11px] text-slate-700 dark:text-slate-300 outline-none flex-1"
                          >
                            <option value="General">General</option>
                            <option value="Work">Work</option>
                            <option value="Coding">Coding</option>
                            <option value="Personal">Personal</option>
                          </select>
                          <button
                            onClick={(e) => handleSaveRename(session.id, e)}
                            className="px-2.5 py-1 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-[11px] font-semibold flex items-center space-x-1"
                          >
                            <Check className="w-3 h-3" />
                            <span>Save</span>
                          </button>
                        </div>
                      </div>
                    ) : (
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center space-x-1">
                          {session.pinned && <Pin className="w-3 h-3 text-amber-500 fill-amber-500 shrink-0" />}
                          <span className={`text-xs truncate block ${isSelected ? 'font-semibold text-slate-900 dark:text-slate-100' : 'text-slate-700 dark:text-slate-300'}`}>
                            {session.title}
                          </span>
                        </div>
                        <div className="flex items-center space-x-1.5 mt-0.5">
                          <span className="text-[10px] text-slate-400 dark:text-slate-500 font-medium">
                            {session.category || 'General'}
                          </span>
                          <span className="text-slate-300 dark:text-slate-700 text-[9px]">&bull;</span>
                          <span className="text-[10px] text-slate-400 dark:text-slate-500 font-medium">
                            {formatRelativeTime(session.updatedAt || session.createdAt)}
                          </span>
                        </div>
                        {session.tags && session.tags.length > 0 && (
                          <div className="flex flex-wrap gap-1 mt-1">
                            {session.tags.map((tag, idx) => (
                              <span key={idx} className="inline-flex items-center px-1.5 py-0.2 rounded text-[9px] font-semibold bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400 border border-slate-200/20">
                                #{tag}
                              </span>
                            ))}
                          </div>
                        )}
                      </div>
                    )}
                  </div>

                  {/* Inline Action Controls */}
                  {!isEditing && (
                    <div className="flex items-center space-x-0.5 opacity-0 group-hover:opacity-100 transition-opacity ml-2 mt-0.5 shrink-0">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setSessions((prev) =>
                            prev.map((s) => (s.id === session.id ? { ...s, pinned: !s.pinned } : s))
                          );
                        }}
                        title={session.pinned ? 'Unpin Session' : 'Pin Session'}
                        className={`p-1 rounded-md transition-colors ${
                          session.pinned ? 'text-amber-500' : 'text-slate-400 hover:text-amber-500 hover:bg-slate-100 dark:hover:bg-slate-800'
                        }`}
                      >
                        <Pin className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={(e) => handleStartRename(session, e)}
                        title="Rename Session"
                        className="p-1 text-slate-400 hover:text-blue-500 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-md transition-colors"
                      >
                        <Pencil className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={(e) => handleDeleteSession(session.id, e)}
                        title="Delete Session"
                        className="p-1 text-slate-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-950/20 rounded-md transition-colors"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
      </div>

      {/* Footer copyright */}
      <div className="p-3 border-t border-slate-200/50 dark:border-slate-800/65 bg-slate-100/50 dark:bg-slate-900/50 text-[10px] text-slate-400 text-center font-semibold">
        AI Studio &bull; Developed with Pride
      </div>
    </div>
  );

  return (
    <div className="flex h-screen w-screen overflow-hidden bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 transition-colors font-sans antialiased">
      
      {/* 1. Desktop Permanent Left Sidebar (collapsible with transition animation) */}
      <aside 
        className={`hidden lg:block shrink-0 h-full transition-all duration-300 ease-in-out border-r border-slate-200/80 dark:border-slate-800 overflow-hidden ${
          isSidebarCollapsed ? 'w-0 border-r-0' : 'w-72'
        }`}
      >
        <div className="w-72 h-full">
          {renderSidebarContent()}
        </div>
      </aside>

      {/* 2. Responsive mobile Drawer layout */}
      {isMobileDrawerOpen && (
        <div className="fixed inset-0 z-50 flex lg:hidden">
          {/* Backdrop blur clickoff */}
          <div
            className="fixed inset-0 bg-slate-950/45 backdrop-blur-xs transition-opacity"
            onClick={() => setIsMobileDrawerOpen(false)}
          />

          {/* Slide-out drawer wrapper */}
          <div className="relative w-76 max-w-[85vw] bg-white dark:bg-slate-900 h-full shadow-2xl z-10 flex flex-col border-r border-slate-200/80 dark:border-slate-800">
            {renderSidebarContent()}
          </div>
        </div>
      )}

      {/* 3. Main Workspace Container (Header, Chat Area, Input Box) */}
      <div className="flex-1 flex flex-col h-full overflow-hidden min-w-0 relative">
        
        {/* Toggle Collapse Sidebar button floating in top left corner (only on desktop when collapsed) */}
        {isSidebarCollapsed && (
          <button
            onClick={() => setIsSidebarCollapsed(false)}
            title="Expand Sidebar"
            className="hidden lg:flex fixed left-3 top-3.5 z-30 p-1.5 bg-white dark:bg-slate-900 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-500 dark:text-slate-300 border border-slate-200 dark:border-slate-800 rounded-lg shadow-sm transition-all flex items-center justify-center hover:scale-105 active:scale-95"
          >
            <ChevronRight className="w-4 h-4 stroke-[2.5px]" />
          </button>
        )}

        {/* Top Header App Bar */}
        <Header
          onClearChat={handleClearChat}
          messageCount={messages.length}
          isDarkMode={isDarkMode}
          onToggleDarkMode={toggleDarkMode}
          onExport={handleExportTranscript}
          onOpenDrawer={() => {
            // Mobile opens overlay drawer, desktop toggles sidebar collapse!
            if (window.innerWidth < 1024) {
              setIsMobileDrawerOpen(true);
            } else {
              setIsSidebarCollapsed(!isSidebarCollapsed);
            }
          }}
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
          onOpenTemplates={() => setIsTemplatesOpen(true)}
          autoScroll={autoScroll}
          onToggleAutoScroll={() => setAutoScroll(!autoScroll)}
          // Active session metadata passed directly into Header
          sessionTitle={currentSession?.title}
          sessionCategory={currentSession?.category}
          sessionPinned={currentSession?.pinned}
          onTogglePin={() => {
            setSessions((prev) =>
              prev.map((s) => (s.id === currentSessionId ? { ...s, pinned: !s.pinned } : s))
            );
          }}
        />

        {/* Central Chat Output Scrollable Pane */}
        <main
          id="outputScrollView"
          onScroll={handleScroll}
          className="flex-1 overflow-y-auto px-4 py-4 sm:py-6 relative scrollbar-thin"
        >
          <div className="max-w-3xl mx-auto space-y-4">
            
            {/* Filtering message notice banner */}
            {searchQuery.trim() && (
              <div className="bg-blue-50 dark:bg-blue-950/30 border border-blue-200/50 dark:border-blue-900/35 text-blue-700 dark:text-blue-300 px-4 py-2.5 rounded-xl text-xs flex items-center justify-between mb-2 shadow-3xs">
                <span className="font-semibold">Showing matches for "{searchQuery}" ({filteredMessages.length} found)</span>
                <button
                  onClick={() => setSearchQuery('')}
                  className="font-bold underline hover:text-blue-800 dark:hover:text-blue-200 cursor-pointer"
                >
                  Reset filter
                </button>
              </div>
            )}

            {/* Premium Empty State Workspace (Bento Grid Style) */}
            {messages.length === 0 && !isLoading && !searchQuery.trim() && (
              <div className="py-8 sm:py-12 max-w-2xl mx-auto text-center">
                <div className="w-14 h-14 bg-gradient-to-tr from-blue-500 to-indigo-600 text-white rounded-2xl flex items-center justify-center mx-auto mb-5 shadow-md">
                  <Bot className="w-7 h-7 stroke-[2px]" />
                </div>
                <h2 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-white tracking-tight">
                  Welcome to AI Chat Studio
                </h2>
                <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1.5 mb-8 max-w-md mx-auto leading-relaxed">
                  Analyze multimodal images with offline OCR text transcription, save prompt templates, and ask anything.
                </p>

                {/* Structured Bento prompt cards by category */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5 text-left">
                  
                  {/* Category 1: Technical */}
                  <div className="p-4 bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl shadow-3xs flex flex-col justify-between">
                    <div>
                      <div className="flex items-center space-x-2 text-blue-600 dark:text-blue-400 mb-2">
                        <Code className="w-4 h-4" />
                        <span className="text-[11px] font-bold uppercase tracking-wider">Coding & Design</span>
                      </div>
                      <p className="text-xs text-slate-500 dark:text-slate-400 mb-3.5">
                        Ask coding questions, refactor functions, or generate React boilerplate.
                      </p>
                    </div>
                    <div className="space-y-1.5">
                      {[
                        'Write a custom React Hook for debounce',
                        'Refactor this function for performance',
                      ].map((promptText) => (
                        <button
                          key={promptText}
                          onClick={() => handleSendMessage(promptText)}
                          className="w-full text-left px-3 py-2 bg-slate-50 hover:bg-slate-100 dark:bg-slate-950 dark:hover:bg-slate-800 rounded-xl text-[11px] font-semibold text-slate-700 dark:text-slate-300 border border-slate-200/40 dark:border-slate-800 transition-all hover:scale-[1.01] active:scale-99 block truncate"
                          title={promptText}
                        >
                          "{promptText}"
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Category 2: Content & Writing */}
                  <div className="p-4 bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl shadow-3xs flex flex-col justify-between">
                    <div>
                      <div className="flex items-center space-x-2 text-emerald-600 dark:text-emerald-400 mb-2">
                        <PencilLine className="w-4 h-4" />
                        <span className="text-[11px] font-bold uppercase tracking-wider">Writing & Analytics</span>
                      </div>
                      <p className="text-xs text-slate-500 dark:text-slate-400 mb-3.5">
                        Draft summaries, translate text paragraphs, or explore metaphors.
                      </p>
                    </div>
                    <div className="space-y-1.5">
                      {[
                        'Summarize a text in concise bullet points',
                        'Explain quantum mechanics to a 5 year old',
                      ].map((promptText) => (
                        <button
                          key={promptText}
                          onClick={() => handleSendMessage(promptText)}
                          className="w-full text-left px-3 py-2 bg-slate-50 hover:bg-slate-100 dark:bg-slate-950 dark:hover:bg-slate-800 rounded-xl text-[11px] font-semibold text-slate-700 dark:text-slate-300 border border-slate-200/40 dark:border-slate-800 transition-all hover:scale-[1.01] active:scale-99 block truncate"
                          title={promptText}
                        >
                          "{promptText}"
                        </button>
                      ))}
                    </div>
                  </div>

                </div>

                {/* OCR Instruction note */}
                <div className="mt-8 p-3 bg-blue-50/50 dark:bg-blue-950/20 border border-blue-200/30 dark:border-blue-900/30 rounded-2xl max-w-md mx-auto text-center flex items-center justify-center space-x-2">
                  <div className="w-2.5 h-2.5 rounded-full bg-blue-500 animate-pulse shrink-0" />
                  <span className="text-[11px] text-slate-500 dark:text-slate-400 font-medium">
                    Pro-tip: Drop or select any image to automatically transcribe code/text.
                  </span>
                </div>
              </div>
            )}

            {/* List of active message components */}
            <div id="outputContainer" className="space-y-1">
              {filteredMessages.map((message) => (
                <MessageItem
                  key={message.id}
                  message={message}
                  isDarkMode={isDarkMode}
                  onReaction={handleReaction}
                  onDelete={handleDeleteMessageItem}
                  onRetry={handleRetryMessage}
                  onSuggestionClick={handleSendMessage}
                />
              ))}
            </div>


            <div ref={messagesEndRef} />
          </div>
        </main>

        {/* Floating Back to Top Button */}
        {showBackToTop && (
          <button
            onClick={() => {
              const scrollView = document.getElementById('outputScrollView');
              if (scrollView) {
                scrollView.scrollTo({ top: 0, behavior: 'smooth' });
              }
            }}
            className="fixed bottom-24 right-6 z-30 p-2.5 bg-slate-900 dark:bg-slate-100 hover:bg-slate-800 dark:hover:bg-white text-white dark:text-slate-900 rounded-full shadow-lg transition-all flex items-center justify-center hover:scale-108 active:scale-95"
            title="Scroll to top"
          >
            <ArrowUp className="w-4.5 h-4.5 stroke-[2.5px]" />
          </button>
        )}

        {/* Modal: Prompt Templates Library Modal */}
        {isTemplatesOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div className="fixed inset-0 bg-slate-950/60 backdrop-blur-xs transition-opacity" onClick={() => setIsTemplatesOpen(false)} />
            <div className="relative bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 w-full max-w-lg overflow-hidden z-10 flex flex-col max-h-[85vh] animate-in fade-in zoom-in-95 duration-150">
              <div className="p-4 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
                <div className="flex items-center space-x-2.5">
                  <div className="w-8 h-8 rounded-lg bg-blue-50 dark:bg-blue-950 text-blue-600 dark:text-blue-400 flex items-center justify-center shadow-3xs">
                    <BookOpen className="w-4.5 h-4.5" />
                  </div>
                  <div>
                    <h3 className="font-bold text-xs text-slate-900 dark:text-white">Prompt Snippets Library</h3>
                    <p className="text-[10px] text-slate-400 dark:text-slate-500 font-semibold">Store and inject templates into active drafts</p>
                  </div>
                </div>
                <button
                  onClick={() => setIsTemplatesOpen(false)}
                  className="p-1.5 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-full hover:bg-slate-100 dark:hover:bg-slate-800"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <div className="flex-1 overflow-y-auto p-4 space-y-3.5 scrollbar-thin">
                {!showAddTemplateForm ? (
                  <button
                    onClick={() => setShowAddTemplateForm(true)}
                    className="w-full py-2.5 px-3 border border-dashed border-slate-300 dark:border-slate-800 hover:border-blue-500 dark:hover:border-blue-500 hover:bg-blue-50/20 text-blue-600 dark:text-blue-400 rounded-xl text-xs font-semibold flex items-center justify-center space-x-1.5 transition-all"
                  >
                    <Plus className="w-4 h-4 stroke-[2.5px]" />
                    <span>Create New Prompt Snippet</span>
                  </button>
                ) : (
                  <div className="p-4 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl space-y-3 shadow-3xs">
                    <h4 className="text-xs font-bold text-slate-800 dark:text-slate-200 uppercase tracking-wider">New Prompt Snippet</h4>
                    <input
                      type="text"
                      placeholder="Snippet Title (e.g. Code Review)"
                      value={newTemplateTitle}
                      onChange={(e) => setNewTemplateTitle(e.target.value)}
                      className="w-full bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-lg px-3 py-1.5 text-xs outline-none focus:border-blue-500 font-semibold text-slate-800 dark:text-slate-100"
                    />
                    <textarea
                      rows={3}
                      placeholder="Type prompt text block snippet..."
                      value={newTemplatePrompt}
                      onChange={(e) => setNewTemplatePrompt(e.target.value)}
                      className="w-full bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-lg px-3 py-1.5 text-xs outline-none focus:border-blue-500 resize-none text-slate-800 dark:text-slate-100"
                    />
                    <div className="flex justify-end space-x-2 pt-1">
                      <button
                        onClick={() => setShowAddTemplateForm(false)}
                        className="px-3 py-1 text-xs font-bold text-slate-500 dark:text-slate-400 hover:bg-slate-200/50 dark:hover:bg-slate-800 rounded-lg transition-colors"
                      >
                        Cancel
                      </button>
                      <button
                        onClick={() => {
                          if (newTemplateTitle.trim() && newTemplatePrompt.trim()) {
                            setPromptTemplates((prev) => [
                              ...prev,
                              { id: `t-${Date.now()}`, title: newTemplateTitle.trim(), prompt: newTemplatePrompt.trim() },
                            ]);
                            setNewTemplateTitle('');
                            setNewTemplatePrompt('');
                            setShowAddTemplateForm(false);
                          }
                        }}
                        className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-semibold shadow-3xs"
                      >
                        Save Snippet
                      </button>
                    </div>
                  </div>
                )}

                <div className="space-y-3 pt-1">
                  {promptTemplates.map((template) => {
                    const isEditingThis = editingTemplateId === template.id;
                    return (
                      <div
                        key={template.id}
                        className="p-3 bg-slate-50 dark:bg-slate-950/40 border border-slate-200/60 dark:border-slate-800/60 rounded-xl space-y-2.5"
                      >
                        {isEditingThis ? (
                          <div className="space-y-2">
                            <input
                              type="text"
                              value={editTitleVal}
                              onChange={(e) => setEditTitleVal(e.target.value)}
                              className="w-full bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-lg px-2.5 py-1 text-xs outline-none font-semibold text-slate-800 dark:text-slate-100"
                            />
                            <textarea
                              rows={3}
                              value={editPromptVal}
                              onChange={(e) => setEditPromptVal(e.target.value)}
                              className="w-full bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-lg px-2.5 py-1 text-xs outline-none resize-none text-slate-800 dark:text-slate-100"
                            />
                            <div className="flex justify-end space-x-2 pt-1">
                              <button
                                onClick={() => setEditingTemplateId(null)}
                                className="px-2.5 py-1 text-xs font-bold text-slate-500 dark:text-slate-400"
                              >
                                Cancel
                              </button>
                              <button
                                onClick={() => {
                                  setPromptTemplates((prev) =>
                                    prev.map((t) => (t.id === template.id ? { ...t, title: editTitleVal, prompt: editPromptVal } : t))
                                  );
                                  setEditingTemplateId(null);
                                }}
                                className="px-2.5 py-1 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold"
                              >
                                Update Snippet
                              </button>
                            </div>
                          </div>
                        ) : (
                          <div>
                            <div className="flex items-center justify-between">
                              <h4 className="text-xs font-bold text-slate-900 dark:text-slate-100">{template.title}</h4>
                              <div className="flex items-center space-x-1.5">
                                <button
                                  onClick={() => {
                                    setInputMessageText((prev) => (prev ? prev + '\n' + template.prompt : template.prompt));
                                    setIsTemplatesOpen(false);
                                  }}
                                  className="px-2.5 py-1 bg-slate-900 hover:bg-slate-800 dark:bg-slate-100 dark:hover:bg-white text-white dark:text-slate-900 rounded-lg text-[10px] font-bold shadow-3xs transition-all"
                                >
                                  Insert
                                </button>
                                <button
                                  onClick={() => {
                                    setEditingTemplateId(template.id);
                                    setEditTitleVal(template.title);
                                    setEditPromptVal(template.prompt);
                                  }}
                                  className="p-1 text-slate-400 hover:text-blue-500 rounded hover:bg-slate-100 dark:hover:bg-slate-900"
                                  title="Edit Template"
                                >
                                  <Pencil className="w-3.5 h-3.5" />
                                </button>
                                <button
                                  onClick={() => {
                                    setPromptTemplates((prev) => prev.filter((t) => t.id !== template.id));
                                  }}
                                  className="p-1 text-slate-400 hover:text-red-500 rounded hover:bg-red-50 dark:hover:bg-red-950/10"
                                  title="Delete Template"
                                >
                                  <Trash2 className="w-3.5 h-3.5" />
                                </button>
                              </div>
                            </div>
                            <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-1.5 line-clamp-2 font-mono whitespace-pre-wrap leading-relaxed">
                              {template.prompt}
                            </p>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>

              <div className="p-3 border-t border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/50 text-center text-[10px] text-slate-400 font-semibold">
                Click 'Insert' to inject prompt templates directly into your active draft
              </div>
            </div>
          </div>
        )}

        {/* Bottom Chat Typing Area Component */}
        <ChatInput
          onSendMessage={(txt) => {
            handleSendMessage(txt);
            setInputMessageText('');
          }}
          onImageSelected={handleImageSelected}
          isLoading={isLoading}
          isOcrProcessing={isOcrProcessing}
          selectedImage={selectedImage}
          onRemoveSelectedImage={() => setSelectedImage(null)}
          text={inputMessageText}
          onTextChange={setInputMessageText}
        />
      </div>
    </div>
  );
};
