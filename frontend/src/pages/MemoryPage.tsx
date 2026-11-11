import React, { useEffect, useState } from 'react';
import { Brain, Trash2, ShieldAlert } from 'lucide-react';
import { api } from '../services/api';
import { Memory } from '../types';

export const MemoryPage: React.FC = () => {
  const [memories, setMemories] = useState<Memory[]>([]);

  useEffect(() => {
    loadMemories();
  }, []);

  const loadMemories = async () => {
    try {
      const res = await api.get('/memory');
      if (res.data.success) {
        setMemories(res.data.data);
      }
    } catch (err) {
      console.error('Failed to load memories', err);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await api.delete(`/memory/${id}`);
      setMemories(memories.filter((m) => m.id !== id));
    } catch (err) {
      console.error('Failed to delete memory', err);
    }
  };

  const handleClearAll = async () => {
    if (!confirm('Are you sure you want to clear all long-term memories?')) return;
    try {
      await api.delete('/memory/all');
      setMemories([]);
    } catch (err) {
      console.error('Failed to clear memories', err);
    }
  };

  return (
    <div className="flex-1 overflow-y-auto p-8 space-y-6 bg-slate-950">
      <div className="flex items-center justify-between border-b border-slate-800 pb-4">
        <div>
          <h1 className="text-xl font-bold text-slate-100">Personal Memory Bank</h1>
          <p className="text-sm text-slate-400">Extracted long-term user facts, preferences, procedural rules, and goal memories.</p>
        </div>

        <button
          onClick={handleClearAll}
          className="bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/20 font-medium px-4 py-2 rounded-xl flex items-center gap-2 transition-all text-xs"
        >
          <ShieldAlert className="w-4 h-4" />
          <span>Clear All Memory</span>
        </button>
      </div>

      <div className="space-y-3">
        {memories.map((mem) => (
          <div key={mem.id} className="bg-slate-900 border border-slate-800 rounded-2xl p-4 flex items-center justify-between hover:border-slate-700 transition-all">
            <div className="flex items-center gap-4">
              <span className={`px-2.5 py-1 rounded-lg text-xs font-semibold uppercase tracking-wider ${
                mem.type === 'SEMANTIC' ? 'bg-indigo-500/10 text-indigo-400 border border-indigo-500/20' :
                mem.type === 'PROCEDURAL' ? 'bg-purple-500/10 text-purple-400 border border-purple-500/20' :
                'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
              }`}>
                {mem.type}
              </span>
              <p className="text-sm font-medium text-slate-200">{mem.content}</p>
            </div>

            <div className="flex items-center gap-4 text-xs text-slate-500">
              <span>Importance: {(mem.importance * 100).toFixed(0)}%</span>
              <button onClick={() => handleDelete(mem.id)} className="text-slate-500 hover:text-red-400 transition-colors p-1">
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
