# REST & SSE API Reference

## Authentication
- `POST /api/auth/register`: Register user account
- `POST /api/auth/login`: Authenticate and receive JWT access/refresh tokens
- `POST /api/auth/refresh`: Refresh expired access token

## Chat & Conversations
- `POST /api/chat` (SSE): Stream tokens for user message
- `GET /api/conversations`: List user conversation history
- `GET /api/conversations/{id}/messages`: Fetch message history for a conversation

## Memory & Knowledge
- `GET /api/memory`: List extracted memories
- `POST /api/memory`: Add manual memory
- `DELETE /api/memory/{id}`: Delete specific memory
- `DELETE /api/memory/all`: Clear memory bank

## Documents & RAG
- `POST /api/documents/upload`: Upload file for text extraction and pgvector chunking
- `GET /api/documents`: List user uploaded documents
- `DELETE /api/documents/{id}`: Delete document and associated vector chunks
- `POST /api/knowledge/search`: Perform semantic vector similarity search

## Tools & Code Execution
- `GET /api/tools`: Discover registered system tools
- `POST /api/tools/{name}/execute`: Execute tool with audit logging
