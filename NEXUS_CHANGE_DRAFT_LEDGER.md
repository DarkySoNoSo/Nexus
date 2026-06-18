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
| DRAFT-039 | Phase F Communication | Begin Nexi-native conversation batching and shared App/Widget decision queue. | build-verified | `nexy_bridge_api.py` stores communication ingest, groups conversations, writes `nexy_decisions`; isolated Python smoke test passed. Android `assembleNexusDebug` and `assembleDigipadDebug` passed on 2026-06-18. |
| DRAFT-040 | Phase F Timeline | Render Zeitstrahl as readable Android cards and expose `/api/timeline` as Nexi alias. | build-verified | Python compile passed; HTTP smoke test proved `/api/timeline` and `/api/nexy/timeline` return the same structured items. Android debug build passed on 2026-06-18. |
| DRAFT-041 | Phase F Files | Convert Android Dateien page into a navigable Explorer backed by safe Nexi file-list API. | build-verified | `/api/files/list` lists only below `NEXUS_STORAGE`; HTTP smoke test covered root, subfolder, legacy folder alias, and `../` path escape blocked with HTTP 400. Android debug build passed on 2026-06-18. |
| DRAFT-042 | Phase F Separation | Remove visible legacy Master/Chef wording from the active Android UI and keep Digi Dragon/DigiPad separate from Nexi core. | build-verified | App resource name changed to Nexus Nexi Native; home, messages, web, Nexi channel, status, and Digi Dragon separation screens now present Nexi as the single active brain while legacy method/API names remain only for compatibility. Android debug build passed on 2026-06-18. |

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
