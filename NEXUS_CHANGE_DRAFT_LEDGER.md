# NEXUS CHANGE DRAFT LEDGER

Status: active root mirror
Created: 2026-06-01
Root sync: 2026-06-16
Owner: Patrick / Nexus

## Canonical Source

The live Nexus system currently stores the full operational change ledger at:

`C:\MasterIndex_Storage\_NEXUS_SYSTEM\drafts\NEXUS_CHANGE_DRAFT_LEDGER.md`

Latest verified local snapshot:

- Local file size: `83,914 bytes`
- Local LastWriteTime: `2026-06-10 17:42:42 Europe/Zurich`
- Local SHA256: `BDEC6803452A7365BF55F0D60BA59CF56FFAA766A4B81051F9C88488A9EF837A`
- Local Nexus route: `/drafts`

This root file exists so the GitHub URL stays discoverable and no longer points to the outdated v40.44 seed ledger.

## Rules

- Draft changes stay visible until Patrick explicitly approves or rejects them.
- Protected files are never edited without explicit approval.
- `C:\MasterIndex_Storage\architecture\NEXUS_VISUAL_OVERVIEW.html` is protected.
- A code change is not considered live until the running Nexus process was restarted and verified.
- A feature is not considered done until the route/API/UI was tested with evidence.
- The file manager must be a first-class page, not only a link from the home screen.
- The Index Chef must learn from reviewed decisions and must not silently move uncertain data.
- Protected Core policy is binding: writing from protection class GELB upward requires documentation.
- ROT writes require Vault/Backup, source, hash, classification, before/after check, healthcheck, and rollback note.

## Current Draft Items

