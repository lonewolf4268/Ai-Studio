import React, { useState } from 'react';
import { Sparkles, Trash2, Bot, Sun, Moon, Download, FileText, FileCode, Menu, Search, X, BookOpen, ArrowDownCircle } from 'lucide-react';

interface HeaderProps {
  onClearChat: () => void;
  messageCount: number;
  isDarkMode: boolean;
  onToggleDarkMode: () => void;
  onExport: (format: 'md' | 'txt') => void;
  onOpenDrawer: () => void;
  searchQuery: string;
  onSearchChange: (query: string) => void;
  onOpenTemplates: () => void;
  autoScroll: boolean;
  onToggleAutoScroll: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  onClearChat,
  messageCount,
  isDarkMode,
  onToggleDarkMode,
  onExport,
  onOpenDrawer,
  searchQuery,
  onSearchChange,
  onOpenTemplates,
  autoScroll,
  onToggleAutoScroll,
}) => {
  const [showExportMenu, setShowExportMenu] = useState(false);
  const [isSearchOpen, setIsSearchOpen] = useState(false);

  return (
    <header className="sticky top-0 z-20 bg-white/90 dark:bg-gray-900/90 backdrop-blur-md border-b border-gray-200 dark:border-gray-800 px-4 py-3 flex items-center justify-between shadow-xs transition-colors">
      <div className="flex items-center space-x-3">
        <button
          onClick={onOpenDrawer}
          title="Open chat history drawer"
          className="p-2 text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-full transition-colors"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div className="w-10 h-10 rounded-full bg-blue-600 flex items-center justify-center text-white shadow-xs hidden sm:flex">
          <Bot className="w-5 h-5" />
        </div>
        <div>
          <div className="flex items-center space-x-1.5">
            <h1 className="text-base font-semibold text-gray-900 dark:text-gray-100 tracking-tight">AI Studio</h1>
            <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-blue-50 dark:bg-blue-950/60 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-900">
              <Sparkles className="w-3 h-3 mr-1" />
              Gemini
            </span>
          </div>
          <p className="text-xs text-gray-500 dark:text-gray-400 hidden sm:block">By Lonewolf • OCR & Markdown Enabled</p>
        </div>
      </div>

      <div className="flex items-center space-x-2 relative">
        {/* Search input / toggle */}
        {messageCount > 0 && (
          <div className="flex items-center">
            {isSearchOpen ? (
              <div className="flex items-center bg-gray-100 dark:bg-gray-800 border border-gray-300 dark:border-gray-700 rounded-full px-3 py-1 space-x-2">
                <Search className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => onSearchChange(e.target.value)}
                  placeholder="Filter messages..."
                  autoFocus
                  className="bg-transparent text-xs text-gray-900 dark:text-gray-100 outline-none w-28 sm:w-40"
                />
                <button
                  onClick={() => {
                    onSearchChange('');
                    setIsSearchOpen(false);
                  }}
                  className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </div>
            ) : (
              <button
                onClick={() => setIsSearchOpen(true)}
                title="Search chat messages"
                className="p-2 text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-full transition-colors"
              >
                <Search className="w-4 h-4" />
              </button>
            )}
          </div>
        )}

        {/* Export transcript */}
        {messageCount > 0 && (
          <div className="relative">
            <button
              id="export-chat-btn"
              onClick={() => setShowExportMenu(!showExportMenu)}
              title="Export chat transcript"
              className="p-2 text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-full transition-colors flex items-center space-x-1"
            >
              <Download className="w-4 h-4" />
            </button>

            {showExportMenu && (
              <div className="absolute right-0 mt-2 w-48 bg-white dark:bg-gray-800 rounded-xl shadow-lg border border-gray-200 dark:border-gray-700 py-1.5 z-30">
                <button
                  onClick={() => {
                    onExport('md');
                    setShowExportMenu(false);
                  }}
                  className="w-full text-left px-4 py-2 text-xs text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center space-x-2"
                >
                  <FileCode className="w-3.5 h-3.5 text-blue-500" />
                  <span>Export as Markdown (.md)</span>
                </button>
                <button
                  onClick={() => {
                    onExport('txt');
                    setShowExportMenu(false);
                  }}
                  className="w-full text-left px-4 py-2 text-xs text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center space-x-2"
                >
                  <FileText className="w-3.5 h-3.5 text-emerald-500" />
                  <span>Export as Text (.txt)</span>
                </button>
              </div>
            )}
          </div>
        )}

        {/* Prompt Templates */}
        <button
          onClick={onOpenTemplates}
          title="Prompt Templates Library"
          className="p-2 text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-full transition-colors flex items-center space-x-1"
        >
          <BookOpen className="w-4 h-4" />
        </button>

        {/* Auto-scroll toggle */}
        <button
          onClick={onToggleAutoScroll}
          title={autoScroll ? 'Auto-scroll on streaming: Enabled' : 'Auto-scroll on streaming: Disabled'}
          className={`p-2 rounded-full transition-colors flex items-center space-x-1 ${
            autoScroll
              ? 'text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/60'
              : 'text-gray-400 dark:text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-800'
          }`}
        >
          <ArrowDownCircle className="w-4 h-4" />
        </button>

        {/* Dark mode toggle */}
        <button
          id="dark-mode-toggle"
          onClick={onToggleDarkMode}
          title={isDarkMode ? 'Switch to light mode' : 'Switch to dark mode'}
          className="p-2 text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-full transition-colors"
        >
          {isDarkMode ? <Sun className="w-4 h-4 text-amber-400" /> : <Moon className="w-4 h-4" />}
        </button>

        {/* Clear chat */}
        {messageCount > 0 && (
          <button
            id="clear-chat-btn"
            onClick={onClearChat}
            title="Clear chat history"
            className="p-2 text-gray-500 hover:text-red-600 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/50 rounded-full transition-colors"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        )}
      </div>
    </header>
  );
};

