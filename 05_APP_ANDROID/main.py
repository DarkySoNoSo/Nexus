# -*- coding: utf-8 -*-
"""
Nexus Android Daemon Application
Responsive mobile layout for the Buildozer/Kivy APK.
"""

import threading
from datetime import datetime

from kivy.app import App
from kivy.clock import Clock
from kivy.core.window import Window
from kivy.graphics import Color, Line, Rectangle
from kivy.metrics import dp, sp
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.gridlayout import GridLayout
from kivy.uix.label import Label
from kivy.uix.scrollview import ScrollView
from kivy.uix.textinput import TextInput
from kivy.utils import platform

try:
    from sync_manager import OfflineSyncManager
except ImportError:
    from .sync_manager import OfflineSyncManager

# Desktop simulator only. On Android this caused the app to render as a tiny
# 400x700 surface in the lower-left corner of the real phone screen.
if platform not in ("android", "ios"):
    Window.size = (430, 820)

Window.clearcolor = (0, 0, 0, 1)


class ColorBox(BoxLayout):
    def __init__(self, bg_color=(0.02, 0.02, 0.025, 1), border_color=None, border_width=1, **kwargs):
        super().__init__(**kwargs)
        self.bg_color = bg_color
        self.border_color = border_color
        self.border_width = border_width
        self.bind(pos=self._redraw, size=self._redraw)

    def _redraw(self, *args):
        self.canvas.before.clear()
        with self.canvas.before:
            Color(*self.bg_color)
            Rectangle(pos=self.pos, size=self.size)
            if self.border_color:
                Color(*self.border_color)
                Line(rectangle=(self.x, self.y, self.width, self.height), width=self.border_width)


class WrappedLabel(Label):
    def __init__(self, **kwargs):
        kwargs.setdefault("markup", True)
        kwargs.setdefault("halign", "left")
        kwargs.setdefault("valign", "middle")
        super().__init__(**kwargs)
        self.bind(width=self._wrap_width, texture_size=self._fit_height)

    def _wrap_width(self, *_):
        self.text_size = (max(self.width, 1), None)

    def _fit_height(self, *_):
        if self.size_hint_y is None:
            self.height = max(dp(22), self.texture_size[1] + dp(4))


