import React from 'react';

const SUGGESTIONS = [
  {
    icon: '📚',
    title: 'Check document inventory',
    prompt: 'Which documents do you have?',
  },
  {
    icon: '📑',
    title: 'Summarize knowledge base',
    prompt: 'Summarize the key points from the uploaded documents.',
  },
  {
    icon: '🔍',
    title: 'Find specific information',
    prompt: 'What are the main requirements or policies mentioned in the documents?',
  },
];

export default function EmptyState({ onSelectPrompt }) {
  return (
    <div className="empty-state" aria-label="Start a conversation">
      <div className="empty-state-hero-badge" aria-hidden="true">
        <div className="empty-state-glow" />
        <span className="empty-state-icon">💬</span>
      </div>

      <h2 className="empty-state-title">Ask Your Knowledge Base</h2>
      <p className="empty-state-description">
        Upload PDF, DOCX, or TXT documents on the left. The RAG assistant retrieves relevant context from ChromaDB to answer with high precision.
      </p>

      <div className="empty-state-suggestions-wrapper">
        <p className="suggestions-headline">Try asking:</p>
        <div className="empty-state-grid">
          {SUGGESTIONS.map((item, i) => (
            <button
              key={i}
              type="button"
              className="suggestion-card"
              onClick={() => onSelectPrompt && onSelectPrompt(item.prompt)}
              aria-label={`Ask: ${item.prompt}`}
            >
              <div className="suggestion-card-header">
                <span className="suggestion-icon">{item.icon}</span>
                <span className="suggestion-title">{item.title}</span>
              </div>
              <p className="suggestion-text">"{item.prompt}"</p>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
