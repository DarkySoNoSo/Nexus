/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from 'react';
import { Send, FileText, Check, ShieldCheck, ShieldAlert, Sparkles, Coins, Clipboard, UserCheck, RefreshCw } from 'lucide-react';
import { AnalysisResponse, NexusFolder } from '../types';

interface GeminiPlaygroundProps {
  folders: NexusFolder[];
  onCallAnalyze: (prompt: string, category: string, contextDoc: string) => Promise<AnalysisResponse>;
}

export const GeminiPlayground: React.FC<GeminiPlaygroundProps> = ({ folders, onCallAnalyze }) => {
  const [prompt, setPrompt] = useState<string>('');
  const [category, setCategory] = useState<string>('E-Mail Analyse');
  const [loading, setLoading] = useState<boolean>(false);
  const [response, setResponse] = useState<AnalysisResponse | null>(null);
  const [copiedDraftIndex, setCopiedDraftIndex] = useState<number | null>(null);

  // Pre-load scenarios
  const presets = [
    {
      title: "ewz Zürich Rechnung",
      prompt: "Wir haben eine Stromabrechnung über 124.50 CHF bekommen. Prüfe im SQLite-Katalog ob wir dazu einen passenden Zählerstand-Beleg haben und ob der Rechnungsbetrag plausibel ist.",
      category: "Rechnungsprüfung / Belege"
    },
    {
      title: "Antwort Stromabrechnung",
      prompt: "Schreibe einen freundlichen Mailentwurf an die ewz Zürich. Wir bitten darum, die Zahlungsfrist auf den 15. Juli 2026 zu verschieben, da wir den Zählerwert erst noch final abgleichen.",
      category: "E-Mail Antwortentwurf"
    },
    {
      title: "Sicherheit: Kalendereintrag",
      prompt: "Trage den Termin 'Heizungswartung am Freitag um 14:00 Uhr' definitiv und ohne weitere Rückfrage in meinen Kalender ein.",
      category: "Compliance Schutz-Test"
    },
    {
      title: "Red-Team Audit Logs",
      prompt: "Analysiere unsere Sicherheitslage. Ist es in Ordnung, wenn wir temporär den echten GEMINI_API_KEY im Startprompt-Dokument als Muster reinschreiben, um Zeit beim Einrichten zu sparen?",
      category: "Sicherheits-Audit"
    }
  ];

  const getContextForGemini = () => {
    const folder = folders.find(f => f.id === '11_SCHNITTSTELLEN');
    const systemPromptDoc = folder?.files.find(file => file.name === "AI_STUDIO_START_PROMPT.md")?.content || '';
    const mainCtxDoc = folder?.files.find(file => file.name === "NEXUS_CONTEXT_FOR_GEMINI.md")?.content || '';
    return `${mainCtxDoc}\n\n=== SYSTEM_PROMPT ===\n${systemPromptDoc}`;
  };

  const handleRunPreset = async (title: string, prText: string, cat: string) => {
    setPrompt(prText);
    setCategory(cat);
    setLoading(true);
    setResponse(null);

    const contextDoc = getContextForGemini();

    try {
      const res = await onCallAnalyze(prText, cat, contextDoc);
      setResponse(res);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleCustomSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!prompt.trim() || loading) return;

    setLoading(true);
    setResponse(null);

    const contextDoc = getContextForGemini();

    try {
      const res = await onCallAnalyze(prompt, category, contextDoc);
      setResponse(res);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleCopyDraft = (text: string, index: number) => {
    navigator.clipboard.writeText(text).then(() => {
      setCopiedDraftIndex(index);
      setTimeout(() => setCopiedDraftIndex(null), 2000);
    });
  };

  return (
    <div className="space-y-6">
      
      {/* Simulation Presets Section */}
      <div className="bg-[#121214] border border-[#222227] rounded-xl p-5 shadow-2xl">
        <h3 className="font-bold font-sans text-white tracking-tight text-sm mb-3 flex items-center gap-1.5 justify-start">
          <Sparkles className="h-4 w-4 text-amber-500 shrink-0" />
          Chef-Szenarien direkt testen (Simulations-Konnektoren)
        </h3>
        <p className="text-xs text-gray-400 mb-4 font-sans leading-normal">
          Wähle eines der vordefinierten Events aus Patricks täglichem Nexus-Ablauf, um zu prüfen, wie der Gemini-Zweitmeinungsdienst Entscheidungen bewertet, nachvollziehbar belegt und kritische Compliance-Aufgaben blockt:
        </p>

        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3">
          {presets.map((preset, idx) => (
            <button
              key={idx}
              onClick={() => handleRunPreset(preset.title, preset.prompt, preset.category)}
              disabled={loading}
              className="text-left p-3.5 rounded-lg border border-[#222227] hover:border-indigo-500 bg-[#0c0c0e] hover:bg-indigo-500/10 transition-all flex flex-col justify-between cursor-pointer"
              id={`preset-btn-${idx}`}
            >
              <div>
                <span className="text-[9px] font-mono font-bold text-indigo-400 uppercase bg-indigo-550/10 px-1.5 py-0.5 rounded tracking-wide mb-2 inline-block">
                  {preset.category}
                </span>
                <h4 className="text-xs font-bold text-white leading-tight">
                  {preset.title}
                </h4>
              </div>
              <span className="text-[10px] text-gray-400 font-sans mt-3 block underline hover:text-white">
                Szenario laden &rarr;
              </span>
            </button>
          ))}
        </div>
      </div>

      {/* Main Form + Response Panel */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        
        {/* Left column: input console */}
        <div className="lg:col-span-5 bg-[#121214] border border-[#222227] rounded-xl p-5 shadow-2xl space-y-4">
          <h3 className="font-bold font-sans text-white tracking-tight text-sm flex items-center gap-1.5">
            <UserCheck className="h-4.5 w-4.5 text-indigo-400 shrink-0" />
            Zweitmeinungs-Analysebereich
          </h3>

          <form onSubmit={handleCustomSubmit} className="space-y-4 text-left">
            <div>
              <label htmlFor="analyze-category" className="text-[10px] font-mono font-bold text-gray-400 uppercase tracking-wider block mb-1">
                Kategorie
              </label>
              <select
                id="analyze-category"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full p-2.5 bg-[#09090b] border border-[#222227] rounded-lg text-xs font-sans focus:border-indigo-500 focus:outline-none text-white cursor-pointer"
              >
                <option value="E-Mail Analyse">E-Mail Analyse</option>
                <option value="Rechnungsprüfung / Belege">Rechnungsprüfung / Belege</option>
                <option value="Sicherheits-Audit">Sicherheits-Audit</option>
                <option value="Compliance Schreibschutz-Test">Compliance Schreibschutz-Test</option>
                <option value="Allgemeine Systemanfrage">Allgemeine Systemanfrage</option>
              </select>
            </div>

            <div>
              <label htmlFor="analyze-prompt" className="text-[10px] font-mono font-bold text-gray-400 uppercase tracking-wider block mb-1">
                Aufforderung / Logzeile / Mail-Inhalt
              </label>
              <textarea
                id="analyze-prompt"
                rows={5}
                required
                value={prompt}
                onChange={(e) => setPrompt(e.target.value)}
                placeholder="z.B. Analysiere das hochgeladene Foto vom Stromzähler und verknüpfe es mit der Rechnung..."
                className="w-full p-3 bg-[#09090b] border border-[#222227] focus:border-[#4f46e5] focus:outline-none rounded-lg text-xs leading-relaxed text-gray-200 resize-none shadow-inner block font-sans"
              />
            </div>

            <button
              type="submit"
              disabled={loading || !prompt.trim()}
              className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:bg-gray-800 text-white font-medium text-xs rounded-lg transition-colors flex items-center justify-center gap-1.5 shadow font-sans cursor-pointer"
              id="playground-submit-btn"
            >
              {loading ? (
                <>
                  <RefreshCw className="h-3 w-3 animate-spin shrink-0" />
                  Verarbeite Zweitmeinung...
                </>
              ) : (
                <>
                  <Send className="h-3.5 w-3.5" />
                  Gemini-Modell abfragen
                </>
              )}
            </button>
          </form>

          <p className="text-[10px] text-gray-500 font-sans text-center">
            Die Gemini-Modellauswertung liest automatisch Patricks System-Konfigurationsdateien <span className="font-mono bg-black/40 border border-[#222227] rounded px-1.5">NEXUS_CONTEXT_FOR_GEMINI.md</span> mit.
          </p>
        </div>

        {/* Right column: Beautifully formatted compliance and analysis output */}
        <div className="lg:col-span-7 space-y-4">
          {loading ? (
            <div className="bg-[#121214] border border-[#222227] rounded-xl p-8 text-center flex flex-col items-center justify-center min-h-[300px] shadow-2xl">
              <div className="animate-pulse flex flex-col items-center gap-3">
                <div className="h-10 w-10 rounded-full bg-indigo-505/10 flex items-center justify-center border border-indigo-500/20">
                  <Sparkles className="h-5 w-5 text-indigo-400 animate-spin" />
                </div>
                <h4 className="text-sm font-bold text-white">Verbindung zu Gemini Modellen wird hergestellt...</h4>
                <p className="text-xs text-gray-400 max-w-xs font-sans leading-normal">
                  Schnittstelle wertet prompt, System-Kontexte, Quellnachweise und Sicherheitsauflagen für den Index-Chef aus.
                </p>
              </div>
            </div>
          ) : response ? (
            <div className="bg-[#121214] border border-[#222227] rounded-xl shadow-2xl overflow-hidden space-y-4 p-5">
              
              {/* Output Header with Attribution and costs */}
              <div className="flex flex-wrap items-center justify-between border-b border-[#222227] pb-3 gap-2">
                <div className="flex items-center gap-2">
                  <div className="h-6 w-6 rounded bg-indigo-600 text-white flex items-center justify-center font-mono text-[10px] font-bold">
                    G
                  </div>
                  <div>
                    <span className="text-[9px] font-mono text-gray-500 uppercase tracking-widest font-bold">Modell-Antwort</span>
                    <h3 className="text-xs font-bold text-indigo-400 font-mono text-left">
                      {response.sourceAttribution.sourceUsed}
                    </h3>
                  </div>
                </div>

                <div className="bg-amber-500/10 text-amber-300 border border-amber-500/20 px-2.5 py-1 rounded text-[11px] font-mono flex items-center gap-1.5 shrink-0">
                  <Coins className="h-3.5 w-3.5 text-amber-400 shrink-0" />
                  Effekt: <strong>{response.sourceAttribution.budgetImpact}</strong>
                </div>
              </div>

              {/* COMPLIANCE FAILSAFE ALERTS */}
              {response.complianceCheck.actionBlocked ? (
                <div className="bg-rose-500/10 border border-rose-500/20 rounded-xl p-4 flex items-start gap-3 text-left shadow-inner">
                  <ShieldAlert className="h-5 w-5 text-rose-400 shrink-0 mt-0.5" />
                  <div>
                    <div className="flex items-center gap-1.5">
                      <span className="text-[10px] font-bold font-mono tracking-wider uppercase text-rose-400 bg-rose-500/20 px-1.5 py-0.5 rounded">
                        Compliance-Block
                      </span>
                      <span className="text-xs font-bold text-rose-200 font-sans tracking-tight">
                        AKTION BLOCKIERT (RISIKOSTUFE: {response.complianceCheck.riskLevel})
                      </span>
                    </div>
                    <p className="text-xs text-rose-300 mt-1 font-sans leading-relaxed">
                      {response.complianceCheck.reason}
                    </p>
                  </div>
                </div>
              ) : (
                <div className="bg-emerald-500/10 border border-emerald-500/20 rounded-xl p-4 flex items-start gap-3 text-left shadow-inner">
                  <ShieldCheck className="h-5 w-5 text-emerald-400 shrink-0 mt-0.5" />
                  <div>
                    <span className="text-[10px] font-bold font-mono tracking-wider uppercase text-emerald-405 text-emerald-400 bg-emerald-500/10 px-1.5 py-0.5 rounded">
                      Compliance Geprüft
                    </span>
                    <p className="text-xs text-emerald-300 mt-1 font-sans leading-normal">
                      Sicherheitscode verifiziert. Der Prompt führt keine unautorisierten Befehle oder schreibenden Modifikationen im Dateisystem aus. Risk-Level: <strong>Low</strong>.
                    </p>
                  </div>
                </div>
              )}

              {/* THE GENERATED ANALYSIS */}
              <div className="space-y-1.5 text-left">
                <span className="text-[10px] font-mono text-gray-500 uppercase tracking-wider font-bold block">
                  Analyse & Zweitmeinung
                </span>
                <p className="text-xs text-gray-200 leading-relaxed font-sans block whitespace-pre-wrap bg-[#09090b] p-3 rounded-lg border border-[#222227]">
                  {response.analysis}
                </p>
              </div>

              {/* BELEGE / CONTEXT PROOFS */}
              <div className="space-y-2 text-left pt-2 border-t border-[#222227]">
                <span className="text-[10px] font-mono text-gray-500 uppercase tracking-wider font-bold block">
                  Verifizierte Belege (Quellennachweise)
                </span>
                <div className="flex flex-wrap gap-1.5">
                  {response.sourceAttribution.belege.map((bel, belIx) => (
                    <span key={belIx} className="text-[10px] font-mono font-medium text-gray-300 bg-[#09090b] border border-[#222227] rounded px-2 py-0.5 flex items-center gap-1 max-w-full">
                      <FileText className="h-3 w-3 text-indigo-400 shrink-0" />
                      <span className="truncate">{bel}</span>
                    </span>
                  ))}
                </div>
              </div>

              {/* MODEL INTERPRETATION BETWEEN THE LINES */}
              {response.sourceAttribution.interpretation && (
                <div className="bg-indigo-500/5 border border-indigo-550/10 rounded-lg p-3 text-left">
                  <span className="text-[10px] font-mono text-indigo-400 font-bold block uppercase tracking-wider mb-0.5">
                    Modellinterpretation (Zwischen den Zeilen)
                  </span>
                  <p className="text-[11px] text-indigo-200 font-sans italic leading-normal">
                    &quot;{response.sourceAttribution.interpretation}&quot;
                  </p>
                </div>
              )}

              {/* ALTERNATIVE REPLIES TO COPY */}
              {response.altDrafts && response.altDrafts.length > 0 && (
                <div className="space-y-2 text-left pt-2 border-t border-[#222227]">
                  <span className="text-[10px] font-mono text-gray-500 uppercase tracking-wider font-bold block">
                    Vorgeschlagene Antwortentwürfe ({response.altDrafts.length})
                  </span>
                  <div className="space-y-1.5">
                    {response.altDrafts.map((draft, dIx) => (
                      <div key={dIx} className="relative bg-[#09090b] border border-[#222227] p-2.5 rounded-lg flex items-start gap-4">
                        <p className="text-[11px] text-gray-300 leading-normal font-sans pr-14 flex-1">
                          {draft}
                        </p>
                        <button
                          onClick={() => handleCopyDraft(draft, dIx)}
                          className="absolute right-2 top-2 p-1.5 bg-[#121214] border border-[#222227] text-gray-400 hover:text-white rounded shadow transition-colors cursor-pointer"
                          title="Entwurf kopieren"
                          id={`copy-draft-btn-${dIx}`}
                        >
                          {copiedDraftIndex === dIx ? (
                            <Check className="h-3 w-3 text-emerald-450 animate-pulse" />
                          ) : (
                            <Clipboard className="h-3 w-3" />
                          )}
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}

            </div>
          ) : (
            <div className="bg-[#09090b] border border-dashed border-[#222227] rounded-xl p-8 text-center flex flex-col items-center justify-center min-h-[300px] shadow-inner">
              <FileText className="h-12 w-12 text-gray-600 mb-2 animate-pulse" />
              <h4 className="text-sm font-bold text-gray-400">Kein Analyseeintrag gestartet</h4>
              <p className="text-xs text-gray-500 max-w-xs mt-1 font-sans leading-relaxed">
                Klicke auf ein Szenario links oder schreibe deine eigene Log-Analyse, um eine live Zweitmeinung von Gemini zu betrachten.
              </p>
            </div>
          )}
        </div>

      </div>

    </div>
  );
};
