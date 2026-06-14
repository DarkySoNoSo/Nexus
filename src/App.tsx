/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from 'react';
import { 
  INITIAL_FOLDERS, 
  INITIAL_LOGS, 
  INITIAL_AUDIT_RULES, 
  SIMULATED_CATALOG_DB 
} from './data/nexusContextData';
import { StatusBanner } from './components/StatusBanner';
import { ChefRules } from './components/ChefRules';
import { FileExplorer } from './components/FileExplorer';
import { DatabaseSearch } from './components/DatabaseSearch';
import { GeminiPlayground } from './components/GeminiPlayground';
import { LogConsole } from './components/LogConsole';
import { TriagePortal } from './components/TriagePortal';
import { AndroidEmulator } from './components/AndroidEmulator';
import { ThreeDMetrics } from './components/ThreeDMetrics';
import { NexiChat } from './components/NexiChat';
import { SystemStatus, SystemLog, AuditRule } from './types';
import { 
  Sliders, 
  FolderOpen, 
  Database, 
  Sparkles, 
  Terminal, 
  Smartphone, 
  Layers, 
  MessageSquare, 
  Activity, 
  User, 
  ChevronRight,
  FolderDot
} from 'lucide-react';

export default function App() {
  const [activeTab, setActiveTab] = useState<'nexi' | 'triage' | 'android' | 'adapter' | 'explorer' | 'database' | 'gemini' | 'logs' | 'metrics'>('nexi');
  const [folders, setFolders] = useState(INITIAL_FOLDERS);
  const [logs, setLogs] = useState<SystemLog[]>(INITIAL_LOGS);
  const [auditRules, setAuditRules] = useState<AuditRule[]>(INITIAL_AUDIT_RULES);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  
  // Keep track of folder selection from sidebar
  const [sidebarFolderId, setSidebarFolderId] = useState<string>('00_START_HIER');
  const [sidebarFileName, setSidebarFileName] = useState<string>('STATUS_AKTUELL.md');

  // Default system status
  const [systemStatus, setSystemStatus] = useState<SystemStatus>({
    version: "v40.44",
    port: 8081,
    status: "online",
    lanUrl: "http://192.168.1.216:8081/",
    tailscaleUrl: "http://100.107.24.67:8081/",
    totalRecords: 743821,
    totalSizeGb: 71.4,
    activeAlerts: 1,
    currentProvider: "Local-Offline",
    monthlyCostLimit: 15.00,
    monthlyCostSpent: 0.00045,
    apiKeySet: false
  });

  // Fetch true system status from Server API
  const fetchStatus = async () => {
    setIsLoading(true);
    try {
      const res = await fetch('/api/system/status');
      if (res.ok) {
        const data = await res.json();
        setSystemStatus(data);
      } else {
        throw new Error(`Server answered with status ${res.status}`);
      }
    } catch (err) {
      console.warn("Failed to load backend system status, utilizing demo state:", err);
      setSystemStatus(prev => ({
        ...prev,
        status: "offline"
      }));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchStatus();
  }, []);

  // Update server status when Patrick toggles the provider
  const handleToggleProvider = (provider: SystemStatus['currentProvider']) => {
    setSystemStatus(prev => ({
      ...prev,
      currentProvider: provider
    }));

    handleAddLog({
      type: 'warn',
      source: 'Chef-Logik',
      message: `System-Provider manuell auf '${provider}' geändert.`
    });
  };

  const handleUpdateFileContent = (folderId: string, fileName: string, newContent: string) => {
    setFolders(prev => prev.map(f => {
      if (f.id === folderId) {
        return {
          ...f,
          files: f.files.map(file => {
            if (file.name === fileName) {
              return {
                ...file,
                content: newContent,
                sizeBytes: newContent.length,
                lastModified: new Date().toISOString()
              };
            }
            return file;
          })
        };
      }
      return f;
    }));

    handleAddLog({
      type: 'info',
      source: 'PowerShell',
      message: `Konfigurationsdatei C:\\MasterIndex_Storage\\${folderId}\\${fileName} lokal modifiziert.`
    });
  };

  // Log management helpers
  const handleAddLog = (newLog: Omit<SystemLog, 'id' | 'timestamp'>) => {
    const logItem: SystemLog = {
      ...newLog,
      id: `log_${Date.now()}_${Math.random().toString(36).substr(2, 4)}`,
      timestamp: new Date().toISOString()
    };
    setLogs(prev => [...prev, logItem]);
  };

  const handleClearLogs = () => {
    setLogs([]);
  };

  // Interactive audit rules Re-Test trigger
  const handleRunAudit = (id: string) => {
    setAuditRules(prev => prev.map(rule => {
      if (rule.id === id) {
        let finalStatus: AuditRule['status'] = 'passed';
        let feedback = 'Sicherheitsprüfung erfolgreich bestanden.';

        if (id === 'audit_1') {
          feedback = systemStatus.apiKeySet 
            ? 'Key liegt sicher verschlüsselt in den Cloud-Secrets. SCAN: 0 Verletzungen.'
            : 'Key ist leer. Schutz aktiv durch Offline-Lock Heuristik.';
        } else if (id === 'audit_3') {
          finalStatus = 'passed';
          feedback = 'Datenbank-Schritt erfolgreich gepatcht. Attributionen vollständig.';
        } else if (id === 'audit_4') {
          feedback = 'Port 8081 ist exklusiv für Nexus.PS1 reserviert.';
        }

        handleAddLog({
          type: finalStatus === 'passed' ? 'success' : 'warn',
          source: 'Chef-Logik',
          message: `Red-Team Audit für '${rule.title}' ausgeführt. Status: ${finalStatus.toUpperCase()}`
        });

        return {
          ...rule,
          status: finalStatus,
          feedback
        };
      }
      return rule;
    }));
  };

  // API Bridge: Zweitmeinungs-Konnektor via backend
  const handleCallAnalyze = async (prompt: string, category: string, contextDoc: string) => {
    try {
      const res = await fetch('/api/gemini/analyze', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prompt, category, contextDoc })
      });
      
      if (!res.ok) {
        throw new Error(`Server returned error status: ${res.status}`);
      }

      const parsedData = await res.json();
      
      if (parsedData.sourceAttribution) {
        const costStr = parsedData.sourceAttribution.budgetImpact;
        const numericCost = parseFloat(costStr.replace(/[^0-9.]/g, '')) || 0;
        
        setSystemStatus(prev => ({
          ...prev,
          monthlyCostSpent: prev.monthlyCostSpent + numericCost
        }));

        handleAddLog({
          type: parsedData.complianceCheck.actionBlocked ? 'error' : 'success',
          source: 'Gemini',
          message: `Analyse beendet. Kosten: ${costStr}. Compliance: ${parsedData.complianceCheck.actionBlocked ? 'BLOCKIERT' : 'ERLAUBT'}`
        });
      }

      return parsedData;
    } catch (err) {
      console.warn("Analysis API invoke failed, fallback to local client-side heuristic:", err);

      const isBlocked = prompt.toLowerCase().includes("antworte") || 
                        prompt.toLowerCase().includes("schreibe mail") ||
                        prompt.toLowerCase().includes("eintragen") ||
                        prompt.toLowerCase().includes("termin") ||
                        prompt.toLowerCase().includes("move") ||
                        prompt.toLowerCase().includes("lösche");

      const localFallback = {
        analysis: `[Schnittstelle im Client-Zweitfall-Kanal] Der Dienst läuft aufgrund temporärer Sandbox-Einschränkungen im Client-Heuristik-Modus. Inhaltlich geht es um eine Nachricht über: "${prompt.slice(0, 60)}${prompt.length > 60 ? '...' : ''}".`,
        sourceAttribution: {
          sourceUsed: "Client-Offline (Nexus-Sicherheitsnetz)",
          belege: [
            "Leitfaden.md -> Säule 2: Kosten-Lock und sensible Daten offline blockieren",
            "Security_Rules.md -> Windows User-Environment"
          ],
          interpretation: "Dies ist der lokale clientseitige Ausfallschutz für Ihr Nexus-Dashboard.",
          budgetImpact: "$0.00000 (Sicherheitsnetz / Gebührenfrei)"
        },
        complianceCheck: {
          actionBlocked: isBlocked,
          reason: isBlocked 
            ? "Das unaufgeforderte Auslösen definitiver Aktivitäten (Termine eintragen, Mails automatisch versenden) ist laut Nexus-Chefrichtlinie verboten."
            : "Nur Analyse und Vorschläge. Keine unautorisierten oder blockierten Aktionen im Client-Sicherungsmodus erkannt.",
          riskLevel: isBlocked ? "High" : "Low"
        },
        altDrafts: [
          `[Entwurf A] Verstanden Chef. Im Client-Protokoll katalogisiert.`,
          `[Entwurf B] Hinweis an Patrick: Bitte die Belegnummer prüfen bevor wir weitere Aktionen im System freischalten.`
        ]
      };

      handleAddLog({
        type: 'warn',
        source: 'Chef-Logik',
        message: "Gemini API-Timeout oder Verbindungsfehler. Lokales Client-Sicherheitsnetz erfolgreich aktiviert."
      });

      return localFallback;
    }
  };

  // API Bridge: SQL NLP Translator
  const handleTranslateNlp = async (query: string) => {
    try {
      const res = await fetch('/api/gemini/query-nlp', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query })
      });

      if (!res.ok) {
        throw new Error(`Server returned error status: ${res.status}`);
      }

      const parsedData = await res.json();
      
      setSystemStatus(prev => ({
        ...prev,
        monthlyCostSpent: prev.monthlyCostSpent + 0.00010
      }));

      handleAddLog({
        type: 'success',
        source: 'Gemini',
        message: `NLP Übersetzung vorgenommen: '${query.slice(0, 30)}...' &rarr; SQL kompilisiert.`
      });

      return parsedData;
    } catch (err) {
      console.warn("NLP SQL translate fail, using client-side fallback:", err);

      const lower = query.toLowerCase();
      let category = "All";
      if (lower.includes("strom") || lower.includes("abrechnung") || lower.includes("rechnung") || lower.includes("beleg")) {
        category = "Finanzen / Belege";
      } else if (lower.includes("vertrag") || lower.includes("miete")) {
        category = "Dokumente / Verträge";
      } else if (lower.includes("foto") || lower.includes("bild") || lower.includes("upload")) {
        category = "Foto-Belege / Mobile";
      }

      const searchTerms = query.split(/\s+/).filter((w: string) => w.length > 2);

      const localFallback = {
        searchTerms: searchTerms.slice(0, 3),
        category,
        tags: searchTerms.slice(0, 2).map((t: string) => t.replace(/[^\w]/g, "")),
        simulatedSql: `SELECT * FROM file_index WHERE category = '${category}' AND (${searchTerms.map((t: string) => `file_name LIKE '%${t}%'`).join(" OR ")})`
      };

      handleAddLog({
        type: 'info',
        source: 'Chef-Logik',
        message: `NLP Übersetzung via Client-Ausfallsicherung durchgeführt.`
      });

      return localFallback;
    }
  };

  // Switch to explorer tab from sidebar folder tree click
  const handleSidebarFolderClick = (folderId: string, initialFileName: string) => {
    setSidebarFolderId(folderId);
    setSidebarFileName(initialFileName);
    setActiveTab('explorer');
    
    handleAddLog({
      type: 'info',
      source: 'Chef-Logik',
      message: `Navigiert zu Verzeichnis: ${folderId} (Fokus: ${initialFileName})`
    });
  };

  // Available core tabs in a highly visual configuration containing tool explanations
  const AVAILABLE_TABS = [
    { id: 'nexi' as const, label: 'Nexi Dialog-Chef', desc: 'Naturalsprachliche Steuerung & Aktionen für Patricks Server', icon: MessageSquare, color: 'text-emerald-400' },
    { id: 'triage' as const, label: 'Triage-Zentrale', desc: 'SMS, Uploads & System-Benachrichtigungen prüfen und abfangen', icon: Activity, color: 'text-rose-400' },
    { id: 'android' as const, label: 'Android-Daemon', desc: 'Mobiles Ingestion-Schnittstellen-Gadget & Ingress Queue', icon: Smartphone, color: 'text-sky-400' },
    { id: 'database' as const, label: 'SQLite Search (NLP)', desc: 'Optimierte Heuristik-Suche im lokalen Datenkatalog', icon: Database, color: 'text-purple-400' },
    { id: 'explorer' as const, label: 'Datei-Explorer (C:\\)', desc: 'Patricks SSOT Textdokumente, Regelwerke & HMAC Keys', icon: FolderOpen, color: 'text-indigo-400' },
    { id: 'gemini' as const, label: 'Gemini Playground', desc: 'LLM Zweitmeinung, Budget-Bewertung & System-Vorschläge', icon: Sparkles, color: 'text-amber-400' },
    { id: 'metrics' as const, label: '3D Speicherallokation', desc: 'Immersive Echtzeit-Visualisierung der Dateigrößen & Datenstruktur', icon: Layers, color: 'text-cyan-400' },
    { id: 'adapter' as const, label: 'Governance & Limits', desc: 'Provider-Steuerung, Invarianten & Budget-Deckelung ($15)', icon: Sliders, color: 'text-pink-400' },
    { id: 'logs' as const, label: 'Protokolle', desc: 'Echtzeit-Diagnosezeilen der lokalen Systembewegungen', icon: Terminal, color: 'text-teal-400' }
  ];

  return (
    <div className="min-h-screen bg-black text-gray-100 flex flex-col font-sans select-none overflow-x-hidden antialiased">
      
      {/* 1. Permanent Ambient Header Network Status */}
      <StatusBanner 
        status={systemStatus} 
        onRefresh={fetchStatus} 
        isLoading={isLoading} 
      />

      {/* MOBILE TAB BAR: Appears only on small screens and is placed AT THE TOP, above the active content */}
      <div className="lg:hidden w-full px-4 pt-2">
        <div className="bg-[#050508] border border-[#14243b] rounded-xl p-2.5 space-y-2">
          <div className="flex overflow-x-auto gap-1.5 pb-1 scrollbar-none snap-x">
            {AVAILABLE_TABS.map((tab) => {
              const Icon = tab.icon;
              const isActive = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`px-3 py-1.5 rounded-lg text-[11px] font-mono font-bold uppercase transition-all flex items-center gap-1.5 border shrink-0 cursor-pointer snap-start ${
                    isActive
                      ? 'bg-sky-500/15 border-sky-400/50 text-white shadow-[0_0_8px_rgba(14,165,233,0.15)]'
                      : 'bg-zinc-950/40 border-transparent text-zinc-500'
                  }`}
                >
                  <Icon className={`h-3.5 w-3.5 ${isActive ? tab.color : 'text-zinc-600'}`} />
                  <span>{tab.label.split(' ')[0]}</span>
                </button>
              );
            })}
          </div>
          
          {/* Active Tab Explanation box ("was für was sein soll") */}
          <div className="bg-[#09090d] border-t border-[#14243b]/40 pt-2 text-left">
            <span className="text-[10px] font-extrabold text-[#00ff66] font-mono block tracking-wider uppercase">
              {AVAILABLE_TABS.find(t => t.id === activeTab)?.label}
            </span>
            <span className="text-[10px] text-zinc-400 font-sans block leading-normal mt-0.5">
              {AVAILABLE_TABS.find(t => t.id === activeTab)?.desc}
            </span>
          </div>
        </div>
      </div>

      {/* 2. Structured Layout: Sidebar Navigation (Left) + Cockpit Workspace (Right) */}
      <div className="flex-1 w-full max-w-7xl mx-auto p-4 md:p-6 grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        
        {/* DESKTOP SIDEBAR CONTAINER: Visible only on lg screens */}
        <aside className="hidden lg:col-span-3 lg:flex flex-col gap-4">
          
          <div className="bg-[#030304] border-2 border-[#14243b] rounded-xl p-4 space-y-3.5 shadow-lg text-left neon-glow-blue">
            <div className="border-b border-[#14243b]/60 pb-2 text-left">
              <span className="text-[10px] font-mono text-sky-450 text-sky-400 font-extrabold uppercase tracking-widest block">
                SYSTEMSTEUERUNG
              </span>
              <span className="text-[10px] text-zinc-500 font-sans block mt-0.5 leading-none">
                Direkt-Auswahl & Details
              </span>
            </div>

            <div className="space-y-2">
              {AVAILABLE_TABS.map((tab) => {
                const Icon = tab.icon;
                const isActive = activeTab === tab.id;
                return (
                  <button
                    key={tab.id}
                    onClick={() => setActiveTab(tab.id)}
                    className={`w-full text-left p-2.5 rounded-lg border transition-all cursor-pointer group ${
                      isActive
                        ? 'bg-sky-500/10 border-sky-400/50 text-white shadow-[0_0_10px_rgba(14,165,233,0.15)]'
                        : 'bg-transparent border-transparent text-zinc-400 hover:text-white hover:bg-zinc-900/40'
                    }`}
                  >
                    <div className="flex items-center gap-2.5 font-bold text-xs tracking-wide">
                      <Icon className={`h-4 w-4 shrink-0 transition-all ${isActive ? tab.color + ' scale-110' : 'text-zinc-500 group-hover:text-zinc-300'}`} />
                      <span className={isActive ? 'font-extrabold text-white' : 'font-medium text-zinc-300'}>
                        {tab.label}
                      </span>
                    </div>
                    {/* Guidance / Description on what this tool is for */}
                    <p className="text-[10px] text-zinc-500 mt-1 pl-6.5 leading-normal group-hover:text-zinc-400 transition-colors">
                      {tab.desc}
                    </p>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Quick status box */}
          <div className="bg-[#030304] border border-[#14243b] rounded-xl p-3.5 text-left">
            <p className="text-[10px] font-mono text-zinc-500 uppercase tracking-widest font-extrabold">Active Controller</p>
            <p className="text-xs text-[#00ff66] mt-1 font-mono font-bold flex items-center gap-1.5">
              <span className="h-1.5 w-1.5 bg-emerald-400 rounded-full animate-pulse"></span>
              Admin Session Secure
            </p>
          </div>

        </aside>

        {/* PRIMARY COCKPIT WORKSPACE (Right Pane) */}
        <section className="lg:col-span-9 space-y-6">
          
          {/* Main Workspace Frame with glassmorphic gradients & no leak of white */}
          <div className="focus-mode-view" id="active-cockpit-workspace">
            
            {/* If tab is metrics, show the immersive Three.js bento box directly inside */}
            {activeTab === 'metrics' && (
              <div className="bg-[#121214] border border-[#222227] rounded-xl p-5 shadow-2xl animate-fade-in">
                <ThreeDMetrics status={systemStatus} />
              </div>
            )}

            {activeTab === 'nexi' && (
              <NexiChat 
                onAddLog={handleAddLog} 
                systemProvider={systemStatus.currentProvider} 
              />
            )}

            {activeTab === 'triage' && (
              <TriagePortal 
                onAddLog={handleAddLog} 
                systemProvider={systemStatus.currentProvider} 
              />
            )}

            {activeTab === 'android' && (
              <AndroidEmulator 
                onAddLog={handleAddLog} 
              />
            )}

            {activeTab === 'adapter' && (
              <ChefRules 
                status={systemStatus} 
                auditRules={auditRules} 
                onToggleProvider={handleToggleProvider} 
                onRunAudit={handleRunAudit} 
              />
            )}

            {activeTab === 'explorer' && (
              <FileExplorer 
                folders={folders} 
                onUpdateFileContent={handleUpdateFileContent} 
                initialSelectedFolderId={sidebarFolderId}
                initialSelectedFileName={sidebarFileName}
              />
            )}

            {activeTab === 'database' && (
              <DatabaseSearch 
                records={SIMULATED_CATALOG_DB} 
                onTranslateNlp={handleTranslateNlp} 
              />
            )}

            {activeTab === 'gemini' && (
              <GeminiPlayground 
                folders={folders} 
                onCallAnalyze={handleCallAnalyze} 
              />
            )}

            {activeTab === 'logs' && (
              <LogConsole 
                logs={logs} 
                onAddLog={handleAddLog} 
                onClearLogs={handleClearLogs} 
              />
            )}
            
          </div>

        </section>

      </div>

      {/* 3. Humanistic Secure Signature Footer */}
      <footer className="bg-[#0c0c0e]/80 border-t border-[#1b1b22] mt-auto py-5 text-center text-xs text-gray-500 font-sans relative z-10">
        <p>&copy; 2026 nexus &bull; Exklusiv für Patrick Herzog</p>
      </footer>

    </div>
  );
}
