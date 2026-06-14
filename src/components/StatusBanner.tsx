/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { Activity, ShieldAlert, ShieldCheck, Database, Link, RefreshCw, Sparkles, Key } from 'lucide-react';
import { SystemStatus } from '../types';

interface StatusBannerProps {
  status: SystemStatus;
  onRefresh: () => void;
  isLoading: boolean;
}

export const StatusBanner: React.FC<StatusBannerProps> = ({ status, onRefresh, isLoading }) => {
  return (
    <div className="bg-[#050508] border-b border-[#14243b] text-zinc-300 py-2.5 px-4 mb-2">
      <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3">
        {/* Title and version */}
        <div className="flex items-center gap-2">
          <span className="relative flex h-2 w-2">
            <span className={`animate-ping absolute inline-flex h-full w-full rounded-full ${status.status === 'online' ? 'bg-emerald-400' : 'bg-amber-400'} opacity-75`}></span>
            <span className={`relative inline-flex rounded-full h-2 w-2 ${status.status === 'online' ? 'bg-emerald-500' : 'bg-amber-500'}`}></span>
          </span>
          <h1 className="text-sm font-extrabold font-mono tracking-wider text-white">
            NEXUS
          </h1>
          <span className="text-[9px] text-[#00ff66] font-mono bg-[#112d1c] px-1.5 py-0.5 rounded border border-[#114b2d] uppercase tracking-wider">
            {status.status === 'online' ? 'Active' : 'Offline'}
          </span>
        </div>

        {/* Network status metrics (compact horizontal row) */}
        <div className="flex flex-wrap items-center gap-2 text-[10px] font-mono">
          {/* LAN Connection */}
          <div className="bg-[#09090d] px-2 py-1 rounded border border-[#14243b] flex items-center gap-1">
            <span className="text-zinc-500">PORT:</span>
            <span className="text-sky-400 font-bold">8081</span>
          </div>

          {/* Tailscale Link */}
          <div className="bg-[#09090d] px-2 py-1 rounded border border-[#14243b] flex items-center gap-1">
            <span className="text-zinc-500">VPN:</span>
            <span className="text-purple-400 font-bold">100.107.24.xx</span>
          </div>

          {/* SQLite DB stats */}
          <div className="bg-[#09090d] px-2 py-1 rounded border border-[#14243b] flex items-center gap-1">
            <span className="text-zinc-500">DB:</span>
            <span className="text-emerald-400 font-bold">{status.totalRecords.toLocaleString('de-DE')}</span>
            <span className="text-zinc-650 text-zinc-600">({status.totalSizeGb} GB)</span>
          </div>

          {/* API Key Status */}
          <div className={`px-2 py-1 rounded border flex items-center gap-1 ${
            status.apiKeySet 
              ? 'bg-[#112d1c] border-emerald-950/40 text-emerald-400' 
              : 'bg-[#2b181a] border-red-950/40 text-red-400'
          }`}>
            <span>AI:</span>
            <span className="font-bold">{status.apiKeySet ? 'ACTIVE' : 'OFFLINE'}</span>
          </div>
        </div>

        {/* Action triggers */}
        <div className="flex items-center gap-1.5 md:w-auto justify-end">
          <button
            onClick={onRefresh}
            disabled={isLoading}
            className="px-2 py-1 bg-zinc-900 hover:bg-zinc-800 text-zinc-300 rounded border border-zinc-800 flex items-center gap-1 text-[10px] font-mono transition-all cursor-pointer"
            id="refresh-status-btn"
          >
            <RefreshCw className={`h-3 w-3 ${isLoading ? 'animate-spin text-sky-400' : ''}`} />
            Refresh
          </button>
        </div>
      </div>

      {/* Warning if API key is not configured */}
      {!status.apiKeySet && (
        <div className="max-w-7xl mx-auto mt-2 bg-amber-500/5 border border-amber-500/10 text-amber-200 px-3 py-1.5 rounded text-[10px] flex items-center gap-1 shadow-sm">
          <Key className="h-3 w-3 text-amber-500 shrink-0" />
          <span>
            <strong>Kein GEMINI_API_KEY:</strong> Läuft im <strong>Heuristik-Simulator</strong>. Key in Settings &gt; Secrets hinterlegen.
          </span>
        </div>
      )}
    </div>
  );
};
