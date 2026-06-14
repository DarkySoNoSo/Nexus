# -*- coding: utf-8 -*-
"""
Nexus v40.44 - Android Daemon Application
Grafik-Design: Schwarz / Neon-Blau / Neon-Grün - Robust & Kontrastreich
"""

import os
import sys
import threading
from datetime import datetime

# Kivy Framework Imports
from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.gridlayout import GridLayout
from kivy.uix.label import Label
from kivy.uix.textinput import TextInput
from kivy.uix.button import Button
from kivy.uix.scrollview import ScrollView
from kivy.clock import Clock
from kivy.graphics import Color, Rectangle, Line
from kivy.core.window import Window

# Import lokaler Sync-Manager
try:
    from sync_manager import OfflineSyncManager
except ImportError:
    # Fallback zur Sicherheit
    from .sync_manager import OfflineSyncManager

# Default Fenstergröße für den Desktop-Simulator (schönes mobiles Format)
Window.size = (400, 700)

class ColorBox(BoxLayout):
    """Eine Basis-Box-Komponente mit Hintergrundfarbe und neonfarbenen Rahmen."""
    def __init__(self, bg_color, border_color=None, border_width=1, **kwargs):
        super(ColorBox, self).__init__(**kwargs)
        self.bg_color = bg_color
        self.border_color = border_color
        self.border_width = border_width
        self.bind(pos=self.update_canvas, size=self.update_canvas)

    def update_canvas(self, *args):
        self.canvas.before.clear()
        with self.canvas.before:
            # Hintergrundfarbe zeichnen
            Color(*self.bg_color)
            self.rect = Rectangle(pos=self.pos, size=self.size)
            
            # optionaler Neon-Rahmen
            if self.border_color:
                Color(*self.border_color)
                self.line = Line(
                    rectangle=(self.pos[0], self.pos[1], self.size[0], self.size[1]),
                    width=self.border_width
                )

