export interface ChatMessage {
  id: string;
  sender: 'You' | 'Ai' | 'App';
  message: string;
  timestamp: string;
  imageUri?: string;
  extractedText?: string;
  reaction?: 'thumbs-up' | 'thumbs-down';
}

export interface ChatSession {
  id: string;
  title: string;
  messages: ChatMessage[];
  createdAt: number;
  pinned?: boolean;
  category?: string;
}

export interface ApiChatResponse {
  text?: string;
  error?: string;
}

