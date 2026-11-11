export interface User {
  id: string;
  email: string;
  name: string;
  preferences?: Record<string, string>;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  email: string;
  name: string;
}

export interface Conversation {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
}

export interface Message {
  id: string;
  conversationId: string;
  role: 'user' | 'assistant' | 'system' | 'tool';
  content: string;
  model?: string;
  tokenCount?: number;
  metadata?: Record<string, any>;
  createdAt: string;
}

export interface Memory {
  id: string;
  type: 'SEMANTIC' | 'EPISODIC' | 'PROCEDURAL' | 'TASK';
  content: string;
  importance: number;
  confidence: number;
  createdAt: string;
  updatedAt: string;
}

export interface Document {
  id: string;
  filename: string;
  mimeType: string;
  fileSize: number;
  checksum: string;
  createdAt: string;
}

export interface Task {
  id: string;
  title: string;
  description?: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  createdAt: string;
  completedAt?: string;
}
