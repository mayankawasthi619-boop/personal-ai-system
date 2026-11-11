import React, { useEffect, useState } from 'react';
import { CheckSquare, Plus, CheckCircle, Clock } from 'lucide-react';
import { api } from '../services/api';
import { Task } from '../types';

export const TasksPage: React.FC = () => {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [newTitle, setNewTitle] = useState('');

  useEffect(() => {
    loadTasks();
  }, []);

  const loadTasks = async () => {
    try {
      const res = await api.get('/tasks');
      if (res.data.success) {
        setTasks(res.data.data);
      }
    } catch (err) {
      console.error('Failed to load tasks', err);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim()) return;
    try {
      const res = await api.post('/tasks', { title: newTitle.trim(), priority: 'MEDIUM' });
      if (res.data.success) {
        setTasks([res.data.data, ...tasks]);
        setNewTitle('');
      }
    } catch (err) {
      console.error('Failed to create task', err);
    }
  };

  const toggleTaskStatus = async (task: Task) => {
    const nextStatus = task.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED';
    try {
      const res = await api.put(`/tasks/${task.id}`, { status: nextStatus });
      if (res.data.success) {
        setTasks(tasks.map((t) => (t.id === task.id ? res.data.data : t)));
      }
    } catch (err) {
      console.error('Failed to update task', err);
    }
  };

  return (
    <div className="flex-1 overflow-y-auto p-8 space-y-6 bg-slate-950 max-w-4xl">
      <div className="border-b border-slate-800 pb-4">
        <h1 className="text-xl font-bold text-slate-100">Tasks & Goal Planner</h1>
        <p className="text-sm text-slate-400">Track active goals, scheduled tasks, and autonomous agent executions.</p>
      </div>

      <form onSubmit={handleCreate} className="flex gap-3">
        <input
          type="text"
          value={newTitle}
          onChange={(e) => setNewTitle(e.target.value)}
          placeholder="Add a new goal or task..."
          className="flex-1 bg-slate-900 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-indigo-500"
        />
        <button type="submit" className="bg-indigo-600 hover:bg-indigo-500 text-white font-medium px-4 py-2.5 rounded-xl flex items-center gap-2 transition-all">
          <Plus className="w-4 h-4" />
          <span>Add Task</span>
        </button>
      </form>

      <div className="space-y-3">
        {tasks.map((t) => (
          <div key={t.id} className="bg-slate-900 border border-slate-800 rounded-2xl p-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <button onClick={() => toggleTaskStatus(t)} className="text-slate-400 hover:text-indigo-400 transition-colors">
                {t.status === 'COMPLETED' ? <CheckCircle className="w-5 h-5 text-emerald-400" /> : <Clock className="w-5 h-5 text-slate-500" />}
              </button>
              <span className={`text-sm font-medium ${t.status === 'COMPLETED' ? 'line-through text-slate-500' : 'text-slate-200'}`}>
                {t.title}
              </span>
            </div>

            <span className="text-xs px-2.5 py-0.5 rounded-full bg-slate-800 text-slate-400 uppercase font-semibold">
              {t.priority}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};
