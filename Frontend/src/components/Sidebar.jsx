import React, { useState } from 'react';
import DocumentList from './DocumentList';
import UploadModal from './UploadModal';

export default function Sidebar({ documents, loading, uploading, error, onUpload, onDelete, onClearError }) {
  const [showUpload, setShowUpload] = useState(false);

  const handleUpload = async (files) => {
    await onUpload(files);
    setShowUpload(false);
  };

  return (
    <aside className="sidebar" aria-label="Documents panel">
      <div className="sidebar-header">
        <div className="sidebar-title-group">
          <div className="sidebar-icon-wrapper" aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
              <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
            </svg>
          </div>
          <h2 className="sidebar-title">Documents</h2>
        </div>
        <button
          type="button"
          className="btn btn-primary btn-sm"
          onClick={() => setShowUpload(true)}
          disabled={uploading}
          aria-label="Upload new documents"
        >
          <span aria-hidden="true">+</span> Add Files
        </button>
      </div>

      {error && (
        <div className="sidebar-error" role="alert">
          <span className="error-text">{error}</span>
          <button
            type="button"
            className="error-dismiss"
            onClick={onClearError}
            aria-label="Dismiss error"
          >
            ✕
          </button>
        </div>
      )}

      <DocumentList
        documents={documents}
        loading={loading}
        onDelete={onDelete}
      />

      {showUpload && (
        <UploadModal
          uploading={uploading}
          onUpload={handleUpload}
          onClose={() => setShowUpload(false)}
        />
      )}
    </aside>
  );
}
