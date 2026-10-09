import React, { useEffect, useRef } from 'react';
import ChatMessage from './ChatMessage';
import LoadingIndicator from './LoadingIndicator';
import EmptyState from './EmptyState';

export default function ChatWindow({ messages, loading, onSelectPrompt }) {
  const bottomRef = useRef(null);

  // Auto-scroll to latest message
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  return (
    <div className="chat-window" role="log" aria-label="Conversation" aria-live="polite">
      {messages.length === 0 && !loading ? (
        <EmptyState onSelectPrompt={onSelectPrompt} />
      ) : (
        <div className="chat-messages">
          {messages.map((msg) => (
            <ChatMessage key={msg.id} message={msg} />
          ))}
          {loading && <LoadingIndicator />}
          <div ref={bottomRef} aria-hidden="true" />
        </div>
      )}
    </div>
  );
}
