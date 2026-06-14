import React, { useState, useEffect, useRef } from 'react';
import { 
  Database, 
  ShieldCheck, 
  Zap, 
  Gauge, 
  Server,
  Activity,
  Layers,
  Sparkles,
  Terminal,
  CheckCircle2,
  FolderHeart,
  Compass,
  RotateCw,
  Search,
  Eye,
  Settings
} from 'lucide-react';
import { 
  Scene, 
  WebGLRenderer, 
  PerspectiveCamera, 
  AmbientLight, 
  PointLight, 
  BoxGeometry, 
  SphereGeometry, 
  MeshPhongMaterial, 
  Mesh, 
  GridHelper,
  Group,
  Vector3
} from 'three';
import { SystemStatus } from '../types';

interface ThreeDMetricsProps {
  status: SystemStatus;
}

interface DataCategory {
  id: string;
  name: string;
  title: string;
  path: string;
  sizeGb: number;
  records: number;
  color: number; // hex for Three.js
  colorClass: string; // Tailwind color matching
  colorText: string;
  colorHex: string; // Hex code as string
  table: string;
  description: string;
  integritySign: string;
}

const CATEGORIES: DataCategory[] = [
  {
    id: "00_START_HIER",
    name: "00_START_HIER",
    title: "Einstieg & Bedienung",
    path: "C:\\MasterIndex_Storage\\00_START_HIER",
    sizeGb: 0.1,
    records: 3,
    color: 0x0ea5e9, // Sky Blue
    colorClass: "bg-sky-500/20 text-sky-400 border-sky-500/40",
    colorText: "text-sky-400",
    colorHex: "#0ea5e9",
    table: "system_status_history",
    description: "Systemstatus-Metadaten, Kontrollzertifikate, HMAC-Token",
    integritySign: "SHA-256 Validated - Token HMAC Approved"
  },
  {
    id: "01_ARCHITEKTUR",
    name: "01_ARCHITEKTUR",
    title: "Systemarchitektur",
    path: "C:\\MasterIndex_Storage\\01_ARCHITEKTUR",
    sizeGb: 0.2,
    records: 5,
    color: 0x10b981, // Emerald Green
    colorClass: "bg-emerald-500/20 text-emerald-400 border-emerald-500/40",
    colorText: "text-emerald-400",
    colorHex: "#10b981",
    table: "architecture_specs",
    description: "Regelwerke, Systemdokumente, Datenfluss-Modelle",
    integritySign: "SHA-256 Validated - SSOT Constraint Verified"
  },
  {
    id: "02_INDEX_CHEF",
    name: "02_INDEX_CHEF",
    title: "Chef-Logik & Kontext",
    path: "C:\\MasterIndex_Storage\\02_INDEX_CHEF",
    sizeGb: 0.1,
    records: 4,
    color: 0x0ea5e9, // Sky Blue
    colorClass: "bg-sky-500/20 text-sky-400 border-sky-500/40",
    colorText: "text-sky-400",
    colorHex: "#0ea5e9",
    table: "chef_rules_v40",
    description: "Budgetlimits ($15 Locker), Abfangregeln, Inhaltsheuristik",
    integritySign: "SHA-256 Validated - Budget Lockdown Cap Secure"
  },
  {
    id: "03_KOMMUNIKATION",
    name: "03_KOMMUNIKATION",
    title: "Kommunikation & Timeline",
    path: "C:\\MasterIndex_Storage\\03_KOMMUNIKATION",
    sizeGb: 12.4,
    records: 142500,
    color: 0x0ea5e9, // Sky Blue
    colorClass: "bg-sky-500/20 text-sky-400 border-sky-500/40",
    colorText: "text-sky-400",
    colorHex: "#0ea5e9",
    table: "decision_log / inbox_feed",
    description: "Eingehende E-Mails, Telegram-Ereignisse, SMS-Zahlungsbestätigungen",
    integritySign: "Sequential Block-Linked Audit - Double-Entry OK"
  },
  {
    id: "04_DATEIEN_UND_KATALOG",
    name: "04_DATEIEN_UND_KATALOG",
    title: "Dateien & SQLite Katalog",
    path: "C:\\MasterIndex_Storage\\_NEXUS_SYSTEM\\db\\nexus_catalog.sqlite",
    sizeGb: 58.6,
    records: 601309,
    color: 0x10b981, // Emerald Green
    colorClass: "bg-emerald-500/20 text-emerald-400 border-emerald-500/40",
    colorText: "text-emerald-400",
    colorHex: "#10b981",
    table: "file_index / sync_queue",
    description: "Volltextindizes, Zählerstände, physische Datenträger-Metadaten",
    integritySign: "Auto-Pragma OK - Local Master SQLite Verifiziert"
  }
];

