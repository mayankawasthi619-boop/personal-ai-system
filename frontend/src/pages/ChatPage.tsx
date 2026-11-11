import React, { useEffect, useState, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api, streamChatApi } from '../services/api';
import { Message } from '../types';
import { Header } from '../components/Header';
import { MessageBubble } from '../components/MessageBubble';
import { ChatInput } from '../components/ChatInput';

export const ChatPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const conversationId = searchParams.get('id');

  const [messages, setMessages] = useState<Message[]>([]);
  const [selectedModel, setSelectedModel] = useState('auto');
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (conversationId) {
      loadMessages(conversationId);
    } else {
      setMessages([]);
    }
  }, [conversationId]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const loadMessages = async (id: string) => {
    try {
      const res = await api.get(`/conversations/${id}/messages`);
      if (res.data.success) {
        setMessages(res.data.data);
      }
    } catch (err) {
      console.error('Failed to load conversation messages', err);
    }
  };

  const handleSendMessage = async (userContent: string) => {
    const userMsg: Message = {
      id: 'temp-' + Date.now(),
      conversationId: conversationId || '',
      role: 'user',
      content: userContent,
      createdAt: new Date().toISOString(),
    };

    const assistantMsgId = 'assistant-' + Date.now();
    const assistantMsg: Message = {
      id: assistantMsgId,
      conversationId: conversationId || '',
      role: 'assistant',
      content: '',
      model: selectedModel,
      createdAt: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, userMsg, assistantMsg]);
    setIsLoading(true);

    let accumulatedText = '';

    await streamChatApi(
      {
        conversationId: conversationId || undefined,
        message: userContent,
        model: selectedModel,
      },
      (chunk) => {
        accumulatedText += chunk;
        setMessages((prev) =>
          prev.map((msg) =>
            msg.id === assistantMsgId ? { ...msg, content: accumulatedText } : msg
          )
        );
      },
      () => {
        setIsLoading(false);
        if (!accumulatedText) {
          setMessages((prev) =>
            prev.map((msg) =>
              msg.id === assistantMsgId && !msg.content
                ? { ...msg, content: "Hello! I am your Personal AI Assistant. How can I help you today?" }
                : msg
            )
          );
        }
      },
      (err) => {
        console.error('Streaming error', err);
        setIsLoading(false);
        const errorDisplay = accumulatedText
          ? accumulatedText + `\n\n⚠️ [Error: ${err?.message || 'Connection interrupted'}]`
          : `⚠️ ${err?.message || 'Failed to get AI response. Please refresh or log in again.'}`;
        setMessages((prev) =>
          prev.map((msg) =>
            msg.id === assistantMsgId ? { ...msg, content: errorDisplay } : msg
          )
        );
      },
      (newId) => {
        if (!conversationId) {
          setSearchParams({ id: newId }, { replace: true });
          window.dispatchEvent(new Event('conversations-updated'));
        }
      }
    );
    window.dispatchEvent(new Event('conversations-updated'));
  };

  return (
    <div className="flex-1 flex flex-col h-screen bg-slate-950">
      <Header title="Conversational AI Assistant" selectedModel={selectedModel} onModelChange={setSelectedModel} />

      <main className="flex-1 overflow-y-auto p-6 space-y-6 max-w-4xl mx-auto w-full">
        {messages.length === 0 ? (
          <div className="h-full flex flex-col items-center justify-center text-center space-y-4 my-auto pt-20">
            <div className="p-4 bg-indigo-600/10 text-indigo-400 rounded-3xl border border-indigo-500/20">
              <span className="text-3xl">🤖</span>
            </div>
            <h3 className="text-xl font-bold text-slate-100">Welcome to Personal AI</h3>
            <p className="text-sm text-slate-400 max-w-md">
              Ask anything, query your documents via RAG, inspect memory, run Python code in docker sandboxes, or search the web.
            </p>
          </div>
        ) : (
          messages.map((msg) => <MessageBubble key={msg.id} message={msg} />)
        )}
        <div ref={messagesEndRef} />
      </main>

      <footer className="p-4 max-w-4xl mx-auto w-full">
        <ChatInput onSendMessage={handleSendMessage} isLoading={isLoading} />
      </footer>
    </div>
  );
};