| ID | Area | Change | Status | Evidence Needed |
| --- | --- | --- | --- | --- |
| DRAFT-001 | Runtime | Verify that latest `Nexus.PS1` is the live process after restart. | verified-live | `/api/health` returns expected JSON `ok:true` on port 8081 after restart. |
| DRAFT-002 | API | Confirm API routes return JSON errors, not login HTML, for unauthenticated API calls. | open | HTTP status and content-type check on `/api/state`. |
| DRAFT-003 | Dateimanager | Rebuild `/files` as standalone file-management surface. | live-basic-verified | Desktop/mobile visual validation still open; route and folder APIs respond. |
| DRAFT-004 | Index Chef | Convert current catalog into governed chef workflow: scan, understand, review, commit, learn. | live-basic-verified | `/chef`, `/api/chef/status`, and `/api/review/queue` respond; learning-rule creation still open. |
| DRAFT-005 | Context | Make context entry and memory storage obvious and persistent. | open | `/context` works; `/api/context`; `/api/memory`; backup restore test. |
| DRAFT-006 | Workers | Show visible worker status and durable job errors. | open | Job starts, status updates, error visible, no endless timeout loop. |
| DRAFT-007 | Upload | Verify upload on desktop and Android Firefox, including documents and full Download sync route. | open | Test upload file arrives in target folder; failure has clear message. |
| DRAFT-008 | Design | Replace decorative dashboard complexity with operational layout. | live-basic-verified | Mobile visual check still open; live HTML now has menu toggle, module map, collapsed folder drawer. |
| DRAFT-009 | Documentation | Create current-state documentation and function map from actual code, not promises. | partial | Existing docs generated; needs audit alignment after next implementation pass. |
| DRAFT-010 | Communication | Keep Gmail/notification/event ingestion modular and optional. | open | Source-specific collectors write normalized events to DB/timeline. |
| DRAFT-011 | Governance | Maintain assistant error-prevention ledger before future actions. | active | `NEXUS_ASSISTANT_ERROR_PREVENTION_LEDGER.md` exists and must be checked before posts/changes. |
| DRAFT-012 | Documentation | Create deterministic state-reconstruction protocol directive as persistent MasterIndex standard. | implemented-file-only | File exists: 8451 bytes, 254 lines, SHA256 `1C77E5EF7BC5D4213D175A2A569960D0412BBC35B12A0DF303D32CE4181DD4D9`. |
| DRAFT-013 | UI Navigation | Make key Nexus pages discoverable from the first screen: Meine Seite, Dateien, Index-Chef, KI-Core. | live-basic-verified | `Nexus.PS1` parse OK; restart accepted; `/`, `/context`, `/files`, `/chef` return HTTP 200; Browser DOM/viewport confirms `.primary-nav` near top. |
| DRAFT-014 | Communication Assistant | Analyze incoming WhatsApp/SMS/Gmail notification events: category, priority, response need, context match, calendar candidate, questions, separate `/communication` view. | live-basic-verified | `Nexus.PS1` parse OK; restart accepted; `/communication` HTTP 200; deterministic analyzer live. |
| DRAFT-015 | Communication Chef | Add a separate communication-only Chef channel and selectable per-message decision buttons. | live-basic-verified | Decision API and communication Chef log verified. |
| DRAFT-016 | Communication Alerts | Escalate urgent incoming messages into durable alerts and push them to Android via ntfy when configured. | live-basic-verified | Alert persistence and ntfy status verified. |
| DRAFT-017 | Context Chef Chat | Add a separate chat window on `/context` for conversational context entry. | live-basic-verified | `/context` and context Chef APIs verified. |
| DRAFT-018 | Chef Logic | Research and implement Phase 1 of the governed Chef Brain contract. | live-basic-verified | `chef_brain.py` compiled; Chef Brain APIs verified. |
| DRAFT-019 | Context Chef Approval Gate | Convert Kontext-Chef memory writes into proposal -> approve/reject -> learn flow. | live-verified | Approve/reject flow verified. |
| DRAFT-020 | Communication Chef Logic | Upgrade Kommunikations-Chef to deterministic rubric v2. | live-verified | Chef logic test and UI smoke verified. |
| DRAFT-021 | Communication Chef Focus | Add decision-learning profile and `/api/communication/priorities`. | live-verified | Priority API and focus cards verified. |
| DRAFT-022 | Communication Widget | Add incoming-message widget page and Termux widget installer. | live-verified | `/widget/messages` and installer route verified. |
| DRAFT-023 | Cost Control | Add hard OpenAI/KI cost lock through `nexus_ai_disabled.flag`. | live-verified | Paid worker routes return locked status while flag exists. |
| DRAFT-024 | Termux Widget Install | Expose Android messages-widget installer through Nexus. | live-verified | Installer route verified. |
| DRAFT-025 | Android Message Display | Add Termux/Termux:API notification display installer. | live-verified | Installer route verified. |
| DRAFT-026 | Native Android Widget | Upgrade Nexus Collector Android app with real `AppWidgetProvider`. | live-download-verified | APK download route verified; phone placement pending. |
| DRAFT-027 | Draft Visibility | Add first-class `/drafts` page and homepage link. | live-verified | `/drafts` HTTP 200 and root navigation verified. |
| DRAFT-028 | Cost Incident Analysis | Create local forensic report for 2026-06-03 cost incident. | report-created | Report exists under `_NEXUS_SYSTEM\reports`. |
| DRAFT-029 | Cost Brake Failure Explanation | Extend cost incident report with likely reason visible cost brake did not stop invoices. | report-updated | Report section added. |
| DRAFT-030 | Cost Control Simplification | Remove extra cost routes; keep existing lock file. | live-verified | Removed routes return 404; existing lock test passes. |
| DRAFT-031 | Health Endpoint Optimization | Add short in-memory cache to `/api/health`. | live-verified | Warm health checks reduced to sub-120 ms range. |
| DRAFT-032 | Internal Route Modularization | Consolidate small route areas into reusable internal handlers. | live-verified | Route suite and UI smoke verified. |
| DRAFT-033 | Compact Main Buttons | Reduce oversized cockpit buttons and module tiles. | live-basic-verified | Main CSS adjusted and smoke tested. |
| DRAFT-034 | Mobile Visual Depth | Add deeper black/orange visual depth and no-overflow checks. | live-verified | Mobile screenshot and layout measurement verified. |
| DRAFT-035 | Compact Space 3D UI | Switch homepage to darker compact space/3D visual system. | live-verified | Mobile layout and button measurements verified. |
| DRAFT-036 | Nexi Core | Integrate `nexi_security_shield.py` for quarantine staging, UTF-8-sig handling, and local API data masking. | implemented-verified | Smoke test passed; DB quick check OK. |
| DRAFT-037 | Nexi Core | Integrate `nexi_transaction_engine.py` for ACID locks, semantic cache, and circuit breaker. | implemented-verified | Transaction engine smoke test passed; DB quick check OK. |
| DRAFT-038 | Governance | Define Protected Core and require documentation for writes from GELB upward. | implemented-docs | `docs/rules/NEXUS_PROTECTED_CORE_POLICY.md`, `docs/rules/WRITE_DOCUMENTATION_POLICY.md`, and change record exist in repo. |
| DRAFT-039 | Phase F Communication | Begin Nexi-native conversation batching and shared App/Widget decision queue. | build-verified | `nexy_bridge_api.py` stores communication ingest, groups conversations, writes `nexy_decisions`; isolated Python smoke test passed. Android `assembleDebug` passed on 2026-06-18. DigiPad for Fiona is handled as iPhone Web/PWA, not as a standard Android artifact. |
| DRAFT-040 | Phase F Timeline | Render Zeitstrahl as readable Android cards and expose `/api/timeline` as Nexi alias. | build-verified | Python compile passed; HTTP smoke test proved `/api/timeline` and `/api/nexy/timeline` return the same structured items. Android debug build passed on 2026-06-18. |
| DRAFT-041 | Phase F Files | Convert Android Dateien page into a navigable Explorer backed by safe Nexi file-list API. | build-verified | `/api/files/list` lists only below `NEXUS_STORAGE`; HTTP smoke test covered root, subfolder, legacy folder alias, and `../` path escape blocked with HTTP 400. Android debug build passed on 2026-06-18. |
| DRAFT-042 | Phase F Separation | Remove visible legacy Master/Chef wording from the active Android UI and keep Digi Dragon/DigiPad separate from Nexi core. | build-verified | App resource name changed to Nexus Nexi Native; home, messages, web, Nexi channel, status, and Digi Dragon separation screens now present Nexi as the single active brain while legacy method/API names remain only for compatibility. Android debug build passed on 2026-06-18. |
| DRAFT-043 | Dragon Core MVP | Add local Digi Dragon visualization, touch interaction, values, training, arena, free fight, care, evolution, and codex. | build-verified | `docs/dragon/DRAGON_CORE_GDD.md` defines the target model. Android now draws a compact 3D-style evolution wall with Wasser/Erde/Feuer/Luft/Schatten paths, selectable forms, shelf depth, podest shadows, glow, and shaded dragon figures. Android flavors were removed so the native build produces only one APK, `app-debug.apk`; `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-044 | Mobile Navigation Polish | Remove the permanent top navigation button and make Status a real subpage with a clean return action. | build-verified | Shared Android header now shows only title/subtitle. Subpages use one full-width `Zurueck zur Zentrale` action; Status no longer masquerades as home. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-045 | Mobile UX Rework | Reframe Android home as a work cockpit and split Dragon Core into purposeful modes. | build-verified | Home now uses descriptive module tiles for Arbeit, Separat, System, and Ansicht. Dragon now has Zuhause, Entwicklung, Kampf, and System modes so Bridge/Nexi details no longer pollute the main Dragon flow. `docs/architecture/MOBILE_UX_REWORK_NOTES.md` records the follow-up direction. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-046 | Mobile Home Rail | Rework Android home toward a side-drawer inspired layout. | build-verified | Zentrale now uses a left navigation rail for Nexi, Eingang, Zeit, Dateien, Dragon, and System, with compact work cards on the right. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-047 | Mobile Header And Themes | Center the Nexis brand, turn the home rail into a slide control, and add Neon/OLED accents. | build-verified | Header brand now renders as large centered `NEXIS` in the active accent color. Home rail defaults to a narrow slide handle and expands on tap. New theme actions add Neon gruen and OLED schwarz. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-048 | Mobile 3D Brand Title | Render the top title as a block-style 3D logo instead of a flat text label. | build-verified | Android header now draws `NEXIS` with grey extrusion, dark outline, accent gradient fill, and highlight using a custom Canvas TextView. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-049 | Dragon Visual Depth | Improve Dragon Core visuals with element habitats and stronger 3D dragon silhouettes. | build-verified | Dragon canvas now paints habitat scenes per element, stone/ruin depth, elemental environment cues, and stronger egg/juvenile/adult/ancient figures with horns, wings, tails, highlights, shadows, and selected-path glow. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-050 | Dragon Silhouette Correction | Replace the top Dragon figure because the old shape did not read as a dragon. | build-verified | Main Dragon canvas now draws a clear dragon body: curled tail, oval torso, long neck, angular head, horns, membrane wings, legs/claws, eye, jaw line, and flame. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-051 | Dragon Habitat Preparation | Prepare Dragon Core for future real dragon images and improve habitat visualization. | build-verified | Added `docs/dragon/DRAGON_IMAGE_ASSET_PLAN.md`. Canvas habitats now distinguish Kleines Nest, Hoehle, Geheiligtes Lager, Drachenhort, and Schwebende Insel in addition to element effects. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-052 | Water Dragon Assets | Add real image assets for the Wasser dragon line. | build-verified | Added `dragon_wasser_ei/jung/adult/ahn.jpg` drawables and Android bitmap rendering for the Wasser element path. Canvas habitat, glow, and shadow remain active behind the image. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-053 | Earth Dragon Assets | Add real image assets for the Erde dragon line. | build-verified | Added `dragon_erde_ei/jung/adult/ahn.jpg` drawables and Android bitmap rendering for the Erde element path. Existing XP thresholds and habitat rendering are reused. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-054 | Fire Dragon Assets | Add real image assets for the Feuer dragon line. | build-verified | Added `dragon_feuer_ei/jung/adult/ahn.jpg` drawables and Android bitmap rendering for the Feuer element path. Existing XP thresholds and habitat rendering are reused. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-055 | Air Dragon Assets | Add real image assets for the Luft dragon line. | build-verified | Added `dragon_luft_ei/jung/adult/ahn.jpg` drawables and Android bitmap rendering for the Luft element path. Existing XP thresholds and habitat rendering are reused. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-056 | Shadow Dragon Assets | Add real image assets for the Schatten dragon line. | build-verified | Added `dragon_schatten_ei/jung/adult/ahn.jpg` drawables and Android bitmap rendering for the Schatten element path. Existing XP thresholds and habitat rendering are reused. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-057 | Wandering Shadow Dragon | Add a small animated Schatten-Jungdrache across the Android pages. | build-verified | Root UI now uses a FrameLayout with a non-clickable Canvas overlay. The overlay renders `dragon_schatten_jung.jpg` in small format, walking along the bottom and climbing page edges without touching Nexi, server, or data logic. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-058 | Wandering Dragon Polish | Improve the small Schatten-Jungdrache overlay after visual review. | build-verified | White source-image background is converted to transparency at load time, route avoids the NEXIS header, movement cycle is slowed, and animated wing, leg, tail, and horn strokes add visible body motion. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-059 | Water Dragon Environments | Add real Wasser habitat, training, and arena background assets. | build-verified | Added `dragon_wasser_habitat/training/arena.jpg` drawables. Dragon visual now renders the Wasser habitat for home/evolution, the Wasser training place for training, and the Wasser arena for arena/free fight with cover-crop and readability overlay. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-060 | Earth Dragon Environments | Add real Erde habitat, training, and arena background assets. | build-verified | Added `dragon_erde_habitat/training/arena.jpg` drawables. Dragon visual now renders Erde-specific backgrounds for home/evolution, training, and arena/free fight with the same cover-crop readability layer. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-061 | Air Dragon Environments | Add real Luft habitat, training, and arena background assets. | build-verified | Added `dragon_luft_habitat/training/arena.jpg` drawables. Dragon visual now renders Luft-specific backgrounds for home/evolution, training, and arena/free fight with the same cover-crop readability layer. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-062 | Shadow Dragon Environments | Add real Schatten habitat, training, and arena background assets. | build-verified | Added `dragon_schatten_habitat/training/arena.jpg` drawables. Dragon visual now renders Schatten-specific backgrounds for home/evolution, training, and arena/free fight with the same cover-crop readability layer. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-063 | Fire Dragon Environments | Add real Feuer habitat, training, and arena background assets. | build-verified | Added `dragon_feuer_habitat/training/arena.jpg` drawables. Dragon visual now renders Feuer-specific backgrounds for home/evolution, training, and arena/free fight with the same cover-crop readability layer. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-064 | Dragon Playable State Core | Make Digi Dragon's local gameplay loop functional. | build-verified | Added distinct local training actions for strength, endurance, flight/speed, focus, and instinct; care/rest now have separate resource effects; arena/free fight calculate local enemy-vs-dragon power, track wins/losses, update XP, energy, stress, mood, and bond, and report evolution unlocks in the UI. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-065 | Dragon Attack Battle Reports | Make arena and free fights feel like actual combat. | build-verified | Added element-specific attack sets, named arena/free-fight enemies, a local three-round battle report with attack effects, damage, counters/shields, rewards, and shortened last-action display in the Dragon summary. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-066 | Dragon Active Battle State | Add player-controlled combat rounds. | build-verified | Arena/free fight now start a persistent local battle state with enemy, HP, focus, round, reward, and visible enemy intent. The Kampf page exposes the four element attacks as player choices; each round resolves the chosen move against the enemy intent, updates HP/focus, and supports retreat without recording a loss. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-067 | Dragon Visual Polish | Fix Dragon page visual contrast and element accenting. | build-verified | Dragon pages now use the active dragon element as UI accent color for titles, buttons, and borders. Dragon bitmap backgrounds are removed via edge-connected transparency cleanup, including the wandering Schatten dragon, which is smaller and no longer crosses the header/back navigation area. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-068 | Wandering Dragon Autonomous Motion | Replace simple menu-dragon looping with mixed movement. | build-verified | The wandering Schatten dragon now follows a segmented autonomous path with ground walking, climbing, hover pauses, and short flight arcs. Wing beats accelerate in flight, legs tuck during flight, and the route stays below protected header/navigation space. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-069 | Dragon UI Declutter And Large Habitat | Remove redundant text snippets and enlarge Dragon visual space. | build-verified | Removed non-home header subtitles and page-description snippets, removed the Dragon intro filler line, enlarged the non-evolution Dragon visual from 224dp to 430dp, and strengthened edge-connected transparency cleanup for imported dragon images. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-070 | Sidebar Pet And Natural Flight Polish | Make the menu pet transparent, narrower, controllable from the sidebar, and remove unnatural 180-degree turns. | build-verified | Added closed/open sidebar Dragon and Pet controls, moved Digi Dragon into compact quick access, lowered and shrank quick-access buttons, added a Pet visibility toggle, applied aggressive light-pixel transparency only to the menu Shadow Dragon, and replaced 180-degree pet rotations with mirrored travel plus small arc-based tilts. `:app:assembleDebug` passed on 2026-06-18. |
| DRAFT-071 | Archive Restore Build Audit | Restore archived Dragon UI/assets after cleanup and verify the single APK remains functional. | build-verified | Reapplied archived Dragon/Menu work, restored Wasser/Erde/Feuer/Luft/Schatten dragon and environment drawables, restored Dragon design docs, kept only one built APK, fixed Locale-sensitive string handling, protected SMS receiver action handling, and ensured the generated APK contains SMS, notification listener, and widget declarations. `:app:clean :app:assembleDebug :app:lintDebug` passed on 2026-06-18. APK manifest was checked with `aapt`; only `app-debug.apk` exists. |

## Recent Local Ledger Sections Present In Canonical File

The canonical local ledger additionally contains later Nachtrag sections for:

- Android SSOT/widget work
- Chef-first policy state
- Chef unlimited cost tracking
- AI global unlock state
- Index Chef review CSRF fix
- Index Chef app/widget batch
- communication batching and widget fixes
- Total Commander main access repair
- USB security file generation

The complete body remains in the canonical local file listed above. This root mirror intentionally avoids embedding credentials or private local access secrets.
