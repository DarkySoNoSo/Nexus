# -*- coding: utf-8 -*-
"""
Nexus Android mobile client.
Offline-first quick input and synchronization surface for the Nexus master system.
"""

import os
import threading
from datetime import datetime

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


class Panel(BoxLayout):
    def __init__(self, bg=(0.015, 0.015, 0.017, 1), border=(0.16, 0.18, 0.22, 1), accent=None, **kwargs):
        super().__init__(**kwargs)
        self.bg = bg
        self.border = border
        self.accent = accent
        self.bind(pos=self._draw, size=self._draw)

    def _draw(self, *_):
        self.canvas.before.clear()
        with self.canvas.before:
            Color(0.0, 0.0, 0.0, 0.45)
            Rectangle(pos=(self.x + dp(3), self.y - dp(3)), size=self.size)
            Color(*self.bg)
            Rectangle(pos=self.pos, size=self.size)
            Color(*self.border)
            Line(rectangle=(self.x, self.y, self.width, self.height), width=1)
            Color(1.0, 1.0, 1.0, 0.045)
            Line(points=[self.x + dp(1), self.top - dp(1), self.right - dp(1), self.top - dp(1)], width=1)
            if self.accent:
                Color(*self.accent)
                Rectangle(pos=(self.x, self.y), size=(dp(3), self.height))


class WrappedLabel(Label):
    def __init__(self, **kwargs):
        kwargs.setdefault("markup", True)
        kwargs.setdefault("halign", "left")
        kwargs.setdefault("valign", "middle")
        super().__init__(**kwargs)
        self.bind(width=self._set_width, texture_size=self._fit_height)

    def _set_width(self, *_):
        self.text_size = (max(self.width, 1), None)

    def _fit_height(self, *_):
        if self.size_hint_y is None:
            self.height = max(dp(20), self.texture_size[1] + dp(2))