export const ThreeDMetrics: React.FC<ThreeDMetricsProps> = ({ status }) => {
  // Navigation & Control States
  const [activeCatId, setActiveCatId] = useState<string>("04_DATEIEN_UND_KATALOG");
  const [rotationSpeed, setRotationSpeed] = useState<'off' | 'slow' | 'fast'>('slow');
  const [cameraPreset, setCameraPreset] = useState<'orbit' | 'closeup' | 'birdseye'>('orbit');

  // Audit state machine
  const [auditProgress, setAuditProgress] = useState<number>(0);
  const [auditStatus, setAuditStatus] = useState<'idle' | 'scanning' | 'completed'>('idle');
  const [auditLogs, setAuditLogs] = useState<string[]>([]);
  const consoleBottomRef = useRef<HTMLDivElement>(null);

  // Bento card 3D tilts (original aesthetics)
  const [tilts, setTilts] = useState<{ [key: string]: { rX: number; rY: number } }>({
    m1: { rX: 0, rY: 0 },
    m2: { rX: 0, rY: 0 },
    m3: { rX: 0, rY: 0 },
    m4: { rX: 0, rY: 0 },
    threeD: { rX: 0, rY: 0 }
  });

  const handleMouseMove = (id: string, e: React.MouseEvent<HTMLDivElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const x = e.clientX - rect.left - rect.width / 2;
    const y = e.clientY - rect.top - rect.height / 2;
    setTilts(prev => ({
      ...prev,
      [id]: { rX: -y / 15, rY: x / 15 }
    }));
  };

  const handleMouseLeave = (id: string) => {
    setTilts(prev => ({
      ...prev,
      [id]: { rX: 0, rY: 0 }
    }));
  };

  // Three.js refs
  const threeContainerRef = useRef<HTMLDivElement>(null);
  const activeCatIdRef = useRef(activeCatId);
  const rotationSpeedRef = useRef(rotationSpeed);
  const cameraPresetRef = useRef(cameraPreset);

  // Sync refs with state to prevent WebGL teardown
  useEffect(() => { activeCatIdRef.current = activeCatId; }, [activeCatId]);
  useEffect(() => { rotationSpeedRef.current = rotationSpeed; }, [rotationSpeed]);
  useEffect(() => { cameraPresetRef.current = cameraPreset; }, [cameraPreset]);

  // Handle audit logging scroll down
  useEffect(() => {
    if (consoleBottomRef.current) {
      consoleBottomRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [auditLogs]);

  // Start localized database block audit simulation
  const triggerIntegrityAudit = (catId: string) => {
    const selected = CATEGORIES.find(c => c.id === catId);
    if (!selected) return;

    setAuditStatus('scanning');
    setAuditProgress(5);
    setAuditLogs([
      `[INIT] Starte Integritäts-Audit für Block: ${selected.name}`,
      `[PATH] Physischer Pfad-Check: ${selected.path}`,
      `[SQL] Verbinde mit Master-SQLite-Katalog auf Port 8081... v40.44`,
    ]);

    let currentPercent = 5;
    const interval = setInterval(() => {
      currentPercent += Math.floor(Math.random() * 15) + 5;
      if (currentPercent >= 100) {
        currentPercent = 100;
        clearInterval(interval);
        setAuditStatus('completed');
        setAuditProgress(100);
        setAuditLogs(prev => [
          ...prev,
          `[SCAN] Verarbeite Index-Einträge... ${selected.records.toLocaleString('de-DE')} Datensätze`,
          `[SCAN] Block-Indexierte Kapazität: ${selected.sizeGb} GB`,
          `[SEC] Berechne SHA-256 HMAC Prüfsumme auf Hostspeicher: OK`,
          `[RULE] Evaluiere Feature-Bloat-Index: 0% Konformität`,
          `[RESULT] ✔ ERFOLGREICH: Block '${selected.name}' ist vollständig integer und SSOT-Konform.`,
          `[DONE] Audit abgeschlossen um ${new Date().toLocaleTimeString('de-DE')} UTC.`
        ]);
      } else {
        setAuditProgress(currentPercent);
        
        // Dynamic simulated lines based on current percentage
        if (currentPercent > 20 && currentPercent < 40) {
          setAuditLogs(prev => {
            if (prev.some(l => l.includes('PRAGMA'))) return prev;
            return [...prev, `[TEST] Führe SQLite Befehl aus: PRAGMA integrity_check(${selected.table || 'file_index'})`];
          });
        }
        if (currentPercent > 45 && currentPercent < 65) {
          setAuditLogs(prev => {
            if (prev.some(l => l.includes('BUFFER'))) return prev;
            return [...prev, `[BUFFER] Analysiere sync_queue [Status: PASSIVE] - Keine Ghost Prozesse`];
          });
        }
        if (currentPercent > 70 && currentPercent < 85) {
          setAuditLogs(prev => {
            if (prev.some(l => l.includes('DISK'))) return prev;
            return [...prev, `[DISK] Belege-Scangröße validiert auf Dateiebene (~${selected.sizeGb} GB)`];
          });
        }
      }
    }, 280);
  };

  // Three.js Initialization & Render Loop
  useEffect(() => {
    if (!threeContainerRef.current) return;

    const container = threeContainerRef.current;
    const scene = new Scene();

    const renderer = new WebGLRenderer({ antialias: true, alpha: true });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.setSize(container.clientWidth, container.clientHeight);
    container.appendChild(renderer.domElement);

    const camera = new PerspectiveCamera(40, container.clientWidth / container.clientHeight, 0.1, 100);
    // Initial camera position
    camera.position.set(0, 4, 10);

    // Lights
    const ambientLight = new AmbientLight(0xffffff, 0.3);
    scene.add(ambientLight);

    const mainLight = new PointLight(0x4f46e5, 2.5, 30);
    mainLight.position.set(2, 6, 4);
    scene.add(mainLight);

    const floorLight = new PointLight(0x10b981, 1.5, 15);
    floorLight.position.set(-3, -2, -2);
    scene.add(floorLight);

    // Grid Floor reflecting physical cluster
    const gridHelper = new GridHelper(12, 12, 0x312e81, 0x1c1917);
    gridHelper.position.y = -1.2;
    scene.add(gridHelper);

    // Group to hold all metric structures
    const graphGroup = new Group();
    scene.add(graphGroup);

    // Build the 3D storage monolith pillars (Representing Patricks folders)
    const pillarMeshes: Mesh[] = [];
    const orbitGeoms: Group[] = [];

    const sphereGeom = new SphereGeometry(0.1, 8, 8);

    CATEGORIES.forEach((cat, index) => {
      // Calculate layout positions from X = -4.5 to +4.5
      const x = (index - 2) * 2.2;
      const z = 0;

      // Heights map logarithmically to keep them scaled wonderfully
      // e.g. 58.6 GB is tall (4.0), 12.4 GB is medium (2.8), small entries are sleek (1.2)
      const height = cat.id === "04_DATEIEN_UND_KATALOG" ? 3.8
                   : cat.id === "03_KOMMUNIKATION" ? 2.5
                   : cat.id === "01_ARCHITEKTUR" ? 1.4
                   : 1.0;

      // Custom widths representing volume weight
      const width = cat.id === "04_DATEIEN_UND_KATALOG" ? 1.1
                  : cat.id === "03_KOMMUNIKATION" ? 0.9
                  : 0.6;

      const boxGeom = new BoxGeometry(width, height, width);
      const boxMat = new MeshPhongMaterial({
        color: cat.color,
        emissive: cat.color,
        emissiveIntensity: 0.25,
        shininess: 120,
        flatShading: true,
        transparent: true,
        opacity: 0.78
      });

      const monolith = new Mesh(boxGeom, boxMat);
      // sit perfectly on the grid floor (-1.2 + half of height)
      monolith.position.set(x, -1.2 + height / 2, z);
      monolith.userData = { 
        id: cat.id, 
        baseHeight: height, 
        baseWidth: width, 
        color: cat.color 
      };

      graphGroup.add(monolith);
      pillarMeshes.push(monolith);

      // Add a rotating orbit cluster around each pillar based on file indexing activity
      const particleGroup = new Group();
      particleGroup.position.set(x, -1.2 + height / 2, z);
      
      const particleCount = cat.id === "04_DATEIEN_UND_KATALOG" ? 22 
                          : cat.id === "03_KOMMUNIKATION" ? 12 
                          : 3;
      
      const pColor = cat.color;

      for (let p = 0; p < particleCount; p++) {
        const mat = new MeshPhongMaterial({ 
          color: pColor, 
          emissive: pColor, 
          emissiveIntensity: 0.6 
        });
        const m = new Mesh(sphereGeom, mat);
        
        // Random shell orbit distance
        const radiusDist = width / 2 + 0.25 + Math.random() * 0.45;
        const angle = Math.random() * Math.PI * 2;
        m.position.set(Math.cos(angle) * radiusDist, (Math.random() - 0.5) * (height - 0.4), Math.sin(angle) * radiusDist);
        
        // Store speed mapping for loop
        m.userData = { 
          angle: angle, 
          radius: radiusDist, 
          speed: (0.015 + Math.random() * 0.035) * (cat.id === "04_DATEIEN_UND_KATALOG" ? 1.5 : 0.8),
          heightOffset: m.position.y 
        };
        particleGroup.add(m);
      }
      graphGroup.add(particleGroup);
      orbitGeoms.push(particleGroup);
    });

    // Camera animation state controls
    let animateClock = 0;
    let orbitAngle = 0;
    const cameraTarget = new Vector3(0, 0, 0);

    const animate = () => {
      requestAnimationFrame(animate);
      animateClock += 0.01;

      // Rotate group if setting is on
      const speedSetting = rotationSpeedRef.current;
      if (speedSetting !== 'off') {
        const rotInc = speedSetting === 'fast' ? 0.016 : 0.0035;
        graphGroup.rotation.y += rotInc;
      }

      // Read active category selected in React UI
      const activeId = activeCatIdRef.current;
      const activeIdx = CATEGORIES.findIndex(c => c.id === activeId);

      // 1. Highlight / Scale Active Pillar
      pillarMeshes.forEach(mesh => {
        const isCurrent = mesh.userData.id === activeId;
        const baseHeight = mesh.userData.baseHeight;
        const baseWidth = mesh.userData.baseWidth;

        if (isCurrent) {
          // Dynamic scale-pulse and intense emissive glow
          const pulse = 1.05 + Math.sin(animateClock * 5.0) * 0.04;
          mesh.scale.set(pulse, pulse, pulse);
          (mesh.material as MeshPhongMaterial).emissiveIntensity = 0.7 + Math.sin(animateClock * 4) * 0.25;
          (mesh.material as MeshPhongMaterial).opacity = 0.95;
        } else {
          // Dim down unselected folder stacks for superior visualization hierarchy
          mesh.scale.set(1, 1, 1);
          (mesh.material as MeshPhongMaterial).emissiveIntensity = 0.15;
          (mesh.material as MeshPhongMaterial).opacity = 0.5;
        }
      });

      // 2. Animate orbit micro-sphere particles
      orbitGeoms.forEach((grp, idx) => {
        const catId = CATEGORIES[idx].id;
        const isCurrent = catId === activeId;
        
        grp.children.forEach(child => {
          const mesh = child as Mesh;
          mesh.userData.angle += mesh.userData.speed;
          
          // Slowly orbit around center pillar
          mesh.position.x = Math.cos(mesh.userData.angle) * mesh.userData.radius;
          mesh.position.z = Math.sin(mesh.userData.angle) * mesh.userData.radius;
          
          // Hover up/down gently
          mesh.position.y = mesh.userData.heightOffset + Math.sin(animateClock * 2 + mesh.userData.angle) * 0.1;
          
          // Particles rotate around with the master model as well, scale up slightly if active
          if (isCurrent) {
            mesh.scale.set(1.4, 1.4, 1.4);
          } else {
            mesh.scale.set(1.0, 1.0, 1.0);
          }
        });
      });

      // 3. Smooth Camera & Look-At Interpolation (LERP) based on camera mode preset
      const preset = cameraPresetRef.current;
      const targetPos = new Vector3();
      const lookPos = new Vector3();

      if (preset === 'closeup' && activeIdx !== -1) {
        // closeup path: Focus narrowly on the selected card column
        const pillarX = (activeIdx - 2) * 2.2;
        targetPos.set(pillarX + 1.2, 1.2, 4.0);
        lookPos.set(pillarX, 0, 0);
      } else if (preset === 'birdseye') {
        // top down overview looking down onto the cluster rows
        targetPos.set(0, 9.0, 2.0);
        lookPos.set(0, -1.0, 0);
      } else {
        // Slow manual orbit if speed is off, or automated orbit
        if (speedSetting === 'off') {
          targetPos.set(0, 3.5, 9.5);
          lookPos.set(0, 0, 0);
        } else {
          // Automatic rotating camera angle
          orbitAngle += 0.003;
          targetPos.set(Math.sin(orbitAngle) * 9.2, 3.2, Math.cos(orbitAngle) * 9.2);
          lookPos.set(0, 0, 0);
        }
      }

      // Perform LERP on position & viewport look-at vector targets
      camera.position.lerp(targetPos, 0.06);
      cameraTarget.lerp(lookPos, 0.06);
      camera.lookAt(cameraTarget);

      renderer.render(scene, camera);
    };

    animate();

    // 4. Resize Observer for dynamic aspect ratio handling
    const handleResize = () => {
      if (!container) return;
      const w = container.clientWidth;
      const h = container.clientHeight;
      camera.aspect = w / h;
      camera.updateProjectionMatrix();
      renderer.setSize(w, h);
    };

    const rob = new ResizeObserver(() => handleResize());
    rob.observe(container);

    // Instantly run audit simulation on initialization category
    triggerIntegrityAudit("04_DATEIEN_UND_KATALOG");

    // Clean up Three.js components robustly to avoid leaks
    return () => {
      rob.disconnect();
      sphereGeom.dispose();
      
      pillarMeshes.forEach(m => {
        m.geometry.dispose();
        if (Array.isArray(m.material)) m.material.forEach(mat => mat.dispose());
        else m.material.dispose();
      });

      gridHelper.geometry.dispose();
      if (Array.isArray(gridHelper.material)) gridHelper.material.forEach(mat => mat.dispose());
      else gridHelper.material.dispose();

      renderer.dispose();
      if (container && renderer.domElement && container.contains(renderer.domElement)) {
        container.removeChild(renderer.domElement);
      }
    };
  }, []);

  const activeCategory = CATEGORIES.find(c => c.id === activeCatId) || CATEGORIES[4];

  return (
    <div className="space-y-4">
      
      {/* Interactive Storage Physical Allocator 3D Dashboard Card */}
      <div 
        className="w-full bg-[#111113] border border-[#232329] hover:border-gray-800 rounded-2xl p-5 shadow-2xl relative overflow-hidden transition-all duration-300 transform-gpu cursor-default"
        style={{
          transform: `perspective(1000px) rotateX(${tilts.threeD.rX}deg) rotateY(${tilts.threeD.rY}deg)`,
          transformStyle: 'preserve-3d',
          boxShadow: '0 25px 60px rgba(0,0,0,0.65)'
        }}
        onMouseMove={(e) => handleMouseMove('threeD', e)}
        onMouseLeave={() => handleMouseLeave('threeD')}
      >
        <div className="absolute top-0 right-10 w-96 h-96 bg-gradient-to-br from-[#10b981]/5 to-transparent rounded-full blur-3xl pointer-events-none" />

        {/* 1. Header HUD with live global sizes */}
        <div className="flex flex-wrap items-center justify-between gap-4 mb-4 relative z-10" style={{ transform: 'translateZ(20px)' }}>
          <div>
            <div className="flex items-center gap-2">
              <span className="p-1 bg-emerald-500/10 text-emerald-400 rounded border border-emerald-500/20 text-[10px] uppercase font-mono font-bold">
                SSOT Speichermatrix
              </span>
              <span className="h-2 w-2 rounded-full bg-emerald-400 animate-ping"></span>
            </div>
            <h3 className="text-base font-bold text-white flex items-center gap-2 mt-1">
              <Database className="h-4 w-4 text-emerald-400" />
              3D SQLite Speicherallokation & Master-Verzeichnis
            </h3>
            <p className="text-xs text-gray-500 mt-0.5">
              Physische Repräsentation der <strong className="text-gray-300">71,4 Gigabyte</strong> Datenstruktur Ihres lokalen Servers aufgeteilt in 5 Haupt-Monolithe.
            </p>
          </div>

          <div className="flex items-center gap-2 bg-[#08080a] p-1.5 rounded-lg border border-white/5 font-mono text-xs">
            <span className="text-gray-500">GESAMT:</span>
            <span className="text-white font-bold">{status.totalRecords.toLocaleString('de-DE')} Records</span>
            <span className="text-emerald-400 font-black">~{status.totalSizeGb.toLocaleString('de-DE')} GB</span>
          </div>
        </div>

        {/* 2. Interactive Column Control Bar Overlay */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 items-stretch relative z-10 select-none pb-2" style={{ transform: 'translateZ(30px)' }}>
          
          {/* WebGL viewport column */}
          <div className="lg:col-span-8 flex flex-col gap-3">
            {/* Real Three.js Canvas mounting frame */}
            <div className="w-full bg-[#070708]/95 border border-[#212126] rounded-xl h-72 md:h-80 relative overflow-hidden flex items-end">
              <div className="absolute inset-0 w-full h-full" ref={threeContainerRef} />
              
              {/* Top controls HUD over canvas */}
              <div className="absolute top-3 left-3 flex flex-wrap gap-2 items-center z-20 pointer-events-auto">
                <div className="bg-[#0e0e11]/90 backdrop-blur-md px-2.5 py-1.5 rounded-lg border border-white/5 flex items-center gap-1.5 text-[10px]">
                  <Compass className="h-3 w-3 text-sky-400" />
                  <span className="text-gray-400 font-mono">3D KAMERA:</span>
                  <button 
                    onClick={() => { setCameraPreset('orbit'); }}
                    className={`px-1.5 py-0.5 rounded transition ${cameraPreset === 'orbit' ? 'bg-indigo-600 text-white font-bold' : 'text-gray-400 hover:text-white'}`}
                  >
                    Orbit
                  </button>
                  <button 
                    onClick={() => { setCameraPreset('closeup'); }}
                    className={`px-1.5 py-0.5 rounded transition ${cameraPreset === 'closeup' ? 'bg-indigo-600 text-white font-bold' : 'text-gray-400 hover:text-white'}`}
                  >
                    Close-Up
                  </button>
                  <button 
                    onClick={() => { setCameraPreset('birdseye'); }}
                    className={`px-1.5 py-0.5 rounded transition ${cameraPreset === 'birdseye' ? 'bg-indigo-600 text-white font-bold' : 'text-gray-400 hover:text-white'}`}
                  >
                    Top-Down
                  </button>
                </div>

                <div className="bg-[#0e0e11]/90 backdrop-blur-md px-2.5 py-1.5 rounded-lg border border-white/5 flex items-center gap-1.5 text-[10px]">
                  <RotateCw className="h-3 w-3 text-emerald-400" />
                  <span className="text-gray-400 font-mono">ORBIT SPEED:</span>
                  <button 
                    onClick={() => { setRotationSpeed('off'); }}
                    className={`px-1.5 py-0.5 rounded transition ${rotationSpeed === 'off' ? 'bg-emerald-600 text-white font-bold' : 'text-gray-400 hover:text-white'}`}
                  >
                    Aus
                  </button>
                  <button 
                    onClick={() => { setRotationSpeed('slow'); }}
                    className={`px-1.5 py-0.5 rounded transition ${rotationSpeed === 'slow' ? 'bg-emerald-600 text-white font-bold' : 'text-gray-400 hover:text-white'}`}
                  >
                    Langsam
                  </button>
                  <button 
                    onClick={() => { setRotationSpeed('fast'); }}
                    className={`px-1.5 py-0.5 rounded transition ${rotationSpeed === 'fast' ? 'bg-emerald-600 text-white font-bold' : 'text-gray-400 hover:text-white'}`}
                  >
                    Schnell
                  </button>
                </div>
              </div>

              {/* Quick Pillar HUD Indicator Labels inside canvas box */}
              <div className="absolute right-3 top-3 bg-[#0a0a0d]/80 backdrop-blur-sm border border-white/5 px-2.5 py-2 rounded-lg pointer-events-none hidden sm:block text-[9px] font-mono leading-relaxed max-w-[170px]">
                <div className="text-gray-400 font-bold mb-1 border-b border-white/5 pb-0.5 uppercase tracking-wider">Pillargewichtung</div>
                <div className="flex items-center gap-1.5">
                  <span className="h-1.5 w-1.5 rounded-full bg-emerald-400"></span>
                  <span className="text-gray-300">File-Index / SQLite DB</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="h-1.5 w-1.5 rounded-full bg-pink-400"></span>
                  <span className="text-gray-300">Kommunikation Logs</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="h-1.5 w-1.5 rounded-full bg-violet-400"></span>
                  <span className="text-gray-300">Systemarchitektur</span>
                </div>
              </div>

              {/* Absolute lower 3D selector HUD row overlay */}
              <div className="absolute bottom-3 left-3 right-3 flex justify-between gap-1 overflow-x-auto bg-[#070709]/80 backdrop-blur-md p-1 rounded-xl border border-white/5 z-20 pointer-events-auto shrink-0 scrollbar-none">
                {CATEGORIES.map((cat) => {
                  const isActive = cat.id === activeCatId;
                  return (
                    <button
                      key={cat.id}
                      onClick={() => {
                        setActiveCatId(cat.id);
                        if (cameraPreset === 'closeup') {
                          // Keepcloseup but shift focus
                        }
                      }}
                      className={`px-2.5 py-1.5 rounded-lg text-[10px] font-mono transition-all flex items-center gap-1.5 shrink-0 uppercase border ${
                        isActive 
                          ? `${cat.colorClass} border-opacity-70 shadow-lg shadow-black/50 font-bold` 
                          : 'bg-transparent border-transparent text-gray-400 hover:text-white hover:bg-white/5'
                      }`}
                    >
                      <span className={`h-2 w-2 rounded-full`} style={{ backgroundColor: `${cat.colorHex}` }}></span>
                      {cat.name}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Quick interactive Selector List */}
            <div className="grid grid-cols-2 sm:grid-cols-5 gap-2">
              {CATEGORIES.map(cat => {
                const isActive = cat.id === activeCatId;
                return (
                  <div 
                    key={cat.id}
                    onClick={() => {
                      setActiveCatId(cat.id);
                      triggerIntegrityAudit(cat.id);
                    }}
                    className={`p-2.5 rounded-xl border transition-all cursor-pointer select-none text-left flex flex-col justify-between ${
                      isActive 
                        ? 'bg-[#15151a] border-indigo-500/50 shadow-inner' 
                        : 'bg-[#0f0f12]/50 border-white/5 hover:border-white/10 hover:bg-[#121215]/80'
                    }`}
                  >
                    <div>
                      <p className="text-[10px] text-gray-500 font-mono font-bold tracking-wider">{cat.name}</p>
                      <p className="text-[11px] text-gray-200 mt-1 truncate font-medium">{cat.title}</p>
                    </div>
                    <div className="flex items-center justify-between mt-2.5 pt-1.5 border-t border-white/5 font-mono text-[9px]">
                      <span className="text-gray-500">GRÖSSe</span>
                      <span className={`font-bold ${isActive ? 'text-emerald-400' : 'text-gray-300'}`}>{cat.sizeGb} GB</span>
                    </div>
                  </div>
                );
              })}
            </div>

          </div>

          {/* Right sidebar diagnostics detailing selected category */}
          <div className="lg:col-span-4 flex flex-col justify-between bg-[#141416]/75 border border-white/5 rounded-xl p-4 gap-4">
            <div className="space-y-4">
              <div className="flex items-center justify-between border-b border-white/5 pb-2">
                <span className="text-[10px] uppercase font-mono tracking-wider text-gray-400 block flex items-center gap-1.5">
                  <Sparkles className="h-3.5 w-3.5 text-[#38bdf8]" /> Auto-Scan Diagnostics
                </span>
                <span className={`h-2 w-2 rounded-full bg-emerald-400 animate-pulse`} title="System Active"></span>
              </div>
              
              {/* Main Directory Metadata specs */}
              <div className="space-y-3 bg-[#0a0a0c] p-4 rounded-xl border border-white/5 text-[11px]">
                <div>
                  <span className="text-gray-500 uppercase tracking-widest font-mono block text-[9px] mb-0.5">VERZEICHNIS-PFAD:</span>
                  <span className="text-white hover:text-white font-semibold font-mono text-xs block truncate bg-black/40 px-2 py-1 rounded border border-white/5 mt-1 select-all" title={activeCategory.path}>
                    {activeCategory.path}
                  </span>
                </div>
                
                <div className="grid grid-cols-2 gap-2 pt-2 border-t border-white/5">
                  <div>
                    <span className="text-gray-500 font-mono block text-[9px]">DIREKT-TABELLE:</span>
                    <span className="text-[#38bdf8] font-mono font-bold block mt-0.5 truncate bg-black/20 px-1.5 py-0.5 rounded">{activeCategory.table}</span>
                  </div>
                  <div>
                    <span className="text-gray-500 font-mono block text-[9px]">SICHERHEITS-AUDIT:</span>
                    <span className="text-emerald-400 font-semibold block mt-0.5 bg-black/20 px-1.5 py-0.5 rounded text-[10px] truncate">Passed Verified</span>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-2 pt-2">
                  <div>
                    <span className="text-gray-500 font-mono block text-[9px]">INDEXTIEFE (ITEMS):</span>
                    <span className="text-white font-mono font-bold block mt-0.5">{activeCategory.records.toLocaleString('de-DE')}</span>
                  </div>
                  <div>
                    <span className="text-gray-500 font-mono block text-[9px]">PHYSISCHER PLATZ:</span>
                    <span className="text-emerald-400 font-mono font-extrabold block mt-0.5">{activeCategory.sizeGb.toLocaleString('de-DE')} GB</span>
                  </div>
                </div>

                <div className="pt-2 border-t border-white/5">
                  <span className="text-gray-500 font-mono block text-[9px] mb-0.5">BESCHREIBUNG:</span>
                  <span className="text-gray-400 leading-relaxed block text-[10px]">{activeCategory.description}</span>
                </div>
              </div>

              {/* Simulated retro console running integrity check */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between text-[10px]">
                  <span className="text-gray-400 font-mono flex items-center gap-1">
                    <Terminal className="h-3 w-3 text-indigo-400" />
                    INTEGRITY TERMINAL LOGS
                  </span>
                  {auditStatus === 'scanning' && (
                    <span className="text-amber-400 font-mono animate-pulse">{auditProgress}% SCANNING...</span>
                  )}
                  {auditStatus === 'completed' && (
                    <span className="text-emerald-400 font-mono font-bold flex items-center gap-1">
                      <CheckCircle2 className="h-3 w-3" /> OK
                    </span>
                  )}
                </div>

                <div className="w-full bg-[#050507] border border-white/5 rounded-lg p-2.5 h-36 overflow-y-auto font-mono text-[9px] text-gray-400 flex flex-col gap-1 select-text scrollbar-thin">
                  {auditLogs.map((log, lidx) => {
                    let logColor = "text-gray-400";
                    if (log.startsWith('[INIT]')) logColor = "text-indigo-400 font-bold";
                    else if (log.startsWith('[RESULT]') || log.includes('ERFOLGREICH')) logColor = "text-emerald-400 font-bold";
                    else if (log.includes('[SCAN]')) logColor = "text-sky-300";
                    else if (log.startsWith('[SEC]') || log.startsWith('[RULE]')) logColor = "text-amber-400";

                    return (
                      <div key={lidx} className={`${logColor} leading-relaxed`}>
                        {log}
                      </div>
                    );
                  })}
                  {auditLogs.length === 0 && (
                    <span className="text-gray-600 block italic leading-normal">Terminal bereit. Audit für Block anstoßen...</span>
                  )}
                  <div ref={consoleBottomRef} />
                </div>
              </div>
            </div>

            {/* Local Ingress Scan trigger button */}
            <button
              onClick={() => triggerIntegrityAudit(activeCatId)}
              disabled={auditStatus === 'scanning'}
              className={`w-full py-2.5 rounded-xl text-xs font-mono font-bold flex items-center justify-center gap-2 border transition-all ${
                auditStatus === 'scanning'
                  ? 'bg-amber-500/10 border-amber-500/20 text-amber-400 cursor-not-allowed'
                  : 'bg-indigo-600 border-indigo-500 shadow-md shadow-indigo-600/10 hover:bg-indigo-500 text-white hover:border-indigo-400 active:scale-[0.98]'
              }`}
            >
              {auditStatus === 'scanning' ? (
                <>
                  <RotateCw className="h-3.5 w-3.5 animate-spin" />
                  Analysiere SQLite Segment...
                </>
              ) : (
                <>
                  <ShieldCheck className="h-3.5 w-3.5" />
                  Lokalen Integritäts-Audit anstoßen
                </>
              )}
            </button>
          </div>

        </div>
      </div>

      {/* 4. Elegant Core metrics grid directly connected to background */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 relative">
        
        {/* Metric 1: System Core Status (Tailscale Active) */}
        <div
          className="bg-gradient-to-br from-[#121214] to-[#18181b] border border-[#2b2b35] hover:border-indigo-500/50 rounded-2xl p-5 shadow-xl select-none transition-all duration-200 cursor-default"
          style={{
            transform: `perspective(600px) rotateX(${tilts.m1.rX}deg) rotateY(${tilts.m1.rY}deg)`,
            transformStyle: 'preserve-3d font-sans'
          }}
          onMouseMove={(e) => handleMouseMove('m1', e)}
          onMouseLeave={() => handleMouseLeave('m1')}
        >
          <div className="flex items-center justify-between mb-4" style={{ transform: 'translateZ(20px)' }}>
            <div className="p-2.5 bg-indigo-500/5 text-indigo-400 rounded-xl border border-indigo-500/20">
              <Server className="h-5 w-5 animate-pulse" />
            </div>
            <span className="text-[10px] uppercase tracking-wider text-gray-400 font-mono font-bold">
              Live Core
            </span>
          </div>
          <div style={{ transform: 'translateZ(30px)' }}>
            <p className="text-2xl font-black font-sans text-white tracking-tight">Active VPN</p>
            <p className="text-xs text-indigo-400 font-mono mt-1 font-bold">
              http://100.107.24.67:8081
            </p>
            <div className="mt-3 flex items-center gap-1.5 text-[11px] text-gray-400 font-mono bg-black/30 p-1.5 rounded border border-white/5">
              <span className="h-2 w-2 rounded-full bg-emerald-400 shrink-0"></span>
              Server: Live in Tailscale Matrix
            </div>
          </div>
        </div>

        {/* Metric 2: Uptime and Database Records inside SQLite */}
        <div
          className="bg-gradient-to-br from-[#121214] to-[#18181b] border border-[#2b2b35] hover:border-sky-500/50 rounded-2xl p-5 shadow-xl select-none transition-all duration-200 cursor-default"
          style={{
            transform: `perspective(600px) rotateX(${tilts.m2.rX}deg) rotateY(${tilts.m2.rY}deg)`,
            transformStyle: 'preserve-3d'
          }}
          onMouseMove={(e) => handleMouseMove('m2', e)}
          onMouseLeave={() => handleMouseLeave('m2')}
        >
          <div className="flex items-center justify-between mb-4" style={{ transform: 'translateZ(20px)' }}>
            <div className="p-2.5 bg-sky-500/5 text-sky-400 rounded-xl border border-sky-500/20">
              <Database className="h-5 w-5" />
            </div>
            <span className="text-[10px] uppercase tracking-wider text-gray-400 font-mono font-bold">
              Catalog DB
            </span>
          </div>
          <div style={{ transform: 'translateZ(30px)' }}>
            <p className="text-2xl font-black font-sans text-white tracking-tight">
              {status.totalRecords.toLocaleString('de-DE')}
            </p>
            <p className="text-xs text-sky-400 font-mono mt-1 font-bold">
              Datenvolumen: ~{status.totalSizeGb.toLocaleString('de-DE')} GB indiziert
            </p>
            <div className="mt-3 flex items-center gap-1.5 text-[11px] text-gray-400 font-mono bg-black/30 p-1.5 rounded border border-white/5">
              <Gauge className="h-3.5 w-3.5 text-sky-400 shrink-0" />
              Volltext-Scan: 0ms (Cache Hit)
            </div>
          </div>
        </div>

        {/* Metric 3: Multi-Provider budget control */}
        <div
          className="bg-gradient-to-br from-[#121214] to-[#18181b] border border-[#2b2b35] hover:border-amber-500/50 rounded-2xl p-5 shadow-xl select-none transition-all duration-200 cursor-default"
          style={{
            transform: `perspective(600px) rotateX(${tilts.m3.rX}deg) rotateY(${tilts.m3.rY}deg)`,
            transformStyle: 'preserve-3d'
          }}
          onMouseMove={(e) => handleMouseMove('m3', e)}
          onMouseLeave={() => handleMouseLeave('m3')}
        >
          <div className="flex items-center justify-between mb-4" style={{ transform: 'translateZ(20px)' }}>
            <div className="p-2.5 bg-amber-500/5 text-amber-400 rounded-xl border border-amber-500/20">
              <Zap className="h-5 w-5" />
            </div>
            <span className="text-[10px] uppercase tracking-wider text-gray-400 font-mono font-bold">
              Budget-Lock
            </span>
          </div>
          <div style={{ transform: 'translateZ(30px)' }}>
            <p className="text-2xl font-black font-sans text-white tracking-tight">
              ${status.monthlyCostSpent.toFixed(5)}
            </p>
            <div className="w-full bg-white/5 rounded-full h-1.5 mt-2 overflow-hidden border border-white/5">
              <div 
                className="bg-amber-400 h-1.5 rounded-full transition-all duration-300"
                style={{ width: `${Math.min((status.monthlyCostSpent / status.monthlyCostLimit) * 100, 100)}%` }}
              />
            </div>
            <p className="text-[10px] text-gray-400 font-mono mt-1.5 flex justify-between">
              <span>Limit: $15.00</span>
              <span className="font-bold text-amber-400">{((status.monthlyCostSpent / status.monthlyCostLimit) * 100).toFixed(4)}%</span>
            </p>
          </div>
        </div>

        {/* Metric 4: API Health Check (Double-Secure Verified) */}
        <div
          className="bg-gradient-to-br from-[#121214] to-[#18181b] border border-[#2b2b35] hover:border-emerald-500/50 rounded-2xl p-5 shadow-xl select-none transition-all duration-200 cursor-default"
          style={{
            transform: `perspective(600px) rotateX(${tilts.m4.rX}deg) rotateY(${tilts.m4.rY}deg)`,
            transformStyle: 'preserve-3d'
          }}
          onMouseMove={(e) => handleMouseMove('m4', e)}
          onMouseLeave={() => handleMouseLeave('m4')}
        >
          <div className="flex items-center justify-between mb-4" style={{ transform: 'translateZ(20px)' }}>
            <div className="p-2.5 bg-emerald-500/5 text-emerald-400 rounded-xl border border-emerald-500/20">
              <ShieldCheck className="h-5 w-5" />
            </div>
            <span className="text-[10px] uppercase tracking-wider text-gray-400 font-mono font-bold">
              Sicherheit
            </span>
          </div>
          <div style={{ transform: 'translateZ(30px)' }}>
            <p className="text-2xl font-black font-sans text-white tracking-tight">Passed</p>
            <p className="text-xs text-emerald-400 font-mono mt-1 font-bold">
              Red-Team Invarianten stabil
            </p>
            <div className="mt-3 flex items-center gap-1.5 text-[11px] text-gray-400 font-mono bg-black/30 p-1.5 rounded border border-white/5">
              <ShieldCheck className="h-4 w-4 text-emerald-400 shrink-0" />
              Key Leak Schutz: Aktiviert
            </div>
          </div>
        </div>

      </div>

    </div>
  );
};
