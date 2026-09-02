import React, { useRef, useState } from 'react';
import { Trash2, Sun, Moon, Download, FileText, FileCode, Menu, Search, X, BookOpen, ArrowDownCircle, Pin, Server } from 'lucide-react';

interface HeaderProps {
  onClearChat: () => void;
  messageCount: number;
  isDarkMode: boolean;
  onToggleDarkMode: () => void;
  onExport: (format: 'md' | 'txt') => void;
  onExportWorkspace: () => void;
  onImportWorkspace: (file: File) => void;
  onOpenDrawer: () => void;
  searchQuery: string;
  onSearchChange: (query: string) => void;
  onOpenTemplates: () => void;
  autoScroll: boolean;
  onToggleAutoScroll: () => void;
  // Optional premium fields for active context
  sessionTitle?: string;
  sessionCategory?: string;
  sessionPinned?: boolean;
  onTogglePin?: () => void;
  onOpenServerSettings?: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  onClearChat,
  messageCount,
  isDarkMode,
  onToggleDarkMode,
  onExport,
  onExportWorkspace,
  onImportWorkspace,
  onOpenDrawer,
  searchQuery,
  onSearchChange,
  onOpenTemplates,
  autoScroll,
  onToggleAutoScroll,
  sessionTitle = 'AI Studio',
  sessionCategory = 'General',
  sessionPinned = false,
  onTogglePin,
  onOpenServerSettings,
}) => {
  const [showExportMenu, setShowExportMenu] = useState(false);
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const importWorkspaceInputRef = useRef<HTMLInputElement>(null);

  // Category specific color mapping for a highly refined accent feel
  const getCategoryColor = (cat: string) => {
    switch (cat.toLowerCase()) {
      case 'coding':
        return 'bg-blue-50 text-blue-700 dark:bg-blue-950/40 dark:text-blue-300 border-blue-100 dark:border-blue-900/30';
      case 'work':
        return 'bg-amber-50 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300 border-amber-100 dark:border-amber-900/30';
      case 'personal':
        return 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300 border-emerald-100 dark:border-emerald-900/30';
      default:
        return 'bg-slate-50 text-slate-700 dark:bg-slate-900 dark:text-slate-300 border-slate-100 dark:border-slate-800/80';
    }
  };

  return (
    <header className="sticky top-0 z-20 bg-slate-50/90 dark:bg-slate-950/95 backdrop-blur-md border-b border-slate-200/60 dark:border-slate-900 px-4 pt-[max(0.625rem,env(safe-area-inset-top))] pb-2.5 flex items-center justify-between transition-colors">
      <div className="flex items-center space-x-3 min-w-0 flex-1">
        <button
          onClick={onOpenDrawer}
          title="Open chat history"
          className="p-1.5 text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-100 hover:bg-slate-200/50 dark:hover:bg-slate-900 rounded-lg transition-colors shrink-0"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div className="h-4 w-[1px] bg-slate-200 dark:bg-slate-800 shrink-0" />

        <div className="min-w-0 flex items-center space-x-2">
          <div className="truncate">
            <div className="flex items-center space-x-2">
              <h1 className="text-sm font-semibold text-slate-900 dark:text-slate-100 tracking-tight truncate max-w-[150px] sm:max-w-[280px]">
                {sessionTitle}
              </h1>
              {onTogglePin && (
                <button
                  onClick={onTogglePin}
                  className={`p-0.5 rounded hover:bg-slate-200/40 dark:hover:bg-slate-800/40 transition-colors shrink-0 ${
                    sessionPinned ? 'text-amber-500' : 'text-slate-400 hover:text-slate-600'
                  }`}
                  title={sessionPinned ? 'Unpin Chat' : 'Pin Chat'}
                >
                  <Pin className={`w-3.5 h-3.5 ${sessionPinned ? 'fill-amber-500' : ''}`} />
                </button>
              )}
              <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-medium border ${getCategoryColor(sessionCategory)} shrink-0`}>
                {sessionCategory}
              </span>
            </div>
            <p className="text-[10px] text-slate-400 dark:text-slate-500 truncate hidden sm:block">
              AI Chat Workspace &bull; Gemini Flash
            </p>
          </div>
        </div>
      </div>

      <div className="flex items-center space-x-1 sm:space-x-1.5 shrink-0 ml-4">
        {/* Search Input toggle */}
        {messageCount > 0 && (
          <div className="relative flex items-center">
            {isSearchOpen ? (
              <div className="flex items-center bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-full px-2.5 py-1 space-x-1.5 shadow-2xs">
                <Search className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => onSearchChange(e.target.value)}
                  placeholder="Filter chat..."
                  autoFocus
                  className="bg-transparent text-xs text-slate-800 dark:text-slate-200 outline-none w-24 sm:w-36 font-normal"
                />
                <button
                  onClick={() => {
                    onSearchChange('');
                    setIsSearchOpen(false);
                  }}
                  className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 shrink-0"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </div>
            ) : (
              <button
                onClick={() => setIsSearchOpen(true)}
                title="Search chat"
                className="p-1.5 text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-100 hover:bg-slate-200/50 dark:hover:bg-slate-900 rounded-lg transition-colors"
              >
                <Search className="w-4 h-4" />
              </button>
            )}
          </div>
        )}

        {/* Prompt Templates */}
        <button
          onClick={onOpenTemplates}
          title="Templates Library"
          className="p-1.5 text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-100 hover:bg-slate-200/50 dark:hover:bg-slate-900 rounded-lg transition-colors"
        >
          <BookOpen className="w-4 h-4" />
        </button>

        {/* Auto-scroll toggle */}
        <button
          onClick={onToggleAutoScroll}
          title={autoScroll ? 'Auto-scroll on streaming: Active' : 'Auto-scroll on streaming: Paused'}
          className={`p-1.5 rounded-lg transition-colors flex items-center justify-center ${
            autoScroll
              ? 'text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/40 border border-blue-100/50 dark:border-blue-900/30'
              : 'text-slate-400 dark:text-slate-500 hover:bg-slate-200/50 dark:hover:bg-slate-900 border border-transparent'
          }`}
        >
          <ArrowDownCircle className="w-4 h-4" />
        </button>

        {/* Export and workspace backup */}
        <div className="relative">
            <button
              id="export-chat-btn"
              onClick={() => setShowExportMenu(!showExportMenu)}
               title="Export or backup workspace"
              className="p-1.5 text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-100 hover:bg-slate-200/50 dark:hover:bg-slate-900 rounded-lg transition-colors"
            >
              <Download className="w-4 h-4" />
            </button>

            {showExportMenu && (
              <>
                <div className="fixed inset-0 z-20" onClick={() => setShowExportMenu(false)} />
                <div className="absolute right-0 mt-1.5 w-56 bg-white dark:bg-slate-900 rounded-xl shadow-lg border border-slate-200 dark:border-slate-800 py-1.5 z-30">
                  {messageCount > 0 && (
                    <>
                      <button
                        onClick={() => {
                          onExport('md');
                          setShowExportMenu(false);
                        }}
                        className="w-full text-left px-3.5 py-2 text-xs text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800/50 flex items-center space-x-2 transition-colors"
                      >
                        <FileCode className="w-3.5 h-3.5 text-blue-500" />
                        <span className="font-medium">Export Markdown (.md)</span>
                      </button>
                      <button
                        onClick={() => {
                          onExport('txt');
                          setShowExportMenu(false);
                        }}
                        className="w-full text-left px-3.5 py-2 text-xs text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800/50 flex items-center space-x-2 transition-colors"
                      >
                        <FileText className="w-3.5 h-3.5 text-emerald-500" />
                        <span className="font-medium">Export Plain Text (.txt)</span>
                      </button>
                    </>
                  )}
                  <div className="my-1 border-t border-slate-100 dark:border-slate-800" />
                  <button
                    onClick={() => {
                      onExportWorkspace();
                      setShowExportMenu(false);
                    }}
                    className="w-full text-left px-3.5 py-2 text-xs text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800/50 flex items-center space-x-2 transition-colors"
                  >
                    <Download className="w-3.5 h-3.5 text-violet-500" />
                    <span className="font-medium">Backup Workspace (.json)</span>
                  </button>
                  <button
                    onClick={() => {
                      importWorkspaceInputRef.current?.click();
                      setShowExportMenu(false);
                    }}
                    className="w-full text-left px-3.5 py-2 text-xs text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800/50 flex items-center space-x-2 transition-colors"
                  >
                    <FileText className="w-3.5 h-3.5 text-amber-500" />
                    <span className="font-medium">Restore Workspace (.json)</span>
                  </button>
                </div>
              </>
            )}
        </div>

        <input
          ref={importWorkspaceInputRef}
          type="file"
          accept="application/json,.json"
          className="hidden"
          onChange={(event) => {
            const file = event.target.files?.[0];
            if (file) onImportWorkspace(file);
            event.target.value = '';
          }}
        />

        <div className="h-4 w-[1px] bg-slate-200 dark:bg-slate-800" />

        {/* Server connection settings */}
        {onOpenServerSettings && (
          <button
            id="server-settings-btn"
            onClick={onOpenServerSettings}
            title="Backend Server Settings"
            className="p-1.5 text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-100 hover:bg-slate-200/50 dark:hover:bg-slate-900 rounded-lg transition-colors"
          >
            <Server className="w-4 h-4 text-blue-500" />
          </button>
        )}

        {/* Dark mode toggle */}
        <button
          id="dark-mode-toggle"
          onClick={onToggleDarkMode}
          title={isDarkMode ? 'Switch to light mode' : 'Switch to dark mode'}
          className="p-1.5 text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-100 hover:bg-slate-200/50 dark:hover:bg-slate-900 rounded-lg transition-colors"
        >
          {isDarkMode ? <Sun className="w-4 h-4 text-amber-500" /> : <Moon className="w-4 h-4" />}
        </button>

        {/* Clear chat */}
        {messageCount > 0 && (
          <button
            id="clear-chat-btn"
            onClick={onClearChat}
            title="Clear Chat Content"
            className="p-1.5 text-slate-400 hover:text-red-500 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/20 rounded-lg transition-colors"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        )}
      </div>
    </header>
  );
};
