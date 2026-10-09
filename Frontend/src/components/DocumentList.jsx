import React from 'react';
import DocumentItem from './DocumentItem';

export default function DocumentList({ documents, loading, onDelete }) {
  if (loading && (!documents || documents.length === 0)) {
    return (
      <div className="doc-list-state">
        <div className="spinner" aria-label="Loading documents" />
        <p className="doc-list-state-text">Loading knowledge base...</p>
      </div>
    );
  }

  if (!documents || documents.length === 0) {
    return (
      <div className="doc-list-state doc-list-empty">
        <div className="empty-folder-icon" aria-hidden="true">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
            <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path>
          </svg>
        </div>
        <p className="empty-primary">No documents indexed</p>
        <p className="doc-list-empty-hint">
          Upload PDF, Word, or TXT documents to power the RAG assistant.
        </p>
      </div>
    );
  }

  return (
    <div className="doc-list-container">
      <div className="doc-list-toolbar">
        <span className="doc-count-label">
          {documents.length} {documents.length === 1 ? 'Document' : 'Documents'}
        </span>
      </div>
      <ul className="doc-list" role="list" aria-label="Uploaded documents">
        {documents.map((doc) => (
          <DocumentItem key={doc.id} document={doc} onDelete={onDelete} />
        ))}
      </ul>
    </div>
  );
}
