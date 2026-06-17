/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from 'react';
import { Terminal, Play, RefreshCw, AlertCircle, Info, Check, ShieldCheck } from 'lucide-react';
import { SystemLog } from '../types';

interface LogConsoleProps {
  logs: SystemLog[];
  onAddLog: (log: Omit<SystemLog, 'id' | 'timestamp'>) => void;
  onClearLogs: () => void;
}

export const LogConsole: React.FC<LogConsoleProps> = ({ logs, onAddLog, onClearLogs }) => {
  const [isRunningSmoke, setIsRunningSmoke] = useState<boolean>(false);
  const [isRunningUpload, setIsRunningUpload] = useState<boolean>(false);

  // Run the Gemini Smoke Test script simulation
  const handleRunSmokeTest = () => {
    if (isRunningSmoke || isRunningUpload) return;
    setIsRunningSmoke(true);

    onAddLog({
      type: 'info',
      source: 'PowerShell',
      message: 'Executing: powershell -File G:\\...\\run_gemini_smoke_test.ps1'
    });

    setTimeout(() => {
      onAddLog({
        type: 'info',
        source: 'PowerShell',
        message: 'Loading Environment. Extraction: GEMINI_API_KEY from User-Environment...'
      });
    }, 800);

    setTimeout(() => {
      onAddLog({
        type: 'info',
        source: 'Gemini',
        message: 'Initializing connection bridge to models/gemini-3.5-flash...'
      });
    }, 1500);

    setTimeout(() => {
      onAddLog({
        type: 'success',
        source: 'Gemini',
        message: 'Google Cloud Ingress OK! Connected to projects/1061155924625 (gen-lang-client-0205938784).'
      });
    }, 2200);

    setTimeout(() => {
      onAddLog({
        type: 'success',
        source: 'PowerShell',
        message: 'SMOKE-TEST ERFOLGREICH BEENDET. Code: 0. Antwort-Latenz: 420ms.'
      });
      setIsRunningSmoke(false);
    }, 3000);
  };

  // Run the Context Schema Upload tool simulation
  const handleRunUpload = () => {
    if (isRunningSmoke || isRunningUpload) return;
    setIsRunningUpload(true);

    onAddLog({
      type: 'info',
      source: 'PowerShell',
      message: 'Executing: powershell -File C:\\MasterIndex_Storage\\_NEXUS_SYSTEM\\integrations\\google_ai_studio\\Upload_Nexus_Context_To_Gemini_Files.ps1'
    });

    setTimeout(() => {
      onAddLog({
        type: 'info',
        source: 'PowerShell',
        message: 'Parsing manifest: G:\\Meine Ablage\\Nexus\\01_ARCHITEKTUR\\nexus_file_manifest.json...'
      });
    }, 800);

    setTimeout(() => {
      onAddLog({
        type: 'info',
        source: 'Chef-Logik',
        message: 'Loading context files: [NEXUS_GESAMTDOKUMENTATION_STAND_20260607.md, NEXUS_ARCHITEKTUR.md]...'
      });
    }, 1600);

    setTimeout(() => {
      onAddLog({
        type: 'success',
        source: 'Gemini',
        message: 'Uploaded NEXUS_CONTEXT_FOR_GEMINI.md (1.5 KB) -> URI: file-nxsctx00192a'
      });
    }, 2400);

    setTimeout(() => {
      onAddLog({
        type: 'success',
        source: 'Gemini',
        message: 'Uploaded AI_STUDIO_START_PROMPT.md (1.8 KB) -> URI: file-stprom00438b'
      });
    }, 3200);

    setTimeout(() => {
      onAddLog({
        type: 'success',
        source: 'PowerShell',
        message: 'Writing local result cache: C:\\...\\integrations\\google_ai_studio\\uploaded_files_result.json'
      });
    }, 4000);

    setTimeout(() => {
      onAddLog({
        type: 'success',
        source: 'PowerShell',
        message: 'CONTEXT SYNC VORSTÄNDIG ABGESCHLOSSEN. Zeitstempel aktualisiert.'
      });
      setIsRunningUpload(false);
    }, 4800);
  };

  return (
    <div className="bg-slate-950 border border-slate-900 rounded-xl overflow-hidden shadow-xl flex flex-col min-h-[450px]">
      
      {/* Console Topbar */}
      <div className="bg-slate-900 px-4 py-3 border-b border-slate-855 flex flex-wrap items-center justify-between gap-3 text-left">
        <div className="flex items-center gap-2">
          <Terminal className="h-4 w-4 text-emerald-400" />
          <span className="text-xs font-mono font-bold text-slate-200">
            Nexus Local PowerShell Console (Port 8081)
          </span>
        </div>

        {/* Console operational buttons */}
        <div className="flex items-center gap-2">
          <button
            onClick={handleRunSmokeTest}
            disabled={isRunningSmoke || isRunningUpload}
            className="px-2.5 py-1.5 bg-slate-800 hover:bg-slate-750 disabled:bg-slate-900 text-[11px] font-mono text-emerald-400 hover:text-emerald-300 rounded border border-slate-700/50 flex items-center gap-1.5 transition-colors disabled:opacity-50"
            id="run-smoke-test-btn"
          >
            {isRunningSmoke ? (
              <RefreshCw className="h-3 w-3 animate-spin text-emerald-400" />
            ) : (
              <Play className="h-3 w-3" />
            )}
            run-smoke-test.ps1
          </button>

          <button
            onClick={handleRunUpload}
            disabled={isRunningSmoke || isRunningUpload}
            className="px-2.5 py-1.5 bg-slate-800 hover:bg-slate-750 disabled:bg-slate-900 text-[11px] font-mono text-sky-400 hover:text-sky-300 rounded border border-slate-700/50 flex items-center gap-1.5 transition-colors disabled:opacity-50"
            id="run-upload-context-btn"
          >
            {isRunningUpload ? (
              <RefreshCw className="h-3 w-3 animate-spin text-sky-400" />
            ) : (
              <Play className="h-3 w-3" />
            )}
            Upload-Context.ps1
          </button>

          <button
            onClick={onClearLogs}
            className="text-[11px] font-mono text-slate-500 hover:text-slate-300 underline block"
            id="clear-logs-btn"
          >
            Clear
          </button>
        </div>
      </div>

      {/* Terminal Screen area */}
      <div className="flex-1 p-4 overflow-y-auto max-h-[350px] font-mono text-[11.5px] leading-relaxed space-y-2 text-left bg-slate-950 shadow-inner select-all">
        {logs.map((log) => {
          let typeColor = 'text-slate-300';
          let indicatorSymbol = '&gt;';
          if (log.type === 'success') {
            typeColor = 'text-emerald-400';
            indicatorSymbol = '✓';
          } else if (log.type === 'warn') {
            typeColor = 'text-amber-400';
            indicatorSymbol = '⚠';
          } else if (log.type === 'error') {
            typeColor = 'text-rose-500';
            indicatorSymbol = '✗';
          }

          let sourceLabel = '';
          if (log.source === 'PowerShell') sourceLabel = 'PS';
          else if (log.source === 'SQLite') sourceLabel = 'SQLITE';
          else if (log.source === 'Gemini') sourceLabel = 'GEMINI';
          else if (log.source === 'Chef-Logik') sourceLabel = 'CHEF';

          return (
            <div key={log.id} className="grid grid-cols-1 md:grid-cols-12 gap-1 items-start hover:bg-slate-900/40 p-0.5 rounded transition-colors group">
              {/* Timestamp */}
              <span className="md:col-span-2 text-slate-600 text-[10px] select-none">
                [{log.timestamp.split('T')[1].slice(0, 8)}]
              </span>
              
              {/* Source Indicator */}
              <span className="md:col-span-2 text-slate-500 font-bold text-[10px] select-none uppercase tracking-wide">
                [{sourceLabel}]
              </span>

              {/* Message text with matching state color */}
              <div className={`md:col-span-8 flex items-start gap-1 p-0.5 ${typeColor}`}>
                <span className="text-slate-600 block leading-none mr-1 select-none font-bold">
                  {indicatorSymbol}
                </span>
                <span className="break-all font-mono leading-normal block">
                  {log.message}
                </span>
              </div>
            </div>
          );
        })}
      </div>

      {/* Script info footer */}
      <div className="bg-slate-900/60 p-3.5 border-t border-slate-900 flex items-start gap-2 text-[11px] text-slate-400 font-sans leading-normal text-left">
        <Info className="h-4 w-4 text-slate-400 shrink-0 mt-0.5" />
        <div>
          <span>
            <strong>Lokaler PowerShell Host:</strong> Klicke auf die Buttons oben, um die Ausführung der echten PowerShell-Scripte auf Patricks Rechner zu visualisieren. Diese Skripte regeln den Initial-Handshake sowie den kontinuierlichen Bildupload zur Gemini Files API.
          </span>
        </div>
      </div>

    </div>
  );
};
