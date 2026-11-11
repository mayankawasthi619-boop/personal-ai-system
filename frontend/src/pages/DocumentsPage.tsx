import React, { useEffect, useState } from 'react';
import { Upload, FileText, Trash2, FileCheck, Layers } from 'lucide-react';
import { api } from '../services/api';
import { Document } from '../types';

export const DocumentsPage: React.FC = () => {
  const [documents, setDocuments] = useState<Document[]>([]);
  const [isUploading, setIsUploading] = useState(false);

  useEffect(() => {
    loadDocuments();
  }, []);

  const loadDocuments = async () => {
    try {
      const res = await api.get('/documents');
      if (res.data.success) {
        setDocuments(res.data.data);
      }
    } catch (err) {
      console.error('Failed to load documents', err);
    }
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files || e.target.files.length === 0) return;
    const file = e.target.files[0];

    const formData = new FormData();
    formData.append('file', file);

    setIsUploading(true);
    try {
      await api.post('/documents/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      loadDocuments();
    } catch (err) {
      console.error('Failed to upload document', err);
    } finally {
      setIsUploading(false);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await api.delete(`/documents/${id}`);
      setDocuments(documents.filter((d) => d.id !== id));
    } catch (err) {
      console.error('Failed to delete document', err);
    }
  };

  return (
    <div className="flex-1 overflow-y-auto p-8 space-y-6 bg-slate-950">
      <div className="flex items-center justify-between border-b border-slate-800 pb-4">
        <div>
          <h1 className="text-xl font-bold text-slate-100">Knowledge Base & RAG Documents</h1>
          <p className="text-sm text-slate-400">Upload PDF, DOCX, TXT, or CSV files to index into pgvector for context retrieval.</p>
        </div>

        <label className="cursor-pointer bg-indigo-600 hover:bg-indigo-500 text-white font-medium px-4 py-2.5 rounded-xl flex items-center gap-2 transition-all shadow-md shadow-indigo-600/20 active:scale-95">
          <Upload className="w-4 h-4" />
          <span>{isUploading ? 'Ingesting Vector Chunks...' : 'Upload Document'}</span>
          <input type="file" onChange={handleFileUpload} disabled={isUploading} className="hidden" accept=".pdf,.docx,.txt,.csv,.json" />
        </label>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {documents.map((doc) => (
          <div key={doc.id} className="bg-slate-900 border border-slate-800 rounded-2xl p-5 hover:border-slate-700 transition-all flex flex-col justify-between">
            <div className="flex items-start gap-3">
              <div className="p-2.5 bg-indigo-500/10 text-indigo-400 rounded-xl border border-indigo-500/20">
                <FileText className="w-6 h-6" />
              </div>
              <div className="truncate">
                <h3 className="font-semibold text-slate-200 text-sm truncate">{doc.filename}</h3>
                <span className="text-xs text-slate-500">{(doc.fileSize / 1024).toFixed(1)} KB</span>
              </div>
            </div>

            <div className="mt-4 pt-4 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
              <div className="flex items-center gap-1.5 text-emerald-400">
                <FileCheck className="w-3.5 h-3.5" />
                <span>RAG Vectorized</span>
              </div>
              <button onClick={() => handleDelete(doc.id)} className="text-slate-500 hover:text-red-400 transition-colors p-1">
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
