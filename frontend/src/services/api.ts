import axios from 'axios';

const rawBase = (import.meta.env.VITE_API_URL as string) || '';
const API_BASE_URL = rawBase 
  ? (rawBase.endsWith('/api') ? rawBase : `${rawBase.replace(/\/+$/, '')}/api`) 
  : '/api';

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('access_token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Stream Chat Helper using fetch + ReadableStream
export async function streamChatApi(
  payload: { conversationId?: string; message: string; model?: string },
  onChunk: (chunk: string) => void,
  onComplete: () => void,
  onError: (err: any) => void,
  onConversationId?: (id: string) => void
) {
  let token = localStorage.getItem('access_token');
  if (!token) {
    window.location.href = '/login';
    return;
  }

  try {
    let response = await fetch(`${API_BASE_URL}/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(payload),
    });

    // Handle token expiration: attempt transparent refresh
    if (response.status === 401) {
      const refreshToken = localStorage.getItem('refresh_token');
      if (refreshToken) {
        try {
          const refreshRes = await fetch(`${API_BASE_URL}/auth/refresh`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken }),
          });
          if (refreshRes.ok) {
            const refreshData = await refreshRes.json();
            if (refreshData.success && refreshData.data?.accessToken) {
              token = refreshData.data.accessToken;
              localStorage.setItem('access_token', token as string);
              // Retry chat request with new token
              response = await fetch(`${API_BASE_URL}/chat`, {
                method: 'POST',
                headers: {
                  'Content-Type': 'application/json',
                  Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify(payload),
              });
            }
          }
        } catch {
          // Refresh failed
        }
      }

      if (response.status === 401) {
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        localStorage.removeItem('user');
        window.location.href = '/login';
        throw new Error('Session expired. Please log in again.');
      }
    }

    if (!response.ok) {
      let errorMsg = `Chat request failed with status: ${response.status}`;
      try {
        const errorData = await response.json();
        if (errorData?.error?.message) {
          errorMsg = errorData.error.message;
        }
      } catch {}
      throw new Error(errorMsg);
    }

    const convId = response.headers.get('X-Conversation-Id');
    if (convId && onConversationId) {
      onConversationId(convId);
    }

    const reader = response.body?.getReader();
    const decoder = new TextDecoder();

    if (!reader) return;

    let buffer = '';
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      const text = decoder.decode(value, { stream: true });
      buffer += text;

      if (buffer.includes('data:')) {
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';
        for (const line of lines) {
          if (line.startsWith('data:')) {
            let dataContent = line.slice(5);
            if (dataContent.startsWith(' ')) {
              dataContent = dataContent.slice(1);
            }
            onChunk(dataContent);
          }
        }
      } else {
        onChunk(buffer);
        buffer = '';
      }
    }

    if (buffer) {
      if (buffer.startsWith('data:')) {
        let dataContent = buffer.slice(5);
        if (dataContent.startsWith(' ')) {
          dataContent = dataContent.slice(1);
        }
        onChunk(dataContent);
      } else {
        onChunk(buffer);
      }
    }

    onComplete();
  } catch (err) {
    onError(err);
  }
}
