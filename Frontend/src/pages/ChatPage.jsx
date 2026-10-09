import React from 'react';
import Sidebar from '../components/Sidebar';
import ChatWindow from '../components/ChatWindow';
import ChatInput from '../components/ChatInput';
import { useDocuments } from '../hooks/useDocuments';
import { useChat } from '../hooks/useChat';

/**
 * Main two-panel layout:
 * ┌──────────────┬──────────────────────────────────┐
 * │  Documents   │          Chat Window             │
 * │  (sidebar)   ├──────────────────────────────────┤
 * │              │          Chat Input              │
 * └──────────────┴──────────────────────────────────┘
 */
export default function ChatPage({ documentState }) {
  const localDocState = useDocuments();
  const {
    documents, loading: docsLoading, uploading,
    error: docsError, upload, remove, clearError,
  } = documentState || localDocState;

  const { messages, loading: chatLoading, sendQuestion } = useChat();

  const handleSelectPrompt = (promptText) => {
    sendQuestion(promptText);
  };

  return (
    <main className="chat-page">
      <Sidebar
        documents={documents}
        loading={docsLoading}
        uploading={uploading}
        error={docsError}
        onUpload={upload}
        onDelete={remove}
        onClearError={clearError}
      />

      <section className="chat-section" aria-label="Chat area">
        <div className="chat-section-header">
          <div className="chat-header-avatar">
            <span aria-hidden="true">🤖</span>
          </div>
          <div className="chat-header-info">
            <h2 className="chat-section-title">AI Knowledge Assistant</h2>
            <p className="chat-section-subtitle">Grounded Question Answering from ChromaDB Vector Store</p>
          </div>
        </div>

        <ChatWindow
          messages={messages}
          loading={chatLoading}
          onSelectPrompt={handleSelectPrompt}
        />

        <ChatInput onSend={sendQuestion} disabled={chatLoading} />
      </section>
    </main>
  );
}
