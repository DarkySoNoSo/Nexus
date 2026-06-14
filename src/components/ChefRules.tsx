/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { ShieldCheck, ShieldAlert, AlertTriangle, Coins, EyeOff, Bot, Sliders, CheckSquare, Settings } from 'lucide-react';
import { AuditRule, SystemStatus } from '../types';

interface ChefRulesProps {
  status: SystemStatus;
  auditRules: AuditRule[];
  onToggleProvider: (provider: SystemStatus['currentProvider']) => void;
  onRunAudit: (id: string) => void;
}

export const ChefRules: React.FC<ChefRulesProps> = ({
  status,
  auditRules,
  onToggleProvider,
  onRunAudit
}) => {
  return (
    <div className="space-y-6">
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
                  <h4 className="text-xs font-semibold text-gray-200">Schutz vor AI-Slop</h4>
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
