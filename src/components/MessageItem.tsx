import React, { useState, useEffect, useRef } from 'react';
import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { motion, AnimatePresence } from 'motion/react';
import { AlertCircle, FileText, CheckCircle2, Clock, Copy, Check, ThumbsUp, ThumbsDown, RotateCcw, Trash2 } from 'lucide-react';
import { ChatMessage } from '../types';
import Prism from 'prismjs';
import 'prismjs/themes/prism-tomorrow.css';
import 'prismjs/components/prism-javascript';
import 'prismjs/components/prism-typescript';
import 'prismjs/components/prism-jsx';
import 'prismjs/components/prism-tsx';
import 'prismjs/components/prism-python';
import 'prismjs/components/prism-json';
import 'prismjs/components/prism-bash';
import 'prismjs/components/prism-css';
import 'prismjs/components/prism-markup';
import 'prismjs/components/prism-sql';

interface MessageItemProps {
  message: ChatMessage;
  isDarkMode?: boolean;
  onReaction?: (messageId: string, reaction: 'thumbs-up' | 'thumbs-down' | undefined) => void;
  onDelete?: (messageId: string) => void;
  onRetry?: (messageId: string) => void;
  onSuggestionClick?: (suggestion: string) => void;
}

export const MessageItem: React.FC<MessageItemProps> = ({ message, onReaction, onDelete, onRetry, onSuggestionClick }) => {
  const [showFullTime, setShowFullTime] = useState(false);
  const [copied, setCopied] = useState(false);
  const [contextMenu, setContextMenu] = useState<{ x: number, y: number } | null>(null);
  const contextMenuRef = useRef<HTMLDivElement>(null);
  
  const isUser = message.sender === 'You';
  const isApp = message.sender === 'App';

  const fullTimestamp = `${new Date().toLocaleDateString()} at ${message.timestamp}`;

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (contextMenuRef.current && !contextMenuRef.current.contains(e.target as Node)) {
        setContextMenu(null);
      }
    };
    if (contextMenu) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [contextMenu]);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(message.message);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
      setContextMenu(null);
    } catch (err) {
      console.error('Failed to copy text: ', err);
    }
  };

  const handleContextMenu = (e: React.MouseEvent) => {
    e.preventDefault();
    let x = e.clientX;
    let y = e.clientY;
    
    // adjust for screen edge
    if (window.innerWidth - x < 200) x = x - 200;
    if (window.innerHeight - y < 150) y = y - 150;
    
    setContextMenu({ x, y });
  };

  const handleThumbsUp = () => {
    if (onReaction) {
      const newReaction = message.reaction === 'thumbs-up' ? undefined : 'thumbs-up';
      onReaction(message.id, newReaction);
    }
  };

  const handleThumbsDown = () => {
    if (onReaction) {
      const newReaction = message.reaction === 'thumbs-down' ? undefined : 'thumbs-down';
      onReaction(message.id, newReaction);
    }
  };

  if (isApp) {
    return (
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex justify-center my-2 px-4"
      >
        <div className="flex items-center space-x-2 bg-red-50 dark:bg-red-950/50 border border-red-200 dark:border-red-900 text-red-700 dark:text-red-300 px-3.5 py-2 rounded-xl text-xs max-w-md shadow-xs">
          <AlertCircle className="w-4 h-4 shrink-0 text-red-500" />
          <span className="font-medium">{message.message}</span>
        </div>
      </motion.div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 10, scale: 0.98 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      transition={{ duration: 0.2 }}
      className={`flex flex-col my-2 group ${isUser ? 'items-end' : 'items-start'}`}
    >
      <div 
        className="relative max-w-[85%] sm:max-w-[75%]"
        onContextMenu={handleContextMenu}
      >
        <div
          className={`rounded-[20px] px-4 py-3 shadow-xs ${
            isUser
              ? 'bg-[#0084FF] text-white rounded-br-xs'
              : 'bg-[#EEEEEE] dark:bg-gray-800 text-gray-900 dark:text-gray-100 rounded-bl-xs'
          }`}
        >
          {/* Attached Image preview if any */}
          {message.imageUri && (
            <div className="mb-2 rounded-lg overflow-hidden border border-black/10 bg-black/5">
              <img
                src={message.imageUri}
                alt="Attached content"
                className="max-h-60 w-auto object-contain rounded-lg"
                referrerPolicy="no-referrer"
              />
              {message.extractedText && (
                <div className="bg-black/20 p-2 text-[11px] flex items-center space-x-1.5 mt-1 rounded-sm">
                  <FileText className="w-3.5 h-3.5 shrink-0 opacity-80" />
                  <span className="truncate opacity-90">OCR: {message.extractedText}</span>
                </div>
              )}
            </div>
          )}

          {/* Message body */}
          {isUser ? (
            <div className="text-sm whitespace-pre-wrap break-words leading-relaxed">
              {message.message}
            </div>
          ) : (
            <div className="markdown-body dark:text-gray-100">
              <Markdown
                remarkPlugins={[remarkGfm]}
                components={{
                  code({ node, inline, className, children, ...props }: any) {
                    const match = /language-(\w+)/.exec(className || '');
                    if (!inline && match) {
                      const lang = match[1];
                      const codeContent = String(children).replace(/\n$/, '');
                      let highlighted = codeContent;
                      try {
                        if (Prism.languages[lang]) {
                          highlighted = Prism.highlight(codeContent, Prism.languages[lang], lang);
                        }
                      } catch (e) {
                        console.error('Prism highlighting error:', e);
                      }

                      return (
                        <div className="relative group/code my-2.5 rounded-xl overflow-hidden border border-gray-700/60 bg-gray-900 text-xs font-mono shadow-md">
                          <div className="flex items-center justify-between px-3.5 py-1.5 bg-gray-800 text-gray-400 text-[11px] border-b border-gray-700">
                            <span className="uppercase font-semibold tracking-wider text-[10px] text-cyan-400">
                              {lang}
                            </span>
                            <button
                              onClick={() => navigator.clipboard.writeText(codeContent)}
                              className="hover:text-white flex items-center space-x-1 transition-colors bg-gray-700/50 hover:bg-gray-700 px-2 py-0.5 rounded text-[10px]"
                              title="Copy code"
                            >
                              <Copy className="w-3 h-3" />
                              <span>Copy</span>
                            </button>
                          </div>
                          <pre className="p-3.5 overflow-x-auto m-0 bg-transparent text-gray-100">
                            <code
                              className={className}
                              dangerouslySetInnerHTML={{ __html: highlighted }}
                              {...props}
                            />
                          </pre>
                        </div>
                      );
                    }
                    return (
                      <code className={className} {...props}>
                        {children}
                      </code>
                    );
                  },
                }}
              >
                {message.message}
              </Markdown>
            </div>
          )}
        </div>

        {/* Action buttons for AI messages (Copy & Reactions) */}
        {!isUser && (
          <div className="absolute -right-16 top-1 flex flex-col space-y-1 opacity-0 group-hover:opacity-100 transition-opacity">
            <button
              onClick={handleCopy}
              title={copied ? 'Copied!' : 'Copy to clipboard'}
              className="p-1.5 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 text-gray-500 dark:text-gray-400 hover:text-blue-600 dark:hover:text-blue-400 rounded-full shadow-xs flex items-center justify-center"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
            </button>
            <button
              onClick={handleThumbsUp}
              title="Good response"
              className={`p-1.5 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-full shadow-xs flex items-center justify-center transition-colors ${
                message.reaction === 'thumbs-up'
                  ? 'text-blue-600 dark:text-blue-400 border-blue-400'
                  : 'text-gray-500 dark:text-gray-400 hover:text-blue-600 dark:hover:text-blue-400'
              }`}
            >
              <ThumbsUp className="w-3.5 h-3.5" />
            </button>
            <button
              onClick={handleThumbsDown}
              title="Poor response"
              className={`p-1.5 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-full shadow-xs flex items-center justify-center transition-colors ${
                message.reaction === 'thumbs-down'
                  ? 'text-red-600 dark:text-red-400 border-red-400'
                  : 'text-gray-500 dark:text-gray-400 hover:text-red-600 dark:hover:text-red-400'
              }`}
            >
              <ThumbsDown className="w-3.5 h-3.5" />
            </button>
          </div>
        )}
      </div>

      <div
        onClick={() => setShowFullTime(!showFullTime)}
        title="Click to toggle full date/time stamp"
        className={`text-[10px] text-gray-400 dark:text-gray-500 mt-1 px-2 flex items-center space-x-1 cursor-pointer hover:text-gray-600 dark:hover:text-gray-300 transition-colors select-none ${
          isUser ? 'justify-end' : 'justify-start'
        }`}
      >
        <span>{message.sender}</span>
        <span>•</span>
        <span className="flex items-center space-x-0.5">
          <Clock className="w-2.5 h-2.5 mr-0.5 opacity-70 inline" />
          {showFullTime ? fullTimestamp : message.timestamp}
        </span>
        {isUser && <CheckCircle2 className="w-2.5 h-2.5 text-blue-500" />}
        {message.reaction === 'thumbs-up' && <ThumbsUp className="w-2.5 h-2.5 text-blue-500 ml-1 inline" />}
        {message.reaction === 'thumbs-down' && <ThumbsDown className="w-2.5 h-2.5 text-red-500 ml-1 inline" />}
      </div>

      <AnimatePresence>
        {contextMenu && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            transition={{ duration: 0.1 }}
            ref={contextMenuRef}
            className="fixed z-[100] w-40 bg-white dark:bg-gray-800 rounded-lg shadow-xl border border-gray-200 dark:border-gray-700 py-1 overflow-hidden"
            style={{ top: contextMenu.y, left: contextMenu.x }}
          >
            <button
              onClick={handleCopy}
              className="w-full text-left px-3 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center space-x-2 transition-colors"
            >
              {copied ? <Check className="w-4 h-4 text-emerald-600" /> : <Copy className="w-4 h-4" />}
              <span>{copied ? 'Copied' : 'Copy'}</span>
            </button>
            {onRetry && (
              <button
                onClick={() => {
                  onRetry(message.id);
                  setContextMenu(null);
                }}
                className="w-full text-left px-3 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center space-x-2 transition-colors"
              >
                <RotateCcw className="w-4 h-4" />
                <span>Retry</span>
              </button>
            )}
            {onDelete && (
              <button
                onClick={() => {
                  onDelete(message.id);
                  setContextMenu(null);
                }}
                className="w-full text-left px-3 py-2 text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 flex items-center space-x-2 transition-colors"
              >
                <Trash2 className="w-4 h-4" />
                <span>Delete</span>
              </button>
            )}
          </motion.div>
        )}
      </AnimatePresence>

      {message.suggestions && message.suggestions.length > 0 && (
        <motion.div 
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex flex-wrap gap-2 mt-3 w-full"
        >
          {message.suggestions.map((suggestion, idx) => (
            <button
              key={idx}
              onClick={() => onSuggestionClick?.(suggestion)}
              className="px-3 py-1.5 text-xs font-medium text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/20 hover:bg-blue-100 dark:hover:bg-blue-900/40 rounded-full border border-blue-200 dark:border-blue-800 transition-colors"
            >
              {suggestion}
            </button>
          ))}
        </motion.div>
      )}
    </motion.div>
  );
};
