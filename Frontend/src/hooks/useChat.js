import { useState, useCallback } from 'react';
import { askQuestion } from '../services/api';

/**
 * Custom hook that manages the chat conversation state.
 *
 * Message shape:
 * {
 *   id: string,
 *   role: 'user' | 'assistant',
 *   content: string,
 *   sources: SourceResponse[],  // only on assistant messages
 *   error: boolean,             // true when this message is an error
 *   timestamp: Date,
 * }
 */
export function useChat() {
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);

  const addMessage = (role, content, sources = [], error = false) => ({
    id: crypto.randomUUID(),
    role,
    content,
    sources,
    error,
    timestamp: new Date(),
  });

  const sendQuestion = useCallback(async (question) => {
    if (!question.trim() || loading) return;

    const trimmedQuestion = question.trim();
    const userMessage = addMessage('user', trimmedQuestion);
    setMessages((prev) => [...prev, userMessage]);
    setLoading(true);

    try {
      const response = await askQuestion(trimmedQuestion);
      const assistantMessage = addMessage(
        'assistant',
        response.answer || '',
        response.sources || []
      );
      setMessages((prev) => [...prev, assistantMessage]);
    } catch (err) {
      const errorText =
        err.response?.data?.message ||
        err.message ||
        'An error occurred while processing your question. Please try again.';
      const errorMessage = addMessage('assistant', errorText, [], true);
      setMessages((prev) => [...prev, errorMessage]);
    } finally {
      setLoading(false);
    }
  }, [loading]);

  const clearMessages = useCallback(() => setMessages([]), []);

  return { messages, loading, sendQuestion, clearMessages };
}

