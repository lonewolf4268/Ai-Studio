import React, { useRef, useState, useEffect } from 'react';
import { Paperclip, ArrowRightCircle, Loader2, X, Image as ImageIcon, Mic, MicOff, Eye, Edit3 } from 'lucide-react';
import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

interface ChatInputProps {
  onSendMessage: (text: string, image?: { base64: string; mimeType: string }) => void;
  onImageSelected: (file: File) => void;
  isLoading: boolean;
  isOcrProcessing: boolean;
  selectedImage: { file: File; previewUrl: string; ocrText?: string } | null;
  onRemoveSelectedImage: () => void;
  text?: string;
  onTextChange?: (text: string) => void;
}

export const ChatInput: React.FC<ChatInputProps> = ({
  onSendMessage,
  onImageSelected,
  isLoading,
  isOcrProcessing,
  selectedImage,
  onRemoveSelectedImage,
  text: externalText,
  onTextChange: externalOnTextChange,
}) => {
  const [internalText, setInternalText] = useState('');
  const text = externalText !== undefined ? externalText : internalText;
  const setText = (val: string | ((prev: string) => string)) => {
    const nextVal = typeof val === 'function' ? val(text) : val;
    if (externalOnTextChange) {
      externalOnTextChange(nextVal);
    }
    setInternalText(nextVal);
  };
  const [isListening, setIsListening] = useState(false);
  const [isPreviewMode, setIsPreviewMode] = useState(false);
  const recognitionRef = useRef<any>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    return () => {
      if (recognitionRef.current) {
        try {
          recognitionRef.current.stop();
        } catch (e) {}
      }
    };
  }, []);

  const toggleVoiceRecording = () => {
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) {
      alert('Speech recognition is not supported in this browser. Try Chrome or Edge.');
      return;
    }

    if (isListening) {
      if (recognitionRef.current) {
        recognitionRef.current.stop();
      }
      setIsListening(false);
    } else {
      try {
        const recognition = new SpeechRecognition();
        recognition.continuous = false;
        recognition.interimResults = true;
        recognition.lang = 'en-US';

        recognition.onstart = () => {
          setIsListening(true);
        };

        recognition.onresult = (event: any) => {
          let transcript = '';
          for (let i = event.resultIndex; i < event.results.length; i++) {
            transcript += event.results[i][0].transcript;
          }
          setText(transcript);
          if (textareaRef.current) {
            textareaRef.current.style.height = 'auto';
            textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 150)}px`;
          }
        };

        recognition.onerror = (event: any) => {
          console.warn('Speech recognition error:', event.error);
          setIsListening(false);
        };

        recognition.onend = () => {
          setIsListening(false);
        };

        recognitionRef.current = recognition;
        recognition.start();
      } catch (e) {
        console.error('Failed to start recognition:', e);
        setIsListening(false);
      }
    }
  };

  const handleSubmit = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (isLoading || isOcrProcessing) return;

    const trimmed = text.trim();
    if (!trimmed && !selectedImage) return;

    if (isListening && recognitionRef.current) {
      recognitionRef.current.stop();
      setIsListening(false);
    }

    onSendMessage(trimmed);
    setText('');
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  const handleTextChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setText(e.target.value);
    // Auto-expand textarea up to 150px
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 150)}px`;
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      onImageSelected(file);
    }
    // reset input
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <div className="sticky bottom-0 bg-white/95 dark:bg-gray-900/95 backdrop-blur-md border-t border-gray-200 dark:border-gray-800 p-3 sm:p-4 shadow-md transition-colors">
      <div className="max-w-3xl mx-auto">
        {/* Selected Image thumbnail preview banner */}
        {selectedImage && (
          <div className="mb-2.5 p-2 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl flex items-center justify-between shadow-xs">
            <div className="flex items-center space-x-3 overflow-hidden">
              <div className="relative w-12 h-12 rounded-lg overflow-hidden shrink-0 border border-gray-300 dark:border-gray-600 bg-gray-100 dark:bg-gray-700">
                <img
                  src={selectedImage.previewUrl}
                  alt="Upload preview"
                  className="w-full h-full object-cover"
                />
              </div>
              <div className="text-xs truncate">
                <p className="font-medium text-gray-800 dark:text-gray-200 truncate flex items-center">
                  <ImageIcon className="w-3.5 h-3.5 mr-1 text-blue-600 dark:text-blue-400" />
                  {selectedImage.file.name}
                </p>
                <p className="text-gray-500 dark:text-gray-400 text-[11px]">
                  {isOcrProcessing ? (
                    <span className="text-blue-600 dark:text-blue-400 flex items-center">
                      <Loader2 className="w-3 h-3 animate-spin mr-1 inline" /> Extracting text via OCR...
                    </span>
                  ) : selectedImage.ocrText ? (
                    <span className="text-emerald-700 dark:text-emerald-400">OCR text extracted</span>
                  ) : (
                    'Ready to send'
                  )}
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={onRemoveSelectedImage}
              className="p-1.5 text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 hover:bg-gray-200 dark:hover:bg-gray-700 rounded-full transition-colors"
              title="Remove image"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Input Bar matching Android Activity layout */}
        <form onSubmit={handleSubmit} className="flex items-end space-x-2">
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            className="hidden"
            id="image-file-input"
            onChange={handleFileChange}
          />

          {/* Text Input area or Preview Area */}
          <div className="flex-1 flex items-end bg-gray-50 dark:bg-gray-800 border border-gray-300 dark:border-gray-700 focus-within:border-blue-500 focus-within:ring-2 focus-within:ring-blue-100 dark:focus-within:ring-blue-950 rounded-2xl px-3 py-1.5 transition-all">
            
            <div className="flex-1 min-w-0 py-1">
              {isPreviewMode ? (
                <div className="w-full text-sm text-gray-900 dark:text-gray-100 min-h-[24px] max-h-36 overflow-y-auto markdown-body">
                  {text.trim() ? (
                    <Markdown remarkPlugins={[remarkGfm]}>{text}</Markdown>
                  ) : (
                    <span className="text-gray-400 italic">Nothing to preview yet...</span>
                  )}
                </div>
              ) : (
                <textarea
                  ref={textareaRef}
                  id="inputEditText"
                  rows={1}
                  value={text}
                  onChange={handleTextChange}
                  onKeyDown={handleKeyDown}
                  placeholder={isListening ? 'Listening... Speak now' : 'Enter text here (Markdown supported)'}
                  disabled={isLoading}
                  className="w-full bg-transparent border-0 resize-none outline-none text-sm text-gray-900 dark:text-gray-100 placeholder:text-gray-400 dark:placeholder:text-gray-500 max-h-36 overflow-y-auto block"
                />
              )}
            </div>

            {/* Action Icons inside the input area at the end */}
            <div className="flex items-center space-x-1 shrink-0 ml-1 mb-0.5">
              {/* Markdown Preview toggle button */}
              <button
                type="button"
                title={isPreviewMode ? 'Switch to edit mode' : 'Preview formatted markdown'}
                onClick={() => setIsPreviewMode(!isPreviewMode)}
                className={`p-1.5 rounded-full transition-colors flex items-center justify-center ${
                  isPreviewMode
                    ? 'bg-blue-100 text-blue-600 dark:bg-blue-900/50 dark:text-blue-400'
                    : 'text-gray-400 hover:text-gray-700 dark:hover:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-700'
                }`}
              >
                {isPreviewMode ? <Edit3 className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>

              {/* File attachment button */}
              <button
                id="uploadButton"
                type="button"
                title="Attach an image for OCR and AI analysis"
                disabled={isLoading || isOcrProcessing}
                onClick={() => fileInputRef.current?.click()}
                className="p-1.5 rounded-full text-gray-400 hover:text-gray-700 dark:hover:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center"
              >
                {isOcrProcessing ? (
                  <Loader2 className="w-4 h-4 text-blue-600 animate-spin" />
                ) : (
                  <Paperclip className="w-4 h-4" />
                )}
              </button>

              {/* Voice recording button */}
              <button
                id="micButton"
                type="button"
                title={isListening ? 'Stop listening' : 'Record voice input'}
                disabled={isLoading || isOcrProcessing}
                onClick={toggleVoiceRecording}
                className={`p-1.5 rounded-full transition-colors flex items-center justify-center ${
                  isListening
                    ? 'text-red-500 bg-red-50 dark:bg-red-900/20 animate-pulse'
                    : 'text-gray-400 hover:text-gray-700 dark:hover:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-700'
                }`}
              >
                {isListening ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4" />}
              </button>
            </div>
          </div>

          {/* Send button */}
          <button
            id="sendButton"
            type="submit"
            title="Send message"
            disabled={isLoading || isOcrProcessing || (!text.trim() && !selectedImage)}
            className="p-2.5 rounded-full bg-blue-600 hover:bg-blue-700 active:scale-95 text-white transition-all disabled:opacity-40 disabled:cursor-not-allowed shrink-0 flex items-center justify-center shadow-sm"
          >
            {isLoading ? (
              <Loader2 className="w-5 h-5 animate-spin" />
            ) : (
              <ArrowRightCircle className="w-5 h-5" />
            )}
          </button>
        </form>

        {/* Token count indicator */}
        <div className="flex items-center justify-between px-2 pt-1.5 text-[11px] text-gray-400 dark:text-gray-500">
          <span>{isListening ? 'Listening... Speak clearly' : 'Shift + Enter for new line'}</span>
          <span className="font-mono">
            ~{Math.max(0, Math.ceil((text.trim().split(/\s+/).filter(Boolean).length * 1.3) + (selectedImage ? 250 : 0)))} tokens
          </span>
        </div>
      </div>
    </div>
  );
};

