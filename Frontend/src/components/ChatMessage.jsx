import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

export default function ChatMessage({ message }) {
  const isUser = message.role === 'user';
  const hasSources = message.sources && message.sources.length > 0;

  return (
    <div
      className={`chat-message ${isUser ? 'chat-message-user' : 'chat-message-assistant'} ${message.error ? 'chat-message-error' : ''}`}
      role="article"
      aria-label={`${isUser ? 'You' : 'Assistant'}: ${message.content.slice(0, 80)}`}
    >
      <div className="chat-message-avatar" aria-hidden="true">
        {isUser ? (
          <span className="avatar-user">👤</span>
        ) : message.error ? (
          <span className="avatar-error">⚠️</span>
        ) : (
          <span className="avatar-ai">⚡</span>
        )}
      </div>

      <div className="chat-message-body">
        <div className="chat-message-role-row">
          <span className="chat-message-role">{isUser ? 'You' : 'RAG Assistant'}</span>
          <time
            className="chat-message-time"
            dateTime={message.timestamp.toISOString()}
            aria-label={`Sent at ${message.timestamp.toLocaleTimeString()}`}
          >
            {message.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </time>
        </div>

        <div className="chat-message-content">
          {isUser ? (
            <p className="chat-paragraph">{message.content}</p>
          ) : message.error ? (
            <p className="chat-paragraph">{message.content}</p>
          ) : (
            <ReactMarkdown remarkPlugins={[remarkGfm]}>
              {message.content}
            </ReactMarkdown>
          )}
        </div>

        {hasSources && (
          <div className="chat-message-sources">
            <div className="sources-header">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
                <polyline points="10 9 9 9 8 9"></polyline>
              </svg>
              <span className="sources-label">Retrieved Sources</span>
            </div>
            <div className="sources-list" aria-label="Source documents">
              {message.sources.map((source, i) => (
                <div key={i} className="source-chip" title={source.snippet || source.documentName}>
                  <span className="source-chip-icon">📄</span>
                  <span className="source-chip-name">{source.documentName}</span>
                  {source.chunkIndex !== null && source.chunkIndex !== undefined && (
                    <span className="source-chip-chunk">chunk #{source.chunkIndex + 1}</span>
                  )}
                  {source.pageNumber && (
                    <span className="source-chip-page">p. {source.pageNumber}</span>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
