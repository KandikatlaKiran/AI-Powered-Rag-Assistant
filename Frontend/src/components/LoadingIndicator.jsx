import React from 'react';

export default function LoadingIndicator() {
  return (
    <div className="chat-message chat-message-assistant loading-message" aria-live="polite" aria-busy="true">
      <div className="chat-message-avatar" aria-hidden="true">
        <span className="avatar-ai">⚡</span>
      </div>
      <div className="chat-message-body">
        <div className="chat-message-role-row">
          <span className="chat-message-role">RAG Assistant</span>
          <span className="thinking-badge">Searching ChromaDB...</span>
        </div>
        <div className="loading-card">
          <div className="loading-dots" aria-label="Assistant is thinking">
            <span />
            <span />
            <span />
          </div>
          <span className="loading-caption">Synthesizing answer with Llama 3.2:3B</span>
        </div>
      </div>
    </div>
  );
}
