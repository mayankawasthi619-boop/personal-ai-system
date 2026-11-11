# Personal AI Platform - Architecture Guide

## Overview
Personal AI Platform is designed around a modular, privacy-first, zero-telemetry architecture.

```
Client (React + TS + Vite)
       │
       ▼ (REST / SSE Stream)
API Gateway & Security (Spring Boot 3.4 + Security + JWT)
       │
       ▼
AI Orchestrator (Intent Detection & Context Assembly)
  ├── Memory System (Semantic, Episodic, Procedural - pgvector HNSW)
  ├── RAG Engine (Apache Tika + Recursive Chunker - pgvector HNSW)
  ├── Tool System (Web Search, Calculator, Docker Code Execution Sandbox)
  └── Model Router (Local Ollama / Cloud OpenAI / Anthropic)
```

## Key Subsystems
1. **Model Router**: Intelligently routes incoming prompts between local Ollama instances and cloud models based on task complexity and privacy mode settings.
2. **Memory Engine**: Dynamic importance scoring and hybrid vector search combine cosine distance with temporal decay.
3. **RAG Pipeline**: Document text extraction via Apache Tika, recursive overlapping chunking, and pgvector cosine vector storage.
4. **Code Execution Sandbox**: Ephemeral Docker isolation using `--net=none`, CPU limits, and non-privileged execution contexts.
