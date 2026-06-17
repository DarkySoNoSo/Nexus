# -*- coding: utf-8 -*-
"""
Nexus Android mobile client.
Native Kivy control surface for Chef, messages, timeline, files handoff,
collector/offline queue, and connection diagnostics.
"""

import os
import threading
import webbrowser
from urllib.parse import urlencode

import requests
from kivy.app import App
from kivy.clock import Clock
from kivy.core.window import Window
from kivy.graphics import Color, Line, Rectangle
from kivy.metrics import dp, sp
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.scrollview import ScrollView
from kivy.uix.textinput import TextInput
from kivy.utils import platform

try:
    from sync_manager import OfflineSyncManager
except ImportError:
    from .sync_manager import OfflineSyncManager


if platform not in ("android", "ios"):
    Window.size = (430, 820)

Window.clearcolor = (0.0, 0.0, 0.0, 1)


THEMES = {
    "ORANGE": {
        "accent": (1.0, 0.48, 0.06, 1),
        "accent2": (1.0, 0.72, 0.20, 1),
        "panel_border": (0.33, 0.18, 0.10, 1),
        "button": (0.93, 0.43, 0.04, 1),
        "button2": (0.14, 0.09, 0.03, 1),
    },
    "CYBER": {
        "accent": (0.00, 0.75, 1.00, 1),
        "accent2": (0.15, 0.38, 1.00, 1),
        "panel_border": (0.05, 0.22, 0.34, 1),
        "button": (0.00, 0.42, 0.72, 1),
        "button2": (0.02, 0.07, 0.15, 1),
    },
    "GREEN": {
        "accent": (0.00, 0.92, 0.48, 1),
        "accent2": (0.58, 1.00, 0.34, 1),
        "panel_border": (0.06, 0.26, 0.14, 1),
        "button": (0.00, 0.56, 0.28, 1),
        "button2": (0.02, 0.13, 0.06, 1),
    },
}

SERVER_CANDIDATES = [
    "http://100.107.24.67:8081",
    "http://192.168.1.216:8081",
    "http://127.0.0.1:8081",
]


class Panel(BoxLayout):
    def __init__(self, app=None, bg=(0.008, 0.009, 0.010, 1), border=None, accent=False, **kwargs):
        super().__init__(**kwargs)
        self.app = app
        self.bg = bg
        self.border = border
        self.accent = accent
        self.bind(pos=self._draw, size=self._draw)

    def _draw(self, *_):
        theme = self.app.theme if self.app else THEMES["ORANGE"]
        border = self.border or theme["panel_border"]
        self.canvas.before.clear()
        with self.canvas.before:
            Color(0.0, 0.0, 0.0, 0.55)
            Rectangle(pos=(self.x + dp(3), self.y - dp(3)), size=self.size)
            Color(*self.bg)
            Rectangle(pos=self.pos, size=self.size)
            Color(*border)
            Line(rectangle=(self.x, self.y, self.width, self.height), width=1)
            Color(1.0, 1.0, 1.0, 0.035)
            Line(points=[self.x + dp(1), self.top - dp(1), self.right - dp(1), self.top - dp(1)], width=1)
            if self.accent:
                Color(*theme["accent"])
                Rectangle(pos=(self.x, self.y), size=(dp(3), self.height))

    def redraw(self):
        self._draw()
        for child in self.children:
            if hasattr(child, "redraw"):
                child.redraw()


class WrappedLabel(Label):
    def __init__(self, **kwargs):
        kwargs.setdefault("markup", True)
        kwargs.setdefault("halign", "left")
        kwargs.setdefault("valign", "top")
        super().__init__(**kwargs)
        self.bind(width=self._set_width, texture_size=self._fit_height)

    def _set_width(self, *_):
        self.text_size = (max(self.width, 1), None)

    def _fit_height(self, *_):
        if self.size_hint_y is None:
            self.height = max(dp(22), self.texture_size[1] + dp(6))


