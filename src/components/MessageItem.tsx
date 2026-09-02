import React, { useState, useEffect, useRef } from 'react';
import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { motion, AnimatePresence } from 'motion/react';
import { AlertCircle, FileText, CheckCircle2, Copy, Check, ThumbsUp, ThumbsDown, RotateCcw, Trash2, Sparkles, User, RefreshCw } from 'lucide-react';
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

const CodeBlock = ({ lang, codeContent, className, props, highlighted }: any) => {
  const [copied, setCopied] = useState(false);

  const handleCopyCode = async () => {
    try {
      await navigator.clipboard.writeText(codeContent);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy code: ', err);
    }
  };

  return (
    <div className="relative group/code my-3 rounded-xl overflow-hidden border border-slate-200 dark:border-slate-800 bg-slate-900 text-slate-100 text-xs font-mono shadow-sm">
      <div className="flex items-center justify-between px-3.5 py-2 bg-slate-950 text-slate-400 text-[10px] border-b border-slate-800">
        <span className="uppercase font-bold tracking-wider text-cyan-400">
          {lang}
        </span>
        <button
          onClick={handleCopyCode}
          className="hover:text-white flex items-center space-x-1 transition-colors bg-slate-900 px-2 py-0.5 rounded border border-slate-800"
          title={copied ? "Copied!" : "Copy snippet"}
        >
          {copied ? <Check className="w-3 h-3 text-emerald-500" /> : <Copy className="w-3 h-3" />}
          <span className={copied ? "text-emerald-500" : ""}>{copied ? "Copied!" : "Copy"}</span>
        </button>
      </div>
      <pre className="p-4 overflow-x-auto m-0 bg-transparent text-slate-200 leading-relaxed">
        <code
          className={className}
          dangerouslySetInnerHTML={{ __html: highlighted }}
          {...props}
        />
      </pre>
    </div>
  );
};

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
        initial={{ opacity: 0, y: 6 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex justify-center my-3 px-4"
      >
        <div className="flex items-center space-x-2.5 bg-red-50 dark:bg-red-950/20 border border-red-200/65 dark:border-red-900/30 text-red-700 dark:text-red-400 px-4 py-2.5 rounded-xl text-xs max-w-lg shadow-2xs">
          <AlertCircle className="w-4 h-4 shrink-0 text-red-500 dark:text-red-400" />
          <span className="font-semibold leading-relaxed">{message.message}</span>
        </div>
      </motion.div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 12, scale: 0.99 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      transition={{ duration: 0.22, ease: [0.16, 1, 0.3, 1] }}
      className={`flex w-full my-4 group ${isUser ? 'justify-end' : 'justify-start'}`}
    >
      <div className={`flex items-start max-w-[90%] sm:max-w-[82%] space-x-3.5 ${isUser ? 'flex-row-reverse space-x-reverse' : 'flex-row'}`}>
        
        {/* Identity Avatar Icon */}
        <div className="shrink-0 pt-0.5">
          {isUser ? (
            <div className="w-8 h-8 rounded-full bg-slate-100 dark:bg-slate-800 border border-slate-200/50 dark:border-slate-700 flex items-center justify-center text-slate-600 dark:text-slate-300 shadow-3xs">
              <User className="w-4.5 h-4.5" />
            </div>
          ) : (
            <div className="w-8 h-8 rounded-full bg-blue-50 dark:bg-blue-950/50 border border-blue-100 dark:border-blue-900/35 flex items-center justify-center text-blue-600 dark:text-blue-400 shadow-3xs">
              <Sparkles className="w-4 h-4" />
            </div>
          )}
        </div>

        {/* Message Container Area */}
        <div className="flex-1 min-w-0">
          
          {/* Metadata Row above Bubble */}
          <div className={`flex items-center space-x-2 mb-1 text-[11px] text-slate-400 dark:text-slate-500 font-medium ${isUser ? 'justify-end' : 'justify-start'}`}>
            <span>{isUser ? 'You' : 'Assistant'}</span>
            <span>&bull;</span>
            <span 
              onClick={() => setShowFullTime(!showFullTime)} 
              className="hover:text-slate-600 dark:hover:text-slate-300 transition-colors cursor-pointer select-none"
              title="Toggle full details"
            >
              {showFullTime ? fullTimestamp : message.timestamp}
            </span>
            {isUser && <CheckCircle2 className="w-3 h-3 text-blue-500 dark:text-blue-400 inline" />}
          </div>

          {/* Actual bubble or flat layout */}
          <div 
            onContextMenu={handleContextMenu}
            className={`transition-all ${
              isUser
                ? 'bg-slate-900 text-slate-100 dark:bg-slate-800 dark:text-slate-100 rounded-2xl rounded-tr-3xs px-4 py-3 shadow-2xs'
                : 'bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800/80 rounded-2xl rounded-tl-3xs px-5 py-4 shadow-3xs text-slate-800 dark:text-slate-100'
            }`}
          >
            {/* File attachments inside message if any */}
            {(message.attachments && message.attachments.length > 0) ? (
              <div className="mb-3.5 flex flex-wrap gap-2">
                {message.attachments.map((att, idx) => (
                  <div key={idx} className="rounded-xl overflow-hidden border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950/40 p-1">
                    {att.mimeType.startsWith('image/') ? (
                      <img
                        src={att.uri}
                        alt="Attached file"
                        className="max-h-60 w-auto object-contain rounded-lg"
                        referrerPolicy="no-referrer"
                      />
                    ) : (
                      <div className="px-3 py-4 flex flex-col items-center justify-center text-slate-500 min-w-[120px]">
                        <FileText className="w-8 h-8 mb-2 opacity-50" />
                        <span className="text-[11px] font-medium max-w-[100px] truncate">{att.name}</span>
                      </div>
                    )}
                    {att.extractedText && (
                      <div className="mt-1.5 bg-slate-100 dark:bg-slate-900/80 px-2.5 py-1.5 text-[11px] text-slate-600 dark:text-slate-400 rounded-lg border border-slate-200/50 dark:border-slate-800/40 flex items-center space-x-1.5">
                        <FileText className="w-3.5 h-3.5 shrink-0 text-slate-400" />
                        <span className="truncate font-mono"><strong className="font-semibold text-slate-700 dark:text-slate-300">Text Extracted</strong></span>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ) : message.imageUri && (
              <div className="mb-3.5 rounded-xl overflow-hidden border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950/40 p-1">
                <img
                  src={message.imageUri}
                  alt="Attached OCR upload"
                  className="max-h-60 w-auto object-contain rounded-lg"
                  referrerPolicy="no-referrer"
                />
                {message.extractedText && (
                  <div className="mt-1.5 bg-slate-100 dark:bg-slate-900/80 px-2.5 py-1.5 text-[11px] text-slate-600 dark:text-slate-400 rounded-lg border border-slate-200/50 dark:border-slate-800/40 flex items-center space-x-1.5">
                    <FileText className="w-3.5 h-3.5 shrink-0 text-slate-400" />
                    <span className="truncate font-mono"><strong className="font-semibold text-slate-700 dark:text-slate-300">OCR:</strong> {message.extractedText}</span>
                  </div>
                )}
              </div>
            )}

            {/* Content Formatting */}
            {isUser ? (
              <div className="text-sm whitespace-pre-wrap break-words leading-relaxed">
                {message.message}
              </div>
            ) : !message.message ? (
              <div className="flex items-center space-x-2 py-1 text-slate-500 dark:text-slate-400">
                <div className="flex space-x-1 items-center">
                  <div className="w-2 h-2 rounded-full bg-blue-500 dark:bg-blue-400 animate-bounce [animation-delay:-0.3s]" />
                  <div className="w-2 h-2 rounded-full bg-blue-500 dark:bg-blue-400 animate-bounce [animation-delay:-0.15s]" />
                  <div className="w-2 h-2 rounded-full bg-blue-500 dark:bg-blue-400 animate-bounce" />
                </div>
                <span className="text-xs font-semibold select-none tracking-wide animate-pulse">Thinking...</span>
              </div>
            ) : (
              <div className="markdown-body text-slate-800 dark:text-slate-100 leading-relaxed text-sm">
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
                          <CodeBlock
                            lang={lang}
                            codeContent={codeContent}
                            className={className}
                            highlighted={highlighted}
                            props={props}
                          />
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

          {/* Action Toolbar underneath bubble instead of high-floating buttons */}
          <div className={`flex items-center space-x-1.5 mt-2 opacity-60 group-hover:opacity-100 transition-opacity ${isUser ? 'mr-1 justify-end' : 'ml-1 justify-start'}`}>
            <button
              onClick={handleCopy}
              title={copied ? 'Copied!' : 'Copy text'}
              className="p-1 text-slate-400 hover:text-slate-800 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-900 rounded-md transition-colors"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-emerald-500" /> : <Copy className="w-3.5 h-3.5" />}
            </button>
            
            {!isUser && (
              <>
                <div className="w-[1px] h-3 bg-slate-200 dark:bg-slate-800" />

                <button
                  onClick={handleThumbsUp}
                  title="Helpful output"
                  className={`p-1 rounded-md transition-colors ${
                    message.reaction === 'thumbs-up'
                      ? 'text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/30'
                      : 'text-slate-400 hover:text-blue-600 dark:hover:text-blue-400 hover:bg-slate-100 dark:hover:bg-slate-900'
                  }`}
                >
                  <ThumbsUp className="w-3.5 h-3.5" />
                </button>
                <button
                  onClick={handleThumbsDown}
                  title="Unsatisfactory output"
                  className={`p-1 rounded-md transition-colors ${
                    message.reaction === 'thumbs-down'
                      ? 'text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-950/30'
                      : 'text-slate-400 hover:text-red-600 dark:hover:text-red-400 hover:bg-slate-100 dark:hover:bg-slate-900'
                  }`}
                >
                  <ThumbsDown className="w-3.5 h-3.5" />
                </button>
              </>
            )}

            {onRetry && (
              <>
                <div className="w-[1px] h-3 bg-slate-200 dark:bg-slate-800" />
                <button
                  onClick={() => onRetry(message.id)}
                  title={isUser ? "Resend prompt" : "Regenerate reply"}
                  className="p-1 text-slate-400 hover:text-slate-800 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-900 rounded-md transition-colors"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                </button>
              </>
            )}

            {onDelete && (
              <>
                <div className="w-[1px] h-3 bg-slate-200 dark:bg-slate-800" />
                <button
                  onClick={() => onDelete(message.id)}
                  title="Remove message"
                  className="p-1 text-slate-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-950/10 rounded-md transition-colors"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </>
            )}
          </div>

          {/* Quick-follow-up suggestions rendered inside clean pill borders */}
          {message.suggestions && message.suggestions.length > 0 && (
            <motion.div 
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              className="flex flex-wrap gap-2 mt-3.5"
            >
              {message.suggestions.map((suggestion, idx) => (
                <button
                  key={idx}
                  onClick={() => onSuggestionClick?.(suggestion)}
                  className="px-3 py-1.5 text-xs font-semibold text-slate-700 dark:text-slate-300 hover:text-blue-600 dark:hover:text-blue-400 bg-white dark:bg-slate-900 hover:bg-slate-50 dark:hover:bg-slate-800 rounded-full border border-slate-200 dark:border-slate-800 shadow-3xs transition-all hover:scale-101 active:scale-99"
                >
                  {suggestion}
                </button>
              ))}
            </motion.div>
          )}

        </div>
      </div>

      {/* Context Menu Popup */}
      <AnimatePresence>
        {contextMenu && (
          <motion.div
            initial={{ opacity: 0, scale: 0.96 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.96 }}
            transition={{ duration: 0.12 }}
            ref={contextMenuRef}
            className="fixed z-50 w-44 bg-white dark:bg-slate-900 rounded-xl shadow-xl border border-slate-200 dark:border-slate-800 py-1.5 overflow-hidden font-sans"
            style={{ top: contextMenu.y, left: contextMenu.x }}
          >
            <button
              onClick={handleCopy}
              className="w-full text-left px-3.5 py-2 text-xs text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800 flex items-center space-x-2 transition-colors font-medium"
            >
              {copied ? <Check className="w-4 h-4 text-emerald-500" /> : <Copy className="w-4 h-4" />}
              <span>{copied ? 'Copied to clipboard' : 'Copy message'}</span>
            </button>
            {onRetry && (
              <button
                onClick={() => {
                  onRetry(message.id);
                  setContextMenu(null);
                }}
                className="w-full text-left px-3.5 py-2 text-xs text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800 flex items-center space-x-2 transition-colors font-medium"
              >
                <RotateCcw className="w-4 h-4 text-blue-500" />
                <span>Retry prompt</span>
              </button>
            )}
            {onDelete && (
              <button
                onClick={() => {
                  onDelete(message.id);
                  setContextMenu(null);
                }}
                className="w-full text-left px-3.5 py-2 text-xs text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/20 flex items-center space-x-2 transition-colors font-semibold"
              >
                <Trash2 className="w-4 h-4" />
                <span>Delete message</span>
              </button>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
};
