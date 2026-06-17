const baseEl = document.getElementById("baseUrl");
const tokenEl = document.getElementById("token");
const output = document.getElementById("output");

function defaultBase() {
  return localStorage.getItem("digipad.baseUrl") || window.location.origin;
}

function loadSettings() {
  baseEl.value = defaultBase();
  tokenEl.value = localStorage.getItem("digipad.token") || "";
}

function saveSettings() {
  const base = (baseEl.value || "").trim().replace(/\/+$/, "");
  const token = (tokenEl.value || "").trim();
  localStorage.setItem("digipad.baseUrl", base || window.location.origin);
  localStorage.setItem("digipad.token", token);
  write("Gespeichert.\nAPI: " + (base || window.location.origin) + "\nToken: " + (token ? "gespeichert" : "fehlt"));
}

function write(text) {
  output.textContent = text;
}

function render(data) {
  if (!data || typeof data !== "object") return String(data);

  if (data.ok === false) {
    return "Fehler\n" + (data.error || data.message || JSON.stringify(data, null, 2));
  }

  const root = data.status || data;
  const profile = root.profile || data.profile || {};
  const pet = root.pet || data.pet || {};
  const lines = [];

  lines.push("OK");

  if (profile.display_name) lines.push("Profil: " + profile.display_name);
  if (profile.access_level) lines.push("Zugriff: " + profile.access_level);

  if (data.message) {
    lines.push("");
    lines.push(data.message);
  }

  if (pet.name) {
    lines.push("");
    lines.push("DigiDragon: " + pet.name);
    lines.push("Stufe: " + (pet.stage_label || pet.stage || "?"));
    lines.push("Level: " + (pet.level ?? 0) + " | XP: " + (pet.xp ?? 0));
    lines.push("Pfad: " + (pet.evolution_path || "unknown"));
    lines.push("");
    lines.push("Energie: " + (pet.energy ?? 0) + " | Stimmung: " + (pet.mood ?? 0));
    lines.push("Bindung: " + (pet.bond ?? 0) + " | Kampfbereit: " + (pet.battle_ready ?? 0));
    lines.push("HP: " + (pet.hp ?? 0) + "/" + (pet.max_hp ?? 0));
    lines.push("Battle Rating: " + (pet.battle_rating ?? 0));

    if (Array.isArray(pet.attacks) && pet.attacks.length) {
      lines.push("");
      lines.push("Attacken");
      pet.attacks.slice(0, 8).forEach(a => {
        lines.push("- " + (a.name || a.id || "?") + " [" + (a.element || "?") + "/" + (a.class || "?") + "]");
      });
    }
  }

  if (Array.isArray(data.level_notes) && data.level_notes.length) {
    lines.push("");
    lines.push("Level");
    data.level_notes.forEach(x => lines.push("- " + x));
  }

  if (Array.isArray(data.unlocks) && data.unlocks.length) {
    lines.push("");
    lines.push("Neue Freischaltungen");
    data.unlocks.forEach(x => lines.push("- " + x));
  }

  if (data.code) {
    lines.push("");
    lines.push("Battle Code");
    lines.push(data.code);
  }

  return lines.join("\n") || JSON.stringify(data, null, 2);
}

async function callApi(method, path, body) {
  const base = (baseEl.value || defaultBase()).trim().replace(/\/+$/, "");
  const token = (tokenEl.value || "").trim();

  if (!token) {
    write("Token fehlt.\nToken einmalig von Patricks Host übernehmen.");
    return;
  }

  write("Lade...\n" + base + path);

  const opt = {
    method,
    headers: {
      "X-DigiPad-Token": token
    }
  };

  if (method === "POST") {
    opt.headers["Content-Type"] = "application/json";
    opt.body = body || "{}";
  }

  try {
    const res = await fetch(base + path, opt);
    const txt = await res.text();
    let data;
    try { data = JSON.parse(txt); } catch { data = txt; }
    if (!res.ok) {
      write("HTTP " + res.status + "\n" + (typeof data === "string" ? data : render(data)));
      return;
    }
    write(render(data));
  } catch (err) {
    write("Nicht erreichbar.\n" + err.message + "\n\nPrüfen: API-Adresse, Port 8788, WLAN/Tailscale.");
  }
}

document.getElementById("saveBtn").addEventListener("click", saveSettings);

document.querySelectorAll("[data-get]").forEach(btn => {
  btn.addEventListener("click", () => callApi("GET", btn.dataset.get));
});

document.querySelectorAll("[data-post]").forEach(btn => {
  btn.addEventListener("click", () => callApi("POST", btn.dataset.post, btn.dataset.body || "{}"));
});

loadSettings();
write("Bereit.\nAPI und Token speichern, dann Status drücken.");
