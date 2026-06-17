/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from 'react';
import { Search, Database, Cpu, Table, Tag, RefreshCw } from 'lucide-react';
import { SearchResultItem } from '../types';

interface DatabaseSearchProps {
  records: SearchResultItem[];
  onTranslateNlp: (query: string) => Promise<{
    searchTerms: string[];
    category: string;
    tags: string[];
    simulatedSql: string;
  }>;
}

export const DatabaseSearch: React.FC<DatabaseSearchProps> = ({ records, onTranslateNlp }) => {
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [isTranslating, setIsTranslating] = useState<boolean>(false);
  
  // NLP state
  const [nlpQuery, setNlpQuery] = useState<string>('');
  const [generatedSql, setGeneratedSql] = useState<string>('');
  const [structuredFilter, setStructuredFilter] = useState<{
    searchTerms: string[];
    category: string;
    tags: string[];
  } | null>(null);

  // Auto query examples
  const examples = [
    "Zeige mir alle Belege für Strom",
    "Mietverträge und Wohnungsdokumente",
    "Zählerstände hochgeladen mit Fotos",
    "Steuererklärung Entwürfe"
  ];

  // Perform regular client filtering
  const filteredRecords = records.filter((rec) => {
    // If we have parsed NLP filters, prioritize those
    if (structuredFilter) {
      const matchCategory = structuredFilter.category === "All" || rec.category === structuredFilter.category;
      const matchTerms = structuredFilter.searchTerms.some(term => {
        const t = term.toLowerCase();
        return rec.title.toLowerCase().includes(t) || 
               rec.snippet.toLowerCase().includes(t) ||
               rec.tags.some(tag => tag.toLowerCase().includes(t));
      });
      return matchCategory && (structuredFilter.searchTerms.length === 0 || matchTerms);
    }

    // Default basic search bar filtering
    const s = searchTerm.toLowerCase();
    if (!s) return true;
    return rec.title.toLowerCase().includes(s) ||
           rec.category.toLowerCase().includes(s) ||
           rec.snippet.toLowerCase().includes(s) ||
           rec.tags.some(t => t.toLowerCase().includes(s));
  });

  // Handle NLP Translator call
  const handleNlpTranslate = async (queryText: string) => {
    const textToUse = queryText || nlpQuery;
    if (!textToUse.trim()) return;

    setNlpQuery(textToUse);
    setIsTranslating(true);
    setSearchTerm(''); // Clear basic input

    try {
      const nlpResult = await onTranslateNlp(textToUse);
      setGeneratedSql(nlpResult.simulatedSql);
      setStructuredFilter({
        searchTerms: nlpResult.searchTerms,
        category: nlpResult.category,
        tags: nlpResult.tags
      });
    } catch (err) {
      console.error(err);
    } finally {
      setIsTranslating(false);
    }
  };

  // Reset filter back to basic
  const handleClearNlp = () => {
    setNlpQuery('');
    setGeneratedSql('');
    setStructuredFilter(null);
    setSearchTerm('');
  };

  return (
    <div className="space-y-6">
      
      {/* Search Console Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Left pane: NL Translation Controller */}
        <div className="lg:col-span-1 bg-[#121214] border border-[#222227] rounded-xl p-5 shadow-2xl space-y-4">
          <div className="flex items-center gap-2">
            <Cpu className="h-5 w-5 text-indigo-400 shrink-0" />
            <h3 className="font-bold font-sans text-white tracking-tight text-sm">
              Gemini SQL-Schnittstelle (NLP)
            </h3>
          </div>
          <p className="text-xs text-gray-400 leading-normal">
            Suche in natürlicher Sprache. Der Gemini-Schnittstellen-Kern übersetzt dein Suchinteresse direkt in SQLite-Filterausdrücke:
          </p>

          <div className="space-y-2">
            <div className="relative">
              <input
                type="text"
                placeholder="z.B. Zeige Belege für Strom..."
                value={nlpQuery}
                onChange={(e) => setNlpQuery(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleNlpTranslate('')}
                className="w-full pl-3 pr-12 py-2.5 bg-[#09090b] text-gray-100 placeholder-gray-500 border border-[#222227] focus:border-[#4f46e5] focus:outline-none rounded-lg text-xs font-sans block"
                id="nlp-query-input"
              />
              <button
                onClick={() => handleNlpTranslate('')}
                disabled={isTranslating}
                className="absolute right-1 top-1 bottom-1 p-1 px-3 bg-indigo-650 bg-indigo-600 hover:bg-indigo-500 disabled:bg-gray-800 rounded text-[11px] font-sans h-[31px] mt-[1.5px] font-bold text-white transition-colors cursor-pointer"
                id="nlp-translate-btn"
              >
                {isTranslating ? <RefreshCw className="h-3 w-3 animate-spin" /> : 'SQL'}
              </button>
            </div>

            {/* Quick Suggestions */}
            <div className="space-y-1 pt-1.5">
              <span className="text-[10px] font-mono text-gray-500 font-bold block uppercase tracking-wider mb-1">
                Beispiele für Patrick:
              </span>
              {examples.map((ex, idx) => (
                <button
                  key={idx}
                  onClick={() => handleNlpTranslate(ex)}
                  className="w-full text-left p-2.5 bg-[#0a0a0c] hover:bg-[#18181f] border border-[#222227] hover:border-[#6366f1]/50 rounded text-xs text-gray-400 hover:text-white transition-all duration-150 truncate block cursor-pointer"
                  id={`nlp-suggestion-btn-${idx}`}
                >
                  {ex}
                </button>
              ))}
            </div>
          </div>

          {/* Render Resulting SQL compilation */}
          {generatedSql && (
            <div className="mt-4 pt-3 border-t border-[#222227] space-y-2.5">
              <div className="flex items-center justify-between">
                <span className="text-[10px] uppercase font-mono tracking-widest text-indigo-400 font-bold">
                  Kompilierte SQLite-Abfrage
                </span>
                <button
                  onClick={handleClearNlp}
                  className="text-[10px] text-gray-400 hover:text-white font-mono underline block cursor-pointer"
                  id="reset-nlp-btn"
                >
                  Zurücksetzen
                </button>
              </div>

              <div className="bg-[#050507] p-3 rounded-lg border border-[#222227] text-[11px] font-mono text-teal-400 block shadow-inner select-all overflow-x-auto">
                {generatedSql}
              </div>

              {structuredFilter && (
                <div className="space-y-1.5">
                  <div className="flex items-center gap-1">
                    <span className="text-[10px] text-gray-500 font-mono">Kategorie-Schutz:</span>
                    <span className="text-[10px] font-mono font-semibold text-indigo-400 bg-indigo-950/20 border border-indigo-900/30 px-1.5 py-0.5 rounded">
                      {structuredFilter.category}
                    </span>
                  </div>
                  <div className="flex flex-wrap items-center gap-1">
                    <span className="text-[10px] text-gray-500 font-mono">Filterterme:</span>
                    {structuredFilter.searchTerms.map((term, tIdx) => (
                      <span key={tIdx} className="text-[10px] font-mono bg-indigo-650/15 text-indigo-300 border border-indigo-505/20 px-1.5 py-0.5 rounded">
                        {term}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Right Pane: Direct Filter & Database Records Table */}
        <div className="lg:col-span-2 bg-[#121214] border border-[#222227] rounded-xl p-5 shadow-2xl space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <Table className="h-5 w-5 text-gray-400 shrink-0" />
              <h3 className="font-bold font-sans text-white tracking-tight text-sm">
                Datenbank-Katalog ({filteredRecords.length} Treffer)
              </h3>
            </div>

            {/* Basic filter input */}
            {!generatedSql && (
              <div className="relative w-48">
                <input
                  type="text"
                  placeholder="Katalog filtern..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full pl-8 pr-2.5 py-1.5 bg-[#09090b] text-gray-100 placeholder-gray-500 border border-[#222227] focus:border-[#4f46e5] focus:outline-none rounded-lg text-xs font-sans"
                  id="direct-filter-input"
                />
                <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-gray-500" />
              </div>
            )}
          </div>

          <div className="overflow-x-auto border border-[#222227] rounded-lg">
            <table className="min-w-full divide-y divide-[#222227] text-left">
              <thead className="bg-[#141416]/80 font-mono text-[9px] font-bold text-gray-400 uppercase tracking-wider">
                <tr>
                  <th scope="col" className="px-3.5 py-2.5">Dateiname</th>
                  <th scope="col" className="px-3.5 py-2.5">Kategorie</th>
                  <th scope="col" className="px-3.5 py-2.5">Indizierungs-Inhalt</th>
                  <th scope="col" className="px-3.5 py-2.5 text-right">Lokal-Pfad</th>
                </tr>
              </thead>
              <tbody className="bg-[#121214] divide-y divide-[#222227] text-[11px] font-sans text-gray-400">
                {filteredRecords.length > 0 ? (
                  filteredRecords.map((rec) => (
                    <tr key={rec.id} className="hover:bg-[#161619] transition-colors">
                      <td className="px-3.5 py-3 font-mono font-bold text-white border-r border-[#222227]/40 max-w-[150px] truncate">
                        {rec.title}
                      </td>
                      <td className="px-3.5 py-3 border-r border-[#222227]/40 whitespace-nowrap">
                        <span className="font-mono text-[10px] text-indigo-300 bg-indigo-950/20 border border-indigo-900/40 px-1.5 py-0.5 rounded font-medium">
                          {rec.category}
                        </span>
                      </td>
                      <td className="px-3.5 py-3 border-r border-[#222227]/40 max-w-xs">
                        <div className="space-y-1.5 text-left">
                          <p className="leading-tight text-gray-300 font-sans text-xs">{rec.snippet}</p>
                          <div className="flex flex-wrap gap-1">
                            {rec.tags.map((tag, tagIx) => (
                              <span key={tagIx} className="flex items-center gap-0.5 text-[9px] font-mono text-gray-400 bg-black/20 border border-white/5 rounded px-1.5 py-0.5">
                                <Tag className="h-2.5 w-2.5 text-indigo-400" />
                                {tag}
                              </span>
                            ))}
                          </div>
                        </div>
                      </td>
                      <td className="px-3.5 py-3 text-right max-w-[150px] truncate font-mono text-[10px] text-gray-500">
                        {rec.fileOrigin}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={4} className="px-3.5 py-8 text-center text-gray-500 italic font-sans bg-[#0c0c0e]">
                      Keine Datensätze gefunden. Ändere den Suchtext oder setze die SQL-Abfrage zurück.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <div className="bg-[#09090b] rounded-lg p-3 border border-[#222227] flex items-start gap-1.5 text-xs text-gray-400 leading-snug font-sans">
            <Database className="h-4 w-4 text-indigo-405 text-indigo-400 shrink-0 mt-0.5" />
            <span>
              <strong>SQLite-Katalog:</strong> Dies repräsentiert Patricks Live-Masterindex aus <code className="font-mono text-[11px] bg-black/40 border border-[#222227] px-1 py-0.5 text-emerald-400 rounded select-all">C:\MasterIndex_Storage\_NEXUS_SYSTEM\db\nexus_catalog.sqlite</code>. Patricks PowerShell-Skript spiegelt alle Ingestionen dorthin.
            </span>
          </div>

        </div>

      </div>

    </div>
  );
};