class NexusDaemonApp(App):
    def build(self):
        self.title = 'NEXUS DAEMON v40.44'
        self.sync_manager = OfflineSyncManager()
        
        # Haupt-Layout: Black Canvas
        root_layout = ColorBox(
            bg_color=(0, 0, 0, 1), # Tiefschwarz
            orientation='vertical',
            padding=15,
            spacing=10
        )
        
        # 1. HEADER: Branding & Version
        header = ColorBox(
            bg_color=(0.015, 0.015, 0.025, 1), 
            border_color=(0, 0.5, 0.9, 0.8), # Reines Neon-Blau
            border_width=1.5,
            orientation='vertical',
            size_hint_y=None,
            height=85,
            padding=[10, 5, 10, 5]
        )
        
        banner_title = Label(
            text='[b]NEXUS DAEMON v40.44[/b]',
            markup=True,
            font_size='18sp',
            color=(0, 0.75, 1, 1), # Helles Neon-Blau
            halign='center'
        )
        
        self.status_label = Label(
            text='Lade System-Verbindung...',
            font_name='Roboto',
            font_size='11sp',
            color=(1, 0.6, 0.2, 1) # Gelb-Orange für Warnung
        )
        
        header.add_widget(banner_title)
        header.add_widget(self.status_label)
        root_layout.add_widget(header)

        # 2. SEED CONFIGURATION: IP & Port
        config_box = ColorBox(
            bg_color=(0.01, 0.01, 0.015, 1),
            border_color=(0.1, 0.2, 0.35, 1),
            border_width=1,
            orientation='horizontal',
            size_hint_y=None,
            height=50,
            padding=5,
            spacing=5
        )
        
        config_box.add_widget(Label(
            text='Master-IP:',
            font_size='11sp',
            color=(0.6, 0.7, 0.8, 1),
            size_hint_x=0.25
        ))
        
        self.ip_input = TextInput(
            text=self.sync_manager.server_url,
            multiline=False,
            font_size='12sp',
            background_color=(0.04, 0.04, 0.06, 1),
            foreground_color=(0, 0.85, 1, 1), # Cyan Text
            cursor_color=(0, 0.85, 1, 1),
            padding=[6, 6, 6, 6]
        )
        self.ip_input.bind(text=self.update_server_url)
        config_box.add_widget(self.ip_input)
        root_layout.add_widget(config_box)

        # 3. QUICK-INGEST: Daten erfassen
        ingest_box = ColorBox(
            bg_color=(0.015, 0.015, 0.02, 1),
            border_color=(0, 0.7, 0.4, 0.6), # Neon-Grün
            border_width=1,
            orientation='vertical',
            size_hint_y=None,
            height=190,
            padding=10,
            spacing=8
        )
        
        ingest_title = Label(
            text='[b]MOBILES COCKPIT - SCHNELLBELEG[/b]',
            markup=True,
            font_size='11sp',
            color=(0, 0.8, 0.4, 1), # Grün
            size_hint_y=None,
            height=20,
            halign='left'
        )
        ingest_box.add_widget(ingest_title)
        
        # Eingabe: Kategorie & Betreff
        grid_fields = GridLayout(cols=2, spacing=5, size_hint_y=None, height=110)
        
        grid_fields.add_widget(Label(text='Kategorie:', font_size='11sp', color=(0.7, 0.7, 0.7, 1), size_hint_x=0.3))
        self.cat_input = TextInput(
            text='Finanzen / Belege',
            multiline=False,
            font_size='12sp',
            background_color=(0.02, 0.02, 0.03, 1),
            foreground_color=(1, 1, 1, 1)
        )
        grid_fields.add_widget(self.cat_input)
        
        grid_fields.add_widget(Label(text='Titel/Quelle:', font_size='11sp', color=(0.7, 0.7, 0.7, 1), size_hint_x=0.3))
        self.title_input = TextInput(
            text='Stromrechnung ewz Zürich',
            multiline=False,
            font_size='12sp',
            background_color=(0.02, 0.02, 0.03, 1),
            foreground_color=(1, 1, 1, 1)
        )
        grid_fields.add_widget(self.title_input)
        
        grid_fields.add_widget(Label(text='Beschreibung / Wert:', font_size='11sp', color=(0.7, 0.7, 0.7, 1), size_hint_x=0.3))
        self.body_input = TextInput(
            text='Belegfoto ewz Strom 124.50 CHF eingereicht.',
            multiline=False,
            font_size='11sp',
            background_color=(0.02, 0.02, 0.03, 1),
            foreground_color=(1, 1, 1, 1)
        )
        grid_fields.add_widget(self.body_input)
        
        ingest_box.add_widget(grid_fields)
        
        # Submit Button
        submit_btn = Button(
            text='IN DIE OFFLINE-QUEUE SPEICHERN',
            font_size='11sp',
            font_name='Roboto',
            bold=True,
            background_normal='',
            background_color=(0, 0.6, 0.3, 1), # Reines Kontrast-Grün
            color=(1, 1, 1, 1),
            size_hint_y=None,
            height=32
        )
        submit_btn.bind(on_press=self.submit_quick_event)
        ingest_box.add_widget(submit_btn)
        
        root_layout.add_widget(ingest_box)

        # 4. QUEUE COUNTER & SYNC CONTROLS
        queue_box = ColorBox(
            bg_color=(0.015, 0.015, 0.025, 1),
            border_color=(0, 0.5, 0.9, 0.8), # Blau
            border_width=1,
            orientation='horizontal',
            size_hint_y=None,
            height=55,
            padding=8,
            spacing=8
        )
        
        self.queue_info = Label(
            text='Queue: 0 Einträge',
            font_size='12sp',
            bold=True,
            color=(0, 0.8, 1, 1),
            size_hint_x=0.5
        )
        queue_box.add_widget(self.queue_info)
        
        sync_btn = Button(
            text='JETZT ABGLEICHEN',
            font_size='11sp',
            bold=True,
            background_normal='',
            background_color=(0, 0.4, 0.8, 1), # Blau
            color=(1, 1, 1, 1),
            size_hint_x=0.5
        )
        sync_btn.bind(on_press=self.trigger_sync_now)
        queue_box.add_widget(sync_btn)
        root_layout.add_widget(queue_box)

        # 5. DIAGNOSTIC LOG CONSOLE (Sehr kontrastreich, JetBrains ähnlich)
        log_section = ColorBox(
            bg_color=(0.005, 0.005, 0.01, 1),
            border_color=(0.1, 0.15, 0.25, 1),
            border_width=1.5,
            orientation='vertical',
            padding=5,
            spacing=3
        )
        
        log_section.add_widget(Label(
            text='DIAGNOSTIK TERMINAL EMULATION',
            font_size='10sp',
            color=(0.5, 0.6, 0.7, 1),
            size_hint_y=None,
            height=15,
            halign='left'
        ))
        
        self.console_scroll = ScrollView()
        self.console_text = Label(
            text='=== NEXUS MOBILE ENGINE STARTUP ===\nLese lokale DB "nexus_offline.db" ein...\n',
            font_name='Roboto',
            font_size='10sp',
            color=(0, 1, 0.5, 1), # Neon Matrix-Grün
            size_hint_y=None,
            halign='left',
            valign='top'
        )
        self.console_text.bind(texture_size=self._update_text_width)
        self.console_scroll.add_widget(self.console_text)
        log_section.add_widget(self.console_scroll)
        root_layout.add_widget(log_section)

        # Starte Background Daemon Heartbeat Thread
        self.add_log("OfflineSyncManager gestartet. Warte auf Netzwerk-Trigger...")
        self.update_info_displays()
        
        # Einmal pro Sekunde GUI-Werte & Konnektivität aktualisieren
        Clock.schedule_interval(self.update_gui_heartbeat, 3.0)
        
        return root_layout

    def _update_text_width(self, instance, size):
        self.console_text.height = size[1]
        self.console_text.text_size = (instance.width, None)

    def add_log(self, text):
        now_str = datetime.now().strftime("%H:%M:%S")
        self.console_text.text += f"[{now_str}] {text}\n"
        # Automatisches Scrollen nach unten
        self.console_scroll.scroll_y = 0

    def update_server_url(self, instance, value):
        self.sync_manager.server_url = value.strip()

    def update_gui_heartbeat(self, dt):
        """Standard-Hintergrund-Check: Aktualisiert Verbindung & Queue-Zahlen."""
        threading.Thread(target=self._async_heartbeat_task).start()

    def _async_heartbeat_task(self):
        try:
            is_connected = self.sync_manager.check_connection()
            pending = self.sync_manager.get_pending_count()
            
            # Ausführung auf dem Hauptthread einplanen
            Clock.schedule_once(lambda dt: self._apply_heartbeat_ui(is_connected, pending))
        except Exception as e:
            Clock.schedule_once(lambda dt: self.add_log(f"Fehler im Heartbeat: {str(e)}"))

    def _apply_heartbeat_ui(self, is_connected, pending):
        self.queue_info.text = f"Queue: {pending} Einträge"
        if is_connected:
            self.status_label.text = f"[ ONLINE ] Verbunden mit Master: {self.sync_manager.server_url}"
            self.status_label.color = (0, 1, 0.4, 1) # Grün
        else:
            self.status_label.text = f"[ OFFLINE ] Keine Verbindung zu: {self.sync_manager.server_url}"
            self.status_label.color = (1, 0.3, 0.3, 1) # Starkes Rot

    def update_info_displays(self):
        try:
            pending = self.sync_manager.get_pending_count()
            self.queue_info.text = f"Queue: {pending} Einträge"
        except Exception as e:
            self.add_log(f"DB Error: {str(e)}")

    def submit_quick_event(self, instance):
        """Schreibt ein manuell erfasstes Event direkt in die SQLite Queue."""
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
            "source": "Android App Client - Mobile"
        }
        
        try:
            self.sync_manager.queue_event("MOBILE_INGEST", payload)
            self.add_log(f"Event '{title}' erfolgreich lokal indiziert.")
            self.update_info_displays()
            
            # Formular zurücksetzen
            self.body_input.text = ""
        except Exception as e:
            self.add_log(f"Fehler beim Erfassen: {str(e)}")

    def trigger_sync_now(self, instance):
        """Triggert den Ingest-Prozess der ausstehenden Queue-Transaktionen."""
        self.add_log("Starte manuellen Synchronisations-Vorgang...")
        threading.Thread(target=self._async_sync_task).start()

    def _async_sync_task(self):
        try:
            count = self.sync_manager.process_queue_once()
            if count > 0:
                msg = f"Erfolgreich {count} Belege an das Hauptsystem übertragen."
            else:
                msg = "Keine ausstehenden Einträge oder Server ausgelastet."
            Clock.schedule_once(lambda dt: self._on_sync_finished(msg, True))
        except Exception as e:
            Clock.schedule_once(lambda dt: self._on_sync_finished(str(e), False))

    def _on_sync_finished(self, msg, success):
        self.add_log(msg)
        self.update_info_displays()

if __name__ == '__main__':
    NexusDaemonApp().run()
