import React, { useState, useRef } from 'react';
import { Send, Mic, Paperclip, StopCircle, Loader2 } from 'lucide-react';
import { api } from '../services/api';

interface ChatInputProps {
  onSendMessage: (message: string) => void;
  isLoading: boolean;
}

export const ChatInput: React.FC<ChatInputProps> = ({ onSendMessage, isLoading }) => {
  const [input, setInput] = useState('');
  const [isRecording, setIsRecording] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [attachmentNotice, setAttachmentNotice] = useState<string | null>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const recognitionRef = useRef<any>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || isLoading) return;
    onSendMessage(input.trim());
    setInput('');
    setAttachmentNotice(null);
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  const handleTextareaChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInput(e.target.value);
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 180)}px`;
    }
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files || e.target.files.length === 0) return;
    const file = e.target.files[0];
    const formData = new FormData();
    formData.append('file', file);

    setIsUploading(true);
    try {
      const res = await api.post('/documents/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      if (res.data.success) {
        setAttachmentNotice(`✓ Indexed "${file.name}" into RAG vectorstore.`);
        setInput((prev) => (prev ? `${prev}\n[Document Context: ${file.name}]` : `Please summarize or answer questions based on ${file.name}.`));
      }
    } catch (err: any) {
      console.error('File upload failed', err);
      setAttachmentNotice(`⚠️ Failed to upload ${file.name}`);
    } finally {
      setIsUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const toggleRecording = () => {
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) {
      alert('Speech recognition is not supported in this browser. Please use Chrome or Edge.');
      return;
    }

    if (isRecording) {
      recognitionRef.current?.stop();
      setIsRecording(false);
      return;
    }

    try {
      const recognition = new SpeechRecognition();
      recognition.continuous = true;
      recognition.interimResults = true;
      recognition.lang = 'en-US';

      recognition.onresult = (event: any) => {
        let transcript = '';
        for (let i = event.resultIndex; i < event.results.length; i++) {
          transcript += event.results[i][0].transcript;
        }
        if (transcript) {
          setInput((prev) => (prev ? `${prev} ${transcript}` : transcript));
        }
      };

      recognition.onerror = (err: any) => {
        console.error('Voice recognition error:', err);
        setIsRecording(false);
      };

      recognition.onend = () => {
        setIsRecording(false);
      };

      recognition.start();
      recognitionRef.current = recognition;
      setIsRecording(true);
    } catch (e) {
      console.error('Speech recognition failed to start', e);
      setIsRecording(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="relative bg-slate-900 border border-slate-800 rounded-2xl p-2.5 shadow-2xl focus-within:border-indigo-500/50 transition-all">
      {attachmentNotice && (
        <div className="px-3 py-1 mb-1 text-xs text-indigo-400 bg-indigo-500/10 rounded-lg flex items-center justify-between">
          <span>{attachmentNotice}</span>
          <button type="button" onClick={() => setAttachmentNotice(null)} className="text-slate-500 hover:text-slate-300 ml-2">×</button>
        </div>
      )}

      <textarea
        ref={textareaRef}
        rows={1}
        value={input}
        onChange={handleTextareaChange}
        onKeyDown={handleKeyDown}
        placeholder="Ask anything, upload documents for RAG, calculate expressions, or run python: ..."
        className="w-full bg-transparent text-slate-100 placeholder-slate-500 text-sm px-3 py-1.5 focus:outline-none resize-none max-h-44"
      />

      <input
        ref={fileInputRef}
        type="file"
        onChange={handleFileUpload}
        className="hidden"
        accept=".pdf,.docx,.txt,.csv,.json"
      />

      <div className="flex items-center justify-between pt-2 border-t border-slate-800/60 mt-1">
        <div className="flex items-center gap-1 text-slate-400">
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            disabled={isUploading}
            className="p-1.5 hover:bg-slate-800 rounded-lg text-slate-400 hover:text-slate-200 transition-colors"
            title="Attach Document for RAG indexing"
          >
            {isUploading ? <Loader2 className="w-4 h-4 animate-spin text-indigo-400" /> : <Paperclip className="w-4 h-4" />}
          </button>
          <button
            type="button"
            onClick={toggleRecording}
            className={`p-1.5 rounded-lg transition-colors ${
              isRecording ? 'bg-red-500/20 text-red-400 animate-pulse' : 'hover:bg-slate-800 text-slate-400 hover:text-slate-200'
            }`}
            title="Voice Input (Speech-to-Text)"
          >
            <Mic className="w-4 h-4" />
          </button>
        </div>

        <button
          type="submit"
          disabled={!input.trim() || isLoading}
          className="p-2 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-40 text-white rounded-xl transition-all shadow-md shadow-indigo-600/20 active:scale-95 flex items-center justify-center"
        >
          {isLoading ? <StopCircle className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
        </button>
      </div>
    </form>
  );
};
