import React, { useState, useRef, useCallback } from 'react';

export default function ChatInput({ onSend, disabled }) {
  const [value, setValue] = useState('');
  const textareaRef = useRef(null);

  const handleSend = useCallback(() => {
    const trimmed = value.trim();
    if (!trimmed || disabled) return;
    onSend(trimmed);
    setValue('');
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.focus();
    }
  }, [value, disabled, onSend]);

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleChange = (e) => {
    setValue(e.target.value);
    // Auto-adjust height up to max
    e.target.style.height = 'auto';
    e.target.style.height = `${Math.min(e.target.scrollHeight, 140)}px`;
  };

  return (
    <div className="chat-input-wrapper">
      <div className="chat-input-container">
        <label htmlFor="chat-input" className="sr-only">
          Ask a question about your documents
        </label>
        <textarea
          id="chat-input"
          ref={textareaRef}
          className="chat-input-field"
          value={value}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          placeholder="Ask a question about your uploaded documents (e.g., 'Which documents do you have?')..."
          disabled={disabled}
          rows={1}
          aria-label="Message input"
        />

        <div className="chat-input-actions">
          {value.trim().length > 0 && !disabled && (
            <button
              type="button"
              className="chat-clear-btn"
              onClick={() => {
                setValue('');
                if (textareaRef.current) textareaRef.current.style.height = 'auto';
              }}
              title="Clear input"
              aria-label="Clear input"
            >
              ✕
            </button>
          )}

          <button
            type="button"
            className="btn btn-primary chat-send-btn"
            onClick={handleSend}
            disabled={!value.trim() || disabled}
            aria-label="Send message"
            aria-busy={disabled}
            title={disabled ? 'Generating response...' : 'Send message (Enter)'}
          >
            {disabled ? (
              <span className="btn-spinner" aria-hidden="true" />
            ) : (
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="22" y1="2" x2="11" y2="13"></line>
                <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
              </svg>
            )}
          </button>
        </div>
      </div>
      <div className="chat-input-footer">
        <span className="input-shortcut-hint">
          <kbd>Enter</kbd> to send • <kbd>Shift</kbd> + <kbd>Enter</kbd> for new line
        </span>
        <span className="input-model-hint">
          Powered by <strong>llama3.2:3b</strong>
        </span>
      </div>
    </div>
  );
}
