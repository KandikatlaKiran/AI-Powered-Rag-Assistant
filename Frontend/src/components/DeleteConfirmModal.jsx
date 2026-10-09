import React, { useEffect } from 'react';

export default function DeleteConfirmModal({ document, deleting, onConfirm, onCancel }) {
  if (!document) return null;

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && !deleting) {
        onCancel();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [deleting, onCancel]);

  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget && !deleting) {
      onCancel();
    }
  };

  return (
    <div
      className="modal-backdrop"
      onClick={handleBackdropClick}
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-dialog-title"
    >
      <div className="modal modal-danger">
        <div className="modal-header">
          <div className="modal-header-icon-title">
            <span className="modal-warning-badge" aria-hidden="true">⚠️</span>
            <h2 id="delete-dialog-title" className="modal-title">Delete Document</h2>
          </div>
          <button
            className="modal-close"
            onClick={onCancel}
            disabled={deleting}
            aria-label="Close delete confirmation"
          >
            ✕
          </button>
        </div>

        <div className="modal-body">
          <p className="delete-confirm-prompt">
            Are you sure you want to delete <strong className="highlight-filename">{document.fileName}</strong>?
          </p>

          <div className="delete-warning-box">
            <span className="warning-icon" aria-hidden="true">ℹ️</span>
            <div className="warning-content">
              <p className="warning-title">ChromaDB Vector Removal</p>
              <p className="warning-description">
                This will permanently remove the document from the knowledge base and purge all associated vector embeddings. The assistant will no longer retrieve answers from this document.
              </p>
            </div>
          </div>
        </div>

        <div className="modal-footer">
          <button
            type="button"
            className="btn btn-ghost"
            onClick={onCancel}
            disabled={deleting}
          >
            Cancel
          </button>
          <button
            type="button"
            className="btn btn-danger"
            onClick={onConfirm}
            disabled={deleting}
            aria-busy={deleting}
          >
            {deleting ? (
              <>
                <span className="btn-spinner" aria-hidden="true" />
                <span>Deleting from ChromaDB...</span>
              </>
            ) : (
              'Delete Document'
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
