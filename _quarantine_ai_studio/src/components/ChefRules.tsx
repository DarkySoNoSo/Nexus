/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from 'react';
import { 
  ShieldCheck, 
  ShieldAlert, 
  AlertTriangle, 
  Coins, 
  EyeOff, 
  Bot, 
  Sliders, 
  CheckSquare, 
  Settings, 
  Play, 
  Send, 
  Sparkles, 
  Database, 
  Clock, 
  ArrowRight, 
  Check, 
  RefreshCw,
  Info 
} from 'lucide-react';
import { AuditRule, SystemStatus } from '../types';

interface ChefRulesProps {
  status: SystemStatus;
  auditRules: AuditRule[];
  onToggleProvider: (provider: SystemStatus['currentProvider']) => void;
  onRunAudit: (id: string) => void;
}

interface Scenario {
  id: string;
  name: string;
  desc: string;
  rawText: string;
  source: string;
  senderType: 'bekannt' | 'unbekannt' | 'familie' | 'dienstleister' | 'versicherung' | 'schule';
  tags: string[];
  relevanceScore: number;
  recommendedStatus: 'focus' | 'important' | 'done' | 'not_important' | 'needs_context' | 'needs_reply' | 'archived';
  plausibility: {
    sourceSecure: boolean;
    senderVerified: boolean;
    hasContradictions: boolean;
    hasDeadlines: boolean;
    needsPatrickContext: boolean;
  };
  draftReply: string;
  learningRule: string;
}

const SCENARIOS: Scenario[] = [
  {
    id: 'miete',
    name: 'Mietzinserhöhung (Wohnen AG)',
    desc: 'Bestehender Mietvertrag-Bezug mit Erhöhungsbeilage',
    rawText: 'Sehr geehrter Herr Herzog, aufgrund der Referenzzinssatz-Entwicklung erhöht sich der monatliche Mietzins für Ihr Mietobjekt an der Flurstrasse ab dem 01.10.2026 um 45.00 CHF. Ihren aktuellen Mietvertrag finden Sie indiziert unter context/dokumente/Mietvertrag.pdf.',
    source: 'SMS / PDF-Ingestion',
    senderType: 'dienstleister',
    tags: ['Geld', 'Recht', 'Frist', 'Wohnung'],
    relevanceScore: 95,
    recommendedStatus: 'focus',
    plausibility: {
      sourceSecure: true,
      senderVerified: true,
      hasContradictions: false,
      hasDeadlines: true,
      needsPatrickContext: false
    },
    draftReply: 'Guten Tag Wohnen AG, ich habe Ihre Erhöhungsanzeige über 45.00 CHF zur Kenntnis genommen und diese in meiner Buchhaltungsaufstellung (Mietvertrag.pdf) für den Stichtag 01.10.2026 abgeglichen. Viele Grüße, Patrick Herzog',
    learningRule: 'Dauerhafte Mietänderung ab Oktober 2026 vermerkt. Setzt automatischen Fokus-Eintrag für Budgetprüfung Schweizer Franken (CHF).'
  },
  {
    id: 'dhl',
    name: 'DHL Paket Phishing (Aussortiert)',
    desc: 'Unbekannte SMS mit Geld-Aufforderung & Fristdruck',
    rawText: 'Hallo Patrick! Dein Paket von DHL konnte wegen einer ausstehenden Zollgebühr von 1.99 EUR nicht geliefert werden. Bitte bezahle die Gebühr dringenst innerhalb 24h unter dhl-zoll-portal-de.xyz/verfolgung.',
    source: 'SMS',
    senderType: 'unbekannt',
    tags: ['Geld', 'Frist', 'Lieferung'],
    relevanceScore: 15,
    recommendedStatus: 'archived',
    plausibility: {
      sourceSecure: false,
      senderVerified: false,
      hasContradictions: true,
      hasDeadlines: true,
      needsPatrickContext: true
    },
    draftReply: '[Blockierte Systemnachricht] Keine Formulierung erlaubt. Phishing-Schutz hat zugeschlagen.',
    learningRule: 'Aussortieren falscher DHL-Logistik-Links. Sperre automatische Antwort und Archiviere ohne Patrick zu stören.'
  },
  {
    id: 'schule',
    name: 'Waldtag der Primarschule',
    desc: 'Klasseninformation mit Termin- und Rückfragefrist',
    rawText: 'Liebe Eltern der Klasse 3b, am kommenden Donnerstag findet unser herbstlicher Waldtag statt. Bitte geben Sie Ihren Kindern regenfeste Kleidung, Wanderschuhe und einen kleinen Mittagssnack mit. Um eine kurze Rückmeldung zur Teilnahme bis Dienstag um 18:00 Uhr wird dringend gebeten.',
    source: 'Gmail Ingestion',
    senderType: 'schule',
    tags: ['Kind/Familie', 'Termin', 'Frist'],
    relevanceScore: 80,
    recommendedStatus: 'needs_reply',
    plausibility: {
      sourceSecure: true,
      senderVerified: true,
      hasContradictions: false,
      hasDeadlines: true,
      needsPatrickContext: false
    },
    draftReply: 'Hallo zusammen, danke für die Information! Mein Kind ist am Donnerstag beim Waldtag mit regenfester Ausrüstung dabei. Rückmeldung hiermit fristgerecht erledigt. Herzliche Grüße, Patrick Herzog',
    learningRule: 'Eintrag im Familienkalender für nächsten Donnerstag vorgenommen. Erzeuge Antwort-Entwurf zur manuellen Absendung vor Dienstag.'
  },
  {
    id: 'visa',
    name: 'VISA Abbuchung (28.40 CHF)',
    desc: 'Echtzeit-Push einer autorisierten Kartenzahlung',
    rawText: 'Zahlungs-Bestätigung: Am 14.06.2026 wurden 28,40 CHF bei LADE_STATION_WETZIKON erfolgreich abgebucht. Autorisiert über VISA-Karte-Endziffer #4811.',
    source: 'App Notification',
    senderType: 'dienstleister',
    tags: ['Geld'],
    relevanceScore: 90,
    recommendedStatus: 'done',
    plausibility: {
      sourceSecure: true,
      senderVerified: true,
      hasContradictions: false,
      hasDeadlines: false,
      needsPatrickContext: false
    },
    draftReply: 'Automatisch im Masterindex verbucht. Keine manuelle Rückäußerung notwendig.',
    learningRule: 'Double-Entry-Zuordnung: Ladungsgebühr über 28,40 CHF der VISA #4811 erfolgreich zugewiesen. Budget-Schnittstelle im grünen Bereich.'
  }
];

