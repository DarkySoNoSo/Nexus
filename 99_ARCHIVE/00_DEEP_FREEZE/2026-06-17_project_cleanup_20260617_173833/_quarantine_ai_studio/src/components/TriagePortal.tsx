/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  CheckCircle, 
  Trash2, 
  Star, 
  Clock, 
  RotateCcw, 
  Sparkles, 
  Lock, 
  Database,
  Smartphone,
  Check,
  AlertTriangle,
  Flame,
  Plus,
  Send,
  Sliders,
  CheckSquare,
  HelpCircle,
  Inbox,
  ShieldAlert,
  ShieldCheck,
  Search,
  RefreshCw,
  FileCheck
} from 'lucide-react';
import { SIMULATED_CATALOG_DB } from '../data/nexusContextData';

interface TriageEvent {
  id: string;
  timestamp: string;
  source: string;
  title: string;
  body: string;
  category: 'Finanzen / Belege' | 'Dokumente / Verträge' | 'Foto-Belege / Mobile' | 'System-Warnung' | 'Schnittstelle';
  verbatimProof: string;
  interpretation: string;
  isVerifiedInIndex?: boolean;
}

const INITIAL_TRIAGE_EVENTS: TriageEvent[] = [
  {
    id: "triage_1",
    timestamp: "2026-06-10T19:22:00Z",
    source: "Android Collector - SMS Ingest",
    title: "ewz Zürich - Stromabrechnung 2026 ready",
    body: "Rechnungsvertrag CH-8001-ZUE-ewz für Mai 2026: 124,50 CHF fällig am 15.06.2026.",
    category: "Finanzen / Belege",
    verbatimProof: "C:\\MasterIndex_Storage\\_NEXUS_SYSTEM\\communication\\inbox.jsonl [Lauf #184-A]",
    interpretation: "Regelmäßige jährliche Belastung. Deckung durch Haushalts-Konto verifizieren."
  },
  {
    id: "triage_2",
    timestamp: "2026-06-10T19:30:15Z",
    source: "Mobile APK - Foto-Upload",
    title: "Foto-Beleg: Zählerstand_Strom_20260609.jpg",
    body: "Upload von mobilem Device. Zählerwert: 43219.4 kWh übergeben mit Legacy Collector Header.",
    category: "Foto-Belege / Mobile",
    verbatimProof: "X-Nexus-Collector: nexus-collector-apk-v1 / api/communication/ingest",
    interpretation: "Belegfoto dokumentiert genauen Verbrauch. Abzug über Stromabrechnung ewz."
  },
  {
    id: "triage_3",
    timestamp: "2026-06-10T19:45:00Z",
    source: "Windows Master-Indexer - Files API",
    title: "Mietvertrag_Wohnung_Patrick.pdf",
    body: "Neuer Scan im Hauptordner entdeckt. Kaltmiete: 650,00 CHF, Wohnungsbezug Patrick.",
    category: "Dokumente / Verträge",
    verbatimProof: "C:\\MasterIndex_Storage\\context\\dokumente\\Mietvertrag.pdf",
    interpretation: "Statisches Kontextdokument. Wichtig für die Steuererstattung und Wohnungs-Logik."
  },
  {
    id: "triage_4",
    timestamp: "2026-06-10T19:55:00Z",
    source: "ntfy Topic - Benachrichtigung",
    title: "Startbenachrichtigung: nexus-game thread",
    body: "Windows PC erfolgreich über Termux SSH-Key gestartet. Host: 100.107.24.67 (Tailscale).",
    category: "System-Warnung",
    verbatimProof: "Topic: nexus-game-ce1b260b4cd64b61 / ntfy.sh",
    interpretation: "Sicherheits-Informationsschleife. PC gestartet von Patrick via mobiles SSH-Script."
  },
  {
    id: "triage_5",
    timestamp: "2026-06-10T20:02:11Z",
    source: "SQLite Master Guard",
    title: "Doppelte Eingabe-Versuch bei Budgetblockade",
    body: "Schlüssel 'GEMINI_API_KEY' abgefragt. OpenAI_API_KEY Kosten-Limit bei $0.00045, weit unter $15.00.",
    category: "Schnittstelle",
    verbatimProof: "C:\\MasterIndex_Storage\\nexus_ai_policy.json",
    interpretation: "Sicherheitscheck durchgelaufen. Keine Überschreitung. API-Anfragen voll freigegeben."
  },
  {
    id: "triage_6",
    timestamp: "2026-06-11T08:15:30Z",
    source: "Mobile Widget - Schnellbeleg Ingest",
    title: "Supercharger GmbH - Autoladung-Zahlung",
    body: "Ladung beendet. Kostenpunkt: 28,40 CHF abgebucht von VISA-Master-Endziffer #4811.",
    category: "Finanzen / Belege",
    verbatimProof: "X-Nexus-Mobile-Auth Header / api/communication/quick-charge",
    interpretation: "Regelmäßige Kilometer-Ladungskosten. Automatisch in der Kategorie 'Fahrzeug' verbucht."
  },
  {
    id: "triage_7",
    timestamp: "2026-06-11T12:00:00Z",
    source: "Web Scraper - Mailbox Daemon",
    title: "Haftpflichtversicherung Schein 2026.pdf",
    category: "Dokumente / Verträge",
    body: "Aktualisiertes Dokument eingegangen von Versicherungskammer. Jahresbeitrag: 84,20 CHF fällig am 01.07.2026.",
    verbatimProof: "C:\\MasterIndex_Storage\\context\\dokumente\\Haftpflicht_2026.pdf",
    interpretation: "Jährlicher Fixbeitrag. Dem Masterindex zur Plausibilitäts- und Budgetbewertung übergeben."
  },
  {
    id: "triage_8",
    timestamp: "2026-06-11T16:22:45Z",
    source: "Telegram API Inbound Webhook",
    title: "Foto-Beleg: Gastherme_Zaehlerwert_0611.jpg",
    category: "Foto-Belege / Mobile",
    body: "Zustellung über verschlüsseltes Bot-Skript im Heizungskeller. Zählerwert: 11048.9 m³ ermittelt.",
    verbatimProof: "Telegram Msg_ID: #9284 / raw_payload.json",
    interpretation: "Gas-Zählerwert für den Nebenkosten-Zuschlag. Im SQLite-Hauptarchiv fest indiziert."
  },
  {
    id: "triage_9",
    timestamp: "2026-06-12T00:05:10Z",
    source: "Windows Event Log Daemon",
    title: "Erfolgreicher Remote-SSH Zugriff (patrick-mobil)",
    category: "System-Warnung",
    body: "Fernzugriff autorisiert über Mobilgerät 'patrick-stabil'. IP: 10.12.89.24. Port: 22.",
    verbatimProof: "System Log: C:\\Windows\\System32\\winevt\\Logs\\Security.evtx",
    interpretation: "Eindeutige Patrick-Sitzung. Heuristische Validierung zeigt keine Unregelmäßigkeiten."
  },
  {
    id: "triage_10",
    timestamp: "2026-06-12T09:12:00Z",
    source: "SQLite Outbox Daemon - sync_manager.py",
    title: "Android Outbox Synchronisationsprozess",
    category: "Schnittstelle",
    body: "15 ausstehende Offline-DRAFTS erfolgreich gespiegelt. Lokaler Cache bereinigt.",
    verbatimProof: "SQLite sync_queue [Status: PENDING] -> REST Port 8081",
    interpretation: "Double-Entry Safeguard: Alle mobilen Transaktionen sind nun sicher im Masterindex gespeichert."
  }
];