class NexusMobileApp(App):
    def build(self):
        self.title = "NEXUS MOBILE"
        db_path = os.path.join(self.user_data_dir, "nexus_offline.db")
        self.sync_manager = OfflineSyncManager(db_path=db_path)

        root = Panel(
            bg=(0.0, 0.0, 0.0, 1),
            border=(0.0, 0.0, 0.0, 0),
            orientation="vertical",
            padding=[dp(10), dp(10), dp(10), dp(10)],
            spacing=dp(8),
            size_hint=(1, 1),
        )

        root.add_widget(self._build_header())

        scroll = ScrollView(size_hint=(1, 1), do_scroll_x=False)
        content = BoxLayout(orientation="vertical", size_hint_y=None, spacing=dp(8), padding=[0, 0, 0, dp(10)])
        content.bind(minimum_height=content.setter("height"))
        scroll.add_widget(content)
        root.add_widget(scroll)

        content.add_widget(self._build_connection_panel())
        content.add_widget(self._build_input_panel())
        content.add_widget(self._build_queue_panel())
        content.add_widget(self._build_log_panel())

        self.add_log("Mobile Client bereit. Lokales Speichern funktioniert ohne Netzwerk.")
        self.update_info_displays()
        Clock.schedule_interval(self.update_gui_heartbeat, 5.0)
        return root

    def _build_header(self):
        panel = Panel(
            bg=(0.018, 0.020, 0.022, 1),
            border=(0.95, 0.42, 0.08, 0.85),
            accent=(1.0, 0.42, 0.06, 1),
            orientation="vertical",
            size_hint_y=None,
            height=dp(74),
            padding=[dp(12), dp(8), dp(12), dp(8)],
            spacing=dp(2),
        )
        panel.add_widget(WrappedLabel(
            text="[b]NEXUS MOBILE[/b]",
            font_size=sp(22),
            color=(0.96, 0.98, 1.0, 1),
            size_hint_y=None,
            height=dp(32),
        ))
        self.status_label = WrappedLabel(
            text="Verbindung wird geprueft...",
            font_size=sp(11),
            color=(1.0, 0.68, 0.25, 1),
            size_hint_y=None,
            height=dp(24),
        )
        panel.add_widget(self.status_label)
        return panel

    def _build_connection_panel(self):
        panel = Panel(
            bg=(0.012, 0.014, 0.018, 1),
            border=(0.12, 0.17, 0.24, 1),
            orientation="vertical",
            size_hint_y=None,
            height=dp(82),
            padding=dp(8),
            spacing=dp(5),
        )
        panel.add_widget(self._section_label("NEXUS SERVER"))
        self.ip_input = self._input_field("http://100.107.24.67:8081", text=self.sync_manager.server_url)
        self.ip_input.bind(text=self.update_server_url)
        panel.add_widget(self.ip_input)
        return panel

    def _build_input_panel(self):
        panel = Panel(
            bg=(0.014, 0.016, 0.016, 1),
            border=(0.12, 0.32, 0.20, 1),
            accent=(0.0, 0.68, 0.34, 1),
            orientation="vertical",
            size_hint_y=None,
            height=dp(248),
            padding=dp(9),
            spacing=dp(6),
        )
        panel.add_widget(self._section_label("SCHNELLER EINGANG"))
        self.cat_input = self._input_field("Kategorie, z.B. Kontext / Datei / Notiz")
        self.title_input = self._input_field("Kurz benennen")
        self.body_input = self._input_field("Was soll Nexus speichern oder spaeter abgleichen?")
        panel.add_widget(self.cat_input)
        panel.add_widget(self.title_input)
        panel.add_widget(self.body_input)
        btn = self._button("LOKAL SPEICHERN", (0.0, 0.54, 0.30, 1))
        btn.bind(on_press=self.submit_quick_event)
        panel.add_widget(btn)
        return panel

    def _build_queue_panel(self):
        panel = Panel(
            bg=(0.012, 0.014, 0.019, 1),
            border=(0.10, 0.18, 0.29, 1),
            orientation="horizontal",
            size_hint_y=None,
            height=dp(52),
            padding=dp(7),
            spacing=dp(8),
        )
        self.queue_info = WrappedLabel(
            text="Queue: 0",
            font_size=sp(13),
            bold=True,
            color=(0.0, 0.82, 1.0, 1),
            size_hint_x=0.45,
            size_hint_y=None,
            height=dp(36),
        )
        panel.add_widget(self.queue_info)
        sync_btn = self._button("ABGLEICHEN", (0.02, 0.23, 0.52, 1))
        sync_btn.size_hint_x = 0.55
        sync_btn.bind(on_press=self.trigger_sync_now)
        panel.add_widget(sync_btn)
        return panel

    def _build_log_panel(self):
        panel = Panel(
            bg=(0.004, 0.005, 0.007, 1),
            border=(0.10, 0.13, 0.20, 1),
            orientation="vertical",
            size_hint_y=None,
            height=dp(226),
            padding=dp(7),
            spacing=dp(4),
        )
        panel.add_widget(WrappedLabel(
            text="SYSTEM LOG",
            font_size=sp(10),
            color=(0.52, 0.58, 0.66, 1),
            size_hint_y=None,
            height=dp(18),
        ))
        self.console_scroll = ScrollView(do_scroll_x=False)
        self.console_text = Label(
            text='=== NEXUS MOBILE ===\nLokale Queue: nexus_offline.db\n',
            font_size=sp(10),
            color=(0.0, 0.92, 0.48, 1),
            size_hint_y=None,
            halign="left",
            valign="top",
        )
        self.console_text.bind(width=self._update_console_width, texture_size=self._update_console_height)
        self.console_scroll.add_widget(self.console_text)
        panel.add_widget(self.console_scroll)
        return panel

    def _section_label(self, text):
        return WrappedLabel(
            text=f"[b]{text}[/b]",
            font_size=sp(11),
            color=(0.70, 0.76, 0.84, 1),
            size_hint_y=None,
            height=dp(20),
        )

    def _input_field(self, hint, text=""):
        return TextInput(
            text=text,
            hint_text=hint,
            hint_text_color=(0.45, 0.49, 0.55, 1),
            multiline=False,
            font_size=sp(12),
            background_color=(0.020, 0.022, 0.027, 1),
            foreground_color=(0.94, 0.96, 0.98, 1),
            cursor_color=(1.0, 0.55, 0.08, 1),
            padding=[dp(8), dp(7), dp(8), dp(7)],
            size_hint_y=None,
            height=dp(34),
        )

    def _button(self, text, color):
        return Button(
            text=text,
            font_size=sp(11),
            bold=True,
            background_normal="",
            background_down="",
            background_color=color,
            color=(0.98, 0.98, 0.98, 1),
            size_hint_y=None,
            height=dp(36),
        )

    def _update_console_width(self, instance, width):
        instance.text_size = (max(width, 1), None)

    def _update_console_height(self, instance, texture_size):
        instance.height = max(texture_size[1], self.console_scroll.height)

    def add_log(self, text):
        now_str = datetime.now().strftime("%H:%M:%S")
        lines = (self.console_text.text + f"[{now_str}] {text}\n").splitlines()[-12:]
        self.console_text.text = "\n".join(lines) + "\n"
        self.console_scroll.scroll_y = 0

    def update_server_url(self, instance, value):
        value = value.strip()
        if value:
            self.sync_manager.server_url = value.rstrip("/")

    def update_gui_heartbeat(self, dt):
        threading.Thread(target=self._async_heartbeat_task, daemon=True).start()

    def _async_heartbeat_task(self):
        try:
            is_connected = self.sync_manager.check_connection()
            pending = self.sync_manager.get_pending_count()
            Clock.schedule_once(lambda dt: self._apply_heartbeat_ui(is_connected, pending))
        except Exception as exc:
            Clock.schedule_once(lambda dt: self.add_log(f"Heartbeat-Fehler: {exc}"))

    def _apply_heartbeat_ui(self, is_connected, pending):
        self.queue_info.text = f"Queue: {pending}"
        if is_connected:
            self.status_label.text = f"ONLINE | {self.sync_manager.server_url}"
            self.status_label.color = (0.0, 0.95, 0.42, 1)
        else:
            self.status_label.text = f"OFFLINE | {self.sync_manager.server_url}"
            self.status_label.color = (1.0, 0.30, 0.24, 1)

    def update_info_displays(self):
        try:
            pending = self.sync_manager.get_pending_count()
            self.queue_info.text = f"Queue: {pending}"
        except Exception as exc:
            self.add_log(f"DB-Fehler: {exc}")

    def submit_quick_event(self, instance):
        category = self.cat_input.text.strip() or "Allgemein / Eingang"
        title = self.title_input.text.strip()
        body = self.body_input.text.strip()

        if not title:
            self.add_log("Nicht gespeichert: Kurzname fehlt.")
            return

        payload = {
            "title": title,
            "body": body,
            "category": category,
            "source": "Nexus Android Mobile",
        }

        try:
            self.sync_manager.queue_event("MOBILE_INGEST", payload)
            self.add_log(f"Gespeichert: {title}")
            self.title_input.text = ""
            self.body_input.text = ""
            self.update_info_displays()
        except Exception as exc:
            self.add_log(f"Speichern fehlgeschlagen: {exc}")

    def trigger_sync_now(self, instance):
        self.add_log("Abgleich gestartet...")
        threading.Thread(target=self._async_sync_task, daemon=True).start()

    def _async_sync_task(self):
        try:
            count = self.sync_manager.process_queue_once()
            if count > 0:
                msg = f"Abgleich fertig: {count} gesendet."
            else:
                msg = "Nichts gesendet. Queue leer oder Server nicht erreichbar."
            Clock.schedule_once(lambda dt: self._on_sync_finished(msg))
        except Exception as exc:
            Clock.schedule_once(lambda dt: self._on_sync_finished(str(exc)))

    def _on_sync_finished(self, msg):
        self.add_log(msg)
        self.update_info_displays()


if __name__ == "__main__":
    NexusMobileApp().run()
