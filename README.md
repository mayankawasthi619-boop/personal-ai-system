# Personal AI Platform 🤖

A private, modular, extensible Personal AI Assistant platform capable of running 100% locally on your computer or deployed securely to a private server.

Built with **Java 21 LTS**, **Spring Boot 3.4+**, **Spring AI**, **PostgreSQL + pgvector**, **Redis**, and **React 18 + TypeScript + Vite + Tailwind CSS**.

---

## 🌟 Key Features

- 💬 **Conversational AI**: Real-time SSE token streaming, multi-turn chat management, editing, retry, and context window pruning.
- 🧠 **Multi-Tier Memory Architecture**: Short-term conversation context, long-term semantic knowledge, episodic memory, procedural preferences, and task tracking with dynamic importance scoring.
- 📄 **Retrieval-Augmented Generation (RAG)**: Multi-format document parser (PDF, DOCX, TXT, CSV), recursive chunking, pgvector vector storage, and source citation.
- 🔀 **Smart Model Router**: Seamless switching between local LLMs (Ollama, llama.cpp, vLLM) and cloud providers (OpenAI, Anthropic, Gemini) based on task complexity, privacy rules, and latency requirements.
- 🛠️ **Sandboxed Tool Engine**: Secure function calling framework featuring web search, document reader, calculator, date/time, unit converter, and an ephemeral Docker code execution sandbox.
- 🤖 **Autonomous Multi-Step Agents**: Task planning and execution loops with human-in-the-loop safety approvals for high-risk actions.
- 🔒 **Privacy-First & Secure**: JWT access/refresh tokens, RBAC, encrypted persistent state, path traversal shields, and a 100% local model enforcement mode.

---

## 🏗️ Monorepo Structure

```
personal-ai/
├── backend/          # Java 21 Spring Boot backend application
├── frontend/         # React 18 TypeScript Vite frontend UI
├── ai/
│   ├── models/       # Model routing rules & provider settings
│   └── prompts/      # Externalized system prompt templates
├── data/             # Local documents, memory snapshots, and embeddings
├── docker/           # Container scripts (Postgres init, sandbox configs)
├── docker-compose.yml# Multi-container local deployment
└── README.md
```

---

## 🚀 Quick Start (Local Setup)

### Prerequisites

- **Java 21 JDK**
- **Maven 3.9+**
- **Node.js 20+** & **npm**
- **Docker & Docker Compose**
- **Ollama** (optional for local LLM mode)

### 1. Clone & Configure Environment

```bash
cp .env.example .env
```

### 2. Start Services via Docker Compose

```bash
docker compose up -d postgres redis ollama
```

### 3. Run Backend Application

```bash
cd backend
mvn clean spring-boot:run
```

### 4. Run Frontend UI

```bash
cd frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173) in your browser.

---

## 🌐 Cloud Deployment on Render

This repository includes a native [render.yaml](render.yaml) Blueprint specification for zero-downtime automated cloud deployment.

### 1-Click Deployment Steps:
1. Log in to your [Render Dashboard](https://dashboard.render.com/).
2. Click **New +** and select **Blueprint**.
3. Connect your GitHub repository: [`mayankawasthi619-boop/personal-ai-system`](https://github.com/mayankawasthi619-boop/personal-ai-system).
4. Render automatically parses `render.yaml` and provisions:
   - **PostgreSQL Database** (`personal-ai-postgres` with `vector` extension support).
   - **Backend Web Service** (`personal-ai-backend` Docker container with Spring Boot).
   - **Frontend Client** (`personal-ai-frontend` React SPA with automatic backend URL linking).
5. In the Render Dashboard, add your `OPENAI_API_KEY` (or Anthropic / Gemini key) in `personal-ai-backend` environment settings.
6. Click **Apply**. Render will deploy your application and automatically redeploy whenever you push changes to GitHub!

---

## 🔄 Automatic GitHub Sync & CI/CD

- **Repository**: [https://github.com/mayankawasthi619-boop/personal-ai-system](https://github.com/mayankawasthi619-boop/personal-ai-system)
- **Continuous Integration**: Every `git push` to `main` triggers automated Maven unit testing and TypeScript frontend compilation via GitHub Actions.
- **Continuous Deployment**: Render listens for GitHub push webhooks and redeploys the services automatically upon every commit.

---

## 📜 License

Distributed under the MIT License. See `LICENSE` for details.

