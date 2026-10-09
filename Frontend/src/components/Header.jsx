import React from 'react';

export default function Header({ documentCount = 0 }) {
  return (
    <header className="app-header" role="banner">
      <div className="header-brand">
        <div className="header-logo-badge">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8z"/>
            <path d="M12 6v6l4 2"/>
          </svg>
        </div>
        <div className="header-text">
          <div className="header-title-row">
            <h1 className="header-title">RAG Assistant</h1>
            <span className="header-badge">Production v2.0</span>
          </div>
          <span className="header-subtitle">
            LLM: <strong>llama3.2:3b</strong> • Embeddings: <strong>nomic-embed-text</strong> • Vector DB: <strong>ChromaDB 1.4.4</strong>
          </span>
        </div>
      </div>

      <div className="header-status-area">
        {documentCount > 0 && (
          <div className="header-kb-badge">
            <span className="kb-badge-dot" />
            <span className="kb-badge-text">{documentCount} {documentCount === 1 ? 'Doc' : 'Docs'} Indexed</span>
          </div>
        )}
        <div className="header-status">
          <span className="status-dot" aria-label="System online" title="System online" />
          <span className="status-label">Online</span>
        </div>
      </div>
    </header>
  );
}
