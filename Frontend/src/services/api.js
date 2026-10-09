import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

// ─── Document API ────────────────────────────────────────────────────────────

/**
 * Upload one or more documents.
 * @param {File[]} files
 * @param {function} onUploadProgress - axios progress callback
 * @returns {Promise<DocumentResponse[]>}
 */
export async function uploadDocuments(files, onUploadProgress) {
  const results = [];
  for (const file of files) {
    const formData = new FormData();
    formData.append('file', file);
    const { data } = await axios.post('/api/documents', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress,
    });
    results.push(data);
  }
  return results;
}

/**
 * Fetch all documents.
 * @returns {Promise<DocumentResponse[]>}
 */
export async function getDocuments() {
  const { data } = await api.get('/documents');
  return data;
}

/**
 * Fetch a single document by ID.
 * @param {string} id
 * @returns {Promise<DocumentResponse>}
 */
export async function getDocument(id) {
  const { data } = await api.get(`/documents/${id}`);
  return data;
}

/**
 * Delete a document and its ChromaDB vectors.
 * @param {string} id
 * @returns {Promise<void>}
 */
export async function deleteDocument(id) {
  await api.delete(`/documents/${id}`);
}

// ─── Chat API ────────────────────────────────────────────────────────────────

/**
 * Ask a question against uploaded documents via the RAG pipeline (synchronous).
 * @param {string} question
 * @returns {Promise<ChatResponse>}
 */
export async function askQuestion(question) {
  const { data } = await api.post('/chat', { question });
  return data;
}