class NexusDaemonApp(App):
    def build(self):
        self.title = "NEXUS DAEMON v40.45"
        self.sync_manager = OfflineSyncManager()

        outer = ColorBox(
            bg_color=(0, 0, 0, 1),
            orientation="vertical",
            padding=[dp(12), dp(12), dp(12), dp(12)],
            spacing=dp(10),
            size_hint=(1, 1),
        )

        header = ColorBox(
            bg_color=(0.015, 0.018, 0.028, 1),
            border_color=(0.0, 0.55, 1.0, 0.85),
            border_width=1.2,
            orientation="vertical",
            size_hint_y=None,
            height=dp(78),
            padding=[dp(10), dp(7), dp(10), dp(7)],
            spacing=dp(3),
        )
        header.add_widget(WrappedLabel(
            text="[b]NEXUS DAEMON v40.45[/b]",
            font_size=sp(21),
            color=(0.0, 0.78, 1.0, 1),
            size_hint_y=None,
            height=dp(34),
        ))
        self.status_label = WrappedLabel(
            text="Lade System-Verbindung...",
            font_size=sp(12),
            color=(1.0, 0.58, 0.2, 1),
            size_hint_y=None,
            height=dp(30),
        )
        header.add_widget(self.status_label)
        outer.add_widget(header)

        scroller = ScrollView(size_hint=(1, 1), do_scroll_x=False)
        content = BoxLayout(
            orientation="vertical",
            size_hint_y=None,
            spacing=dp(10),
            padding=[0, 0, 0, dp(8)],
        )
        content.bind(minimum_height=content.setter("height"))
        scroller.add_widget(content)
        outer.add_widget(scroller)

        content.add_widget(self._build_config_box())
        content.add_widget(self._build_ingest_box())
        content.add_widget(self._build_sync_box())
        content.add_widget(self._build_console_box())

        self.add_log("OfflineSyncManager gestartet. Warte auf Netzwerk-Trigger...")
        self.update_info_displays()
        Clock.schedule_interval(self.update_gui_heartbeat, 3.0)
        return outer

    def _build_config_box(self):
        box = ColorBox(
            bg_color=(0.010, 0.012, 0.018, 1),
            border_color=(0.10, 0.24, 0.42, 1),
            orientation="vertical",
            size_hint_y=None,
            height=dp(86),
            padding=dp(8),
            spacing=dp(6),
        )
        box.add_widget(WrappedLabel(
            text="[b]MASTER-URL[/b]",
            font_size=sp(12),
            color=(0.58, 0.70, 0.82, 1),
            size_hint_y=None,
            height=dp(20),
        ))
        self.ip_input = TextInput(
            text=self.sync_manager.server_url,
            multiline=False,
            font_size=sp(13),
            background_color=(0.035, 0.040, 0.055, 1),
            foreground_color=(0.0, 0.86, 1.0, 1),
            cursor_color=(0.0, 0.86, 1.0, 1),
            padding=[dp(8), dp(8), dp(8), dp(8)],
            size_hint_y=None,
            height=dp(42),
        )
        self.ip_input.bind(text=self.update_server_url)
        box.add_widget(self.ip_input)
        return box

    def _build_ingest_box(self):
        box = ColorBox(
            bg_color=(0.012, 0.016, 0.018, 1),
            border_color=(0.0, 0.75, 0.40, 0.75),
            orientation="vertical",
            size_hint_y=None,
            height=dp(266),
            padding=dp(10),
            spacing=dp(7),
        )
        box.add_widget(WrappedLabel(
            text="[b]MOBILES COCKPIT - SCHNELLBELEG[/b]",
            font_size=sp(12),
            color=(0.05, 0.90, 0.45, 1),
            size_hint_y=None,
            height=dp(22),
        ))

        fields = GridLayout(cols=1, spacing=dp(6), size_hint_y=None, height=dp(178))
        self.cat_input = self._field("Finanzen / Belege")
        self.title_input = self._field("Stromrechnung ewz Zuerich")
        self.body_input = self._field("Belegfoto ewz Strom 124.50 CHF eingereicht.")
        fields.add_widget(self._label("Kategorie"))
        fields.add_widget(self.cat_input)
        fields.add_widget(self._label("Titel / Quelle"))
        fields.add_widget(self.title_input)
        fields.add_widget(self._label("Beschreibung / Wert"))
        fields.add_widget(self.body_input)
        box.add_widget(fields)

        submit_btn = self._button("IN DIE OFFLINE-QUEUE SPEICHERN", (0.0, 0.62, 0.32, 1))
        submit_btn.bind(on_press=self.submit_quick_event)
        box.add_widget(submit_btn)
        return box

    def _build_sync_box(self):
        box = ColorBox(
            bg_color=(0.015, 0.018, 0.028, 1),
            border_color=(0.0, 0.52, 0.95, 0.85),
            orientation="horizontal",
            size_hint_y=None,
            height=dp(58),
            padding=dp(8),
            spacing=dp(8),
        )
        self.queue_info = WrappedLabel(
            text="Queue: 0 Eintraege",
            font_size=sp(13),
            bold=True,
            color=(0.0, 0.82, 1.0, 1),
            size_hint_x=0.44,
            size_hint_y=None,
            height=dp(42),
        )
        box.add_widget(self.queue_info)
        sync_btn = self._button("JETZT ABGLEICHEN", (0.0, 0.38, 0.78, 1))
        sync_btn.size_hint_x = 0.56
        sync_btn.bind(on_press=self.trigger_sync_now)
        box.add_widget(sync_btn)
        return box

    def _build_console_box(self):
        box = ColorBox(
            bg_color=(0.004, 0.005, 0.008, 1),
            border_color=(0.10, 0.16, 0.28, 1),
            border_width=1.3,
            orientation="vertical",
            size_hint_y=None,
            height=dp(240),
            padding=dp(6),
            spacing=dp(4),
        )
        box.add_widget(WrappedLabel(
            text="DIAGNOSTIK TERMINAL",
            font_size=sp(10),
            color=(0.55, 0.65, 0.75, 1),
            size_hint_y=None,
            height=dp(18),
        ))
        self.console_scroll = ScrollView(do_scroll_x=False)
        self.console_text = Label(
            text='=== NEXUS MOBILE ENGINE STARTUP ===\nLese lokale DB "nexus_offline.db" ein...\n',
            font_size=sp(10),
            color=(0.0, 1.0, 0.52, 1),
            size_hint_y=None,
            halign="left",
            valign="top",
        )
        self.console_text.bind(width=self._update_console_width, texture_size=self._update_console_height)
        self.console_scroll.add_widget(self.console_text)
        box.add_widget(self.console_scroll)
        return box

    def _label(self, text):
        return WrappedLabel(
            text=f"[b]{text}[/b]",
            font_size=sp(10),
            color=(0.68, 0.72, 0.78, 1),
            size_hint_y=None,
            height=dp(18),
        )

    def _field(self, text):
        return TextInput(
            text=text,
            multiline=False,
            font_size=sp(12),
            background_color=(0.025, 0.028, 0.038, 1),
            foreground_color=(1, 1, 1, 1),
            cursor_color=(0.0, 0.86, 1.0, 1),
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
            background_color=color,
            color=(1, 1, 1, 1),
            size_hint_y=None,
            height=dp(40),
        )

    def _update_console_width(self, instance, width):
        instance.text_size = (max(width, 1), None)

    def _update_console_height(self, instance, texture_size):
        instance.height = max(texture_size[1], self.console_scroll.height)

    def add_log(self, text):
        now_str = datetime.now().strftime("%H:%M:%S")
        self.console_text.text += f"[{now_str}] {text}\n"
        self.console_scroll.scroll_y = 0

    def update_server_url(self, instance, value):
        self.sync_manager.server_url = value.strip()

    def update_gui_heartbeat(self, dt):
        threading.Thread(target=self._async_heartbeat_task, daemon=True).start()

    def _async_heartbeat_task(self):
        try:
            is_connected = self.sync_manager.check_connection()
            pending = self.sync_manager.get_pending_count()
            Clock.schedule_once(lambda dt: self._apply_heartbeat_ui(is_connected, pending))
        except Exception as exc:
            Clock.schedule_once(lambda dt: self.add_log(f"Fehler im Heartbeat: {exc}"))

    def _apply_heartbeat_ui(self, is_connected, pending):
        self.queue_info.text = f"Queue: {pending} Eintraege"
        if is_connected:
            self.status_label.text = f"[ ONLINE ] Verbunden mit Master: {self.sync_manager.server_url}"
            self.status_label.color = (0.0, 1.0, 0.42, 1)
        else:
            self.status_label.text = f"[ OFFLINE ] Keine Verbindung zu: {self.sync_manager.server_url}"
            self.status_label.color = (1.0, 0.28, 0.28, 1)

    def update_info_displays(self):
        try:
            pending = self.sync_manager.get_pending_count()
            self.queue_info.text = f"Queue: {pending} Eintraege"
        except Exception as exc:
            self.add_log(f"DB Error: {exc}")

    def submit_quick_event(self, instance):
        cat = self.cat_input.text.strip()
        title = self.title_input.text.strip()
        body = self.body_input.text.strip()

        if not title:
            self.add_log("Fehler: Titel darf nicht leer sein.")
            return

        payload = {
            "title": title,
            "body": body,
            "category": cat,
            "source": "Android App Client - Mobile",
        }

        try:
            self.sync_manager.queue_event("MOBILE_INGEST", payload)
            self.add_log(f"Event '{title}' lokal in Queue gespeichert.")
            self.body_input.text = ""
            self.update_info_displays()
        except Exception as exc:
            self.add_log(f"Fehler beim Erfassen: {exc}")

    def trigger_sync_now(self, instance):
        self.add_log("Starte manuellen Synchronisations-Vorgang...")
        threading.Thread(target=self._async_sync_task, daemon=True).start()

    def _async_sync_task(self):
        try:
            count = self.sync_manager.process_queue_once()
            if count > 0:
                msg = f"Erfolgreich {count} Belege an das Hauptsystem uebertragen."
            else:
                msg = "Keine ausstehenden Eintraege oder Server nicht erreichbar."
            Clock.schedule_once(lambda dt: self._on_sync_finished(msg))
        except Exception as exc:
            Clock.schedule_once(lambda dt: self._on_sync_finished(str(exc)))

    def _on_sync_finished(self, msg):
        self.add_log(msg)
        self.update_info_displays()


if __name__ == "__main__":
    NexusDaemonApp().run()
