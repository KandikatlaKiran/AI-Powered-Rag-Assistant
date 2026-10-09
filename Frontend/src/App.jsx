import React from 'react';
import Header from './components/Header';
import ChatPage from './pages/ChatPage';
import { useDocuments } from './hooks/useDocuments';
import './App.css';

export default function App() {
  const documentState = useDocuments();
  const completedDocCount = documentState.documents.filter(d => d.status === 'COMPLETED').length;

  return (
    <div className="app-layout">
      <Header documentCount={completedDocCount} />
      <ChatPage documentState={documentState} />
    </div>
  );
}
