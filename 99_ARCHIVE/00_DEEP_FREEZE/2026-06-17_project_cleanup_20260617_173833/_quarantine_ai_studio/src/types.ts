/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

export interface NexusFile {
  name: string;
  path: string;
  content: string;
  sizeBytes: number;
  lastModified: string;
  fileType: 'markdown' | 'json' | 'powershell' | 'text';
}

export interface NexusFolder {
  id: string; // e.g. "00_START_HIER"
  name: string; // e.g. "00_START_HIER"
  index: number;
  title: string; // e.g. "Einstieg & Status"
  description: string;
  files: NexusFile[];
}

export type LogType = 'info' | 'warn' | 'error' | 'success';
export type LogSource = 'PowerShell' | 'SQLite' | 'Gemini' | 'Chef-Logik';

export interface SystemLog {
  id: string;
  timestamp: string;
  type: LogType;
  source: LogSource;
  message: string;
}

export interface SystemStatus {
  version: string;
  port: number;
  status: 'online' | 'offline' | 'error' | 'maintenance';
  lanUrl: string;
  tailscaleUrl: string;
  totalRecords: number;
  totalSizeGb: number;
  activeAlerts: number;
  currentProvider: 'OpenAI' | 'Gemini' | 'Local-Offline';
  monthlyCostLimit: number;
  monthlyCostSpent: number;
  apiKeySet: boolean;
}

export interface AuditRule {
  id: string;
  category: string;
  title: string;
  rule: string;
  status: 'passed' | 'warning' | 'failed';
  feedback: string;
}

export interface SearchResultItem {
  id: string;
  title: string;
  category: string;
  snippet: string;
  relevance: number;
  fileOrigin: string;
  tags: string[];
}

export interface AnalysisResponse {
  analysis: string;
  sourceAttribution: {
    sourceUsed: string;
    belege: string[];
    interpretation: string;
    budgetImpact: string;
  };
  complianceCheck: {
    actionBlocked: boolean;
    reason: string;
    riskLevel: 'Low' | 'Medium' | 'High';
  };
  altDrafts: string[];
}
