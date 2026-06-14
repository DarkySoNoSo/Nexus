import React, { useState, useEffect } from 'react';
import { 
  Smartphone, 
  Wifi, 
  WifiOff, 
  Database, 
  Send, 
  RefreshCw, 
  Play, 
  Bug, 
  CheckCircle,
  Clock, 
  Settings,
  AlertTriangle 
} from 'lucide-react';

interface QueuedItem {
  id: string;
  eventType: string;
  payload: string;
  retryCount: number;
  status: 'PENDING' | 'RETRY' | 'SUCCESS' | 'FAILED_LIMIT';
  backoffSec: number;
}

interface AndroidEmulatorProps {
  onAddLog: (log: { type: 'info' | 'warn' | 'error' | 'success'; source: 'PowerShell' | 'SQLite' | 'Gemini' | 'Chef-Logik'; message: string }) => void;
}

export const AndroidEmulator: React.FC<AndroidEmulatorProps> = ({ onAddLog }) => {
  const [isOnline, setIsOnline] = useState<boolean>(true);
  const [serverUrl, setServerUrl] = useState<string>("http://100.115.92.2:8081");
  
  // Real fields mimicking Kivy app inputs
  const [catInput, setCatInput] = useState<string>("Finanzen / Belege");
  const [titleInput, setTitleInput] = useState<string>("Stromrechnung ewz Zürich");
  const [bodyInput, setBodyInput] = useState<string>("Belegfoto ewz Strom 124.50 CHF eingereicht.");

  const [queue, setQueue] = useState<QueuedItem[]>([
    {
      id: "evt_m_01",
      eventType: "MOBILE_INGEST",
      payload: '{"title": "ewz Belegnummer #184-A", "category": "Finanzen / Belege", "body": "Stromabrechnung 124.50 CHF"}',
      retryCount: 0,
      status: "PENDING",
      backoffSec: 0
    },
    {
      id: "evt_m_02",
      eventType: "MOBILE_INGEST",
      payload: '{"title": "Mietzinsbeleg Patrick", "category": "Dokumente / Verträge", "body": "Kaltmiete 650.00 CHF bezahlt"}',
      retryCount: 0,
      status: "PENDING",
      backoffSec: 0
    }
  ]);
  const [logs, setLogs] = useState<string[]>([
    "OfflineSyncManager gestartet. Warte auf Netzwerk-Trigger...",
    "Lese lokale DB 'nexus_offline.db' ein...",
    "=== NEXUS MOBILE ENGINE STARTUP ==="
  ]);
  const [isSimulating, setIsSimulating] = useState<boolean>(false);
  const [deviceTilt, setDeviceTilt] = useState({ rX: -3, rY: 5 });

  // Add simulated device log entry
  const deviceLog = (message: string) => {
    const time = new Date().toLocaleTimeString();
    setLogs(prev => [`[${time}] ${message}`, ...prev.slice(0, 10)]);
  };

  const submitQuickEvent = () => {
    if (!titleInput.trim()) {
      deviceLog("Fehler: Titel darf nicht leer sein.");
      return;
    }

    const payloadObj = {
      title: titleInput,
      body: bodyInput,
      category: catInput,
      source: "Android App Client - Mobile"
    };

    const id = `evt_m_${Math.floor(Math.random() * 90) + 10}`;
    const newEvent: QueuedItem = {
      id,
      eventType: "MOBILE_INGEST",
      payload: JSON.stringify(payloadObj),
      retryCount: 0,
      status: "PENDING",
      backoffSec: 0
    };

    setQueue(prev => [...prev, newEvent]);
    deviceLog(`Event '${titleInput}' erfolgreich lokal indiziert.`);
    
    onAddLog({
      type: 'info',
      source: 'SQLite',
      message: `Android Daemon: Event '${id}' ('${titleInput}') in lokaler SQLite (nexus_offline.db) gepuffert.`
    });

    // Clear body field like real app
    setBodyInput("");
  };

  const handleToggleOnline = () => {
    setIsOnline(!isOnline);
    deviceLog(isOnline ? "[ OFFLINE ] Keine Verbindung zu Master" : "[ ONLINE ] Verbunden mit Master.");
  };

  const handleSyncStep = () => {
    if (isSimulating) return;
    setIsSimulating(true);
    deviceLog("Starte manuellen Synchronisations-Vorgang...");

    setTimeout(() => {
      setQueue(prev => prev.map(item => {
        if (item.status === 'SUCCESS' || item.status === 'FAILED_LIMIT') return item;

        if (isOnline) {
          deviceLog(`[SUCCESS] Rest-API sendet HTTP 200 für '${item.id}'`);
          onAddLog({
            type: 'success',
            source: 'PowerShell',
            message: `Android Ingestion: '${item.id}' erfolgreich per HMAC-Signatur an Master-Server ${serverUrl} übertragen.`
          });
          return { ...item, status: 'SUCCESS', retryCount: item.retryCount + 1 };
        } else {
          const newRetries = item.retryCount + 1;
          const exponentialBackoff = Math.pow(2, newRetries);
          const limitReached = newRetries >= 5;

          deviceLog(`[Errno 110] Timeout zu ${serverUrl}. Exponential Backoff: ${exponentialBackoff}s.`);
          onAddLog({
            type: 'warn',
            source: 'Chef-Logik',
            message: `Daemon Abgleich fehlgeschlagen für '${item.id}'. Backoff-Sperre: ${exponentialBackoff}s aktiv. Kein Datenverlust.`
          });

          return { 
            ...item, 
            status: limitReached ? 'FAILED_LIMIT' : 'RETRY', 
            retryCount: newRetries,
            backoffSec: exponentialBackoff
          };
        }
      }));
      setIsSimulating(false);
    }, 1200);
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch">
      
      {/* 3D phone interface mockup */}
      <div className="lg:col-span-6 flex justify-center items-center py-4 bg-black border border-[#14243b] rounded-2xl relative overflow-hidden shadow-2xl p-6">
        
        {/* Abstract design elements to enforce modern tech frame */}
        <div className="absolute top-2 left-6 text-[9px] font-mono text-zinc-500 tracking-wider">
          NEXUS PORTABLE APK EMULATOR v40.44
        </div>

        {/* 3D Cellphone Card */}
        <div 
          className="w-full max-w-[305px] h-[580px] bg-[#030304] border-4 border-[#14243b] rounded-[2.5rem] p-3 shadow-[0_0_30px_rgba(20,36,59,0.5)] relative transition-transform duration-300 ease-out neon-glow-blue"
          style={{
            transform: `perspective(1000px) rotateX(${deviceTilt.rX}deg) rotateY(${deviceTilt.rY}deg)`,
          }}
          onMouseMove={(e) => {
            const rect = e.currentTarget.getBoundingClientRect();
            const x = e.clientX - rect.left - rect.width/2;
            const y = e.clientY - rect.top - rect.height/2;
            setDeviceTilt({ rX: y / 15, rY: -x / 15 });
          }}
          onMouseLeave={() => setDeviceTilt({ rX: -3, rY: 5 })}
        >
          {/* Speaker Slot / Camera punch hole */}
          <div className="absolute top-2.5 left-1/2 transform -translate-x-1/2 w-28 h-4 bg-[#14243b] rounded-full flex items-center justify-center">
            <div className="w-10 h-0.5 bg-black rounded-full" />
          </div>

          {/* Internal Mobile Display Screen */}
          <div className="w-full h-full bg-black rounded-[2rem] p-3 pt-5 flex flex-col justify-between overflow-hidden relative">
            
            {/* Display Header Status Bar */}
            <div className="flex items-center justify-between text-[10px] text-zinc-400 font-mono mb-2 px-1">
              <span>09:41 AM</span>
              <div className="flex items-center gap-1">
                {isOnline ? (
                  <>
                    <Wifi className="h-3 w-3 text-emerald-400" />
                    <span className="text-emerald-400 font-bold text-[9px]">ONLINE</span>
                  </>
                ) : (
                  <>
                    <WifiOff className="h-3 w-3 text-red-500" />
                    <span className="text-red-500 font-bold text-[9px]">OFFLINE</span>
                  </>
                )}
              </div>
            </div>

            {/* Simulated Android Kivy App Layout */}
            <div className="flex-1 flex flex-col gap-2 py-0.5 overflow-hidden">
              
              {/* Kivy App Header */}
              <div className="bg-[#030304] border-2 border-[#14243b] p-2 rounded-xl text-center">
                <p className="text-xs font-extrabold text-[#00c0ff] tracking-wide uppercase">NEXUS DAEMON v40.44</p>
                <p className={`text-[9px] font-mono mt-0.5 ${isOnline ? 'text-emerald-450 text-emerald-400' : 'text-amber-500'}`}>
                  {isOnline ? `[ ONLINE ] Verbunden mit Master: ${serverUrl}` : `[ OFFLINE ] Keine Verbindung zu: ${serverUrl}`}
                </p>
              </div>

              {/* Seed IP Config Row */}
              <div className="bg-[#030304] border border-[#14243b] p-1.5 rounded-lg flex items-center gap-1">
                <span className="text-[9px] font-mono text-zinc-500">Master-IP:</span>
                <input 
                  type="text" 
                  value={serverUrl} 
                  onChange={(e) => setServerUrl(e.target.value)}
                  className="bg-[#050508] text-[#00d0ff] font-mono rounded text-[10px] px-1 py-0.5 w-full border border-zinc-800 focus:outline-none"
                />
              </div>

              {/* Mobile Quick Ingest Cockpit */}
              <div className="bg-[#030304] border-2 border-[#112d1c] p-2 rounded-xl flex flex-col gap-1.5">
                <span className="text-[9px] font-mono text-emerald-400 font-bold uppercase tracking-wider block">
                  MOBILES COCKPIT - SCHNELLBELEG
                </span>
                
                {/* Simulated Fields */}
                <div className="space-y-1">
                  <div className="flex items-center justify-between text-[9px]">
                    <span className="text-zinc-500">Kategorie:</span>
                    <input 
                      type="text" 
                      value={catInput}
                      onChange={(e) => setCatInput(e.target.value)}
                      className="bg-black text-white text-[10px] px-1 py-0.5 rounded border border-zinc-900 w-36"
                    />
                  </div>
                  <div className="flex items-center justify-between text-[9px]">
                    <span className="text-zinc-500">Titel/Quelle:</span>
                    <input 
                      type="text" 
                      value={titleInput}
                      onChange={(e) => setTitleInput(e.target.value)}
                      className="bg-black text-white text-[10px] px-1 py-0.5 rounded border border-zinc-900 w-36"
                    />
                  </div>
                  <div className="flex flex-col text-[9px]">
                    <span className="text-zinc-500 mb-0.5">Beschreibung / Wert:</span>
                    <input 
                      type="text" 
                      value={bodyInput}
                      onChange={(e) => setBodyInput(e.target.value)}
                      className="bg-black text-white text-[10px] px-1 py-0.5 rounded border border-zinc-900 w-full"
                    />
                  </div>
                </div>

                <button
                  onClick={submitQuickEvent}
                  className="w-full bg-[#009b4c] hover:bg-[#00b056] text-white text-[9px] font-bold py-1.5 rounded-lg tracking-wider transition-all cursor-pointer shadow-lg uppercase"
                >
                  IN DIE OFFLINE-QUEUE SPEICHERN
                </button>
              </div>

              {/* Queue Counters & Actions */}
              <div className="bg-[#030304] border-2 border-[#14243b] p-2 rounded-xl flex items-center justify-between gap-1.5 shadow-md">
                <span className="text-[10px] font-bold text-sky-400 font-mono">
                  Queue: {queue.filter(q => q.status === 'PENDING' || q.status === 'RETRY').length} Einträge
                </span>
                <button
                  onClick={handleSyncStep}
                  disabled={isSimulating || queue.filter(q => q.status === 'PENDING' || q.status === 'RETRY').length === 0}
                  className="bg-[#0066cc] hover:bg-[#0080ff] text-white font-bold text-[9px] px-2 py-1 rounded-lg flex items-center gap-1 transition-all disabled:opacity-35 cursor-pointer uppercase"
                >
                  <RefreshCw className={`h-2.5 w-2.5 ${isSimulating ? 'animate-spin' : ''}`} />
                  JETZT ABGLEICHEN
                </button>
              </div>

              {/* Interactive log list for apk screen */}
              <div className="flex-1 bg-black rounded-lg border border-[#14243b] p-2 flex flex-col overflow-hidden">
                <span className="text-[8px] font-mono text-zinc-500 uppercase tracking-wider mb-1 block">
                  DIAGNOSTIK TERMINAL EMULATION
                </span>
                <div className="flex-1 overflow-y-auto font-mono text-[9px] text-[#00ff66] space-y-1 scrollbar-none leading-tight">
                  {logs.map((log, index) => (
                    <div key={index} className="break-all">
                      {log}
                    </div>
                  ))}
                </div>
              </div>

            </div>

          </div>

        </div>

      </div>

      {/* Sync Scheduler Details & Logic Control */}
      <div className="lg:col-span-6 bg-black border-2 border-[#14243b] rounded-2xl p-6 shadow-2xl flex flex-col justify-between hover:border-sky-500/30 transition-all duration-350">
        
        <div className="space-y-4">
          <div>
            <span className="text-[10px] font-mono text-emerald-400 uppercase tracking-widest block font-bold">
              SYSTEM-PRÄVENTION GEGEN MOBILSPERREN
            </span>
            <h2 className="text-lg font-bold tracking-tight text-white font-sans mt-0.5">
              Kivy Daemon & Queue-Struktur
            </h2>
            <p className="text-xs text-zinc-400 mt-1 leading-relaxed font-sans">
              Die physische Android-Anwendung weicht Verbindungsblockaden autonom aus. Alle SMS-Ingestionen, Zählerstände und Schnellbelege werden lückenlos in der lokalen SQLite (<code className="text-zinc-300 font-mono bg-zinc-950 px-1 rounded">nexus_offline.db</code>) mit Zeitstempel und HMAC-Verschlüsselung verriegelt.
            </p>
          </div>

          {/* Controls toggle bar */}
          <div className="bg-[#030304] p-3 rounded-xl border border-[#14243b] flex flex-wrap gap-2 items-center justify-between">
            <button
              onClick={handleToggleOnline}
              className={`text-xs font-bold py-1.5 px-3 rounded-lg flex items-center gap-1.5 transition-colors cursor-pointer border ${
                isOnline 
                  ? 'bg-emerald-900/40 hover:bg-emerald-800/40 text-emerald-400 border-emerald-500/40' 
                  : 'bg-red-950/40 hover:bg-red-900/40 text-red-400 border-red-500/40'
              }`}
              id="toggle-emulator-online-btn"
            >
              {isOnline ? (
                <>
                  <Wifi className="h-3.5 w-3.5 text-emerald-400" />
                  VPN: Online (Tailscale)
                </>
              ) : (
                <>
                  <WifiOff className="h-3.5 w-3.5 text-red-500" />
                  VPN: Offline
                </>
              )}
            </button>

            <button
              onClick={handleSyncStep}
              disabled={isSimulating || queue.filter(q => q.status === 'PENDING' || q.status === 'RETRY').length === 0}
              className="text-xs bg-sky-600 hover:bg-sky-500 text-white font-bold py-1.5 px-3.5 rounded-lg flex items-center gap-1.5 disabled:opacity-35 disabled:pointer-events-none cursor-pointer transition-all"
              id="trigger-sync-loop-btn"
            >
              <RefreshCw className={`h-3.5 w-3.5 ${isSimulating ? 'animate-spin' : ''}`} />
              Manuell Synchronisieren
            </button>
          </div>

          {/* Current Queue items inside the cache */}
          <div className="space-y-2">
            <span className="text-[10px] uppercase tracking-wider text-zinc-500 font-mono block">
              Inhalt der "sync_queue" Tabelle in SQLite
            </span>

            <div className="space-y-2 max-h-[190px] overflow-y-auto scrollbar-thin">
              {queue.map(item => {
                let payloadLabel = "";
                try {
                  const parsed = JSON.parse(item.payload);
                  payloadLabel = `[${parsed.category}] ${parsed.title}: ${parsed.body}`;
                } catch {
                  payloadLabel = item.payload;
                }
                
                return (
                  <div
                    key={item.id}
                    className="bg-[#030304] rounded-lg p-3 border-2 border-[#14243b] flex items-center justify-between gap-3 text-xs"
                  >
                    <div className="space-y-1 min-w-0">
                      <div className="flex items-center gap-1.5">
                        <span className="font-mono text-[9px] text-[#00c0ff]">{item.id}</span>
                        <span className="font-bold text-zinc-300 font-mono text-[10px] uppercase">{item.eventType}</span>
                      </div>
                      <p className="text-zinc-400 text-[10px] italic truncate">"{payloadLabel}"</p>
                    </div>

                    <div className="text-right shrink-0">
                      <span className={`text-[9px] px-1.5 py-0.5 rounded font-mono font-bold ${
                        item.status === 'SUCCESS' 
                          ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' 
                          : item.status === 'RETRY' 
                          ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                          : item.status === 'FAILED_LIMIT'
                          ? 'bg-red-500/10 text-red-500 border border-red-500/20'
                          : 'bg-zinc-800 text-zinc-400'
                      }`}>
                        {item.status}
                      </span>
                      <div className="text-[9px] text-zinc-500 mt-1 font-mono">
                        Lauf: {item.status === 'SUCCESS' ? item.retryCount - 1 : item.retryCount}/5
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* Backoff diagram visualization */}
        <div className="mt-4 pt-4 border-t border-[#14243b] bg-zinc-950 p-3 rounded-lg flex items-center gap-3 text-[11px] text-zinc-500 font-mono">
          <Clock className="h-4 w-4 text-emerald-400 shrink-0 animate-pulse" />
          <span>
            <strong>Kivy Transaktions-Schnittstelle:</strong> <code className="text-[#00ff66]">HMAC-SHA256</code> signiert. Falls offline, triggert die Queue einen exponentialen Backoff-Loop gegen Datenverlust.
          </span>
        </div>

      </div>

    </div>
  );
};
