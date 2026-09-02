export interface Attachment {
  uri: string;
  mimeType: string;
  name: string;
  extractedText?: string;
  /** IndexedDB key used to restore the attachment after a page reload. */
  storageKey?: string;
}

export interface ChatMessage {
  id: string;
  sender: 'You' | 'Ai' | 'App';
  message: string;
  timestamp: string;
  imageUri?: string; // keeping for backward compatibility
  extractedText?: string; // keeping for backward compatibility
  attachments?: Attachment[];
  reaction?: 'thumbs-up' | 'thumbs-down';
  suggestions?: string[];
}

export interface ChatSession {
  id: string;
  title: string;
  messages: ChatMessage[];
  createdAt: number;
  updatedAt?: number;
  pinned?: boolean;
  category?: string;
  tags?: string[];
}

export interface ApiChatResponse {
  text?: string;
  error?: string;
}

