import React, { useRef, useState, useEffect } from 'react';
import { Paperclip, ArrowUp, Loader2, X, Image as ImageIcon, Mic, MicOff, Eye, Keyboard, Camera, FileIcon } from 'lucide-react';
import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

export interface AttachmentState {
  id: string;
  file: File;
  previewUrl: string;
  ocrText?: string;
  isProcessing?: boolean;
}

interface ChatInputProps {
  onSendMessage: (text: string) => void;
  onFilesSelected: (files: FileList | File[]) => void;
  isLoading: boolean;
  attachments: AttachmentState[];
  onRemoveAttachment: (id: string) => void;
  text?: string;
  onTextChange?: (text: string) => void;
}

export const ChatInput: React.FC<ChatInputProps> = ({
  onSendMessage,
  onFilesSelected,
  isLoading,
  attachments,
  onRemoveAttachment,
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

  const cameraInputRef = useRef<HTMLInputElement>(null);

  const isOcrProcessing = attachments.some(a => a.isProcessing);

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
        // Allow continuous listening so the user can speak naturally with pauses
        recognition.continuous = true; 
        recognition.interimResults = true;
        recognition.lang = 'en-US';

        const startText = text.trim();
        const prefix = startText.length > 0 ? startText + ' ' : '';

        recognition.onstart = () => {
          setIsListening(true);
        };

        recognition.onresult = (event: any) => {
          let transcript = '';
          for (let i = 0; i < event.results.length; i++) {
            transcript += event.results[i][0].transcript;
          }
          setText(prefix + transcript);
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
    if (!trimmed && attachments.length === 0) return;

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
    if (e.target.files && e.target.files.length > 0) {
      onFilesSelected(Array.from(e.target.files));
    }
    // reset input
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
    if (cameraInputRef.current) {
      cameraInputRef.current.value = '';
    }
  };

  // Estimate tokens cleanly
  const wordsCount = text.trim() ? text.trim().split(/\s+/).filter(Boolean).length : 0;
  const tokenCount = Math.max(0, Math.ceil((wordsCount * 1.3) + (attachments.length * 220)));

  return (
    <div className="sticky bottom-0 bg-slate-50/95 dark:bg-slate-950/95 backdrop-blur-md border-t border-slate-200/50 dark:border-slate-900/80 p-3 sm:p-4 transition-colors">
      <div className="max-w-3xl mx-auto">
        <input
          ref={fileInputRef}
          type="file"
          multiple
          className="hidden"
          id="file-input"
          onChange={handleFileChange}
        />
        <input
          ref={cameraInputRef}
          type="file"
          accept="image/*"
          capture="environment"
          className="hidden"
          id="camera-input"
          onChange={handleFileChange}
        />

        {/* Input Dock Card */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800/80 focus-within:border-slate-300 dark:focus-within:border-slate-700/80 focus-within:ring-2 focus-within:ring-slate-100 dark:focus-within:ring-slate-950 rounded-2xl p-2.5 transition-all shadow-2xs">
          
          {/* Top Row: File Thumbnail attachments inline if any */}
          {attachments.length > 0 && (
            <div className="mb-2 flex flex-wrap gap-2">
              {attachments.map((attachment) => (
                <div key={attachment.id} className="p-1.5 bg-slate-50 dark:bg-slate-950 border border-slate-200/50 dark:border-slate-800 rounded-xl flex items-center justify-between shadow-3xs w-64 max-w-full relative group">
                  <div className="flex items-center space-x-2.5 overflow-hidden flex-1 pr-6">
                    <div className="relative w-10 h-10 rounded-lg overflow-hidden shrink-0 border border-slate-200 dark:border-slate-800 bg-slate-100 dark:bg-slate-900 flex items-center justify-center">
                      {attachment.file.type.startsWith('image/') ? (
                        <img
                          src={attachment.previewUrl}
                          alt="Upload preview"
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <FileIcon className="w-5 h-5 text-slate-400" />
                      )}
                    </div>
                    <div className="text-[11px] truncate w-full">
                      <p className="font-semibold text-slate-800 dark:text-slate-200 truncate flex items-center">
                        {attachment.file.type.startsWith('image/') ? (
                          <ImageIcon className="w-3 h-3 mr-1 text-blue-600 dark:text-blue-400 shrink-0" />
                        ) : (
                          <FileIcon className="w-3 h-3 mr-1 text-purple-600 dark:text-purple-400 shrink-0" />
                        )}
                        <span className="truncate">{attachment.file.name}</span>
                      </p>
                      <p className="text-slate-400 dark:text-slate-500 font-medium truncate">
                        {attachment.isProcessing ? (
                          <span className="text-blue-600 dark:text-blue-400 flex items-center">
                            <Loader2 className="w-2.5 h-2.5 animate-spin mr-1 inline" /> Processing...
                          </span>
                        ) : attachment.ocrText ? (
                          <span className="text-emerald-600 dark:text-emerald-400 font-medium flex items-center">
                            Text attached
                          </span>
                        ) : (
                          attachment.file.type.startsWith('image/') ? 'Image analysis ready' : 'File attached'
                        )}
                      </p>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => onRemoveAttachment(attachment.id)}
                    className="absolute right-1.5 top-1/2 -translate-y-1/2 p-1 text-slate-400 hover:text-red-500 hover:bg-slate-200/50 dark:hover:bg-slate-800 rounded-full transition-colors opacity-0 group-hover:opacity-100"
                    title="Remove file"
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                </div>
              ))}
            </div>
          )}

          {/* Middle Row: Editing / Preview Area */}
          <div className="w-full py-1">
            {isPreviewMode ? (
              <div className="w-full text-sm text-slate-800 dark:text-slate-100 min-h-[32px] max-h-36 overflow-y-auto markdown-body px-2.5">
                {text.trim() ? (
                  <Markdown remarkPlugins={[remarkGfm]}>{text}</Markdown>
                ) : (
                  <span className="text-slate-400 dark:text-slate-500 italic">No text entered to preview...</span>
                )}
              </div>
            ) : (
              <div className="relative">
                <textarea
                  ref={textareaRef}
                  id="inputEditText"
                  rows={1}
                  value={text}
                  onChange={handleTextChange}
                  onKeyDown={handleKeyDown}
                  placeholder={isListening ? 'Listening closely... Speak now' : 'Ask anything or paste text (Markdown supported)...'}
                  disabled={isLoading}
                  className="w-full bg-transparent border-0 resize-none outline-none text-sm text-slate-800 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 max-h-36 overflow-y-auto block px-2.5 py-1"
                />
                {isListening && (
                  <div className="absolute inset-y-0 right-3 flex items-center space-x-1">
                    <span className="w-1.5 h-3.5 bg-red-500 rounded-full animate-[pulse_0.8s_infinite]" />
                    <span className="w-1.5 h-5 bg-red-500 rounded-full animate-[pulse_0.8s_infinite_0.15s]" />
                    <span className="w-1.5 h-3.5 bg-red-500 rounded-full animate-[pulse_0.8s_infinite_0.3s]" />
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Bottom Controls Row */}
          <div className="flex items-center justify-between border-t border-slate-100 dark:border-slate-800/60 mt-1.5 pt-2 px-1">
            {/* Left Controls Group */}
            <div className="flex items-center space-x-1">
              {/* File selection */}
              <button
                id="uploadButton"
                type="button"
                title="Attach files"
                disabled={isLoading || isOcrProcessing}
                onClick={() => fileInputRef.current?.click()}
                className="p-1.5 rounded-lg text-slate-400 hover:text-slate-800 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors disabled:opacity-50 flex items-center justify-center"
              >
                {isOcrProcessing ? (
                  <Loader2 className="w-4 h-4 text-blue-600 animate-spin" />
                ) : (
                  <Paperclip className="w-4 h-4" />
                )}
              </button>

              {/* Camera selection */}
              <button
                id="cameraButton"
                type="button"
                title="Take a photo"
                disabled={isLoading || isOcrProcessing}
                onClick={() => cameraInputRef.current?.click()}
                className="p-1.5 rounded-lg text-slate-400 hover:text-slate-800 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors disabled:opacity-50 flex items-center justify-center"
              >
                <Camera className="w-4 h-4" />
              </button>

              {/* Speech transcription */}
              <button
                id="micButton"
                type="button"
                title={isListening ? 'Stop recording' : 'Dictate message'}
                disabled={isLoading || isOcrProcessing}
                onClick={toggleVoiceRecording}
                className={`p-1.5 rounded-lg transition-colors flex items-center justify-center ${
                  isListening
                    ? 'text-red-500 bg-red-50 dark:bg-red-950/40 border border-red-100/30'
                    : 'text-slate-400 hover:text-slate-800 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800'
                }`}
              >
                {isListening ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4" />}
              </button>

              {/* Markdown Toggle */}
              <button
                type="button"
                title={isPreviewMode ? 'Return to editor' : 'Preview parsed markdown'}
                onClick={() => setIsPreviewMode(!isPreviewMode)}
                className={`p-1.5 rounded-lg transition-all flex items-center justify-center ${
                  isPreviewMode
                    ? 'bg-blue-50 text-blue-600 dark:bg-blue-950/40 dark:text-blue-400 border border-blue-100/20'
                    : 'text-slate-400 hover:text-slate-800 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800'
                }`}
              >
                {isPreviewMode ? <Keyboard className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>

            {/* Right Controls Group */}
            <div className="flex items-center space-x-3">
              <span className="text-[10px] text-slate-400 dark:text-slate-500 font-mono hidden sm:inline">
                {tokenCount > 0 ? `~${tokenCount} tokens` : 'Shift+Enter for draft'}
              </span>

              {/* Send Button styled with dynamic background states */}
              <button
                id="sendButton"
                type="submit"
                title="Send instruction"
                disabled={isLoading || isOcrProcessing || (!text.trim() && attachments.length === 0)}
                onClick={handleSubmit}
                className="p-1.5 rounded-xl bg-slate-900 dark:bg-slate-100 hover:bg-slate-800 dark:hover:bg-white text-white dark:text-slate-900 transition-all disabled:opacity-25 disabled:cursor-not-allowed flex items-center justify-center shadow-xs hover:scale-102 active:scale-98"
              >
                {isLoading ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <ArrowUp className="w-4 h-4 stroke-[2.5px]" />
                )}
              </button>
            </div>
          </div>
        </div>

        {/* Tip indicator */}
        <div className="px-1 mt-1 flex items-center justify-between text-[10px] text-slate-400 dark:text-slate-500">
          <span>AI outputs can contain formatting. Preview renders GFM.</span>
          <span className="sm:hidden font-mono">{tokenCount > 0 && `~${tokenCount} t`}</span>
        </div>
      </div>
    </div>
  );
};
