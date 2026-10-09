import React, { useState, useRef, useCallback, useEffect } from 'react';

const ALLOWED_TYPES = [
  'application/pdf',
  'text/plain',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
];
const ALLOWED_EXTENSIONS = ['pdf', 'docx', 'txt'];
const MAX_SIZE_MB = 50;
const MAX_SIZE_BYTES = MAX_SIZE_MB * 1024 * 1024;

function getExtension(name) {
  return name.split('.').pop()?.toLowerCase() || '';
}

function isAllowedFile(file) {
  const ext = getExtension(file.name);
  const typeOk = ALLOWED_TYPES.includes(file.type) || file.type.startsWith('text/');
  return ALLOWED_EXTENSIONS.includes(ext) && typeOk;
}

function formatBytes(bytes) {
  if (!bytes || bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

const UPLOAD_STAGES = [
  'Reading & validating files...',
  'Extracting text & chunking document...',
  'Generating embeddings via nomic-embed-text...',
  'Storing vectors in ChromaDB...',
  'Finalizing document registry...',
];

export default function UploadModal({ uploading, onUpload, onClose }) {
  const [files, setFiles] = useState([]);
  const [dragOver, setDragOver] = useState(false);
  const [validationError, setValidationError] = useState('');
  const [uploadError, setUploadError] = useState('');
  const [stageIndex, setStageIndex] = useState(0);
  const inputRef = useRef(null);

  useEffect(() => {
    let interval = null;
    if (uploading) {
      setStageIndex(0);
      interval = setInterval(() => {
        setStageIndex((prev) => (prev < UPLOAD_STAGES.length - 1 ? prev + 1 : prev));
      }, 1400);
    } else {
      setStageIndex(0);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [uploading]);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && !uploading) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [uploading, onClose]);

  const addFiles = (incoming) => {
    setValidationError('');
    setUploadError('');
    const rejected = [];
    const valid = Array.from(incoming).filter((f) => {
      if (!isAllowedFile(f)) {
        rejected.push(f.name);
        return false;
      }
      if (f.size > MAX_SIZE_BYTES) {
        rejected.push(`${f.name} (exceeds ${MAX_SIZE_MB}MB)`);
        return false;
      }
      return true;
    });

    if (rejected.length > 0) {
      setValidationError(`Unsupported or oversized files skipped: ${rejected.join(', ')}`);
    }

    setFiles((prev) => {
      const existingNames = new Set(prev.map((f) => f.name));
      const deduplicated = valid.filter((f) => !existingNames.has(f.name));
      return [...prev, ...deduplicated];
    });
  };

  const removeFile = (index) => {
    setFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const handleDrop = useCallback((e) => {
    e.preventDefault();
    setDragOver(false);
    if (uploading) return;
    addFiles(e.dataTransfer.files);
  }, [uploading]);

  const handleDragOver = (e) => {
    e.preventDefault();
    if (!uploading) setDragOver(true);
  };

  const handleDragLeave = () => setDragOver(false);

  const handleInputChange = (e) => {
    if (e.target.files && e.target.files.length) {
      addFiles(e.target.files);
    }
    e.target.value = '';
  };

  const handleUpload = async () => {
    if (files.length === 0 || uploading) return;
    setUploadError('');
    try {
      await onUpload(files);
    } catch (err) {
      const msg = err?.response?.data?.message || 'Upload failed. Please check the backend connection and try again.';
      setUploadError(msg);
    }
  };

  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget && !uploading) onClose();
  };

  return (
    <div
      className="modal-backdrop"
      onClick={handleBackdropClick}
      role="dialog"
      aria-modal="true"
      aria-labelledby="upload-title"
    >
      <div className="modal">
        <div className="modal-header">
          <div className="modal-header-icon-title">
            <span className="modal-title-icon" aria-hidden="true">📤</span>
            <h2 id="upload-title" className="modal-title">Upload Documents</h2>
          </div>
          <button
            className="modal-close"
            onClick={onClose}
            disabled={uploading}
            aria-label="Close upload dialog"
          >
            ✕
          </button>
        </div>

        <div className="modal-body">
          {/* Drop zone */}
          <div
            className={`dropzone ${dragOver ? 'dropzone-active' : ''} ${uploading ? 'dropzone-disabled' : ''}`}
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onClick={() => !uploading && inputRef.current?.click()}
            role="button"
            tabIndex={uploading ? -1 : 0}
            aria-label="Drop files here or click to select"
            onKeyDown={(e) => e.key === 'Enter' && !uploading && inputRef.current?.click()}
          >
            <div className="dropzone-icon-wrapper" aria-hidden="true">
              <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="17 8 12 3 7 8" />
                <line x1="12" y1="3" x2="12" y2="15" />
              </svg>
            </div>
            <p className="dropzone-primary">Drag & drop documents here</p>
            <p className="dropzone-secondary">or click to browse files</p>
            <div className="dropzone-tags">
              <span className="file-badge">PDF</span>
              <span className="file-badge">DOCX</span>
              <span className="file-badge">TXT</span>
            </div>
            <p className="dropzone-hint">Max {MAX_SIZE_MB}MB per file • Embeddings created automatically</p>
          </div>

          <input
            ref={inputRef}
            type="file"
            multiple
            accept=".pdf,.docx,.txt,application/pdf,text/plain,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            onChange={handleInputChange}
            style={{ display: 'none' }}
            aria-hidden="true"
          />

          {/* Validation errors */}
          {validationError && (
            <div className="upload-validation-error" role="alert">
              <span className="error-icon" aria-hidden="true">⚠️</span>
              <span>{validationError}</span>
            </div>
          )}

          {/* Selected File list */}
          {files.length > 0 && !uploading && (
            <div className="upload-files-section">
              <div className="upload-files-header">
                <span>Selected Files ({files.length})</span>
                <button
                  type="button"
                  className="clear-all-btn"
                  onClick={() => setFiles([])}
                >
                  Clear All
                </button>
              </div>
              <ul className="upload-file-list" aria-label="Files to upload">
                {files.map((f, i) => (
                  <li key={i} className="upload-file-item">
                    <span className="file-type-pill">{getExtension(f.name).toUpperCase()}</span>
                    <span className="upload-file-name" title={f.name}>{f.name}</span>
                    <span className="upload-file-size">{formatBytes(f.size)}</span>
                    <button
                      type="button"
                      className="upload-file-remove"
                      onClick={() => removeFile(i)}
                      aria-label={`Remove ${f.name}`}
                    >
                      ✕
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* Upload Progress & Stage Stepper */}
          {uploading && (
            <div className="upload-progress-box" aria-live="polite">
              <div className="progress-status-row">
                <span className="btn-spinner" aria-hidden="true" />
                <span className="progress-status-text">{UPLOAD_STAGES[stageIndex]}</span>
              </div>
              <div className="progress-bar-track">
                <div
                  className="progress-bar-fill"
                  style={{ width: `${((stageIndex + 1) / UPLOAD_STAGES.length) * 100}%` }}
                />
              </div>
              <p className="progress-hint">Vector embeddings are being indexed into ChromaDB with nomic-embed-text.</p>
            </div>
          )}

          {/* Upload error */}
          {uploadError && (
            <div className="upload-error" role="alert">
              <span className="error-icon" aria-hidden="true">❌</span>
              <span>{uploadError}</span>
            </div>
          )}
        </div>

        <div className="modal-footer">
          <button
            type="button"
            className="btn btn-ghost"
            onClick={onClose}
            disabled={uploading}
          >
            Cancel
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={handleUpload}
            disabled={files.length === 0 || uploading}
            aria-busy={uploading}
          >
            {uploading ? (
              <>
                <span className="btn-spinner" aria-hidden="true" />
                <span>Processing...</span>
              </>
            ) : (
              `Ingest ${files.length > 0 ? `(${files.length})` : 'Documents'}`
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