export const ChefRules: React.FC<ChefRulesProps> = ({
  status,
  auditRules,
  onToggleProvider,
  onRunAudit
}) => {
  // Simulator State
  const [activeScenarioId, setActiveScenarioId] = useState<string>('miete');
  const [scenarioText, setScenarioText] = useState<string>('');
  const [overrideStatus, setOverrideStatus] = useState<Scenario['recommendedStatus']>('focus');
  const [localPlausibility, setLocalPlausibility] = useState<Scenario['plausibility']>({
    sourceSecure: true,
    senderVerified: true,
    hasContradictions: false,
    hasDeadlines: true,
    needsPatrickContext: false
  });
  const [learningScope, setLearningScope] = useState<'dauerhaft' | 'temporaer' | 'personenbezogen' | 'einmalig'>('dauerhaft');
  const [simulatedLogs, setSimulatedLogs] = useState<string[]>([
    'System: Initialisiere lokalen Evaluator-Kern...',
    'Chef-Logik: Analysiere Schema nexus_catalog.sqlite...',
    'Evaluator: Bereit für Echtzeitsimulation.'
  ]);
  const [showConfettiToast, setShowConfettiToast] = useState<boolean>(false);

  // Sync state with selected scenario preset
  useEffect(() => {
    const current = SCENARIOS.find(s => s.id === activeScenarioId);
    if (current) {
      setScenarioText(current.rawText);
      setOverrideStatus(current.recommendedStatus);
      setLocalPlausibility(current.plausibility);
      addSimulatedLog(`Szenario geladen: "${current.name}" - Analysiere Metadaten...`);
    } else {
      // Custom scenario defaults
      setScenarioText('');
      setOverrideStatus('needs_context');
      setLocalPlausibility({
        sourceSecure: false,
        senderVerified: false,
        hasContradictions: false,
        hasDeadlines: false,
        needsPatrickContext: true
      });
    }
  }, [activeScenarioId]);

  const addSimulatedLog = (msg: string) => {
    const time = new Date().toLocaleTimeString('de-DE');
    setSimulatedLogs(prev => [...prev.slice(-15), `[${time}] ${msg}`]);
  };

  // Live relevance calculation based on text indicators
  const calculateLiveRelevance = (): number => {
    let score = 20;
    const lower = scenarioText.toLowerCase();
    
    if (lower.includes('chf') || lower.includes('eur') || lower.includes('zoll') || lower.includes('miete') || lower.includes('budget')) {
      score += 25; // Money tags
    }
    if (lower.includes('frist') || lower.includes('dringend') || lower.includes('dienstag') || lower.includes('termin') || lower.includes('donnerstag')) {
      score += 25; // Time tags
    }
    if (lower.includes('kind') || lower.includes('primarschule') || lower.includes('eltern') || lower.includes('familie')) {
      score += 20; // Family tags
    }
    if (lower.includes('mietvertrag') || lower.includes('vertrag') || lower.includes('recht')) {
      score += 15; // Legal tags
    }
    if (lower.includes('verfolgung') || lower.includes('paket') || lower.includes('dhl')) {
      score += 5; // Delivery
    }

    if (activeScenarioId === 'dhl') return 15; // Phishing preset limit
    return Math.min(score, 100);
  };

  const getStatusColor = (v: Scenario['recommendedStatus']) => {
    switch (v) {
      case 'focus': return 'bg-rose-500/10 border-rose-500/30 text-rose-400';
      case 'important': return 'bg-amber-500/10 border-amber-500/30 text-amber-400';
      case 'done': return 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400';
      case 'not_important': return 'bg-zinc-500/10 border-white/5 text-gray-400';
      case 'needs_context': return 'bg-purple-500/10 border-purple-500/30 text-purple-400';
      case 'needs_reply': return 'bg-blue-500/10 border-blue-500/30 text-blue-400';
      case 'archived': return 'bg-zinc-800/40 border-white/5 text-zinc-500';
    }
  };

  const handleTransmitDecision = () => {
    addSimulatedLog(`Starte Transaktion für SQLite Master-Katalog (v40.44)...`);
    addSimulatedLog(`BOM-Sicherer Export (UTF-8-sig) wird erzwungen.`);
    addSimulatedLog(`Prüfe Plausibilität: SourceSecured(${localPlausibility.sourceSecure}) Vertrauenswürdig(${localPlausibility.senderVerified})`);
    
    if (localPlausibility.hasContradictions && overrideStatus !== 'archived') {
      addSimulatedLog(`Halt! Plausibilitätsfehler: Widersprüchliche Angaben im Dossier gefunden.`);
      alert("Warnung: Plausibilitäts-Widersprüche verbieten eine sofortige reguläre Verbuchung! Bitte auf 'Archiviert' oder 'Zusatzkontext benötigt' setzen.");
      return;
    }

    addSimulatedLog(`[SQL] INSERT INTO chef_rules_v40 (ruleset, status, decision) VALUES ('${overrideStatus}', 'VERIFIED', 'BOM-sig-saved')`);
    addSimulatedLog(`[LEDGER] Registriere DRAFT Änderung in NEXUS_CHANGE_DRAFT_LEDGER.md`);
    addSimulatedLog(`Erfolgreich übertragen! Patrick wurde per ntfy-Push benachrichtigt.`);
    
    setShowConfettiToast(true);
    setTimeout(() => {
      setShowConfettiToast(false);
    }, 4500);
  };

  return (
    <div className="space-y-6">
      {/* Toast Alert */}
      {showConfettiToast && (
        <div className="fixed bottom-6 right-6 bg-[#162720]/95 backdrop-blur border-2 border-emerald-500/40 text-emerald-300 px-4 py-3 rounded-xl shadow-2xl flex items-center gap-3 animate-bounce z-50 max-w-sm">
          <div className="h-2.5 w-2.5 rounded-full bg-emerald-400 animate-ping"></div>
          <div className="text-xs font-sans leading-relaxed">
            <p className="font-bold">Eintrag erfolgreich verbucht!</p>
            <p className="text-[10px] text-gray-400 font-mono mt-0.5">sqlite_catalog.db [BOM-Safe-UTF8] aktualisiert</p>
          </div>
        </div>
      )}

      {/* Provider-Adapter Selector */}
      <div className="bg-[#121214] border border-[#222227] rounded-xl p-6 shadow-2xl">
        <div className="flex items-center gap-2 mb-4">
          <Settings className="h-5 w-5 text-indigo-400" />
          <h2 className="text-base font-bold font-sans tracking-tight text-white">
            Intelligenter Provider-Adapter
          </h2>
        </div>
        <p className="text-xs text-gray-400 mb-5 leading-relaxed">
          Modelle werden nicht wild miteinander vermischt. Der Register-Chef steuert dediziert, für welchen Zweck welcher Provider freigesetzt wird, und riegelt Budgets lokal ab:
        </p>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {/* Option A: OpenAI */}
          <button
            onClick={() => onToggleProvider('OpenAI')}
            className={`text-left p-4 rounded-lg border transition-all cursor-pointer ${
              status.currentProvider === 'OpenAI'
                ? 'border-indigo-500 bg-indigo-500/10 ring-1 ring-indigo-500 text-white'
                : 'border-[#222227] hover:border-[#3a3a45] bg-[#0c0c0e] text-gray-400 hover:text-white'
            }`}
            id="provider-openai-btn"
          >
            <div className="flex items-center justify-between mb-2">
              <span className="text-[10px] font-mono font-bold uppercase tracking-wider text-indigo-400 bg-indigo-500/10 px-2 py-0.5 rounded">
                Stufe A: OpenAI API
              </span>
              <div className={`h-2.5 w-2.5 rounded-full ${status.currentProvider === 'OpenAI' ? 'bg-indigo-400' : 'bg-gray-800'}`}></div>
            </div>
            <h3 className="text-xs font-bold text-gray-200 mb-1">Chef-Hauptlogik</h3>
            <p className="text-[11px] text-gray-500 leading-relaxed">
              Zuständig für die Hauptagenda, das Sortieren kritischer SQLite-Kataloge und System-Einträgen.
            </p>
          </button>

          {/* Option B: Gemini */}
          <button
            onClick={() => onToggleProvider('Gemini')}
            className={`text-left p-4 rounded-lg border transition-all cursor-pointer ${
              status.currentProvider === 'Gemini'
                ? 'border-sky-500 bg-sky-500/10 ring-1 ring-sky-500 text-white'
                : 'border-[#222227] hover:border-[#3a3a45] bg-[#0c0c0e] text-gray-400 hover:text-white'
            }`}
            id="provider-gemini-btn"
          >
            <div className="flex items-center justify-between mb-2">
              <span className="text-[10px] font-mono font-bold uppercase tracking-wider text-sky-400 bg-sky-500/10 px-2 py-0.5 rounded">
                Stufe B: Gemini API
              </span>
              <div className={`h-2.5 w-2.5 rounded-full ${status.currentProvider === 'Gemini' ? 'bg-sky-400' : 'bg-gray-800'}`}></div>
            </div>
            <h3 className="text-xs font-bold text-gray-200 mb-1">Zweitbeinige Prüfung</h3>
            <p className="text-[11px] text-gray-500 leading-relaxed">
              Ideal für Kommunikationsanalyse, Rechnungsprüfungen, Hochgeschwindigkeits-Zusammenfassungen und robusten Fallback.
            </p>
          </button>

          {/* Option C: Local-Offline */}
          <button
            onClick={() => onToggleProvider('Local-Offline')}
            className={`text-left p-4 rounded-lg border transition-all cursor-pointer ${
              status.currentProvider === 'Local-Offline'
                ? 'border-teal-500 bg-[#162720]/30 ring-1 ring-teal-500 text-white'
                : 'border-[#222227] hover:border-[#3a3a45] bg-[#0c0c0e] text-gray-400 hover:text-white'
            }`}
            id="provider-offline-btn"
          >
            <div className="flex items-center justify-between mb-2">
              <span className="text-[10px] font-mono font-bold uppercase tracking-wider text-teal-400 bg-teal-500/10 px-2 py-0.5 rounded">
                Lock: Offline Heuristik
              </span>
              <div className={`h-2.5 w-2.5 rounded-full ${status.currentProvider === 'Local-Offline' ? 'bg-teal-400' : 'bg-gray-800'}`}></div>
            </div>
            <h3 className="text-xs font-bold text-gray-200 mb-1">Eiserner Kosten-Riegel</h3>
            <p className="text-[11px] text-gray-500 leading-relaxed">
              Zwingt das System bei Überschreitung des Monatslimits ($15.00) oder sensiblen Daten zu reinen Offline-Heuristiken.
            </p>
          </button>
        </div>

        {/* Budget stats progress */}
        <div className="mt-5 bg-[#09090b] rounded-lg p-4 border border-[#222227] flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <Coins className="h-5 w-5 text-amber-500 shrink-0" />
            <div>
              <p className="text-[10px] text-gray-500 font-medium font-sans">Akkumuliertes Monatsbudget (Juni 2026)</p>
              <p className="text-sm font-mono font-bold text-white">
                ${status.monthlyCostSpent.toFixed(5)} <span className="text-gray-500 text-xs">/ ${status.monthlyCostLimit.toFixed(2)} USD</span>
              </p>
            </div>
          </div>
          <div className="flex-1 max-w-md">
            <div className="w-full bg-[#1c1c22] rounded-full h-2 overflow-hidden border border-white/5">
              <div
                className="bg-amber-450 bg-amber-500 h-2 rounded-full transition-all duration-500"
                style={{ width: `${Math.min((status.monthlyCostSpent / status.monthlyCostLimit) * 100, 100)}%` }}
              ></div>
            </div>
            <p className="text-[10px] text-gray-500 mt-1.5 text-right font-mono">
              Verbraucht: {Math.min((status.monthlyCostSpent / status.monthlyCostLimit) * 100, 100).toFixed(5)}%
            </p>
          </div>
        </div>
      </div>

      {/* Interactive Simulator: Evaluator and Plausibility */}
      <div className="bg-[#121214] border border-[#222227] rounded-xl p-6 shadow-2xl relative overflow-hidden">
        <div className="absolute top-0 right-0 w-64 h-64 bg-indigo-500/5 rounded-full blur-[100px] pointer-events-none"></div>
        <div className="absolute bottom-0 left-0 w-64 h-64 bg-emerald-500/5 rounded-full blur-[100px] pointer-events-none"></div>

        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-6">
          <div className="flex items-center gap-2">
            <Sparkles className="h-5 w-5 text-indigo-400" />
            <h2 className="text-base font-bold font-sans tracking-tight text-white">
              Echtzeit-Entscheidungs-Simulator des Index-Chefs
            </h2>
          </div>
          <span className="text-[9px] font-mono text-indigo-400 bg-indigo-500/15 px-2.5 py-1 rounded border border-indigo-500/20 uppercase tracking-widest font-extrabold shadow-sm">
            Triage & Plausibilität v40.44
          </span>
        </div>

        <p className="text-xs text-gray-400 mb-6 leading-relaxed">
          Dieses System evaluiert alle eingehenden Benachrichtigungen, Fotos oder Dokumente nach dem offiziellen 
          <code className="text-emerald-400 bg-black/40 px-1 py-0.5 rounded font-mono ml-1">Bewertungsmodell</code> und prüft sie auf Plausibilität bezüglich bekannter Bestandsdossiers (z.B. Miete, Verträge), um risikolos zu agieren.
        </p>

        {/* Preset Selector Grid */}
        <div className="mb-6">
          <span className="text-[10px] font-mono text-gray-500 uppercase tracking-wider block mb-2.5 font-bold">
            1. Szenario-Vorlage wählen
          </span>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-2.5">
            {SCENARIOS.map(item => (
              <button
                key={item.id}
                onClick={() => {
                  setActiveScenarioId(item.id);
                  addSimulatedLog(`Szenario geändert zu: ${item.name}`);
                }}
                className={`p-3 text-left rounded-lg border text-xs font-sans transition-all cursor-pointer ${
                  activeScenarioId === item.id
                    ? 'border-indigo-500 bg-indigo-500/10 text-white font-bold shadow-md'
                    : 'border-[#222227] bg-[#0c0c0e] hover:border-[#333] hover:bg-[#121215] text-gray-400'
                }`}
                id={`scenario-tab-${item.id}`}
              >
                <div className="truncate">{item.name}</div>
                <div className={`text-[10px] mt-1 font-mono truncate font-normal ${activeScenarioId === item.id ? 'text-indigo-300' : 'text-gray-500'}`}>
                  {item.source}
                </div>
              </button>
            ))}
          </div>
        </div>

        {/* Scenario Input and Parameters */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Column A: Raw text and parameters */}
          <div className="lg:col-span-7 space-y-4 text-left">
            <div>
              <label className="text-[10px] font-mono text-gray-500 uppercase tracking-wider block mb-2 font-bold flex justify-between">
                <span>2. Rohdaten-Eingang des Index-Chefs</span>
                <span className="text-[9px] text-[#00ff66]">LIVE EDTIERBAR</span>
              </label>
              <textarea
                value={scenarioText}
                onChange={(e) => {
                  setScenarioText(e.target.value);
                  addSimulatedLog('Vollwertige Chat- oder Dateianalyse getriggert...');
                }}
                placeholder="Trage hier eine beliebige Testnachricht ein..."
                className="w-full bg-[#09090b] text-gray-250 border border-[#222227] rounded-lg p-3 text-xs font-mono h-28 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 leading-relaxed shadow-inner"
              ></textarea>
            </div>

            {/* Ingestion Indicators */}
            <div className="grid grid-cols-2 gap-3.5">
              <div className="bg-[#0c0c0e] border border-[#1b1b22] px-3.5 py-3 rounded-lg">
                <span className="text-[9px] font-mono text-gray-500 block uppercase font-bold">Kanalquelle (Ebene 2)</span>
                <span className="text-gray-200 text-xs font-medium font-sans flex items-center gap-1.5 mt-1">
                  <Database className="h-3.5 w-3.5 text-indigo-400" />
                  {SCENARIOS.find(s => s.id === activeScenarioId)?.source || 'Manueller Input'}
                </span>
              </div>
              <div className="bg-[#0c0c0e] border border-[#1b1b22] px-3.5 py-3 rounded-lg">
                <span className="text-[9px] font-mono text-gray-500 block uppercase font-bold">Absender-Gruppe</span>
                <span className="text-gray-200 text-xs font-medium uppercase font-sans mt-1 block">
                  {SCENARIOS.find(s => s.id === activeScenarioId)?.senderType || 'Unbekannt'}
                </span>
              </div>
            </div>

            {/* Plausibility Validation checkboxes */}
            <div className="bg-[#0c0c0e] border border-[#222227] p-4 rounded-lg">
              <h4 className="text-[10px] font-mono text-gray-500 uppercase tracking-wider mb-2.5 font-bold flex items-center gap-1.5">
                <ShieldCheck className="h-3.5 w-3.5 text-indigo-400" />
                3. Plausibilitätstests (Index Chef Regelwerk)
              </h4>
              <div className="space-y-2 select-none">
                <label className="flex items-center gap-2.5 text-xs text-gray-300 cursor-pointer hover:text-white">
                  <input
                    type="checkbox"
                    checked={localPlausibility.sourceSecure}
                    onChange={(e) => {
                      setLocalPlausibility(p => ({ ...p, sourceSecure: e.target.checked }));
                      addSimulatedLog(`Klassifizierung: Quelle unmanipuliert -> ${e.target.checked}`);
                    }}
                    className="rounded border-[#2d2d3a] bg-[#0c0c0e] text-indigo-500 focus:ring-0"
                  />
                  <span>Datenquelle kryptografisch gesichert / unmanipuliert</span>
                </label>
                <label className="flex items-center gap-2.5 text-xs text-gray-300 cursor-pointer hover:text-white">
                  <input
                    type="checkbox"
                    checked={localPlausibility.senderVerified}
                    onChange={(e) => {
                      setLocalPlausibility(p => ({ ...p, senderVerified: e.target.checked }));
                      addSimulatedLog(`Klassifizierung: Absender-Eindeutigkeit -> ${e.target.checked}`);
                    }}
                    className="rounded border-[#2d2d3a] bg-[#0c0c0e] text-indigo-500 focus:ring-0"
                  />
                  <span>Absender im sqlite_catalog.db zweifelsfrei verifiziert</span>
                </label>
                <label className="flex items-center gap-2.5 text-xs text-gray-300 cursor-pointer hover:text-white">
                  <input
                    type="checkbox"
                    checked={localPlausibility.hasContradictions}
                    onChange={(e) => {
                      setLocalPlausibility(p => ({ ...p, hasContradictions: e.target.checked }));
                      addSimulatedLog(`Warnung: Widerspruch zu bestehendem Dossier -> ${e.target.checked}`);
                    }}
                    className="rounded border-[#2d2d3a] bg-[#0c0c0e] text-indigo-500 focus:ring-0 animate-pulse-subtle"
                  />
                  <span className={localPlausibility.hasContradictions ? 'text-amber-400 font-medium' : ''}>
                    Widerspruch zu bekannten Archiv-Dossiers vorhanden
                  </span>
                </label>
                <label className="flex items-center gap-2.5 text-xs text-gray-300 cursor-pointer hover:text-white">
                  <input
                    type="checkbox"
                    checked={localPlausibility.hasDeadlines}
                    onChange={(e) => {
                      setLocalPlausibility(p => ({ ...p, hasDeadlines: e.target.checked }));
                      addSimulatedLog(`Klassifizierung: Fristbindung erkannt -> ${e.target.checked}`);
                    }}
                    className="rounded border-[#2d2d3a] bg-[#0c0c0e] text-indigo-500 focus:ring-0"
                  />
                  <span>Zeitkritischer Handlungsbedarf / Fristbindung vorhanden</span>
                </label>
              </div>
            </div>
          </div>

          {/* Column B: Live Analysis & Timeline Recommendation */}
          <div className="lg:col-span-5 space-y-4 text-left">
            {/* Live Metrics Meter */}
            <div className="bg-[#09090c] border border-[#222227] p-4.5 rounded-lg">
              <span className="text-[9px] font-mono text-gray-500 uppercase tracking-wider block mb-1 font-bold">
                Live Relevanz-Minder (Bewertungsmodell)
              </span>
              <div className="flex items-baseline gap-2 mb-2">
                <span className="text-2xl font-mono font-extrabold text-[#00ff66]">
                  {calculateLiveRelevance()}%
                </span>
                <span className="text-[10px] text-gray-500 uppercase font-mono">
                  Score
                </span>
              </div>
              <div className="w-full bg-[#181822] rounded-full h-1.5 overflow-hidden">
                <div
                  className="bg-indigo-500 h-1.5 rounded-full transition-all duration-700"
                  style={{ width: `${calculateLiveRelevance()}%` }}
                ></div>
              </div>
              
              {/* Dynamic Content Badges */}
              <div className="flex flex-wrap gap-1.5 mt-4">
                {calculateLiveRelevance() > 20 && (
                  <span className="text-[9px] font-mono bg-indigo-500/10 text-indigo-300 border border-indigo-500/20 px-2 py-0.5 rounded">
                    #Ingested
                  </span>
                )}
                {scenarioText.toLowerCase().includes('chf') && (
                  <span className="text-[9px] font-mono bg-emerald-500/10 text-emerald-300 border border-emerald-500/20 px-2 py-0.5 rounded">
                    #Finanzen-Geld
                  </span>
                )}
                {scenarioText.toLowerCase().includes('mietvertrag') && (
                  <span className="text-[9px] font-mono bg-cyan-500/10 text-cyan-300 border border-cyan-500/20 px-2 py-0.5 rounded">
                    #Miete-Recht
                  </span>
                )}
                {scenarioText.toLowerCase().includes('waldtag') && (
                  <span className="text-[9px] font-mono bg-pink-500/10 text-pink-300 border border-pink-500/20 px-2 py-0.5 rounded">
                    #Kind-Familie
                  </span>
                )}
                {scenarioText.toLowerCase().includes('dhl') && (
                  <span className="text-[9px] font-mono bg-amber-500/10 text-amber-300 border border-amber-500/20 px-2 py-0.5 rounded">
                    #Lieferung-Zoll
                  </span>
                )}
                {localPlausibility.hasDeadlines && (
                  <span className="text-[9px] font-mono bg-rose-500/10 text-rose-300 border border-rose-500/20 px-2 py-0.5 rounded animate-pulse-subtle">
                    #Fristgebunden
                  </span>
                )}
              </div>
            </div>

            {/* Recommended Status with custom override selector */}
            <div>
              <label className="text-[10px] font-mono text-gray-500 uppercase tracking-wider block mb-1.5 font-bold">
                4. Zuweisung Zeitstrahl-Status (Ueberschreiben)
              </label>
              <select
                value={overrideStatus}
                onChange={(e) => {
                  const val = e.target.value as Scenario['recommendedStatus'];
                  setOverrideStatus(val);
                  addSimulatedLog(`Zeitstrahl-Kategorie manuell deklariert zu: ${val}`);
                }}
                className="w-full bg-[#0c0c0e] border border-[#222227] rounded-lg p-2.5 text-xs text-gray-300 focus:outline-none focus:border-indigo-500 cursor-pointer font-sans"
              >
                <option value="focus">🔥 Focus (Akuter Handlungsbedarf heute)</option>
                <option value="important">⭐ Important (Langfristig wichtig / Ablage)</option>
                <option value="done">✅ Done (Automatisch erledigt & verbucht)</option>
                <option value="not_important">🔇 Not Important (Sinnloses Rauschen)</option>
                <option value="needs_context">❓ Needs Context (Inkonsistent / Belege fehlen)</option>
                <option value="needs_reply">✉️ Needs Reply (Antwort-Zutrieb empfohlen)</option>
                <option value="archived">🗑️ Archived (Systemseitig isoliert / Phishing)</option>
              </select>
            </div>

            {/* Kontext-Lernmodell-Zuweisung */}
            <div>
              <label className="text-[10px] font-mono text-gray-500 uppercase tracking-wider block mb-1.5 font-bold">
                5. Kontextlern-Klassifizierung
              </label>
              <div className="grid grid-cols-2 gap-1.5">
                {[
                  { id: 'dauerhaft', label: 'Dauerhafte Regel' },
                  { id: 'temporaer', label: 'Begrenzter Kontext' },
                  { id: 'personenbezogen', label: 'Beziehung (Familie)' },
                  { id: 'einmalig', label: 'Einmaliger Kontext' }
                ].map(op => (
                  <button
                    key={op.id}
                    onClick={() => {
                      setLearningScope(op.id as any);
                      addSimulatedLog(`Kontext verknüpft als: ${op.label}`);
                    }}
                    className={`p-2 text-center text-[10px] font-sans rounded border cursor-pointer transition ${
                      learningScope === op.id
                        ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300'
                        : 'border-[#222227] bg-[#0c0c0e] text-gray-500 hover:text-gray-300'
                    }`}
                  >
                    {op.label}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Learning rule Output & AI proposal Draft */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-6">
          <div className="bg-[#09090b] border border-[#222227] p-4 rounded-lg">
            <span className="text-[9px] font-mono text-gray-500 block uppercase font-bold mb-1.5">
              Abgeleitete Lernregel (Index-Systemschluss)
            </span>
            <p className="text-xs text-gray-300 font-sans leading-relaxed">
              {SCENARIOS.find(s => s.id === activeScenarioId)?.learningRule || 
               'Abgleich läuft. Ermittle heuristische Muster zur Abspeicherung im Windows Master-Index...'}
            </p>
          </div>

          <div className="bg-[#09090b] border border-[#222227] p-4 rounded-lg">
            <span className="text-[9px] font-mono text-gray-500 block uppercase font-bold mb-1.5">
              Vorbereiteter Antwortentwurf (Asynchron)
            </span>
            <p className="text-xs text-indigo-300 font-mono italic leading-relaxed">
              "{SCENARIOS.find(s => s.id === activeScenarioId)?.draftReply || 
               'Kein Antwortentwurf erforderlich für diesen Nachrichtentyp.'}"
            </p>
          </div>
        </div>

        {/* Executor Trigger Console and Submit button */}
        <div className="mt-6 pt-5 border-t border-[#222227] flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
          <div className="flex-1 text-left">
            <div className="font-mono text-[10px] text-gray-500 uppercase tracking-widest font-extrabold flex items-center gap-1.5 mb-1.5">
              <Clock className="h-3 w-3 text-sky-400" />
              INTEGRIERTES ENTSCHEIDUNGS-PROTOKOLL
            </div>
            <div className="bg-[#07070a] border border-[#222227] rounded-lg p-2.5 h-20 overflow-y-auto font-mono text-[10px] text-gray-400 space-y-1 scrollbar-thin select-all">
              {simulatedLogs.map((log, i) => (
                <div key={i} className="truncate">{log}</div>
              ))}
            </div>
          </div>

          <button
            onClick={handleTransmitDecision}
            className="bg-indigo-600 hover:bg-indigo-500 font-sans cursor-pointer text-white font-bold text-xs py-3.5 px-6 rounded-lg transition-all shadow-lg hover:shadow-indigo-500/20 flex items-center justify-center gap-2 shrink-0 border border-indigo-400/20 active:scale-[0.98]"
          >
            <Send className="h-4 w-4 shrink-0" />
            <span>Zur Triage-Freigabe senden (v40.44 BOM-Safe)</span>
          </button>
        </div>
      </div>

      {/* Pillars of Chef Logic */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Core Pillars info */}
        <div className="bg-[#121214] border border-[#222227] rounded-xl p-6 shadow-2xl flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-2 mb-4">
              <Bot className="h-5 w-5 text-indigo-400" />
              <h2 className="text-base font-bold font-sans tracking-tight text-white">
                Die 4 Säulen der Chef-Logik
              </h2>
            </div>
            <ul className="space-y-4">
              <li className="flex items-start gap-3">
                <span className="w-5 h-5 rounded-full bg-indigo-500/10 text-indigo-300 font-mono text-[10px] font-bold flex items-center justify-center shrink-0 mt-0.5 border border-indigo-500/20">1</span>
                <div>
                  <h4 className="text-xs font-semibold text-gray-200">Nachvollziehbarkeit (SQLite Logging)</h4>
                  <p className="text-[11px] text-gray-400 leading-relaxed mt-0.5">Jede Aktion schreibt Roh-Prompts, Tokens und Attributionen fälschungssicher in <code className="font-mono text-[11px] bg-black/40 border border-[#222227] px-1 py-0.5 rounded text-emerald-400">sqlite_catalog.db</code>.</p>
                </div>
              </li>
              <li className="flex items-start gap-3">
                <span className="w-5 h-5 rounded-full bg-indigo-500/10 text-indigo-300 font-mono text-[10px] font-bold flex items-center justify-center shrink-0 mt-0.5 border border-indigo-500/20">2</span>
                <div>
                  <h4 className="text-xs font-semibold text-gray-200">Autonomie-Grenze (Schreibsperren)</h4>
                  <p className="text-[11px] text-gray-400 leading-relaxed mt-0.5">Keine KI-Modelle dürfen direkt Termine anlegen, Mails versenden oder Dateien eigenständig auf der SSD verschieben.</p>
                </div>
              </li>
              <li className="flex items-start gap-3">
                <span className="w-5 h-5 rounded-full bg-indigo-500/10 text-indigo-300 font-mono text-[10px] font-bold flex items-center justify-center shrink-0 mt-0.5 border border-indigo-500/20">3</span>
                <div>
                  <h4 className="text-xs font-semibold text-gray-200">Double-Entry Safechecks</h4>
                  <p className="text-[11px] text-gray-400 leading-relaxed mt-0.5">Antworten bedürfen einer manuellen Bestätigung via PC oder Handy (X-Nexus-Mobile-Auth).</p>
                </div>
              </li>
              <li className="flex items-start gap-3">
                <span className="w-5 h-5 rounded-full bg-indigo-500/10 text-indigo-300 font-mono text-[10px] font-bold flex items-center justify-center shrink-0 mt-0.5 border border-indigo-500/20">4</span>
                <div>
                  <h4 className="text-xs font-semibold text-gray-250">Schutz vor AI-Slop</h4>
                  <p className="text-[11px] text-gray-400 leading-relaxed mt-0.5">Deutliche, ungeschminkte, wahrheitsgetreue Einbindung von Rohdaten statt ausschweifender Werbeprosa.</p>
                </div>
              </li>
            </ul>
          </div>
          <div className="mt-5 pt-4 border-t border-[#222227] flex items-center gap-1.5 text-[10px] text-gray-500 font-mono">
            <EyeOff className="h-3.5 w-3.5 text-indigo-400" />
            Daten-Sicherheit: Alle Anfragen enden im privaten Cloud Endpoint.
          </div>
        </div>

        {/* Audit compliance checklists */}
        <div className="bg-[#121214] border border-[#222227] rounded-xl p-6 shadow-2xl">
          <div className="flex items-center gap-2 mb-4">
            <CheckSquare className="h-5 w-5 text-indigo-400" />
            <h2 className="text-base font-bold font-sans tracking-tight text-white">
              Red-Team Audit & Compliance
            </h2>
          </div>
          <p className="text-xs text-gray-500 mb-4 leading-relaxed">
            Sicherheitsaudits des lokalen Systems prüfen laufend geöffnete Ports, unverschlüsselte Key-Pfade und die Einhaltung der Datenschutzrichtlinien:
          </p>

          <div className="space-y-3 max-h-[380px] overflow-y-auto scrollbar-thin">
            {auditRules.map((rule) => (
              <div
                key={rule.id}
                className={`p-3.5 rounded-lg border transition-all ${
                  rule.status === 'passed'
                    ? 'border-emerald-500/20 bg-emerald-500/5'
                    : rule.status === 'warning'
                    ? 'border-amber-500/20 bg-amber-500/5'
                    : 'border-rose-500/20 bg-rose-500/5'
                }`}
              >
                <div className="flex items-start justify-between gap-2 text-left">
                  <div>
                    <span className="text-[9px] font-mono text-gray-500 uppercase tracking-wider block mb-0.5">
                      Check: {rule.category}
                    </span>
                    <h4 className="text-xs font-bold text-gray-200 font-sans tracking-tight">
                      {rule.title}
                    </h4>
                    <p className="text-[11px] text-gray-400 mt-1 mb-1 leading-normal font-sans">
                      {rule.rule}
                    </p>
                    <p className={`text-[11px] mt-1 font-mono font-medium ${
                      rule.status === 'passed' ? 'text-emerald-400' : rule.status === 'warning' ? 'text-amber-400' : 'text-rose-400'
                    }`}>
                      &rarr; {rule.feedback}
                    </p>
                  </div>

                  <button
                    onClick={() => onRunAudit(rule.id)}
                    className={`shrink-0 flex items-center gap-1 text-[10px] font-mono font-bold px-2 py-1 rounded border transition-colors cursor-pointer ${
                      rule.status === 'passed'
                        ? 'bg-[#182a1f] text-emerald-300 border-emerald-900/50 hover:bg-[#203a2a]'
                        : rule.status === 'warning'
                        ? 'bg-[#2b2418] text-amber-300 border-amber-900/50 hover:bg-[#3d3220]'
                        : 'bg-[#2b181a] text-rose-300 border-rose-900/50 hover:bg-[#3c1f22]'
                    }`}
                    id={`audit-trigger-${rule.id}`}
                  >
                    Re-Test
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
