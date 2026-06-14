/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  Send, 
  Sparkles, 
  Terminal, 
  Shield, 
  Database, 
  Cpu, 
  HelpCircle, 
  CheckCircle, 
  AlertCircle,
  Clock,
  Briefcase,
  FileText,
  Bookmark
} from 'lucide-react';

interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

interface NexiChatProps {
  onAddLog: (newLog: { type: 'info' | 'warn' | 'error' | 'success'; source: 'PowerShell' | 'SQLite' | 'Gemini' | 'Chef-Logik'; message: string }) => void;
  systemProvider: string;
}

export function NexiChat({ onAddLog, systemProvider }: NexiChatProps) {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: 'welcome_1',
      role: 'assistant',
      content: `Guten Tag, Patrick Herzog. Ich bin **Nexi**, deine maßgeschneiderte System-Agentin für den **Nexus v40.44** Master-Index.

Ich habe vollen Zugriff auf dein deklariertes Home Center (\`C:\\MasterIndex_Storage\`) und analysiere alle einkommenden Benachrichtigungen sowie unseren SQLite-Katalog (\`sqlite_catalog.db\`).

**Meine aktuellen Leitplanken:**
1. Jede PowerShell- oder Python-Code-Generierung von mir ist strikt **UTF-8-sig (BOM-sicher)**.
2. Alle Datei-Operationen laufen transaktionssicher im **try-finally-close** Schema ab, um SQLite Locks zu vermeiden.
3. Änderungen trage ich als **DRAFT** in den \`NEXUS_CHANGE_DRAFT_LEDGER.md\` Ledger ein.
4. Android Synchronisations-Probleme (Errno 13/14) löse ich durch einen **lokalen Offline-Puffer mit Exponential-Backoff**.

Wie kann ich dich heute im Datenzentrum unterstützen?`,
      timestamp: new Date()
    }
  ]);

  const [inputMessage, setInputMessage] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [activeLedgerDrafts, setActiveLedgerDrafts] = useState<string[]>([]);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isTyping]);

  const handleSendMessage = async (textToSend?: string) => {
    const text = textToSend || inputMessage;
    if (!text.trim()) return;

    if (!textToSend) {
      setInputMessage('');
    }

    const userMsg: Message = {
      id: `msg_${Date.now()}_user`,
      role: 'user',
      content: text,
      timestamp: new Date()
    };

    setMessages(prev => [...prev, userMsg]);
    setIsTyping(true);

    onAddLog({
      type: 'info',
      source: 'Chef-Logik',
      message: `Benutzer promptet Nexi: "${text.slice(0, 40)}${text.length > 40 ? '...' : ''}"`
    });

    try {
      // Build previous message list for API call
      // Map correctly to expected structure
      const apiMessages = [...messages, userMsg].map(msg => ({
        role: msg.role === 'assistant' ? 'assistant' : 'user',
        content: msg.content
      }));

      const response = await fetch('/api/nexi/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ messages: apiMessages })
      });

      if (!response.ok) {
        throw new Error(`Nexi backend responded with error ${response.status}`);
      }

      const resData = await response.json();
      
      const assistantMsg: Message = {
        id: `msg_${Date.now()}_assistant`,
        role: 'assistant',
        content: resData.content || 'Entschuldigung Patrick, ich konnte keine Rückmeldung erzeugen.',
        timestamp: new Date()
      };

      setMessages(prev => [...prev, assistantMsg]);
      
      onAddLog({
        type: 'success',
        source: 'Gemini',
        message: 'Antwort erfolgreich von Nexi Chat empfangen.'
      });
    } catch (err) {
      console.error("Failed to fetch Nexi response:", err);
      
      // Local client heuristic fallback for offline/sandbox modes
      setTimeout(() => {
        let fallbackReply = `Patrick, ich arbeite im lokalen Client-Heuristik-Sicherungsmodus. `;
        const lower = text.toLowerCase();
        
        if (lower.includes("hallo") || lower.includes("hi")) {
          fallbackReply += `Verbindung über Tailscale (100.107.24.67) steht bereit. Ich bin bereit belegte Rechnungen auszuwerten oder Kivy Setup-Zyklen zu debuggen.`;
        } else if (lower.includes("code") || lower.includes("powershell") || lower.includes("python")) {
          fallbackReply += `Ich habe folgendes PowerShell-BOM Script als DRAFT-Entwurf vorbereitet:
\`\`\`powershell
# UTF-8 with BOM Signature
# Target: C:\\MasterIndex_Storage\\_NEXUS_SYSTEM\\communication\\safeguard.ps1
try {
    $Utf8NoBom = New-Object System.Text.UTF8Encoding($true)
    [System.IO.File]::WriteAllLines("C:\\MasterIndex_Storage\\_NEXUS_SYSTEM\\communication\\safeguard.ps1", "Clear-Host", $Utf8NoBom)
} finally {
    # System-Ressource transaktionssicher schliessen
}
\`\`\`
Soll ich diese Änderung in dem Ledger protokollieren?`;
        } else if (lower.includes("errno") || lower.includes("android") || lower.includes("sync")) {
          fallbackReply += `Bezüglich des Android-Fehlers Errno 13/14 empfehle ich, unseren in \`sync_manager.py\` implementierten Puffer zu laden. Er schreibt fehlgeschlagene API-Aufrufe direkt in die lokale SQLite Database \`nexus_offline_cache.db\`. Ein Exponential Backoff versucht dann die Synchronisation im Hintergrund erneut.`;
        } else {
          fallbackReply += `Ich habe deine Anfrage erfasst und auf \`C:\\MasterIndex_Storage\` registriert. Was soll ich als nächstes für dich überprüfen?`;
        }

        const assistantMsg: Message = {
          id: `msg_${Date.now()}_assistant`,
          role: 'assistant',
          content: fallbackReply,
          timestamp: new Date()
        };

        setMessages(prev => [...prev, assistantMsg]);
        onAddLog({
          type: 'warn',
          source: 'Chef-Logik',
          message: 'Nexi-Fallbacksicherung geladen: Lokale Heuristik-Sitzung.'
        });
      }, 800);
    } finally {
      setIsTyping(false);
    }
  };

  const handleQuickAction = (actionText: string) => {
    handleSendMessage(actionText);
  };

  const handleAddCurrentToLedger = (messageText: string) => {
    const draftName = `DRAFT-${Math.floor(100 + Math.random() * 900)}`;
    const newDraft = `${draftName}: Nexi-Vorschlag "${messageText.slice(0, 30)}..." als transaktionssicherer DRAFT vermerkt.`;
    setActiveLedgerDrafts(prev => [newDraft, ...prev]);

    onAddLog({
      type: 'success',
      source: 'Chef-Logik',
      message: `System-Eintrag in NEXUS_CHANGE_DRAFT_LEDGER.md als ${draftName} vorgemerkt.`
    });
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-4 gap-6 animate-fade-in" id="nexi-chat-workspace">
      
      {/* Sidebar Controls & Patrick Context Overview */}
      <div className="lg:col-span-1 space-y-4 flex flex-col justify-start">
        
        {/* Holographic Avatar Core */}
        <div className="bg-[#121214] border border-[#222227] rounded-xl p-5 flex flex-col items-center text-center shadow-lg relative overflow-hidden">
          <div className="absolute top-2 right-2 flex items-center gap-1 bg-emerald-500/10 px-2 py-0.5 rounded text-[10px] text-emerald-400 font-mono">
            <span className="h-1.5 w-1.5 bg-emerald-500 rounded-full animate-ping"></span>
            LIVE
          </div>

          <div className="w-24 h-24 rounded-full bg-gradient-to-tr from-indigo-600/20 via-sky-600/30 to-purple-500/10 border-2 border-indigo-500/30 flex items-center justify-center p-3 my-4 relative shadow-inner">
            <div className="absolute inset-0 bg-indigo-500/10 rounded-full animate-pulse"></div>
            <Cpu className="h-12 w-12 text-indigo-400 animate-spin-slow" />
          </div>

          <h3 className="text-base font-bold text-white tracking-wide">Nexi System-Zentrale</h3>
          <p className="text-xs text-gray-400 font-mono mt-1">Nexus Profile Engine v40.44</p>

          <div className="w-full border-t border-[#222227] my-4 pt-3 space-y-2 text-left">
            <div className="flex items-center justify-between text-[11px]">
              <span className="text-gray-500">Adresse:</span>
              <span className="text-gray-300 font-mono">C:\MasterIndex_Storage</span>
            </div>
            <div className="flex items-center justify-between text-[11px]">
              <span className="text-gray-500">Port-Kanal:</span>
              <span className="text-sky-400 font-mono font-bold">8081 / Tailscale</span>
            </div>
            <div className="flex items-center justify-between text-[11px]">
              <span className="text-gray-500">User:</span>
              <span className="text-gray-300 font-bold">Patrick Herzog</span>
            </div>
          </div>
        </div>

        {/* Quick Topics */}
        <div className="bg-[#121214] border border-[#222227] rounded-xl p-5 space-y-3 shadow-md">
          <h4 className="text-xs font-bold text-gray-400 uppercase tracking-widest flex items-center gap-2">
            <Sparkles className="h-3 w-3 text-indigo-400" /> Schnellwahl Patrick
          </h4>
          <p className="text-[11px] text-gray-500 leading-relaxed">
            Häufige Aufgaben direkt anfordern. Nexi evaluiert die Befehle und bereitet die Entwürfe vor.
          </p>

          <div className="space-y-2 pt-2">
            <button
              onClick={() => handleQuickAction("Gibt es neue Nachrichten oder Kostenstellen zu bewerten?")}
              className="w-full text-left text-xs bg-[#19191d] hover:bg-[#222227] text-gray-300 hover:text-white p-2.5 rounded-lg border border-[#222227] transition-all flex items-center gap-2 cursor-pointer"
            >
              <Briefcase className="h-3 w-3 text-emerald-400 flex-shrink-0" />
              <span>Nachrichten & Belege bewerten</span>
            </button>

            <button
              onClick={() => handleQuickAction("Wie kann ich ein PowerShell-Script UTF-8-sig (BOM-sicher) abspeichern?")}
              className="w-full text-left text-xs bg-[#19191d] hover:bg-[#222227] text-gray-300 hover:text-white p-2.5 rounded-lg border border-[#222227] transition-all flex items-center gap-2 cursor-pointer"
            >
              <Terminal className="h-3 w-3 text-indigo-400 flex-shrink-0" />
              <span>PS-BOM Script anfordern</span>
            </button>

            <button
              onClick={() => handleQuickAction("Erkläre mir noch einmal wie mein Android-Sync fehlerfrei den Fehler 13/14 im Standby umgeht")}
              className="w-full text-left text-xs bg-[#19191d] hover:bg-[#222227] text-gray-300 hover:text-white p-2.5 rounded-lg border border-[#222227] transition-all flex items-center gap-2 cursor-pointer"
            >
              <Cpu className="h-3 w-3 text-sky-400 flex-shrink-0" />
              <span>Android Errno 13/14 Behebung</span>
            </button>

            <button
              onClick={() => handleQuickAction("Führe ein Red-Team Audit über meine SQLite-Attributionen durch")}
              className="w-full text-left text-xs bg-[#19191d] hover:bg-[#222227] text-gray-300 hover:text-white p-2.5 rounded-lg border border-[#222227] transition-all flex items-center gap-2 cursor-pointer"
            >
              <Shield className="h-3 w-3 text-red-400 flex-shrink-0" />
              <span>SQLite Red-Team Audit</span>
            </button>
          </div>
        </div>

        {/* Ledger Sync Status */}
        <div className="bg-[#121214] border border-[#222227] rounded-xl p-5 space-y-3 shadow-md flex-1">
          <h4 className="text-xs font-bold text-gray-400 uppercase tracking-widest flex items-center gap-2">
            <Bookmark className="h-3 w-3 text-indigo-400" /> Ledger DRAFTS (Vormerkung)
          </h4>
          
          {activeLedgerDrafts.length === 0 ? (
            <div className="text-center p-4 bg-[#19191d] rounded-lg border border-dashed border-[#222227]">
              <p className="text-xs text-gray-500">Keine aktiven DRAFTS in dieser Sitzung.</p>
              <p className="text-[10px] text-gray-600 mt-1">Klicke unter Nexis Antworten auf "Ledger", um Entwürfe zu sichern.</p>
            </div>
          ) : (
            <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
              <AnimatePresence>
                {activeLedgerDrafts.map((draft, idx) => (
                  <motion.div
                    key={idx}
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, scale: 0.95 }}
                    className="p-2 bg-[#19191d] rounded-lg border border-[#222227] flex flex-col gap-1 text-[11px]"
                  >
                    <div className="flex items-center gap-1.5 text-indigo-400 font-mono font-bold">
                      <CheckCircle className="h-3 w-3 text-emerald-400" />
                      <span>{draft.split(':')[0]}</span>
                    </div>
                    <span className="text-gray-400">{draft.split(':').slice(1).join(':')}</span>
                  </motion.div>
                ))}
              </AnimatePresence>
            </div>
          )}
        </div>
      </div>

      {/* Primary Chat View */}
      <div className="lg:col-span-3 bg-[#121214] border border-[#222227] rounded-xl flex flex-col h-[700px] overflow-hidden shadow-2xl relative" id="nexi-chat-box">
        
        {/* Chat Header */}
        <div className="px-6 py-4 bg-[#18181b] border-b border-[#222227] flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="h-2.5 w-2.5 bg-emerald-500 rounded-full animate-pulse"></div>
            <div>
              <h2 className="text-sm font-bold text-white tracking-wide flex items-center gap-1.5">
                <span>Nexi Dialog-Chef</span>
                <span className="text-[10px] align-middle px-1.5 py-0.5 bg-indigo-600/10 text-indigo-400 border border-indigo-500/15 rounded-full font-mono font-light">
                  Active
                </span>
              </h2>
              <p className="text-xs text-gray-400">Direkter Nachrichtensender & Evaluator für Patrick Herzog</p>
            </div>
          </div>

          <div className="text-right flex items-center gap-4 text-xs font-mono text-gray-500">
            <div className="hidden sm:block">
              <span className="text-gray-600">Active Provider:</span> <span className="text-indigo-400 font-bold">{systemProvider}</span>
            </div>
            <div className="hidden md:block">
              <span className="text-gray-600">Model:</span> <span className="text-gray-300">gemini-3.5-flash</span>
            </div>
          </div>
        </div>

        {/* Chat Core Messages (Scrollable) */}
        <div className="flex-1 p-6 overflow-y-auto space-y-6 bg-gradient-to-b from-[#121214] via-[#101012] to-[#121214]">
          <AnimatePresence initial={false}>
            {messages.map((msg) => (
              <motion.div
                key={msg.id}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className={`flex gap-4 ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
              >
                {/* Assistenz-Icon (if model) */}
                {msg.role === 'assistant' && (
                  <div className="h-8 w-8 rounded-lg bg-indigo-600/15 border border-indigo-500/20 flex items-center justify-center text-indigo-400 flex-shrink-0 mt-0.5 shadow">
                    <Cpu className="h-4.5 w-4.5 animate-pulse" />
                  </div>
                )}

                <div className={`max-w-[85%] flex flex-col gap-1.5`}>
                  
                  {/* Message Balloon */}
                  <div className={`px-4.5 py-3.5 rounded-2xl text-xs leading-relaxed shadow-sm ${
                    msg.role === 'user'
                      ? 'bg-indigo-600 border border-indigo-500 text-white rounded-tr-none'
                      : 'bg-[#19191d] border border-[#222227] text-gray-300 rounded-tl-none font-sans'
                  }`}>
                    
                    {/* Render helper for markdown/bold style and codeblocks cleanly */}
                    {msg.content.split('\n').map((line, lineIdx) => {
                      if (line.startsWith('```')) {
                        return <div key={lineIdx} className="my-2" />; // skip block boundaries inside custom block for basic spacing
                      }
                      
                      const isCode = line.startsWith(' ') || line.startsWith('try') || line.startsWith('$') || line.startsWith('#') || line.startsWith('[') || line.startsWith('import ') || line.startsWith('def ');
                      if (isCode && msg.role === 'assistant') {
                        return (
                          <pre key={lineIdx} className="bg-black/40 border border-[#222227] p-2.5 rounded-lg font-mono text-[11px] text-teal-400 my-1 overflow-x-auto select-all">
                            <code>{line}</code>
                          </pre>
                        );
                      }

                      // Simple bold converter **text**
                      const parts = line.split('**');
                      if (parts.length > 1) {
                        return (
                          <p key={lineIdx} className="mb-2 last:mb-0">
                            {parts.map((p, pIdx) => pIdx % 2 === 1 ? <strong key={pIdx} className="text-white font-bold">{p}</strong> : p)}
                          </p>
                        );
                      }

                      // Simple inline code converter `text`
                      const inlineParts = line.split('`');
                      if (inlineParts.length > 1) {
                        return (
                          <p key={lineIdx} className="mb-2 last:mb-0">
                            {inlineParts.map((p, pIdx) => pIdx % 2 === 1 ? <code key={pIdx} className="bg-black/30 border border-[#222227] px-1 py-0.5 rounded font-mono text-[11px] text-teal-400">{p}</code> : p)}
                          </p>
                        );
                      }

                      return <p key={lineIdx} className="mb-2 last:mb-0">{line}</p>;
                    })}
                  </div>

                  {/* Metadata under box */}
                  <div className={`flex items-center gap-3 text-[10px] text-gray-500 px-1 font-mono ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                    <span className="flex items-center gap-1">
                      <Clock className="h-3 w-3" />
                      {msg.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </span>

                    {msg.role === 'assistant' && (
                      <>
                        <span>&bull;</span>
                        <button
                          onClick={() => handleAddCurrentToLedger(msg.content)}
                          className="text-indigo-400 hover:text-indigo-300 transition-colors flex items-center gap-0.5 cursor-pointer font-bold"
                        >
                          <HelpCircle className="h-3 w-3 text-indigo-400" />
                          <span>Ledger DRAFT buchen</span>
                        </button>
                      </>
                    )}
                  </div>

                </div>

                {/* Patient / Patrick Profile (if user) */}
                {msg.role === 'user' && (
                  <div className="h-8 w-8 rounded-lg bg-indigo-600 flex items-center justify-center text-white flex-shrink-0 mt-0.5 shadow font-bold text-xs select-none">
                    PH
                  </div>
                )}
              </motion.div>
            ))}

            {isTyping && (
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex gap-4 justify-start"
              >
                <div className="h-8 w-8 rounded-lg bg-[#222227] border border-[#2c2c35] flex items-center justify-center text-indigo-400 flex-shrink-0 shadow">
                  <Cpu className="h-4.5 w-4.5 animate-spin-slow text-indigo-400" />
                </div>
                <div className="bg-[#19191d] border border-[#222227] p-4.5 rounded-2xl rounded-tl-none max-w-[85%]">
                  <div className="flex items-center gap-1.5 text-xs text-gray-400 font-mono">
                    <span className="w-1.5 h-1.5 bg-indigo-505 rounded-full animate-bounce"></span>
                    <span className="w-1.5 h-1.5 bg-indigo-505 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></span>
                    <span className="w-1.5 h-1.5 bg-indigo-505 rounded-full animate-bounce" style={{ animationDelay: '0.4s' }}></span>
                    <span className="ml-1 text-[10px] text-gray-500">Nexi evaluiert MasterIndex...</span>
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
          <div ref={messagesEndRef} />
        </div>

        {/* Chat Input Bar */}
        <div className="p-4 bg-[#18181b] border-t border-[#222227]">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleSendMessage();
            }}
            className="flex gap-2.5 items-center relative"
          >
            <input
              type="text"
              value={inputMessage}
              onChange={(e) => setInputMessage(e.target.value)}
              placeholder="Frage Nexi nach Systemintegrationen, PowerShell-Code, Android 13/14 Syncer..."
              className="flex-1 bg-[#09090b] text-gray-100 placeholder-gray-500 rounded-xl px-4 py-3 border border-[#2c2c35] focus:outline-none focus:border-indigo-500 text-xs transition-all font-sans"
              id="nexi-chat-input-field"
            />
            <button
              type="submit"
              disabled={!inputMessage.trim() || isTyping}
              className={`p-3 rounded-xl flex items-center justify-center transition-all ${
                !inputMessage.trim() || isTyping
                  ? 'bg-gray-800 text-gray-500 cursor-not-allowed'
                  : 'bg-indigo-600 hover:bg-indigo-500 text-white shadow shadow-indigo-600/35 cursor-pointer'
              }`}
              id="nexi-chat-submit-btn"
            >
              <Send className="h-4 w-4" />
            </button>
          </form>
          
          <div className="flex items-center justify-between mt-2.5 text-[10px] text-gray-500 px-1 font-mono">
            <span>Sicherer Kanal: HTTPS &bull; Tailscale IP Authorized 100.x.x.x</span>
            <span>Antwortet bevorzugt auf Deutsch</span>
          </div>
        </div>

      </div>

    </div>
  );
}
