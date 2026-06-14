/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from 'react';
import { Folder, FileText, ChevronRight, Save, Copy, Check, FileCode, Info } from 'lucide-react';
import { NexusFolder, NexusFile } from '../types';

interface FileExplorerProps {
  folders: NexusFolder[];
  onUpdateFileContent: (folderId: string, fileName: string, newContent: string) => void;
  initialSelectedFolderId?: string;
  initialSelectedFileName?: string;
}

export const FileExplorer: React.FC<FileExplorerProps> = ({ 
  folders, 
  onUpdateFileContent,
  initialSelectedFolderId,
  initialSelectedFileName
}) => {
  const [selectedFolderId, setSelectedFolderId] = useState<string>(initialSelectedFolderId || folders[0]?.id || '');
  const [selectedFileName, setSelectedFileName] = useState<string>(initialSelectedFileName || folders[0]?.files[0]?.name || '');
  const [editContent, setEditContent] = useState<string>('');
  const [copied, setCopied] = useState<boolean>(false);
  const [saveSuccess, setSaveSuccess] = useState<boolean>(false);

  // Sync with outside state changes if any
  useEffect(() => {
    if (initialSelectedFolderId) {
      setSelectedFolderId(initialSelectedFolderId);
    }
    if (initialSelectedFileName) {
      setSelectedFileName(initialSelectedFileName);
    }
  }, [initialSelectedFolderId, initialSelectedFileName]);

  // Derive selection
  const currentFolder = folders.find((f) => f.id === selectedFolderId);
  const currentFile = currentFolder?.files.find((f) => f.name === selectedFileName);

  // Handle folder switch
  const handleSelectFolder = (id: string) => {
    setSelectedFolderId(id);
    const folder = folders.find((f) => f.id === id);
    if (folder && folder.files.length > 0) {
      setSelectedFileName(folder.files[0].name);
      setEditContent(folder.files[0].content);
    } else {
      setSelectedFileName('');
      setEditContent('');
    }
  };

  // Handle file switch
  const handleSelectFile = (file: NexusFile) => {
    setSelectedFileName(file.name);
    setEditContent(file.content);
    setSaveSuccess(false);
  };

  // Pre-populate input when file is loaded
  useEffect(() => {
    if (currentFile) {
      setEditContent(currentFile.content);
    }
  }, [selectedFolderId, selectedFileName, currentFile]);

  // Copy to clipboard
  const handleCopy = () => {
    if (!editContent) return;
    navigator.clipboard.writeText(editContent).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  };

  // Save changes
  const handleSave = () => {
    if (currentFolder && currentFile) {
      onUpdateFileContent(currentFolder.id, currentFile.name, editContent);
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 2000);
    }
  };

  return (
    <div className="bg-[#121214] border border-[#222227] rounded-xl overflow-hidden grid grid-cols-1 lg:grid-cols-12 min-h-[580px] shadow-2xl relative z-10">
      
      {/* 1. Folders Column (Left) */}
      <div className="lg:col-span-4 border-r border-[#222227] bg-[#0c0c0e] p-4 overflow-y-auto max-h-[580px]">
        <div className="flex items-center gap-1.5 mb-3 px-2">
          <Folder className="h-4 w-4 text-indigo-400" />
          <h3 className="text-xs uppercase tracking-wider font-mono font-bold text-gray-400">
            Nexus System-Stapel
          </h3>
        </div>
        <div className="space-y-1">
          {folders.map((folder) => {
            const isSelected = folder.id === selectedFolderId;
            return (
              <button
                key={folder.id}
                onClick={() => handleSelectFolder(folder.id)}
                className={`w-full text-left p-2.5 rounded-lg text-xs transition-colors flex items-center justify-between cursor-pointer border ${
                  isSelected
                    ? 'bg-indigo-650/15 border-indigo-505/30 text-white font-medium bg-[#1d1d22]'
                    : 'text-gray-400 hover:bg-[#151518] hover:text-white border-transparent'
                }`}
                id={`folder-btn-${folder.id}`}
              >
                <div className="flex items-center gap-2 truncate">
                  <div className={`p-1.5 rounded-md font-mono text-[10px] font-bold leading-none shrink-0 ${
                    isSelected ? 'bg-indigo-600/20 text-indigo-400' : 'bg-gray-800/50 text-gray-400'
                  }`}>
                    {folder.id.split('_')[0]}
                  </div>
                  <div className="truncate">
                    <p className="font-semibold font-sans text-[11px] leading-tight text-left block">
                      {folder.title}
                    </p>
                    <p className="text-[10px] text-gray-500 truncate block mt-0.5">
                      {folder.id}
                    </p>
                  </div>
                </div>
                <ChevronRight className={`h-3.5 w-3.5 shrink-0 ${isSelected ? 'text-indigo-450' : 'text-gray-600'}`} />
              </button>
            );
          })}
        </div>
      </div>

      {/* 2. Files List & Workspace (Right / Details) */}
      <div className="lg:col-span-8 flex flex-col max-h-[580px] bg-[#121214]">
        {currentFolder ? (
          <>
            {/* Folder Header */}
            <div className="border-b border-[#222227] p-4 bg-[#141416]/70 flex flex-wrap items-center justify-between gap-2">
              <div>
                <p className="text-[9px] font-mono uppercase tracking-widest text-indigo-400 font-bold">
                  Kategorie: {currentFolder.id}
                </p>
                <h2 className="text-base font-bold text-white mt-1 leading-tight">
                  {currentFolder.title}
                </h2>
                <p className="text-xs text-gray-400 mt-0.5 font-sans">
                  {currentFolder.description}
                </p>
              </div>
            </div>

            {/* Split layout: Selector + file editor */}
            <div className="flex-1 flex flex-col md:flex-row overflow-hidden min-h-0">
              
              {/* Files sub-navigator (Middle column) */}
              <div className="w-full md:w-52 border-b md:border-b-0 md:border-r border-[#222227] p-3 bg-[#0d0d10] space-y-1 shrink-0 overflow-y-auto max-h-[480px]">
                <p className="text-[9px] font-mono text-gray-500 uppercase tracking-wider px-2 mb-2 font-bold">
                  Dateien ({currentFolder.files.length})
                </p>
                {currentFolder.files.length > 0 ? (
                  currentFolder.files.map((file) => {
                    const isSelected = file.name === selectedFileName;
                    return (
                      <button
                        key={file.name}
                        onClick={() => handleSelectFile(file)}
                        className={`w-full text-left p-2 rounded-lg text-xs flex items-center gap-2 transition-all cursor-pointer border ${
                          isSelected
                            ? 'bg-indigo-600/15 border-indigo-500/20 text-white font-semibold'
                            : 'text-gray-400 hover:bg-[#141417]/80 hover:text-white border-transparent'
                        }`}
                        id={`file-btn-${file.name}`}
                      >
                        {file.fileType === 'markdown' ? (
                          <FileText className="h-3.5 w-3.5 text-indigo-400 shrink-0" />
                        ) : (
                          <FileCode className="h-3.5 w-3.5 text-teal-400 shrink-0" />
                        )}
                        <span className="truncate block text-[11px] font-mono">{file.name}</span>
                      </button>
                    );
                  })
                ) : (
                  <p className="text-[11px] text-gray-500 italic px-2">Keine Dateien vorhanden.</p>
                )}
              </div>

              {/* Document Editor / Preview Workspace */}
              <div className="flex-1 flex flex-col bg-[#111113]/50 min-h-0 overflow-hidden">
                {currentFile ? (
                  <div className="flex-1 flex flex-col min-h-0">
                    {/* File Meta Bar */}
                    <div className="bg-[#151519] border-b border-[#222227] py-2 px-4 flex items-center justify-between text-xs font-mono text-gray-400 text-left">
                      <div className="flex flex-wrap items-center gap-3">
                        <span>Speicherort: <strong className="text-gray-200">C:\MasterIndex_Storage\{currentFile.path}</strong></span>
                        <span className="hidden sm:inline">|</span>
                        <span>Größe: <strong className="text-gray-200">{(currentFile.sizeBytes / 1024).toFixed(2)} KB</strong></span>
                      </div>
                      
                      {/* Copy and Save utility buttons */}
                      <div className="flex items-center gap-1">
                        <button
                          onClick={handleCopy}
                          className="p-1 px-2 text-[11px] bg-[#1a1a1f] border border-[#2c2c35] text-gray-300 hover:text-white hover:bg-[#25252e] rounded flex items-center gap-1 shadow transition-colors cursor-pointer"
                          title="In die Zwischenablage kopieren"
                          id="copy-to-clipboard-btn"
                        >
                          {copied ? <Check className="h-3 w-3 text-emerald-400 animate-pulse" /> : <Copy className="h-3 w-3" />}
                          {copied ? 'Kopiert' : 'Kopieren'}
                        </button>
                        <button
                          onClick={handleSave}
                          className="p-1 px-2 text-[11px] bg-indigo-650 bg-indigo-600 border border-transparent text-white hover:bg-indigo-500 rounded flex items-center gap-1 shadow transition-colors font-medium cursor-pointer"
                          title="Änderungen lokal sichern"
                          id="save-file-btn"
                        >
                          {saveSuccess ? <Check className="h-3 w-3" /> : <Save className="h-3 w-3" />}
                          {saveSuccess ? 'Gespeichert' : 'Speichern'}
                        </button>
                      </div>
                    </div>

                    {/* Editor Text Area */}
                    <div className="flex-1 p-3 min-h-0 flex flex-col">
                      {selectedFolderId === '11_SCHNITTSTELLEN' && (
                        <div className="bg-sky-500/10 border border-sky-500/15 text-sky-200 p-2.5 rounded text-[11px] mb-2 font-sans flex items-start gap-1.5 leading-snug shadow-sm shrink-0">
                          <Info className="h-4 w-4 shrink-0 text-sky-450 mt-0.5" />
                          <span>
                            <strong>AI Studio Kontext:</strong> Diese Konfigurationen dienen als System-Prompts für deine Gemini Sessions. Kopiere sie einfach per "Kopieren" heraus.
                          </span>
                        </div>
                      )}
                      
                      <textarea
                        value={editContent}
                        onChange={(e) => setEditContent(e.target.value)}
                        className="flex-1 w-full bg-[#08080a] text-gray-200 p-4 font-mono text-[11.5px] leading-relaxed rounded-lg border border-[#222227] focus:outline-none focus:border-indigo-550 focus:ring-1 focus:ring-indigo-600 resize-none overflow-y-auto block shadow-inner"
                        id="document-editor-textarea"
                      />
                    </div>
                  </div>
                ) : (
                  <div className="flex-1 flex flex-col items-center justify-center p-6 text-center text-gray-500">
                    <FileText className="h-10 w-10 text-gray-600 mb-2 animate-pulse" />
                    <p className="text-xs">Wähle eine Datei aus dem linken Bereich, um den Inhalt anzuzeigen oder zu bearbeiten.</p>
                  </div>
                )}
              </div>
            </div>
          </>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center p-6 text-center text-gray-500">
            <Folder className="h-12 w-12 text-gray-600 mb-2 animate-pulse" />
            <p className="text-sm">Bitte wähle ein Verzeichnis im linken Navigationsmenü aus.</p>
          </div>
        )}
      </div>
    </div>
  );
};
