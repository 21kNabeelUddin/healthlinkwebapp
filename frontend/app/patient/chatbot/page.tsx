'use client';

import { useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Sparkles, Send, Loader2, Paperclip, X as CloseIcon } from 'lucide-react';

import { useAuth } from '@/contexts/AuthContext';
import { TopNav } from '@/marketing/layout/TopNav';
import { Sidebar } from '@/marketing/layout/Sidebar';
import { Button } from '@/marketing/ui/button';
import { Badge } from '@/marketing/ui/badge';
import { patientSidebarItems } from '@/app/patient/sidebar-items';

type ChatRole = 'user' | 'assistant';

interface ChatMessage {
  id: string;
  role: ChatRole;
  content: string;
  timestamp: number;
  attachments?: AttachmentPreview[];
}

const quickPrompts = [
  'Summarize my upcoming appointments.',
  'Explain how to prepare for a cardiology follow-up.',
  'What lifestyle tips can help with hypertension?',
  'Help me draft a message to my doctor.',
];

const createId = () => crypto.randomUUID?.() ?? Math.random().toString(36).slice(2);

type AttachmentPreview = {
  id: string;
  name: string;
  type: string;
  size: number;
  url: string;
  file: File;
};

export default function PatientChatbotPage() {
  const router = useRouter();
  const { user, logout, isLoading, isAuthenticated } = useAuth();
  const [input, setInput] = useState('');
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: createId(),
      role: 'assistant',
      content:
        'Hi there! I am the HealthLink+ AI companion. Ask me anything about your appointments, medical history, or healthy habits and I will help.',
      timestamp: Date.now(),
    },
  ]);
  const [isThinking, setIsThinking] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const chatEndRef = useRef<HTMLDivElement | null>(null);
  const [attachments, setAttachments] = useState<AttachmentPreview[]>([]);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.replace('/auth/patient/login');
    }
  }, [isLoading, isAuthenticated, router]);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isThinking]);

  const handleLogout = () => {
    logout();
    router.replace('/');
  };

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (!files) return;
    const newAttachments: AttachmentPreview[] = [];
    Array.from(files).forEach((file) => {
      newAttachments.push({
        id: createId(),
        name: file.name,
        type: file.type,
        size: file.size,
        url: URL.createObjectURL(file),
        file,
      });
    });
    setAttachments((prev) => [...prev, ...newAttachments]);
    event.target.value = '';
  };

  const removeAttachment = (id: string) => {
    setAttachments((prev) => {
      const item = prev.find((file) => file.id === id);
      if (item) {
        URL.revokeObjectURL(item.url);
      }
      return prev.filter((file) => file.id !== id);
    });
  };

  const fileToBase64 = (file: File): Promise<string> =>
    new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        const result = reader.result as string;
        const base64 = result.split(',')[1] ?? '';
        resolve(base64);
      };
      reader.onerror = (err) => reject(err);
      reader.readAsDataURL(file);
    });

  const sendMessage = async (prompt?: string) => {
    const content = (prompt ?? input).trim();
    if ((!content && attachments.length === 0) || isThinking) return;

    const attachmentFiles: AttachmentPreview[] = attachments;

    const userMessage: ChatMessage = {
      id: createId(),
      role: 'user',
      content,
      timestamp: Date.now(),
      attachments: attachmentFiles,
    };

    setMessages((prev) => [...prev, userMessage]);
    setInput('');
    setError(null);
    setIsThinking(true);
    attachmentFiles.forEach((file) => URL.revokeObjectURL(file.url));
    setAttachments([]);

    try {
      const attachmentPayload =
        attachmentFiles.length > 0
          ? await Promise.all(
              attachmentFiles.map(async (preview) => ({
                name: preview.name,
                mimeType: preview.type || 'application/octet-stream',
                data: await fileToBase64(preview.file),
              })),
            )
          : [];

      const response = await fetch('/api/chatbot', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          messages: [...messages, userMessage].map((message) => ({
            role: message.role,
            content: message.content,
          })),
          attachments: attachmentPayload,
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        // If the API returned an error but with a reply message, show that
        const errorMessage = data.reply || data.error || 'Failed to reach the AI assistant.';
        throw new Error(errorMessage);
      }

      const assistantMessage: ChatMessage = {
        id: createId(),
        role: 'assistant',
        content: data.reply ?? 'I am sorry, I was unable to generate a response.',
        timestamp: Date.now(),
      };

      setMessages((prev) => [...prev, assistantMessage]);
    } catch (err: any) {
      console.error('Chatbot error:', err);
      const errorMessage = err.message || 'Something went wrong.';
      setError(errorMessage);
      setMessages((prev) => [
        ...prev,
        {
          id: createId(),
          role: 'assistant',
          content: errorMessage.includes('API key') || errorMessage.includes('configured')
            ? errorMessage
            : 'I ran into an issue while thinking. Please try again, or check your network connection.',
          timestamp: Date.now(),
        },
      ]);
    } finally {
      setIsThinking(false);
    }
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      sendMessage();
    }
  };

  if (isLoading || !isAuthenticated || !user) {
    if (!isLoading && !isAuthenticated) {
      return null;
    }
    return null;
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
      <TopNav
        userName={`${user.firstName ?? ''} ${user.lastName ?? ''}`.trim() || 'Patient'}
        userRole="Patient"
        showPortalLinks={false}
        onLogout={handleLogout}
      />

      <div className="flex">
        <Sidebar items={patientSidebarItems} currentPath="/patient/chatbot" />

        <main className="flex-1 p-4 sm:p-6 lg:p-8">
          <div className="max-w-5xl mx-auto space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-white border border-slate-200 shadow-sm mb-3">
                  <Sparkles className="w-4 h-4 text-teal-500" />
                  <span className="text-xs font-semibold text-slate-600 uppercase tracking-widest">
                    HealthLink+ AI Companion
                  </span>
                </div>
                <h1 className="text-3xl text-slate-900 mb-1">AI Chatbot</h1>
                <p className="text-slate-600 max-w-2xl">
                  Ask medical follow-up questions, request summaries, or get lifestyle coaching. This
                  chatbot uses Google Gemini to provide conversational answers. For emergencies,
                  contact your doctor directly.
                </p>
              </div>
            </div>

            <div className="grid lg:grid-cols-[2fr,1fr] gap-6">
              <div className="bg-white/70 backdrop-blur rounded-3xl border border-slate-100 shadow-lg flex flex-col h-[640px]">
                <div className="flex-1 overflow-y-auto px-6 py-6 space-y-6">
                  {messages.map((message) => (
                    <div
                      key={message.id}
                      className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}
                    >
                      <div
                        className={`max-w-xl rounded-2xl px-4 py-3 text-sm leading-relaxed shadow space-y-2 ${
                          message.role === 'user'
                            ? 'bg-gradient-to-br from-teal-500 to-violet-600 text-white'
                            : 'bg-white border border-slate-100 text-slate-800'
                        }`}
                      >
                        {message.content && <p>{message.content}</p>}
                        {message.attachments && message.attachments.length > 0 && (
                          <div className="space-y-2">
                            {message.attachments.map((file) => (
                              <div
                                key={file.id}
                                className={`rounded-xl border ${
                                  message.role === 'user'
                                    ? 'border-white/30 bg-white/10'
                                    : 'border-slate-200 bg-slate-50'
                                } p-2`}
                              >
                                <p className="text-xs font-medium mb-1 flex items-center gap-2">
                                  <Paperclip className="w-3 h-3" />
                                  {file.name}
                                </p>
                                <img
                                  src={file.url}
                                  alt={file.name}
                                  className="max-h-48 rounded-lg w-auto object-cover"
                                />
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>
                  ))}

                  {isThinking && (
                    <div className="flex items-center gap-2 text-sm text-slate-500">
                      <Loader2 className="w-4 h-4 animate-spin" />
                      Thinking...
                    </div>
                  )}
                  <div ref={chatEndRef} />
                </div>

                <div className="border-t border-slate-100 bg-white/80 backdrop-blur rounded-b-3xl px-4 py-4">
                  {error && <p className="text-sm text-red-500 mb-2">{error}</p>}
                  <div className="flex flex-col gap-3">
                    {attachments.length > 0 && (
                      <div className="flex flex-wrap gap-2">
                        {attachments.map((file) => (
                          <div
                            key={file.id}
                            className="flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2 text-xs text-slate-600"
                          >
                            <Paperclip className="w-3 h-3" />
                            <span className="truncate max-w-[120px]">{file.name}</span>
                            <button
                              className="text-slate-400 hover:text-slate-600"
                              onClick={() => removeAttachment(file.id)}
                              type="button"
                            >
                              <CloseIcon className="w-3 h-3" />
                            </button>
                          </div>
                        ))}
                      </div>
                    )}
                    <div className="flex gap-3">
                    <textarea
                      value={input}
                      onChange={(e) => setInput(e.target.value)}
                      onKeyDown={handleKeyDown}
                      placeholder="Ask about appointments, medications, or healthy habits..."
                      className="flex-1 resize-none rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-teal-500"
                      rows={2}
                    />
                      <div className="flex flex-col gap-2">
                        <Button
                          type="button"
                          variant="outline"
                          className="h-12 w-12 rounded-2xl border-slate-200 text-slate-600"
                          onClick={() => fileInputRef.current?.click()}
                        >
                          <Paperclip className="w-4 h-4" />
                        </Button>
                        <input
                          ref={fileInputRef}
                          type="file"
                          accept="image/*"
                          multiple
                          className="hidden"
                          onChange={handleFileChange}
                        />
                        <Button
                          onClick={() => sendMessage()}
                          disabled={isThinking || (!input.trim() && attachments.length === 0)}
                          className="h-12 w-12 rounded-2xl bg-gradient-to-br from-teal-500 to-violet-600 hover:from-teal-600 hover:to-violet-700"
                        >
                          {isThinking ? (
                            <Loader2 className="w-4 h-4 animate-spin" />
                          ) : (
                            <Send className="w-4 h-4" />
                          )}
                        </Button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div className="space-y-4">
                <div className="bg-white rounded-3xl border border-slate-100 shadow p-6">
                  <h2 className="text-lg font-semibold text-slate-900 mb-4">Suggested Prompts</h2>
                  <div className="space-y-3">
                    {quickPrompts.map((prompt) => (
                      <button
                        key={prompt}
                        onClick={() => sendMessage(prompt)}
                        className="w-full text-left px-4 py-3 rounded-2xl border border-slate-200 hover:border-teal-400 hover:bg-teal-50 transition text-sm text-slate-600"
                      >
                        {prompt}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="bg-white rounded-3xl border border-slate-100 shadow p-6 space-y-4">
                  <div className="flex items-center gap-3">
                    <Badge className="bg-slate-900 text-white">Beta</Badge>
                    <p className="text-xs text-slate-500 uppercase tracking-[0.3em]">
                      Responsible AI
                    </p>
                  </div>
                  <p className="text-sm text-slate-600">
                    This chatbot uses Gemini to deliver conversational insights. It is not a
                    replacement for medical professionals. Double-check critical information with
                    your doctor.
                  </p>
                  <Link href="/patient/medical-history" className="text-sm text-teal-600 font-semibold">
                    View my records ➜
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}

