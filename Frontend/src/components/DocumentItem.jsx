import React, { useState } from 'react';
import DeleteConfirmModal from './DeleteConfirmModal';

const STATUS_CONFIG = {
  UPLOADING:  { icon: '⏳', label: 'Uploading...',  className: 'status-processing' },
  PROCESSING: { icon: '⚙️', label: 'Processing...', className: 'status-processing' },
  COMPLETED:  { icon: '✓',  label: 'Indexed',       className: 'status-completed'  },
  FAILED:     { icon: '✕',  label: 'Failed',        className: 'status-failed'     },
  DELETING:   { icon: '⏳', label: 'Deleting...',   className: 'status-processing' },
};

function formatBytes(bytes) {
  if (!bytes || bytes === 0) return '';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function FileIcon({ name }) {
  const ext = name?.split('.').pop()?.toLowerCase();
  if (ext === 'pdf') return <span className="file-icon file-icon-pdf" aria-label="PDF Document">PDF</span>;
  if (ext === 'docx') return <span className="file-icon file-icon-docx" aria-label="Word Document">DOC</span>;
  return <span className="file-icon file-icon-txt" aria-label="Text Document">TXT</span>;
}

export default function DocumentItem({ document, onDelete }) {
  const [showConfirm, setShowConfirm] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const config = STATUS_CONFIG[document.status] || STATUS_CONFIG.PROCESSING;

  const handleConfirmDelete = async () => {
    setDeleting(true);
    try {
      await onDelete(document.id);
      setShowConfirm(false);
    } catch {
      setDeleting(false);
    }
  };

  const uploadedAt = document.uploadedAt
    ? new Date(document.uploadedAt).toLocaleDateString(undefined, {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    : null;

  const isBusy = document.status === 'DELETING' || document.status === 'PROCESSING' || document.status === 'UPLOADING';

  return (
    <>
      <li className={`doc-item ${isBusy ? 'doc-item-busy' : ''}`} aria-label={`Document: ${document.fileName}`}>
        <div className="doc-item-left">
          <FileIcon name={document.fileName} />
        </div>

        <div className="doc-item-body">
          <div className="doc-item-header">
            <span className="doc-item-name" title={document.fileName}>
              {document.fileName}
            </span>
          </div>

          <div className="doc-item-meta">
            <span className={`doc-item-status ${config.className}`}>
              <span className="status-indicator-dot" />
              {config.label}
            </span>

            {document.status === 'COMPLETED' && document.chunkCount > 0 && (
              <span className="doc-item-pill">
                {document.chunkCount} {document.chunkCount === 1 ? 'chunk' : 'chunks'}
              </span>
            )}

            {document.fileSize > 0 && (
              <span className="doc-item-size">{formatBytes(document.fileSize)}</span>
            )}

            {uploadedAt && (
              <span className="doc-item-date">{uploadedAt}</span>
            )}
          </div>

          {document.status === 'FAILED' && document.errorMessage && (
            <p className="doc-item-error" role="alert">
              {document.errorMessage}
            </p>
          )}
        </div>

        <div className="doc-item-actions">
          <button
            type="button"
            className="doc-item-delete-btn"
            onClick={() => setShowConfirm(true)}
            disabled={isBusy}
            aria-label={`Delete ${document.fileName}`}
            title="Delete document and remove vectors"
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="3 6 5 6 21 6"></polyline>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
              <line x1="10" y1="11" x2="10" y2="17"></line>
              <line x1="14" y1="11" x2="14" y2="17"></line>
            </svg>
          </button>
        </div>
      </li>

      {showConfirm && (
        <DeleteConfirmModal
          document={document}
          deleting={deleting}
          onConfirm={handleConfirmDelete}
          onCancel={() => setShowConfirm(false)}
        />
      )}
    </>
  );
}
