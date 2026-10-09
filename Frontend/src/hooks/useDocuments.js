import { useState, useEffect, useCallback, useRef } from 'react';
import { getDocuments, uploadDocuments, deleteDocument } from '../services/api';

const POLLING_INTERVAL_MS = 3000;

/**
 * Custom hook that manages the document list state.
 *
 * Responsibilities:
 * - Fetch document list on mount
 * - Poll for status updates when any document is UPLOADING or PROCESSING
 * - Expose upload and delete handlers
 */
export function useDocuments() {
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);
  const pollingRef = useRef(null);

  const fetchDocuments = useCallback(async () => {
    try {
      const docs = await getDocuments();
      setDocuments(docs);
      return docs;
    } catch (err) {
      console.error('Failed to fetch documents:', err);
      setError('Failed to load documents. Please check your connection.');
      return [];
    }
  }, []);

  // Start/stop polling based on whether any doc is still in-progress
  const managePolling = useCallback((docs) => {
    const hasInProgress = docs.some(
      (d) => d.status === 'UPLOADING' || d.status === 'PROCESSING'
    );

    if (hasInProgress && !pollingRef.current) {
      pollingRef.current = setInterval(async () => {
        const updated = await fetchDocuments();
        const stillInProgress = updated.some(
          (d) => d.status === 'UPLOADING' || d.status === 'PROCESSING'
        );
        if (!stillInProgress) {
          clearInterval(pollingRef.current);
          pollingRef.current = null;
        }
      }, POLLING_INTERVAL_MS);
    }

    if (!hasInProgress && pollingRef.current) {
      clearInterval(pollingRef.current);
      pollingRef.current = null;
    }
  }, [fetchDocuments]);

  // Initial load
  useEffect(() => {
    setLoading(true);
    fetchDocuments()
      .then(managePolling)
      .finally(() => setLoading(false));

    return () => {
      if (pollingRef.current) clearInterval(pollingRef.current);
    };
  }, [fetchDocuments, managePolling]);

  const upload = useCallback(async (files) => {
    setUploading(true);
    setError(null);
    try {
      await uploadDocuments(files);
      const docs = await fetchDocuments();
      managePolling(docs);
    } catch (err) {
      const message =
        err.response?.data?.message ||
        'Upload failed. Please check the file and try again.';
      setError(message);
      throw err;
    } finally {
      setUploading(false);
    }
  }, [fetchDocuments, managePolling]);

  const remove = useCallback(async (id) => {
    setError(null);
    // Optimistic update
    setDocuments((prev) =>
      prev.map((d) => (d.id === id ? { ...d, status: 'DELETING' } : d))
    );
    try {
      await deleteDocument(id);
      setDocuments((prev) => prev.filter((d) => d.id !== id));
    } catch (err) {
      const message =
        err.response?.data?.message || 'Failed to delete document.';
      setError(message);
      // Re-fetch to restore correct state
      await fetchDocuments();
      throw err;
    }
  }, [fetchDocuments]);

  const clearError = useCallback(() => setError(null), []);

  return {
    documents,
    loading,
    uploading,
    error,
    upload,
    remove,
    refresh: fetchDocuments,
    clearError,
  };
}
