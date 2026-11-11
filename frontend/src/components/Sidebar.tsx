import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { MessageSquare, Database, Brain, CheckSquare, Settings, Plus, LogOut, Bot, Trash2 } from 'lucide-react';
import { api } from '../services/api';
import { Conversation } from '../types';

export const Sidebar: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const user = JSON.parse(localStorage.getItem('user') || '{}');

  useEffect(() => {
    loadConversations();
    const handleUpdate = () => loadConversations();
    window.addEventListener('conversations-updated', handleUpdate);
    return () => window.removeEventListener('conversations-updated', handleUpdate);
  }, [location.pathname]);

  const loadConversations = async () => {
    try {
      const res = await api.get('/conversations');
      if (res.data.success) {
        setConversations(res.data.data.content || []);
      }
    } catch (err) {
      console.error('Failed to load conversations', err);
    }
  };

  const handleDeleteConversation = async (e: React.MouseEvent, id: string) => {
    e.stopPropagation();
    try {
      await api.delete(`/conversations/${id}`);
      setConversations((prev) => prev.filter((c) => c.id !== id));
      if (location.search.includes(id)) {
        navigate('/chat');
      }
    } catch (err) {
      console.error('Failed to delete conversation', err);
    }
  };

  const handleLogout = () => {
    localStorage.clear();
    navigate('/login');
  };

  const navItems = [
    { path: '/chat', label: 'Chat Assistant', icon: MessageSquare },
    { path: '/documents', label: 'Documents & RAG', icon: Database },
    { path: '/memory', label: 'Memory Bank', icon: Brain },
    { path: '/tasks', label: 'Tasks & Goals', icon: CheckSquare },
    { path: '/settings', label: 'Settings', icon: Settings },
  ];

  return (
    <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col h-screen select-none">
      {/* Brand Header */}
      <div className="p-4 border-b border-slate-800 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-indigo-600 rounded-xl text-white shadow-lg shadow-indigo-600/30">
            <Bot className="w-5 h-5" />
          </div>
          <div>
            <h1 className="font-bold text-slate-100 text-sm tracking-wide">Personal AI</h1>
            <span className="text-xs text-indigo-400 font-medium">Local & Cloud v1.0</span>
          </div>
        </div>
      </div>

      {/* New Chat Action */}
      <div className="p-3">
        <button
          onClick={() => navigate('/chat')}
          className="w-full py-2.5 px-4 bg-indigo-600 hover:bg-indigo-500 text-white font-medium rounded-xl flex items-center justify-center gap-2 transition-all shadow-md shadow-indigo-600/20 active:scale-95"
        >
          <Plus className="w-4 h-4" />
          <span>New Chat</span>
        </button>
      </div>

      {/* Primary Navigation */}
      <nav className="px-3 space-y-1">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = location.pathname === item.path;
          return (
            <Link
              key={item.path}
              to={item.path}
              className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-slate-800 text-indigo-400 border-l-2 border-indigo-500'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
              }`}
            >
              <Icon className="w-4 h-4" />
              <span>{item.label}</span>
            </Link>
          );
        })}
      </nav>

      {/* Recent Conversations List */}
      <div className="mt-4 flex-1 overflow-y-auto px-3">
        <div className="px-2 py-1 text-xs font-semibold text-slate-500 uppercase tracking-wider">
          Recent Chats
        </div>
        <div className="mt-2 space-y-1">
          {conversations.slice(0, 15).map((conv) => (
            <div
              key={conv.id}
              className="group flex items-center justify-between rounded-lg px-2.5 py-1.5 text-xs text-slate-400 hover:bg-slate-800 hover:text-slate-200 transition-colors"
            >
              <button
                onClick={() => navigate(`/chat?id=${conv.id}`)}
                className="flex-1 text-left truncate mr-2"
                title={conv.title}
              >
                {conv.title}
              </button>
              <button
                onClick={(e) => handleDeleteConversation(e, conv.id)}
                title="Delete Chat"
                className="opacity-0 group-hover:opacity-100 p-1 text-slate-500 hover:text-red-400 hover:bg-slate-700/50 rounded transition-all"
              >
                <Trash2 className="w-3 h-3" />
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* User Profile Footer */}
      <div className="p-3 border-t border-slate-800 flex items-center justify-between bg-slate-950/50">
        <div className="flex items-center gap-2 truncate">
          <div className="w-8 h-8 rounded-full bg-slate-700 flex items-center justify-center font-bold text-xs text-indigo-300">
            {user.name ? user.name.charAt(0).toUpperCase() : 'U'}
          </div>
          <div className="truncate">
            <p className="text-xs font-medium text-slate-200 truncate">{user.name || 'Personal User'}</p>
            <p className="text-[10px] text-slate-500 truncate">{user.email || 'local@system'}</p>
          </div>
        </div>
        <button
          onClick={handleLogout}
          title="Logout"
          className="p-1.5 text-slate-400 hover:text-red-400 hover:bg-slate-800 rounded-lg transition-colors"
        >
          <LogOut className="w-4 h-4" />
        </button>
      </div>
    </aside>
  );
};