class NexusMobileApp(App):
    def build(self):
        self.title = "NEXUS MOBILE"
        self.theme_name = "ORANGE"
        self.theme = THEMES[self.theme_name]
        self.current_page = "home"

        db_path = os.path.join(self.user_data_dir, "nexus_offline.db")
        self.sync_manager = OfflineSyncManager(db_path=db_path, server_url=SERVER_CANDIDATES[0])
        self.server_url = self.sync_manager.server_url

        self._hide_android_system_bars()

        self.root_panel = Panel(app=self, bg=(0.0, 0.0, 0.0, 1), border=(0, 0, 0, 0), orientation="vertical",
                                padding=[dp(10), dp(8), dp(10), dp(8)], spacing=dp(7), size_hint=(1, 1))
        self.root_panel.add_widget(self._build_header())

        self.scroll = ScrollView(size_hint=(1, 1), do_scroll_x=False)
        self.content = BoxLayout(orientation="vertical", size_hint_y=None, spacing=dp(8), padding=[0, 0, 0, dp(8)])
        self.content.bind(minimum_height=self.content.setter("height"))
        self.scroll.add_widget(self.content)
        self.root_panel.add_widget(self.scroll)

        self._render_page("home")
        Clock.schedule_once(lambda dt: self.refresh_connection(), 0.2)
        Clock.schedule_interval(lambda dt: self.refresh_connection(silent=True), 20.0)
        return self.root_panel

    def on_start(self):
        self._hide_android_system_bars()

    def on_resume(self):
        self._hide_android_system_bars()
        self.refresh_connection(silent=True)

    def _hide_android_system_bars(self):
        if platform != "android":
            return
        try:
            from jnius import autoclass
            PythonActivity = autoclass("org.kivy.android.PythonActivity")
            View = autoclass("android.view.View")
            WindowManager = autoclass("android.view.WindowManager")
            activity = PythonActivity.mActivity
            window = activity.getWindow()
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
            decor = window.getDecorView()
            flags = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
            decor.setSystemUiVisibility(flags)
        except Exception:
            pass

    def _build_header(self):
        panel = Panel(app=self, bg=(0.010, 0.011, 0.012, 1), accent=True, orientation="vertical",
                      size_hint_y=None, height=dp(76), padding=[dp(12), dp(8), dp(12), dp(6)], spacing=dp(1))
        self.title_label = WrappedLabel(text="[b]NEXUS MOBILE[/b]", font_size=sp(23),
                                        color=(0.97, 0.98, 1.0, 1), size_hint_y=None, height=dp(34))
        self.status_label = WrappedLabel(text="Verbindung wird geprueft...", font_size=sp(11),
                                         color=self.theme["accent2"], size_hint_y=None, height=dp(24))
        panel.add_widget(self.title_label)
        panel.add_widget(self.status_label)
        return panel

    def _panel(self, title=None, height=None, accent=False):
        panel = Panel(app=self, bg=(0.009, 0.010, 0.011, 1), accent=accent, orientation="vertical",
                      size_hint_y=None, padding=dp(9), spacing=dp(7))
        if height:
            panel.height = height
        if title:
            panel.add_widget(self._section_label(title))
        return panel

    def _section_label(self, text):
        return WrappedLabel(text=f"[b]{text}[/b]", font_size=sp(12), color=self.theme["accent2"],
                            size_hint_y=None, height=dp(24))

    def _text(self, text, size=12, color=(0.92, 0.94, 0.96, 1), bold=False):
        return WrappedLabel(text=("[b]" + text + "[/b]") if bold else text, font_size=sp(size),
                            color=color, size_hint_y=None)

    def _input(self, hint, text="", multiline=False, min_h=38):
        return TextInput(text=text, hint_text=hint, hint_text_color=(0.50, 0.53, 0.58, 1),
                         multiline=multiline, font_size=sp(12), background_color=(0.012, 0.014, 0.016, 1),
                         foreground_color=(0.96, 0.97, 0.98, 1), cursor_color=self.theme["accent"],
                         padding=[dp(8), dp(8), dp(8), dp(8)], size_hint_y=None, height=dp(min_h))

    def _button(self, text, action=None, small=False, fill=True):
        btn = Button(text=text, font_size=sp(10 if small else 11), bold=True, background_normal="",
                     background_down="", background_color=self.theme["button"] if fill else self.theme["button2"],
                     color=(0.02, 0.02, 0.02, 1) if fill else (0.96, 0.96, 0.96, 1),
                     size_hint_y=None, height=dp(32 if small else 36))
        if action:
            btn.bind(on_press=lambda *_: action())
        return btn

    def _button_row(self, items, small=True):
        row = BoxLayout(orientation="horizontal", size_hint_y=None, height=dp(32 if small else 36), spacing=dp(7))
        for text, fn, fill in items:
            row.add_widget(self._button(text, fn, small=small, fill=fill))
        return row

    def _clear_content(self):
        self.content.clear_widgets()

    def _render_page(self, page):
        self.current_page = page
        self._clear_content()
        self.content.add_widget(self._menu_panel())
        if page == "home":
            self.content.add_widget(self._access_panel())
            self.content.add_widget(self._chef_panel(compact=True))
            self.content.add_widget(self._messages_panel(limit=3))
            self.content.add_widget(self._status_panel())
            self.load_messages()
            self.load_chef_log()
        elif page == "chef":
            self.content.add_widget(self._back_title("CHEF"))
            self.content.add_widget(self._chef_panel(compact=False))
            self.load_chef_log()
        elif page == "messages":
            self.content.add_widget(self._back_title("NACHRICHTEN"))
            self.content.add_widget(self._messages_panel(limit=12))
            self.load_messages()
        elif page == "timeline":
            self.content.add_widget(self._back_title("ZEITSTRAHL"))
            self.content.add_widget(self._timeline_panel())
            self.load_timeline()
        elif page == "files":
            self.content.add_widget(self._back_title("DATEIEN"))
            self.content.add_widget(self._files_panel())
        elif page == "collector":
            self.content.add_widget(self._back_title("COLLECTOR"))
            self.content.add_widget(self._access_panel())
            self.content.add_widget(self._collector_panel())
            self.content.add_widget(self._status_panel())
        elif page == "web":
            self.content.add_widget(self._back_title("WEB"))
            self.content.add_widget(self._web_panel())
        elif page == "status":
            self.content.add_widget(self._back_title("STATUS"))
            self.content.add_widget(self._status_panel())
        self.scroll.scroll_y = 1
        self._redraw_all()

    def _back_title(self, title):
        panel = Panel(app=self, bg=(0.006, 0.007, 0.008, 1), orientation="horizontal",
                      size_hint_y=None, height=dp(46), padding=dp(7), spacing=dp(8))
        back = self._button("<", lambda: self._render_page("home"), small=True, fill=False)
        back.size_hint_x = 0.18
        panel.add_widget(back)
        panel.add_widget(self._text(title, size=18, bold=True))
        return panel

    def _menu_panel(self):
        panel = self._panel("MENUE", height=dp(190))
        rows = [
            [("Home", lambda: self._render_page("home"), False), ("Chef", lambda: self._render_page("chef"), True)],
            [("Nachrichten", lambda: self._render_page("messages"), True), ("Dateien", lambda: self._render_page("files"), False)],
            [("Zeitstrahl", lambda: self._render_page("timeline"), False), ("Collector", lambda: self._render_page("collector"), False)],
            [("Web", lambda: self._render_page("web"), False), ("Farbe", self.cycle_theme, False)],
        ]
        for row in rows:
            panel.add_widget(self._button_row(row, small=True))
        return panel

    def _access_panel(self):
        panel = self._panel("ZUGAENGE", height=dp(164), accent=True)
        self.server_input = self._input("Nexus Server", self.server_url, multiline=False)
        self.server_input.bind(text=lambda inst, val: self.set_server_url(val))
        panel.add_widget(self.server_input)
        panel.add_widget(self._button_row([
            ("Verbindung testen", lambda: self.refresh_connection(), True),
            ("Tailscale", lambda: self.set_server_url(SERVER_CANDIDATES[0], True), False),
        ]))
        panel.add_widget(self._button_row([
            ("LAN", lambda: self.set_server_url(SERVER_CANDIDATES[1], True), False),
            ("Status", lambda: self._render_page("status"), False),
        ]))
        return panel

    def _chef_panel(self, compact=False):
        panel = self._panel("CHEF-KANAL", height=dp(236 if compact else 420))
        self.chef_input = self._input("Dem Chef Kontext, Frage oder Auftrag schreiben...", multiline=True, min_h=64)
        panel.add_widget(self.chef_input)
        panel.add_widget(self._button_row([
            ("An Chef senden", self.send_chef, True),
            ("Chef laden", self.load_chef_log, False),
        ]))
        self.chef_output = self._text("Chef-Kanal wird geladen...", size=12)
        panel.add_widget(self.chef_output)
        return panel

    def _messages_panel(self, limit=12):
        panel = self._panel("NACHRICHTEN", height=dp(430 if limit <= 3 else 900))
        self.messages_box = BoxLayout(orientation="vertical", size_hint_y=None, spacing=dp(8))
        self.messages_box.bind(minimum_height=self.messages_box.setter("height"))
        panel.add_widget(self.messages_box)
        panel.add_widget(self._button_row([
            ("Neu laden", self.load_messages, False),
            ("Widget neu", lambda: self.post_status("Widget-Daten werden beim naechsten Refresh aktualisiert."), False),
        ]))
        return panel

    def _collector_panel(self):
        panel = self._panel("SCHNELLER EINGANG", height=dp(272), accent=True)
        self.cat_input = self._input("Kategorie, z.B. Kontext / Datei / Notiz")
        self.title_input = self._input("Kurz benennen")
        self.body_input = self._input("Was soll Nexus speichern oder spaeter abgleichen?", multiline=True, min_h=64)
        panel.add_widget(self.cat_input)
        panel.add_widget(self.title_input)
        panel.add_widget(self.body_input)
        panel.add_widget(self._button("Lokal speichern", self.submit_quick_event))
        panel.add_widget(self._button("Jetzt abgleichen", self.trigger_sync_now, fill=False))
        return panel

    def _timeline_panel(self):
        panel = self._panel("ZEITSTRAHL", height=dp(720))
        self.timeline_box = BoxLayout(orientation="vertical", size_hint_y=None, spacing=dp(6))
        self.timeline_box.bind(minimum_height=self.timeline_box.setter("height"))
        panel.add_widget(self.timeline_box)
        panel.add_widget(self._button("Zeitstrahl laden", self.load_timeline, fill=False))
        return panel

    def _files_panel(self):
        panel = self._panel("DATEIEN", height=dp(240))
        panel.add_widget(self._text("Native Datei-API braucht Browser-Session. Deshalb hier kein Roh-JSON.", size=12))
        panel.add_widget(self._text("Nutze Web oeffnen fuer Dateimanager, Vorschau und Unterordner.", size=12))
        panel.add_widget(self._button("Dateimanager im Browser oeffnen", lambda: self.open_url("/files")))
        return panel

    def _web_panel(self):
        panel = self._panel("WEB", height=dp(220))
        panel.add_widget(self._text(f"Server: {self.server_url}", size=12, bold=True))
        panel.add_widget(self._button("Cockpit oeffnen", lambda: self.open_url("/")))
        panel.add_widget(self._button("Dateien oeffnen", lambda: self.open_url("/files"), fill=False))
        panel.add_widget(self._button("Chef oeffnen", lambda: self.open_url("/chef"), fill=False))
        return panel

    def _status_panel(self):
        panel = self._panel("STATUS", height=dp(170))
        self.queue_info = self._text("Queue: ...", size=12, bold=True)
        self.system_status = self._text("Status wird geladen...", size=11)
        panel.add_widget(self.queue_info)
        panel.add_widget(self.system_status)
        panel.add_widget(self._button_row([
            ("Sync", self.trigger_sync_now, True),
            ("Farbe", self.cycle_theme, False),
        ]))
        self.update_info_displays()
        return panel

    def set_server_url(self, value, update_input=False):
        clean = self.normalize_base(value)
        self._remember_server_url(clean)
        if update_input and hasattr(self, "server_input"):
            self.server_input.text = clean
        self.status_label.text = f"Server: {clean}"

    def _remember_server_url(self, value):
        clean = self.normalize_base(value)
        self.server_url = clean
        self.sync_manager.server_url = clean
        return clean

    def normalize_base(self, value):
        clean = (value or "").strip()
        if not clean:
            clean = SERVER_CANDIDATES[0]
        if not clean.startswith("http://") and not clean.startswith("https://"):
            clean = "http://" + clean
        clean = clean.rstrip("/")
        if "/api/" in clean:
            clean = clean.split("/api/", 1)[0]
        return clean

    def candidates(self):
        result = []
        for raw in [self.server_url] + SERVER_CANDIDATES:
            clean = self.normalize_base(raw)
            if clean not in result:
                result.append(clean)
        return result

    def headers(self):
        return {"X-Nexus-Collector": "nexus-collector-app-v3"}

    def get_json(self, path, timeout=8):
        last = ""
        for base in self.candidates():
            try:
                r = requests.get(base + path, headers=self.headers(), timeout=timeout)
                if r.status_code == 200:
                    self._remember_server_url(base)
                    return r.json()
                last = f"{base}: HTTP {r.status_code}"
            except Exception as exc:
                last = f"{base}: {exc.__class__.__name__}"
        raise RuntimeError(last or "Nexus nicht erreichbar")

    def post_form(self, path, data, timeout=10):
        last = ""
        for base in self.candidates():
            try:
                headers = self.headers()
                headers["Content-Type"] = "application/x-www-form-urlencoded; charset=utf-8"
                r = requests.post(base + path, data=urlencode(data).encode("utf-8"), headers=headers, timeout=timeout)
                if r.status_code == 200:
                    self._remember_server_url(base)
                    try:
                        return r.json()
                    except Exception:
                        return {"ok": True, "text": r.text}
                last = f"{base}: HTTP {r.status_code} {r.text[:120]}"
            except Exception as exc:
                last = f"{base}: {exc.__class__.__name__}"
        raise RuntimeError(last or "Nexus nicht erreichbar")

    def refresh_connection(self, silent=False):
        def work():
            ok = False
            last = ""
            for base in self.candidates():
                try:
                    r = requests.get(base + "/api/widget/messages?limit=1", headers=self.headers(), timeout=5)
                    if r.status_code == 200:
                        ok = True
                        self._remember_server_url(base)
                        break
                    last = f"HTTP {r.status_code}"
                except Exception as exc:
                    last = exc.__class__.__name__
            Clock.schedule_once(lambda dt: self._apply_connection(ok, last, silent))
        threading.Thread(target=work, daemon=True).start()

    def _apply_connection(self, ok, last, silent):
        pending = self.safe_pending_count()
        if hasattr(self, "queue_info"):
            self.queue_info.text = f"Queue: {pending}"
        if ok:
            self.status_label.text = f"ONLINE | {self.server_url}"
            self.status_label.color = self.theme["accent"]
            if hasattr(self, "system_status"):
                self.system_status.text = f"Verbunden: {self.server_url}"
        else:
            self.status_label.text = f"OFFLINE | {self.server_url}"
            self.status_label.color = (1.0, 0.28, 0.24, 1)
            if hasattr(self, "system_status"):
                self.system_status.text = f"Nexus nicht erreichbar: {last}"
            if not silent:
                self.post_status(f"Keine Verbindung: {last}")

    def load_messages(self):
        def work():
            try:
                data = self.get_json("/api/widget/messages?limit=12", timeout=10)
                Clock.schedule_once(lambda dt: self.render_messages(data))
            except Exception as exc:
                Clock.schedule_once(lambda dt: self.render_message_error(str(exc)))
        threading.Thread(target=work, daemon=True).start()

    def render_message_error(self, msg):
        if hasattr(self, "messages_box"):
            self.messages_box.clear_widgets()
            self.messages_box.add_widget(self._text(f"Nachrichten nicht erreichbar: {msg}", size=12))

    def render_messages(self, data):
        if not hasattr(self, "messages_box"):
            return
        self.messages_box.clear_widgets()
        counters = data.get("counters", {}) if isinstance(data, dict) else {}
        header = f"Fokus: {counters.get('focus', 0)} | Alarm: {counters.get('alerts', 0)} | Antwort: {counters.get('needs_reply', 0)}"
        self.messages_box.add_widget(self._text(header, size=12, bold=True))
        items = (data or {}).get("items", [])[:12]
        if not items:
            self.messages_box.add_widget(self._text("Keine offenen Nachrichten.", size=12))
            return
        for idx, item in enumerate(items, 1):
            self.messages_box.add_widget(self._message_card(idx, item))

    def _message_card(self, idx, item):
        card = Panel(app=self, bg=(0.004, 0.005, 0.006, 1), orientation="vertical",
                     size_hint_y=None, padding=dp(8), spacing=dp(6))
        sender = item.get("sender") or item.get("sender_raw") or item.get("title") or "Unbekannt"
        band = item.get("priority_band") or "P?"
        count = int(item.get("conversation_count") or 1)
        preview = item.get("body_preview") or item.get("body") or item.get("summary") or ""
        preview = self.clean_text(preview, 240)
        action = item.get("suggested_action") or "pruefen"
        event_id = item.get("event_id") or ""
        card.add_widget(self._text(f"{idx}. [{band}] {sender}", size=13, bold=True))
        card.add_widget(self._text(f"{count} Nachrichten | {action}", size=10, color=self.theme["accent2"], bold=True))
        card.add_widget(self._text(preview, size=12))
        card.add_widget(self._button_row([
            ("Sehr wichtig", lambda eid=event_id: self.submit_decision(eid, "very_important"), True),
            ("Erledigt", lambda eid=event_id: self.submit_decision(eid, "done"), False),
        ]))
        card.add_widget(self._button_row([
            ("Zeitstrahl", lambda eid=event_id: self.submit_decision(eid, "timeline_focus"), False),
            ("Nicht wichtig", lambda eid=event_id: self.submit_decision(eid, "not_important"), False),
        ]))
        card.add_widget(self._button_row([
            ("Anheften", lambda eid=event_id: self.submit_decision(eid, "pin"), False),
            ("Chef", lambda it=item: self.prefill_chef_context(it), False),
        ]))
        card.bind(minimum_height=card.setter("height"))
        card.height = dp(230)
        return card

    def submit_decision(self, event_id, action):
        if not event_id:
            self.post_status("Keine Event-ID.")
            return
        def work():
            try:
                result = self.post_form("/api/widget/message-action", {
                    "event_id": event_id,
                    "action": action,
                    "scope": "conversation",
                })
                Clock.schedule_once(lambda dt: self._decision_done(result, action))
            except Exception as exc:
                Clock.schedule_once(lambda dt: self.post_status(f"Aktion fehlgeschlagen: {exc}"))
        threading.Thread(target=work, daemon=True).start()

    def _decision_done(self, result, action):
        self.post_status(f"Aktion gespeichert: {action}")
        self.load_messages()

    def prefill_chef_context(self, item):
        sender = item.get("sender") or item.get("title") or "Nachricht"
        preview = item.get("body_preview") or item.get("body") or ""
        self._render_page("chef")
        Clock.schedule_once(lambda dt: self._set_chef_text(sender, preview), 0.1)

    def _set_chef_text(self, sender, preview):
        if hasattr(self, "chef_input"):
            self.chef_input.text = f"Kontext zu dieser Nachricht ({sender}): {self.clean_text(preview, 300)}\n\nMeine Einordnung: "

    def load_chef_log(self):
        def work():
            try:
                data = self.get_json("/api/communication/chef-log", timeout=10)
                Clock.schedule_once(lambda dt: self.render_chef(data))
            except Exception as exc:
                Clock.schedule_once(lambda dt: self.render_chef_error(str(exc)))
        threading.Thread(target=work, daemon=True).start()

    def render_chef(self, data):
        if not hasattr(self, "chef_output"):
            return
        items = (data or {}).get("items", [])[-8:]
        lines = []
        for item in items:
            role = (item.get("role") or "?").upper()
            text = self.clean_text(item.get("text") or "", 420)
            if text:
                lines.append(f"{role}: {text}")
        self.chef_output.text = "\n\n".join(lines) if lines else "Chef-Kanal leer."

    def render_chef_error(self, msg):
        if hasattr(self, "chef_output"):
            self.chef_output.text = f"Chef nicht erreichbar: {msg}"

    def send_chef(self):
        prompt = self.chef_input.text.strip() if hasattr(self, "chef_input") else ""
        if not prompt:
            self.render_chef_error("Schreib zuerst Kontext, Frage oder Auftrag.")
            return
        def work():
            try:
                result = self.post_form("/api/mobile/chef-chat", {"prompt": prompt}, timeout=18)
                Clock.schedule_once(lambda dt: self._chef_sent(result))
            except Exception as exc:
                Clock.schedule_once(lambda dt: self.render_chef_error(str(exc)))
        threading.Thread(target=work, daemon=True).start()

    def _chef_sent(self, result):
        if hasattr(self, "chef_input"):
            self.chef_input.text = ""
        msg = result.get("message") or result.get("answer") or "Chef-Auftrag gesendet."
        if hasattr(self, "chef_output"):
            self.chef_output.text = self.clean_text(msg, 800)
        self.load_chef_log()

    def load_timeline(self):
        def work():
            try:
                data = self.get_json("/api/timeline?limit=40", timeout=8)
                Clock.schedule_once(lambda dt: self.render_timeline(data))
            except Exception as exc:
                Clock.schedule_once(lambda dt: self.render_timeline_error(str(exc)))
        threading.Thread(target=work, daemon=True).start()

    def render_timeline(self, data):
        if not hasattr(self, "timeline_box"):
            return
        self.timeline_box.clear_widgets()
        items = data if isinstance(data, list) else data.get("items", [])
        for item in items[:40]:
            title = item.get("title") or item.get("kind") or "Event"
            ts = item.get("ts") or item.get("timestamp") or ""
            summary = item.get("summary") or item.get("text") or ""
            self.timeline_box.add_widget(self._text(f"[b]{self.clean_text(title, 70)}[/b]\n{ts}\n{self.clean_text(summary, 180)}", size=11))

    def render_timeline_error(self, msg):
        if hasattr(self, "timeline_box"):
            self.timeline_box.clear_widgets()
            self.timeline_box.add_widget(self._text(f"Zeitstrahl nicht erreichbar: {msg}", size=12))

    def submit_quick_event(self):
        category = self.cat_input.text.strip() or "Allgemein / Eingang"
        title = self.title_input.text.strip()
        body = self.body_input.text.strip()
        if not title:
            self.post_status("Nicht gespeichert: Kurzname fehlt.")
            return
        payload = {"title": title, "body": body, "category": category, "source": "Nexus Android Mobile"}
        try:
            self.sync_manager.queue_event("MOBILE_INGEST", payload)
            self.title_input.text = ""
            self.body_input.text = ""
            self.post_status(f"Lokal gespeichert: {title}")
            self.update_info_displays()
        except Exception as exc:
            self.post_status(f"Speichern fehlgeschlagen: {exc}")

    def trigger_sync_now(self):
        self.post_status("Abgleich gestartet...")
        def work():
            try:
                count = self.sync_manager.process_queue_once()
                Clock.schedule_once(lambda dt: self.post_status(f"Abgleich fertig: {count} gesendet."))
            except Exception as exc:
                Clock.schedule_once(lambda dt: self.post_status(f"Sync-Fehler: {exc}"))
            Clock.schedule_once(lambda dt: self.update_info_displays())
        threading.Thread(target=work, daemon=True).start()

    def update_info_displays(self):
        pending = self.safe_pending_count()
        if hasattr(self, "queue_info"):
            self.queue_info.text = f"Queue: {pending}"
        if hasattr(self, "system_status"):
            self.system_status.text = f"Server: {self.server_url}\nQueue offen: {pending}\nTheme: {self.theme_name}"

    def safe_pending_count(self):
        try:
            return self.sync_manager.get_pending_count()
        except Exception:
            return 0

    def open_url(self, path):
        webbrowser.open(self.server_url + path)

    def post_status(self, msg):
        if hasattr(self, "system_status"):
            self.system_status.text = msg
        if hasattr(self, "status_label"):
            self.status_label.text = msg

    def cycle_theme(self):
        names = list(THEMES.keys())
        idx = (names.index(self.theme_name) + 1) % len(names)
        self.theme_name = names[idx]
        self.theme = THEMES[self.theme_name]
        self.status_label.color = self.theme["accent"]
        self._render_page(self.current_page)

    def clean_text(self, text, limit=240):
        clean = " ".join(str(text or "").replace("\r", " ").replace("\n", " ").split())
        if len(clean) > limit:
            return clean[: max(0, limit - 3)] + "..."
        return clean

    def _redraw_all(self):
        try:
            self.root_panel.redraw()
        except Exception:
            pass


if __name__ == "__main__":
    NexusMobileApp().run()