interface TriagePortalProps {
  onAddLog: (log: { type: 'info' | 'warn' | 'error' | 'success'; source: 'PowerShell' | 'SQLite' | 'Gemini' | 'Chef-Logik'; message: string }) => void;
  systemProvider: string;
}

export const TriagePortal: React.FC<TriagePortalProps> = ({ onAddLog, systemProvider }) => {
  const [events, setEvents] = useState<TriageEvent[]>(INITIAL_TRIAGE_EVENTS);
  const [history, setHistory] = useState<{ event: TriageEvent; action: 'fokus' | 'erledigt' | 'archiviert' }[]>([]);
  const [currentIdx, setCurrentIdx] = useState(0);
  const [swipeDirection, setSwipeDirection] = useState<'left' | 'right' | 'up' | null>(null);
  const [dragOffset, setDragOffset] = useState<number>(0);

  // Manual Ingestion State
  const [showIngestForm, setShowIngestForm] = useState<boolean>(false);
  const [newTitle, setNewTitle] = useState<string>('');
  const [newBody, setNewBody] = useState<string>('');
  const [newCategory, setNewCategory] = useState<TriageEvent['category']>('Finanzen / Belege');
  const [newSource, setNewSource] = useState<string>('Manuelle Erfassung');

  // Fact-Check State Machine for active Card
  const [factCheckState, setFactCheckState] = useState<'idle' | 'scanning' | 'analyzed'>('idle');
  const [factAuditLogs, setFactAuditLogs] = useState<string[]>([]);
  const [databaseMatch, setDatabaseMatch] = useState<any | null>(null);
  const [hasDiscrepancy, setHasDiscrepancy] = useState<boolean>(false);
  const [discrepancyType, setDiscrepancyType] = useState<string | null>(null);

  // Stats Counters derived from session history
  const totalProcessed = history.length;
  const numFocused = history.filter(h => h.action === 'fokus').length;
  const numDone = history.filter(h => h.action === 'erledigt').length;
  const numArchived = history.filter(h => h.action === 'archiviert').length;

  const currentEvent = currentIdx < events.length ? events[currentIdx] : null;

  // Track the current key card index shift to reset the inspection state
  useEffect(() => {
    setFactCheckState('idle');
    setFactAuditLogs([]);
    setDatabaseMatch(null);
    setHasDiscrepancy(false);
    setDiscrepancyType(null);
  }, [currentIdx]);

  // Keyboard Navigation for Blazing Fast Desk Operation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!currentEvent) return;

      // Avoid triggering when focused on input/textarea fields
      if (document.activeElement?.tagName === 'INPUT' || document.activeElement?.tagName === 'TEXTAREA') {
        return;
      }

      if (e.key === 'ArrowLeft') {
        setSwipeDirection('left');
        setTimeout(() => handleAction('archiviert'), 100);
      } else if (e.key === 'ArrowRight') {
        setSwipeDirection('right');
        setTimeout(() => handleAction('fokus'), 100);
      } else if (e.key === 'ArrowUp') {
        setSwipeDirection('up');
        setTimeout(() => handleAction('erledigt'), 100);
      } else if (e.key.toLowerCase() === 'f') {
        runActiveFactCheck();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [currentEvent, currentIdx, events, factCheckState]);

  const handleAction = (action: 'fokus' | 'erledigt' | 'archiviert') => {
    if (!currentEvent) return;

    let logMessage = '';
    if (action === 'fokus') {
      logMessage = `Triage FOKUS-HEUTE: '${currentEvent.title}' in Arbeitsliste eingetragen.`;
      onAddLog({ type: 'success', source: 'Chef-Logik', message: logMessage });
    } else if (action === 'erledigt') {
      logMessage = `Triage DONE: '${currentEvent.title}' im sqlite_catalog.db Protokoll abgelegt.`;
      onAddLog({ type: 'info', source: 'Chef-Logik', message: logMessage });
    } else if (action === 'archiviert') {
      logMessage = `Triage MÜLL/ARCHIV: '${currentEvent.title}' aussortiert.`;
      onAddLog({ type: 'warn', source: 'Chef-Logik', message: logMessage });
    }

    setHistory(prev => [{ event: currentEvent, action }, ...prev]);
    setCurrentIdx(prev => prev + 1);
    setSwipeDirection(null);
    setDragOffset(0);
  };

  const handleUndo = () => {
    if (history.length === 0) return;
    const lastItem = history[0];
    setHistory(prev => prev.slice(1));
    setCurrentIdx(prev => prev - 1);
    setSwipeDirection(null);
    setDragOffset(0);
    
    onAddLog({
      type: 'info',
      source: 'Chef-Logik',
      message: `Triage rükgängig: '${lastItem.event.title}' zurück an Stapelspitze.`
    });
  };

  const handleReset = () => {
    setEvents(INITIAL_TRIAGE_EVENTS);
    setHistory([]);
    setCurrentIdx(0);
    setSwipeDirection(null);
    setDragOffset(0);
    onAddLog({
      type: 'success',
      source: 'Chef-Logik',
      message: "Triage-Portal reinitialisiert. Alle Karten neu einsortiert."
    });
  };

  // Run the Fact-Check system for the active card
  const runActiveFactCheck = () => {
    if (!currentEvent) return;
    if (factCheckState === 'scanning' || factCheckState === 'analyzed') return;

    setFactCheckState('scanning');
    setFactAuditLogs([
      "⚡ Initialisiere Integritätsprüfung gegen SQLite-Masterkatalog...",
      "🔍 Öffne C:\\MasterIndex_Storage\\_NEXUS_SYSTEM\\db\\nexus_catalog.sqlite ... Connected."
    ]);

    // Delay simulations to render a majestic cyber scan animation
    setTimeout(() => {
      setFactAuditLogs(prev => [
        ...prev,
        `📊 Scanne Tabellen: 'file_index' und 'decision_log' nach Referenzen für '${currentEvent.id}'...`,
        `🔑 Extrahiere Tokenbelege und Schlüsselheuristiken...`
      ]);
    }, 400);

    setTimeout(() => {
      // Logic-driven search in the simulated master SQL database
      let match = null;
      let discrepancyFound = false;
      let discType = null;

      if (currentEvent.id === 'triage_1') {
        // Look up Ökostrom in the simulated catalog. It is rec_1
        match = SIMULATED_CATALOG_DB.find(r => r.id === 'rec_1');
        discrepancyFound = true;
        discType = 'SOURCE_ANOMALY'; // The index record has C:\MasterIndex_Storage\context\strom\Abrechnung_2026.pdf (PDF file from email, not SMS)
      } else if (currentEvent.id === 'triage_2') {
        match = SIMULATED_CATALOG_DB.find(r => r.id === 'rec_2');
      } else if (currentEvent.id === 'triage_3') {
        match = SIMULATED_CATALOG_DB.find(r => r.id === 'rec_3');
      } else if (currentEvent.id === 'triage_8') {
        match = SIMULATED_CATALOG_DB.find(r => r.id === 'rec_13');
      } else {
        // Generic search by keywords
        const keywords = currentEvent.title.toLowerCase().split(' ');
        match = SIMULATED_CATALOG_DB.find(record => {
          return keywords.some(k => k.length > 3 && (record.title.toLowerCase().includes(k) || record.snippet.toLowerCase().includes(k)));
        });
      }

      setDatabaseMatch(match);
      setHasDiscrepancy(discrepancyFound);
      setDiscrepancyType(discType);

      if (match) {
        if (discrepancyFound && discType === 'SOURCE_ANOMALY') {
          setFactAuditLogs(prev => [
            ...prev,
            `⚠️ METADATEN-DISCREPANZ ERREICHT!`,
            `📂 Physische Datei gefunden: '${match.title}'`,
            `📍 Realer Lokaler Pfad: '${match.fileOrigin}'`,
            `❌ Triage-Eintrag fälschlicherweise als 'Android Collector - SMS Ingest' deklariert!`,
            `💡 ERKLÄRUNG: Dies ist ein Phantom-Einsortierungsfehler im Ingest-Puffer. Die tatsächliche Stromrechnung liegt ordnungsgemäß als physisches PDF-Dokument in Ihrem Index vor, wurde jedoch beim Eintritt im Web-Frontend falsch deklariert.`
          ]);
        } else {
          setFactAuditLogs(prev => [
            ...prev,
            `✅ DATENSATZ GEFUNDEN (100% Match):`,
            `📂 Datei: '${match.title}'`,
            `📍 Pfad: '${match.fileOrigin}'`,
            `🔒 Integritätsstatus: SHA-256 Validated & OK.`
          ]);
        }
      } else {
        setFactAuditLogs(prev => [
          ...prev,
          `ℹ️ Temporärer Transaktionsbeleg / Ereignisfluss.`,
          `⚠️ Kein physisches Dokument im Master-Katalog gefunden (nur im lokalen RPC-Puffer indiziert).`
        ]);
      }

      setFactCheckState('analyzed');
      
      onAddLog({
        type: discrepancyFound ? 'warn' : 'success',
        source: 'SQLite',
        message: `Faktencheck beendet für '${currentEvent.title}': ${match ? (discrepancyFound ? 'Diskrepanz isoliert' : 'Datensatz verifiziert') : 'Warteschlangenbeleg.'}`
      });

    }, 1200);
  };

  // Automated repair to reconcile the data discrepancy
  const executeReconciliationRepair = () => {
    if (!currentEvent || currentEvent.id !== 'triage_1') return;

    const matchedFile = SIMULATED_CATALOG_DB.find(r => r.id === 'rec_1');
    if (!matchedFile) return;

    // Direct mutative update on the state array for immediate feedback
    setEvents(prev => prev.map(e => {
      if (e.id === 'triage_1') {
        return {
          ...e,
          source: "Windows Master-Indexer - Mailbox API",
          verbatimProof: matchedFile.fileOrigin,
          interpretation: "Integritätsgeprüft & Korrigiert. Physischer PDF-Beleg im lokalen Masterindex ('context\\strom').",
          isVerifiedInIndex: true
        };
      }
      return e;
    }));

    setHasDiscrepancy(false);
    setDiscrepancyType(null);
    setFactAuditLogs(prev => [
      ...prev,
      `🔧 METADATEN RECONCILIATION ERFOLGREICH ausgeführt!`,
      `💾 Überschreibe lokale Triage-Attribute...`,
      `✅ Quelle korrigiert zu: 'Windows Master-Indexer - Mailbox API'`,
      `✅ SSOT Beweisquelle verknüpft zu: '${matchedFile.fileOrigin}'`,
      `🎉 Der 'Phantom'-Status der Stromrechnung wurde hiermit offiziell korrigiert und synchronisiert!`
    ]);

    onAddLog({
      type: 'success',
      source: 'SQLite',
      message: `[Reconciliation] Metadaten-Reparatur für '${currentEvent.title}' durchgeführt. Pfad auf '${matchedFile.fileOrigin}' angepasst.`
    });
  };

  // Process Custom Web Ingestion from Patrick's Cockpit
  const handleIngestSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim() || !newBody.trim()) return;

    const newlyCreated: TriageEvent = {
      id: `manuell_${Date.now()}`,
      timestamp: new Date().toISOString(),
      source: newSource || 'Cockpit-Web-Ingest',
      title: newTitle,
      body: newBody,
      category: newCategory,
      verbatimProof: `Web-Terminal Dashboard Ingest // PORT: 8081`,
      interpretation: "Verarbeitung direkt gestartet. Transaktionssicherheit validiert."
    };

    // Insert card right in front of the active stack
    setEvents(prev => {
      const copy = [...prev];
      copy.splice(currentIdx, 0, newlyCreated);
      return copy;
    });

    onAddLog({
      type: 'success',
      source: 'Chef-Logik',
      message: `Manuelle Ingestion '${newTitle}' erfolgreich an Position #${currentIdx + 1} eingereiht.`
    });

    // Reset input fields
    setNewTitle('');
    setNewBody('');
    setShowIngestForm(false);
  };

  const loadPresetIngestion = (title: string, body: string, category: TriageEvent['category'], source: string) => {
    setNewTitle(title);
    setNewBody(body);
    setNewCategory(category);
    setNewSource(source);
    setShowIngestForm(true);
  };

  // Color mappings for neon elements
  const getCategoryTheme = (cat: string) => {
    switch (cat) {
      case 'Finanzen / Belege':
        return { border: 'border-emerald-500/30', glow: 'shadow-[0_0_20px_rgba(16,185,129,0.15)]', badge: 'bg-emerald-500/10 text-emerald-300 border-emerald-500/25', laser: 'bg-emerald-400' };
      case 'Foto-Belege / Mobile':
        return { border: 'border-sky-500/30', glow: 'shadow-[0_0_20px_rgba(14,165,233,0.15)]', badge: 'bg-sky-500/10 text-sky-300 border-sky-500/25', laser: 'bg-sky-455 bg-sky-400' };
      case 'Dokumente / Verträge':
        return { border: 'border-purple-500/30', glow: 'shadow-[0_0_20px_rgba(139,92,246,0.15)]', badge: 'bg-purple-500/10 text-purple-300 border-purple-500/25', laser: 'bg-purple-400' };
      case 'System-Warnung':
        return { border: 'border-red-500/30', glow: 'shadow-[0_0_20px_rgba(239,68,68,0.15)]', badge: 'bg-red-500/10 text-red-300 border-red-500/25', laser: 'bg-red-400' };
      default:
        return { border: 'border-amber-500/30', glow: 'shadow-[0_0_20px_rgba(245,158,11,0.15)]', badge: 'bg-amber-500/10 text-amber-300 border-amber-500/25', laser: 'bg-amber-400' };
    }
  };

  const currentTheme = currentEvent ? getCategoryTheme(currentEvent.category) : null;

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start animate-[fadeIn_0.3s_ease-out]">
      
      {/* Visual Triage Deck - German Deep Dark UX */}
      <div className="lg:col-span-7 bg-[#0b0b0d] border border-[#202025] rounded-2xl p-6 shadow-[0_30px_60px_-15px_rgba(0,0,0,0.8)] relative overflow-hidden transition-all duration-300">
        
        {/* Decorative Grid Pattern and Ambient Glowing Vectors */}
        <div className="absolute inset-0 bg-[radial-gradient(#1c1c24_1px,transparent_1px)] [background-size:16px_16px] opacity-15 pointer-events-none" />
        <div className="absolute top-0 right-0 w-96 h-96 bg-gradient-to-br from-indigo-500/10 via-purple-500/5 to-transparent rounded-full blur-3xl pointer-events-none" />

        {/* Dynamic Glowing border changes based on drag offset */}
        <div className="absolute bottom-0 left-0 right-0 h-[3px] transition-all duration-200"
          style={{
            background: dragOffset > 30 
              ? 'linear-gradient(90deg, transparent, #10b981, transparent)' 
              : dragOffset < -30 
              ? 'linear-gradient(90deg, transparent, #ef4444, transparent)' 
              : 'linear-gradient(90deg, transparent, #4f46e5, transparent)'
          }}
        />
        
        {/* Header Block with Logo & Active Status */}
        <div className="flex items-center justify-between mb-6 relative z-10 border-b border-[#1b1b22] pb-4">
          <div>
            <h2 className="text-sm uppercase tracking-widest font-mono text-white flex items-center gap-2">
              <Smartphone className="h-4.5 w-4.5 text-indigo-400 animate-pulse" />
              <span>Triage-Zentrale</span>
              <span className="text-[10px] font-mono font-medium text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-2 py-0.5 rounded-full">
                Aktiv
              </span>
            </h2>
            <p className="text-xs text-gray-500 mt-1 font-sans">
              Interaktive Ein-Hand-Triage mit integrierter **SQLite-Faktencheck** Engine (SSOT).
            </p>
          </div>
          
          <div className="flex items-center gap-2">
            {history.length > 0 && (
              <button
                onClick={handleUndo}
                className="p-1 px-2 border border-[#25252b] bg-[#121215] hover:bg-[#1b1b22] text-gray-400 hover:text-white rounded-md text-[10px] font-mono flex items-center gap-1 transition-all shadow-inner cursor-pointer"
                title="Rückgängig"
                id="triage-undo-btn-top"
              >
                <RotateCcw className="h-3.5 w-3.5" />
                Rückgängig
              </button>
            )}
            <button
              onClick={handleReset}
              className="p-1 px-2 border border-[#25252b] bg-[#121215] hover:bg-[#1b1b22] text-gray-400 hover:text-white rounded-md text-[10px] font-mono transition-all cursor-pointer"
              id="triage-reset-btn-top"
            >
              Reset
            </button>
          </div>
        </div>

        {/* Dynamic Statistics Bar of the Session */}
        <div className="grid grid-cols-4 gap-2 mb-6 bg-[#111114]/80 backdrop-blur border border-[#1b1b22] rounded-xl p-3 text-center text-xs font-mono">
          <div>
            <span className="text-gray-500 text-[9px] block">STAPEL</span>
            <span className="text-white font-bold text-xs">{Math.max(0, events.length - currentIdx)} übrig</span>
          </div>
          <div>
            <span className="text-emerald-405 text-emerald-400 text-[9px] block">TODAY FOKUS</span>
            <span className="text-emerald-400 font-bold text-xs">+{numFocused}</span>
          </div>
          <div>
            <span className="text-sky-400 text-[9px] block">ERLEDIGT</span>
            <span className="text-sky-400 font-bold text-xs">+{numDone}</span>
          </div>
          <div>
            <span className="text-red-400 text-[9px] block">ARCHIV</span>
            <span className="text-red-400 font-bold text-xs">+{numArchived}</span>
          </div>
        </div>

        {/* Main Interface Screen Stage */}
        <div className="relative min-h-[460px] flex flex-col items-center justify-center py-4 perspective-[1000px] overflow-hidden">
          
          {/* Glowing Backlight visual cues based on drag offset direction */}
          <div className="absolute inset-x-0 top-12 bottom-12 rounded-full blur-[80px] opacity-20 transition-all duration-300 pointer-events-none"
            style={{
              background: dragOffset > 40
                ? 'radial-gradient(circle, #10b981 0%, transparent 70%)'
                : dragOffset < -40
                ? 'radial-gradient(circle, #ef4444 0%, transparent 70%)'
                : 'radial-gradient(circle, #4f46e5 0%, transparent 70%)'
            }}
          />

          <AnimatePresence mode="popLayout">
            {currentEvent ? (
              <motion.div
                key={currentEvent.id}
                className={`w-full max-w-sm bg-gradient-to-b from-[#131317] to-[#0e0e11] border ${currentTheme?.border || 'border-[#282834]'} ${currentTheme?.glow || ''} rounded-2xl p-5 shadow-[0_20px_40px_rgba(0,0,0,0.6)] relative cursor-grab active:cursor-grabbing preserve-3d transition-all duration-300`}
                id={`triage-card-${currentEvent.id}`}
                initial={{ scale: 0.9, y: 15, opacity: 0 }}
                animate={{ scale: 1, y: 0, opacity: 1 }}
                exit={() => {
                  let outX = 0;
                  let outY = 0;
                  if (swipeDirection === 'left') outX = -350;
                  if (swipeDirection === 'right') outX = 350;
                  if (swipeDirection === 'up') outY = -350;
                  return {
                    x: outX,
                    y: outY,
                    rotate: outX / 15,
                    opacity: 0,
                    transition: { duration: 0.25 }
                  };
                }}
                drag="x"
                dragConstraints={{ left: 0, right: 0 }}
                onDrag={(event, info) => {
                  setDragOffset(info.offset.x);
                }}
                onDragEnd={(event, info) => {
                  if (info.offset.x < -90) {
                    setSwipeDirection('left');
                    setTimeout(() => handleAction('archiviert'), 40);
                  } else if (info.offset.x > 90) {
                    setSwipeDirection('right');
                    setTimeout(() => handleAction('fokus'), 40);
                  } else {
                    setDragOffset(0);
                  }
                }}
              >
                
                {/* Dynamic Glowing Laser Scanning Line when Factchecking */}
                {factCheckState === 'scanning' && (
                  <motion.div 
                    className={`absolute left-0 right-0 h-[2.5px] ${currentTheme?.laser || 'bg-indigo-400'} shadow-[0_0_12px_rgba(79,70,229,0.8)] z-40`}
                    initial={{ top: '0%' }}
                    animate={{ top: ['0%', '100%', '0%'] }}
                    transition={{ repeat: Infinity, duration: 2, ease: "easeInOut" }}
                  />
                )}

                {/* Dynamic Drag Overlay Indicators inside the card */}
                {dragOffset > 25 && (
                  <div className="absolute inset-0 bg-emerald-500/10 border-2 border-emerald-400 rounded-2xl flex items-center justify-center backdrop-blur-xs z-30 pointer-events-none animate-[pulse_1s_infinite]">
                    <span className="text-emerald-400 font-mono font-bold text-sm tracking-widest uppercase px-4 py-2 border-2 border-emerald-400 bg-black/90 rotate-12 shadow-lg">
                      &rarr; FOKUS HEUTE
                    </span>
                  </div>
                )}

                {dragOffset < -25 && (
                  <div className="absolute inset-0 bg-red-500/10 border-2 border-red-400 rounded-2xl flex items-center justify-center backdrop-blur-xs z-30 pointer-events-none animate-[pulse_1s_infinite]">
                    <span className="text-red-400 font-mono font-bold text-sm tracking-widest uppercase px-4 py-2 border-2 border-red-400 bg-black/90 -rotate-12 shadow-lg">
                      MÜLL / ARCHIV &larr;
                    </span>
                  </div>
                )}

                {/* Stamp overlay for repaired Master Verifizierungen */}
                {(currentEvent.isVerifiedInIndex) && (
                  <div className="absolute top-12 right-4 border-[3px] border-emerald-500 text-emerald-400 text-[10px] font-mono uppercase tracking-widest font-extrabold px-3 py-1 rounded bg-black/95 shadow z-20 rotate-12 animate-fade-in select-none">
                    SSOT Index-Verifiziert
                  </div>
                )}

                {/* Card Tag Category */}
                <div className="flex items-center justify-between mb-4">
                  <span className="text-[9px] font-mono text-gray-500 font-semibold tracking-wider flex items-center gap-1">
                    <Clock className="h-3 w-3 text-indigo-400" />
                    {new Date(currentEvent.timestamp).toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' })} Uhr
                  </span>
                  
                  <span className={`text-[9px] font-mono font-bold uppercase tracking-wider px-2 py-0.5 rounded border ${currentTheme?.badge || ''}`}>
                    {currentEvent.category}
                  </span>
                </div>

                {/* Source device info */}
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-1.5">
                    <div className="h-2 w-2 rounded-full bg-indigo-500 animate-[ping_1.5s_infinite]" />
                    <span className="text-[10px] font-mono text-gray-400 tracking-tight select-all">
                      {currentEvent.source}
                    </span>
                  </div>
                  
                  {/* Faktencheck Micro-Trigger Button */}
                  <button
                    onClick={(e) => { e.stopPropagation(); runActiveFactCheck(); }}
                    disabled={factCheckState === 'scanning'}
                    className={`px-2 py-0.5 border ${factCheckState === 'analyzed' ? 'border-indigo-500/30 text-indigo-400 bg-indigo-500/5' : 'border-[#333] hover:border-indigo-500/40 text-gray-400 hover:text-indigo-400 bg-black/45'} rounded text-[9px] font-mono tracking-wider flex items-center gap-1 transition-all cursor-pointer`}
                    title="Faktencheck gegen SQLite Index"
                  >
                    <Search className="h-3 w-3 text-indigo-400" />
                    {factCheckState === 'scanning' ? 'Prüfe...' : factCheckState === 'analyzed' ? 'Geprüft' : 'Faktencheck [F]'}
                  </button>
                </div>

                {/* Primary Card Contents */}
                <div className="space-y-3 mb-5">
                  <h3 className="text-sm font-bold text-white tracking-tight leading-snug text-left">
                    {currentEvent.title}
                  </h3>
                  
                  <div className="bg-[#08080a] border border-[#1b1b22] rounded-xl p-3.5 min-h-[110px] flex flex-col justify-between">
                    <p className="text-xs text-gray-300 leading-relaxed font-sans block text-left">
                      &quot;{currentEvent.body}&quot;
                    </p>
                    <span className="text-[9px] text-gray-500 mt-2 font-mono block text-right">
                      ID: {currentEvent.id}
                    </span>
                  </div>
                </div>

                {/* Evidence Details */}
                <div className="space-y-2.5 pt-3.5 border-t border-[#1d1d24] text-[10.5px] leading-relaxed text-left">
                  <div className="flex items-start gap-2">
                    <Database className="h-3.5 w-3.5 text-gray-500 shrink-0 mt-0.5" />
                    <div className="min-w-0 flex-1">
                      <span className="font-semibold text-gray-400 font-sans block">Beweisquelle (Single Source of Truth):</span>
                      <p className="text-emerald-400 font-mono text-[9.5px] bg-[#070708] border border-white/5 p-1 rounded-md mt-0.5 truncate select-all">
                        {currentEvent.verbatimProof}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-start gap-2">
                    <Sparkles className="h-3.5 w-3.5 text-indigo-400 shrink-0 mt-0.5" />
                    <div>
                      <span className="font-semibold text-gray-400 font-sans block">Register-Chef Interpretation:</span>
                      <p className="text-gray-400 italic font-sans mt-0.5 leading-normal">
                        &quot;{currentEvent.interpretation}&quot;
                      </p>
                    </div>
                  </div>
                </div>

                {/* Tactile Keyboard shortcuts help legend on Card base */}
                <div className="mt-5 pt-3 border-t border-[#16161c] flex items-center justify-between text-[9px] text-gray-500 font-mono">
                  <span>[←] Müll</span>
                  <span className="text-gray-400 font-bold">[↑] Erledigt / Safe</span>
                  <span>Fokus [→]</span>
                </div>

              </motion.div>
            ) : (
              <motion.div
                key="empty"
                className="w-full max-w-sm bg-[#08080a] border border-dashed border-[#202025] rounded-2xl p-8 text-center flex flex-col items-center justify-center min-h-[300px]"
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0 }}
              >
                <div className="p-4 bg-indigo-500/5 border border-indigo-500/10 rounded-full mb-4">
                  <CheckCircle className="h-8 w-8 text-indigo-400" />
                </div>
                <h3 className="text-sm font-bold text-white mb-1 font-sans">Alles sauber, Chef!</h3>
                <p className="text-xs text-gray-500 max-w-xs leading-relaxed font-sans">
                  Keine Ingestionen in der Warteschlange. Alle Benachrichtigungen, SMS und Belegfotografien wurden fehlerfrei triagiert und synchonisiert.
                </p>
                <button
                  onClick={handleReset}
                  className="mt-5 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-xs font-semibold transition-colors flex items-center gap-1.5 cursor-pointer shadow-md"
                  id="reload-triage-btn"
                >
                  <RotateCcw className="h-3.5 w-3.5" />
                  Stapel neu laden
                </button>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Expanded Factcheck Results Subpanel (Dynamic HUD feedback) */}
          <AnimatePresence>
            {factCheckState !== 'idle' && (
              <motion.div
                className="w-full max-w-sm mt-4 bg-[#0d0d11] border border-[#23232c] rounded-xl p-4 text-left shadow-lg"
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                exit={{ opacity: 0, height: 0 }}
              >
                <div className="flex items-center justify-between border-b border-[#202028] pb-2 mb-2">
                  <span className="text-[10px] font-mono uppercase tracking-wider text-indigo-400 flex items-center gap-1">
                    <Database className="h-3 w-3 text-indigo-400 animate-pulse" />
                    SQLite-Katalog Live-Crosscheck
                  </span>
                  <span className="text-[9px] font-mono text-gray-500">
                    Host: C:\MasterIndex_Storage
                  </span>
                </div>

                <div className="space-y-1.5 max-h-[140px] overflow-y-auto font-mono text-[9px] text-gray-400 bg-black/40 rounded p-2.5 leading-relaxed scrollbar-none">
                  {factAuditLogs.map((log, lidx) => (
                    <div key={lidx} className="border-b border-white/5 pb-1 last:border-0 leading-normal">
                      {log}
                    </div>
                  ))}
                </div>

                {/* Conflict Resolution Button if Discrepancy is isolated (Solves the Electricity Bill Phantom issue!) */}
                {factCheckState === 'analyzed' && hasDiscrepancy && discrepancyType === 'SOURCE_ANOMALY' && (
                  <motion.div 
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="mt-3 p-2 bg-amber-500/5 border border-amber-500/20 rounded text-[10px] space-y-2 text-left"
                  >
                    <div className="flex items-start gap-1.5">
                      <AlertTriangle className="h-4 w-4 text-amber-500 shrink-0 mt-0.5" />
                      <p className="text-amber-400 font-sans leading-normal">
                        <strong>Metadaten-Anomalie (Phantom):</strong> Die Stromabrechnung kam als physische PDF im Index an (siehe Database Search). Der Triage-Eintrag wurde fälschlicherweise als &quot;SMS Ingest&quot; deklariert.
                      </p>
                    </div>
                    <button
                      onClick={executeReconciliationRepair}
                      className="w-full py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white font-bold rounded text-[9px] font-mono tracking-widest uppercase transition-all flex items-center justify-center gap-1 cursor-pointer"
                    >
                      <FileCheck className="h-3 w-3 text-white" />
                      Integritäts-Fehler Beheben (Metadaten-Sync)
                    </button>
                  </motion.div>
                )}

              </motion.div>
            )}
          </AnimatePresence>

        </div>

        {/* Triage Trigger Action Panel buttons */}
        {currentEvent && (
          <div className="grid grid-cols-3 gap-3 relative z-10 pt-4 border-t border-[#1b1b22]">
            <button
              onClick={() => {
                setSwipeDirection('left');
                setTimeout(() => handleAction('archiviert'), 100);
              }}
              className="flex flex-col items-center justify-center py-2 px-3 bg-[#1c0f12] hover:bg-[#2c1216] border border-red-900/40 text-red-400 hover:text-red-300 rounded-xl transition-all cursor-pointer group active:scale-95 shadow-md"
              id="triage-dislike-btn"
            >
              <Trash2 className="h-4.5 w-4.5 mb-1 group-hover:scale-110 transition-transform text-red-500" />
              <span className="text-[10px] font-mono tracking-wider uppercase font-semibold">Müll</span>
            </button>

            <button
              onClick={() => {
                setSwipeDirection('up');
                setTimeout(() => handleAction('erledigt'), 100);
              }}
              className="flex flex-col items-center justify-center py-2 px-3 bg-[#0d161a] hover:bg-[#10242d] border border-sky-900/40 text-sky-400 hover:text-sky-300 rounded-xl transition-all cursor-pointer group active:scale-95 shadow-md"
              id="triage-done-btn"
            >
              <Check className="h-4.5 w-4.5 mb-1 group-hover:scale-110 transition-transform text-sky-400" />
              <span className="text-[10px] font-mono tracking-wider uppercase font-semibold">Safe / Done</span>
            </button>

            <button
              onClick={() => {
                setSwipeDirection('right');
                setTimeout(() => handleAction('fokus'), 100);
              }}
              className="flex flex-col items-center justify-center py-2 px-3 bg-[#0c1a14] hover:bg-[#112a1d] border border-emerald-900/40 text-emerald-450 text-emerald-400 hover:text-emerald-300 rounded-xl transition-all cursor-pointer group active:scale-95 shadow-md"
              id="triage-like-btn"
            >
              <Star className="h-4.5 w-4.5 mb-1 group-hover:scale-110 transition-transform text-emerald-400" />
              <span className="text-[10px] font-mono tracking-wider uppercase font-semibold">Heute Fokus</span>
            </button>
          </div>
        )}

      </div>

      {/* Side-Audit Panel, manual injection interface & Triage History */}
      <div className="lg:col-span-5 space-y-6">
        
        {/* Dynamic Event manual Injection Queue Engine */}
        <div className="bg-[#0b0b0d] border border-[#202025] rounded-xl p-5 shadow-lg relative">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-xs font-mono uppercase tracking-wider text-white flex items-center gap-1.5">
              <Plus className="h-4 w-4 text-emerald-400" />
              Ingestion-Kanal simulieren
            </h3>
            <button
              onClick={() => setShowIngestForm(!showIngestForm)}
              className="text-[10px] text-indigo-400 hover:text-indigo-300 font-mono underline select-none cursor-pointer"
            >
              {showIngestForm ? 'Einklappen' : 'Ausklappen'}
            </button>
          </div>
          
          <p className="text-[11px] text-gray-500 leading-relaxed mb-3 font-sans">
            Schleuse manuell Eventlogs, Rechnungen oder SMS in den Nexus-Speicherkanal ein, um das Swiping und den Registry-Verlauf live zu testen:
          </p>

          {!showIngestForm ? (
            <div className="space-y-2">
              <span className="text-[9px] font-mono text-gray-400 uppercase tracking-widest block font-bold text-left">
                Schnell-Muster zum Einfügen:
              </span>
              <div className="grid grid-cols-1 gap-1.5 text-left">
                <button
                  onClick={() => loadPresetIngestion(
                    "ntfy: Warnung Batterie kritisch",
                    "Akkustrom Android Daemon bei 7% Kapazität. Sync-Intervall drosseln.",
                    "System-Warnung",
                    "Android Monitor Core - apk-v1"
                  )}
                  className="p-2 text-[11px] bg-[#121215] border border-[#1b1b22] hover:border-amber-500/30 font-sans rounded-md text-gray-400 hover:text-white transition-all text-left block cursor-pointer truncate"
                >
                  ⚠️ <strong>SMS:</strong> Android Akkustatus drosseln
                </button>
                <button
                  onClick={() => loadPresetIngestion(
                    "ewz Beleg: Zählerfoto Flur",
                    "Belegfoto Flur-Digitalzähler eingegangen. Wert: 89131.5 kWh.",
                    "Foto-Belege / Mobile",
                    "Telegram Bot - Channel Ingest"
                  )}
                  className="p-2 text-[11px] bg-[#121215] border border-[#1b1b22] hover:border-indigo-500/30 font-sans rounded-md text-gray-400 hover:text-white transition-all text-left block cursor-pointer truncate"
                >
                  📷 <strong>Foto:</strong> Digitalzähler Flur 89131 kWh
                </button>
                <button
                  onClick={() => loadPresetIngestion(
                    "Stromrechnung Nebenkosten-Zuschlag",
                    "Nebenkosten-Vorauszahlung fällig: 45,00 CHF zum 01.07.2026.",
                    "Finanzen / Belege",
                    "Home Assistant Webhook - Ingest"
                  )}
                  className="p-2 text-[11px] bg-[#121215] border border-[#1b1b22] hover:border-emerald-500/30 font-sans rounded-md text-gray-400 hover:text-white transition-all text-left block cursor-pointer truncate"
                >
                  📈 <strong>Beleg:</strong> Nebenkosten-Zahlung 45 CHF
                </button>
              </div>
            </div>
          ) : (
            <motion.form 
              initial={{ opacity: 0, y: -10 }} 
              animate={{ opacity: 1, y: 0 }} 
              onSubmit={handleIngestSubmit} 
              className="space-y-3 pt-2 text-left"
            >
              <div>
                <label className="text-[9px] font-mono text-gray-500 block mb-1">Titel des Log-Eintrages</label>
                <input
                  type="text"
                  required
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  placeholder="z.B. Zählerstand Gastherme..."
                  className="w-full p-2 bg-[#08080a] border border-[#1c1c22] focus:border-indigo-500 text-xs rounded text-white font-sans focus:outline-none"
                />
              </div>

              <div>
                <label className="text-[9px] font-mono text-gray-500 block mb-1">Text-Körper / Raw Log</label>
                <textarea
                  required
                  rows={2}
                  value={newBody}
                  onChange={(e) => setNewBody(e.target.value)}
                  placeholder="z.B. SMS von +49176...: Bitte Zählerwert übermitteln..."
                  className="w-full p-2 bg-[#08080a] border border-[#1c1c22] focus:border-indigo-500 text-xs rounded text-white font-sans resize-none focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-[9px] font-mono text-gray-500 block mb-1">Kategorie</label>
                  <select
                    value={newCategory}
                    onChange={(e) => setNewCategory(e.target.value as TriageEvent['category'])}
                    className="w-full p-1.5 bg-[#08080a] border border-[#1c1c22] text-xs rounded text-white font-sans focus:outline-none cursor-pointer"
                  >
                    <option value="Finanzen / Belege">Finanzen / Belege</option>
                    <option value="Dokumente / Verträge">Dokumente / Verträge</option>
                    <option value="Foto-Belege / Mobile">Foto-Belege / Mobile</option>
                    <option value="System-Warnung">System-Warnung</option>
                    <option value="Schnittstelle">Schnittstelle</option>
                  </select>
                </div>
                <div>
                  <label className="text-[9px] font-mono text-gray-500 block mb-1">Hardware / API Quelle</label>
                  <input
                    type="text"
                    value={newSource}
                    onChange={(e) => setNewSource(e.target.value)}
                    placeholder="SSH, Android..."
                    className="w-full p-1.5 bg-[#08080a] border border-[#1c1c22] text-xs rounded text-white font-sans focus:outline-none"
                  />
                </div>
              </div>

              <button
                type="submit"
                className="w-full py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs rounded-lg transition-colors flex items-center justify-center gap-1 cursor-pointer"
                id="manual-ingest-submit-btn"
              >
                <Send className="h-3 w-3" />
                In die Warteschlange einschleusen
              </button>
            </motion.form>
          )}

        </div>

        {/* Secure Ledger Protocol Creds */}
        <div className="bg-[#0b0b0d] border border-[#202025] rounded-xl p-5 shadow-lg">
          <h3 className="text-xs font-mono uppercase tracking-widest text-white mb-3 flex items-center gap-1.5 justify-start">
            <Lock className="h-4 w-4 text-indigo-400" />
            Verschlüsseltes System Ledger
          </h3>
          <p className="text-[11px] text-gray-500 leading-normal mb-3 font-sans text-left">
            Die Android Sync Engine authentifiziert eingehende SMS-Belegketten über einen abgesicherten HTTPS-Reverse-Proxy mit folgendem Secrets-Gerüst:
          </p>
          <div className="space-y-1.5 text-[10px] font-mono text-left">
            <div className="flex justify-between p-2 bg-black/40 rounded border border-white/5">
              <span className="text-gray-500">USER:</span>
              <span className="text-indigo-400 font-bold select-all">GAME\NexusMobile</span>
            </div>
            <div className="flex justify-between p-2 bg-black/40 rounded border border-white/5">
              <span className="text-gray-500">PASSWORT token:</span>
              <span className="text-indigo-400 font-bold select-all">Nexus2468__HMAC_SHA256</span>
            </div>
            <div className="flex justify-between p-2 bg-black/40 rounded border border-white/5">
              <span className="text-gray-500">SMB TARGET ENDPOINT:</span>
              <span className="text-gray-300 select-all">\\100.107.24.67\MasterIndex_Storage</span>
            </div>
          </div>
        </div>

        {/* Live Decisions Log History Streams */}
        <div className="bg-[#0b0b0d] border border-[#202025] rounded-xl p-5 shadow-lg min-h-[170px]">
          <h3 className="text-xs font-mono uppercase tracking-widest text-white mb-3 flex items-center gap-1.5 justify-start">
            <Inbox className="h-4 w-4 text-gray-400" />
            Eingeloggte Entscheidungen ({history.length})
          </h3>
          
          <div className="space-y-2 max-h-[190px] overflow-y-auto scrollbar-thin pr-1 text-left">
            {history.length === 0 ? (
              <div className="text-center py-10 text-gray-600 text-xs italic font-sans">
                Keine Swiping-Entscheidungen in dieser UI-Sitzung. Ziehe Karten nach links oder rechts.
              </div>
            ) : (
              history.map((hist, idx) => (
                <div
                  key={idx}
                  className="p-2.5 bg-black/30 rounded-lg border border-[#1b1b22] flex items-center justify-between gap-3 text-xs"
                >
                  <div className="min-w-0">
                    <h4 className="font-bold text-gray-200 truncate pr-1">
                      {hist.event.title}
                    </h4>
                    <span className="text-[9.5px] text-gray-500 font-mono block">
                      Kat: {hist.event.category}
                    </span>
                  </div>
                  <div>
                    <span className={`text-[9px] uppercase font-mono px-2 py-0.5 rounded-full font-bold ${
                      hist.action === 'fokus'
                        ? 'bg-emerald-500/10 text-emerald-450 border border-emerald-500/20'
                        : hist.action === 'erledigt'
                        ? 'bg-sky-500/10 text-sky-400 border border-sky-500/20'
                        : 'bg-red-500/10 text-red-500 border border-red-500/20'
                    }`}>
                      {hist.action}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

      </div>
    </div>
  );
};
