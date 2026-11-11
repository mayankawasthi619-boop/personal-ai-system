import React from 'react';
import { Cpu, ShieldCheck } from 'lucide-react';

interface HeaderProps {
  title: string;
  selectedModel: string;
  onModelChange: (model: string) => void;
}

export const Header: React.FC<HeaderProps> = ({ title, selectedModel, onModelChange }) => {
  return (
    <header className="h-14 bg-slate-900/80 backdrop-blur border-b border-slate-800 px-6 flex items-center justify-between z-10">
      <div className="flex items-center gap-3">
        <h2 className="text-base font-semibold text-slate-100">{title}</h2>
        <span className="flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
          <ShieldCheck className="w-3.5 h-3.5" />
          <span>Private Mode</span>
        </span>
      </div>

      <div className="flex items-center gap-3">
        <div className="flex items-center gap-2 bg-slate-800 border border-slate-700/60 rounded-lg px-3 py-1 text-xs">
          <Cpu className="w-3.5 h-3.5 text-indigo-400" />
          <span className="text-slate-400">Model:</span>
          <select
            value={selectedModel}
            onChange={(e) => onModelChange(e.target.value)}
            className="bg-transparent text-slate-200 font-medium focus:outline-none cursor-pointer"
          >
            <option value="auto">Auto (Hybrid Route)</option>
            <option value="ollama:llama3.2">Ollama - Llama 3.2 (Local)</option>
            <option value="gpt-4o-mini">OpenAI - GPT-4o Mini (Cloud)</option>
          </select>
        </div>
      </div>
    </header>
  );
};
