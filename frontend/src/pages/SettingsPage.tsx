import React, { useState } from 'react';
import { Settings as SettingsIcon, Save } from 'lucide-react';
import { api } from '../services/api';

export const SettingsPage: React.FC = () => {
  const user = JSON.parse(localStorage.getItem('user') || '{}');
  const [name, setName] = useState(user.name || '');
  const [systemMode, setSystemMode] = useState('HYBRID');
  const [temperature, setTemperature] = useState(0.7);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.put('/users/me', {
        name,
        preferences: {
          system_mode: systemMode,
          temperature: temperature.toString(),
        },
      });
      alert('Settings saved successfully');
    } catch (err) {
      console.error('Failed to save settings', err);
    }
  };

  return (
    <div className="flex-1 overflow-y-auto p-8 space-y-6 bg-slate-950 max-w-3xl">
      <div className="border-b border-slate-800 pb-4">
        <h1 className="text-xl font-bold text-slate-100">System & AI Settings</h1>
        <p className="text-sm text-slate-400">Configure AI system mode, model preferences, and user options.</p>
      </div>

      <form onSubmit={handleSave} className="space-y-6">
        <div className="space-y-2">
          <label className="text-sm font-medium text-slate-300">Display Name</label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-indigo-500"
          />
        </div>

        <div className="space-y-2">
          <label className="text-sm font-medium text-slate-300">AI Execution Mode</label>
          <select
            value={systemMode}
            onChange={(e) => setSystemMode(e.target.value)}
            className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-indigo-500"
          >
            <option value="LOCAL">100% Local (Strict Offline Privacy)</option>
            <option value="HYBRID">Hybrid (Local Fast + Cloud Reasoning)</option>
            <option value="CLOUD">Cloud Only (OpenAI / Anthropic)</option>
          </select>
        </div>

        <div className="space-y-2">
          <label className="text-sm font-medium text-slate-300">Temperature: {temperature}</label>
          <input
            type="range"
            min="0"
            max="1"
            step="0.05"
            value={temperature}
            onChange={(e) => setTemperature(parseFloat(e.target.value))}
            className="w-full accent-indigo-600"
          />
        </div>

        <button type="submit" className="bg-indigo-600 hover:bg-indigo-500 text-white font-medium px-5 py-2.5 rounded-xl flex items-center gap-2 transition-all">
          <Save className="w-4 h-4" />
          <span>Save Preferences</span>
        </button>
      </form>
    </div>
  );
};
