"""Modal dialogs for Arc Desktop, mirroring MainActivity.kt dialogs.

Includes:
- SeriesDownloadDialog (Single Episode / Season / Complete Series)
- AudioSelectDialog (Prompt when multiple audio tracks are available)
- StreamingPlayerDialog (ARC Player / VLC / System Default)
- AboutFeaturesDialog (About ARC Features)
- TelegramLoginDialog (QR Code Scan, Phone OTP + 2FA, Manual Session)
- GoogleYouTubeDialog (Browser extraction / cookies.txt / Cookie paste)
"""

from __future__ import annotations

import re
from pathlib import Path
from typing import Optional

from PySide6.QtCore import Qt, QTimer, Signal
from PySide6.QtGui import QColor, QImage, QPixmap
from PySide6.QtWidgets import (
    QApplication, QCheckBox, QDialog, QFileDialog, QFrame, QHBoxLayout,
    QLabel, QLineEdit, QPlainTextEdit, QPushButton, QRadioButton,
    QTabWidget, QVBoxLayout, QWidget,
)

from .. import platform_adapter, theme


def _dialog_style(dialog: QDialog) -> None:
    dialog.setStyleSheet(
        f"QDialog {{ background-color: {theme.ARC_SURFACE}; color: {theme.ARC_WHITE}; }}"
    )


class SeriesDownloadDialog(QDialog):
    """Episode vs Season vs Complete Series scope picker."""

    def __init__(self, title: str, season: int, episode: int, parent=None) -> None:
        super().__init__(parent)
        self.chosen_scope: Optional[str] = None
        self.setWindowTitle("Download TV Series")
        self.setMinimumWidth(380)
        _dialog_style(self)

        layout = QVBoxLayout(self)
        layout.setContentsMargins(22, 20, 22, 20)
        layout.setSpacing(12)

        head = QLabel(title or "TV Series")
        head.setStyleSheet(f"font-size: 16px; font-weight: 800; color: {theme.ARC_WHITE};")
        head.setWordWrap(True)
        layout.addWidget(head)

        sub = QLabel("Choose download scope:")
        sub.setObjectName("Meta")
        layout.addWidget(sub)

        options = [
            (f"Episode S{season}E{episode}", "episode"),
            (f"Season {season} (All Episodes)", "season"),
            ("Complete Series", "series"),
        ]
        for label_text, scope_val in options:
            btn = QPushButton(label_text)
            btn.setObjectName("Primary" if scope_val == "episode" else "")
            btn.setCursor(Qt.PointingHandCursor)
            btn.setFixedHeight(44)
            btn.clicked.connect(lambda _=False, sc=scope_val: self._pick(sc))
            layout.addWidget(btn)

        cancel = QPushButton("Cancel")
        cancel.setObjectName("Ghost")
        cancel.setCursor(Qt.PointingHandCursor)
        cancel.clicked.connect(self.reject)
        layout.addWidget(cancel, 0, Qt.AlignRight)

    def _pick(self, scope: str) -> None:
        self.chosen_scope = scope
        self.accept()


class AudioSelectDialog(QDialog):
    """Select an audio track before Download/Stream if not chosen."""

    def __init__(self, tracks: list[dict], current_lang: str = "", parent=None) -> None:
        super().__init__(parent)
        self.selected_track: Optional[dict] = None
        self.setWindowTitle("Select Audio Language")
        self.setMinimumWidth(360)
        _dialog_style(self)

        layout = QVBoxLayout(self)
        layout.setContentsMargins(22, 20, 22, 20)
        layout.setSpacing(10)

        sub = QLabel("Choose preferred audio track:")
        sub.setObjectName("Meta")
        layout.addWidget(sub)

        for track in tracks:
            lang = str(track.get("language") or "Default")
            btn = QPushButton(lang)
            if lang.lower() == current_lang.lower():
                btn.setObjectName("Primary")
            btn.setCursor(Qt.PointingHandCursor)
            btn.setFixedHeight(42)
            btn.clicked.connect(lambda _=False, t=track: self._pick(t))
            layout.addWidget(btn)

        cancel = QPushButton("Cancel")
        cancel.setObjectName("Ghost")
        cancel.setCursor(Qt.PointingHandCursor)
        cancel.clicked.connect(self.reject)
        layout.addWidget(cancel, 0, Qt.AlignRight)

    def _pick(self, track: dict) -> None:
        self.selected_track = dict(track)
        self.accept()


class StreamingPlayerDialog(QDialog):
    """Select built-in ARC Player vs VLC vs System Default."""

    def __init__(self, current_key: str, vlc_path: str = "", parent=None) -> None:
        super().__init__(parent)
        self.chosen_key = current_key or "arc_player"
        self.setWindowTitle("Streaming Player")
        self.setMinimumWidth(420)
        _dialog_style(self)

        layout = QVBoxLayout(self)
        layout.setContentsMargins(22, 20, 22, 20)
        layout.setSpacing(12)

        info = QLabel("Choose which player opens when you click Stream:")
        info.setObjectName("Meta")
        info.setWordWrap(True)
        layout.addWidget(info)

        choices = [
            ("arc_player", "ARC Player (Built-in)", "Synced HD DASH playback, quality & audio switching, speed & seek"),
            ("vlc", "VLC Media Player", f"Detected: {vlc_path}" if vlc_path else "Uses installed VLC (--input-slave for HD audio)"),
            ("system_chooser", "System Default Handler", "Opens stream with Windows default media association"),
        ]
        self._radios: dict[str, QRadioButton] = {}
        for key, title, desc in choices:
            card = QFrame()
            card.setObjectName("CardSelected" if key == self.chosen_key else "CardRaised")
            card_layout = QHBoxLayout(card)
            card_layout.setContentsMargins(14, 12, 14, 12)
            card_layout.setSpacing(10)
            rb = QRadioButton()
            rb.setChecked(key == self.chosen_key)
            rb.toggled.connect(lambda checked, k=key: self._set_choice(k) if checked else None)
            self._radios[key] = rb
            col = QVBoxLayout()
            col.setSpacing(2)
            t_lbl = QLabel(title)
            t_lbl.setStyleSheet("font-size: 13px; font-weight: 700;")
            d_lbl = QLabel(desc)
            d_lbl.setObjectName("MetaDim")
            d_lbl.setWordWrap(True)
            col.addWidget(t_lbl)
            col.addWidget(d_lbl)
            card_layout.addWidget(rb)
            card_layout.addLayout(col, 1)
            layout.addWidget(card)

        btns = QHBoxLayout()
        btns.addStretch(1)
        cancel = QPushButton("Cancel")
        cancel.setObjectName("Ghost")
        cancel.clicked.connect(self.reject)
        done = QPushButton("Save")
        done.setObjectName("Primary")
        done.clicked.connect(self.accept)
        btns.addWidget(cancel)
        btns.addWidget(done)
        layout.addLayout(btns)

    def _set_choice(self, key: str) -> None:
        self.chosen_key = key


class AboutFeaturesDialog(QDialog):
    """Lists all ARC capabilities matching MainActivity.kt."""

    def __init__(self, version: str, parent=None) -> None:
        super().__init__(parent)
        self.setWindowTitle("About ARC Features")
        self.setMinimumWidth(400)
        _dialog_style(self)

        layout = QVBoxLayout(self)
        layout.setContentsMargins(22, 20, 22, 20)
        layout.setSpacing(10)

        head = QLabel(f"ARC Desktop v{version}")
        head.setStyleSheet("font-size: 17px; font-weight: 800;")
        layout.addWidget(head)

        sub = QLabel("Core features:")
        sub.setObjectName("Meta")
        layout.addWidget(sub)

        features = [
            "Universal Downloader (YouTube & 1000+ sites)",
            "Direct Downloads (16x parallel via aria2c)",
            "Multi-Audio & Live Audio Track Switching",
            "Resolution Control (Up to 4K UHD)",
            "Movies & Series Batch Downloads",
            "Built-in ARC Cinema Player + VLC Support",
            "India's Got Latent Full Catalog",
            "BitTorrent & Magnet Links (.torrent picker)",
            "Telegram Private Channels (QR & Phone OTP Login)",
            "YouTube Cookie Auth & Execution Engine Logs",
        ]
        for feat in features:
            row = QLabel(f"\u2022  {feat}")
            row.setStyleSheet(f"color: {theme.ARC_WHITE}; font-size: 12px;")
            layout.addWidget(row)

        ok_btn = QPushButton("Done")
        ok_btn.setObjectName("Primary")
        ok_btn.clicked.connect(self.accept)
        layout.addSpacing(6)
        layout.addWidget(ok_btn, 0, Qt.AlignRight)


def _render_qr_pixmap(text: str, size_px: int = 240) -> Optional[QPixmap]:
    try:
        import qrcode
        qr = qrcode.QRCode(border=2, box_size=1)
        qr.add_data(text)
        qr.make(fit=True)
        matrix = qr.get_matrix()
        n = len(matrix)
        if n <= 0:
            return None
        img = QImage(n, n, QImage.Format_RGB32)
        white = QColor(247, 247, 247).rgb()
        black = QColor(12, 12, 12).rgb()
        for r in range(n):
            row = matrix[r]
            for c in range(n):
                img.setPixel(c, r, black if row[c] else white)
        return QPixmap.fromImage(img).scaled(
            size_px, size_px, Qt.KeepAspectRatio, Qt.FastTransformation
        )
    except Exception:
        return None


class _TgSignals(QWidget):
    qr_init_done = Signal(dict)
    qr_poll_done = Signal(dict)
    code_sent = Signal(dict)
    signed_in = Signal(dict)


class TelegramLoginDialog(QDialog):
    """QR Code Scan + Phone OTP/2FA + Manual String Session login for Telegram."""

    def __init__(self, bridge, api_id: str, api_hash: str, session: str, parent=None) -> None:
        super().__init__(parent)
        self.bridge = bridge
        self.result_creds: Optional[dict] = None
        self.setWindowTitle("Telegram Login")
        self.setMinimumWidth(460)
        _dialog_style(self)

        self._signals = _TgSignals(self)
        self._signals.qr_init_done.connect(self._on_qr_init)
        self._signals.qr_poll_done.connect(self._on_qr_poll)
        self._signals.code_sent.connect(self._on_code_sent)
        self._signals.signed_in.connect(self._on_signed_in)

        self._qr_session_key = ""
        self._qr_need_pwd = False
        self._phone_code_hash = ""
        self._phone_need_pwd = False
        self._qr_timer = QTimer(self)
        self._qr_timer.setInterval(2000)
        self._qr_timer.timeout.connect(self._poll_qr)

        layout = QVBoxLayout(self)
        layout.setContentsMargins(20, 18, 20, 18)
        layout.setSpacing(12)

        self.tabs = QTabWidget()

        # Tab 1: QR Code
        qr_tab = QWidget()
        qr_layout = QVBoxLayout(qr_tab)
        qr_layout.setSpacing(10)
        self.qr_label = QLabel("Click Generate QR Code to sign in with the Telegram app.")
        self.qr_label.setAlignment(Qt.AlignCenter)
        self.qr_label.setWordWrap(True)
        self.qr_label.setMinimumHeight(240)
        self.qr_label.setStyleSheet(
            f"background-color: {theme.ARC_RAISED}; border: 1px solid {theme.ARC_BORDER};"
            f" border-radius: 12px; padding: 14px; color: {theme.ARC_MUTED};"
        )
        qr_layout.addWidget(self.qr_label)

        self.qr_status = QLabel("Open Telegram Mobile -> Settings -> Devices -> Link Desktop Device")
        self.qr_status.setObjectName("Meta")
        self.qr_status.setWordWrap(True)
        self.qr_status.setAlignment(Qt.AlignCenter)
        qr_layout.addWidget(self.qr_status)

        self.qr_pwd_input = QLineEdit()
        self.qr_pwd_input.setPlaceholderText("2FA Cloud Password (if required)")
        self.qr_pwd_input.setEchoMode(QLineEdit.Password)
        self.qr_pwd_input.setVisible(False)
        qr_layout.addWidget(self.qr_pwd_input)

        qr_btn_row = QHBoxLayout()
        self.qr_start_btn = QPushButton("Generate QR Code")
        self.qr_start_btn.setObjectName("Primary")
        self.qr_start_btn.clicked.connect(self._start_qr)
        self.qr_pwd_btn = QPushButton("Verify 2FA")
        self.qr_pwd_btn.setObjectName("Primary")
        self.qr_pwd_btn.setVisible(False)
        self.qr_pwd_btn.clicked.connect(self._verify_qr_pwd)
        qr_btn_row.addWidget(self.qr_start_btn)
        qr_btn_row.addWidget(self.qr_pwd_btn)
        qr_layout.addLayout(qr_btn_row)
        self.tabs.addTab(qr_tab, "QR Code")

        # Tab 2: Phone OTP
        otp_tab = QWidget()
        otp_layout = QVBoxLayout(otp_tab)
        otp_layout.setSpacing(10)
        self.phone_input = QLineEdit()
        self.phone_input.setPlaceholderText("Phone number with country code (e.g. +919876543210)")
        self.code_input = QLineEdit()
        self.code_input.setPlaceholderText("Verification code")
        self.code_input.setVisible(False)
        self.otp_pwd_input = QLineEdit()
        self.otp_pwd_input.setPlaceholderText("2FA Cloud Password (if enabled)")
        self.otp_pwd_input.setEchoMode(QLineEdit.Password)
        self.otp_pwd_input.setVisible(False)
        self.otp_status = QLabel("Enter your Telegram phone number to receive a login code.")
        self.otp_status.setObjectName("Meta")
        self.otp_status.setWordWrap(True)
        self.otp_btn = QPushButton("Send Code")
        self.otp_btn.setObjectName("Primary")
        self.otp_btn.clicked.connect(self._otp_step)
        for w in (self.phone_input, self.code_input, self.otp_pwd_input, self.otp_status, self.otp_btn):
            otp_layout.addWidget(w)
        otp_layout.addStretch(1)
        self.tabs.addTab(otp_tab, "Phone OTP")

        # Tab 3: Manual API / String Session
        man_tab = QWidget()
        man_layout = QVBoxLayout(man_tab)
        man_layout.setSpacing(8)
        self.man_id = QLineEdit(api_id)
        self.man_id.setPlaceholderText("API ID (optional for QR/OTP, numeric for custom)")
        self.man_hash = QLineEdit(api_hash)
        self.man_hash.setPlaceholderText("API Hash (optional)")
        self.man_sess = QPlainTextEdit(session)
        self.man_sess.setPlaceholderText("Pyrogram String Session")
        self.man_sess.setMaximumHeight(110)
        self.man_status = QLabel("Check or save manual Pyrogram session credentials.")
        self.man_status.setObjectName("Meta")
        self.man_status.setWordWrap(True)
        man_btns = QHBoxLayout()
        self.man_verify_btn = QPushButton("Verify & Save")
        self.man_verify_btn.setObjectName("Primary")
        self.man_verify_btn.clicked.connect(self._verify_manual)
        man_btns.addWidget(self.man_verify_btn)
        for w in (self.man_id, self.man_hash, self.man_sess, self.man_status):
            man_layout.addWidget(w)
        man_layout.addLayout(man_btns)
        self.tabs.addTab(man_tab, "Manual")

        layout.addWidget(self.tabs)

        close_row = QHBoxLayout()
        close_row.addStretch(1)
        close_btn = QPushButton("Close")
        close_btn.setObjectName("Ghost")
        close_btn.clicked.connect(self.reject)
        close_row.addWidget(close_btn)
        layout.addLayout(close_row)

    def _api_creds(self) -> tuple[str, str]:
        return self.man_id.text().strip(), self.man_hash.text().strip()

    def _start_qr(self) -> None:
        self._qr_timer.stop()
        self._qr_need_pwd = False
        self.qr_pwd_input.setVisible(False)
        self.qr_pwd_btn.setVisible(False)
        self.qr_start_btn.setEnabled(False)
        self.qr_status.setText("Generating Telegram QR token...")
        api_id, api_hash = self._api_creds()
        self.bridge.submit(
            {"action": "telegram_qr_init", "api_id": api_id, "api_hash": api_hash},
            lambda res: self._signals.qr_init_done.emit(res),
        )

    def _on_qr_init(self, res: dict) -> None:
        self.qr_start_btn.setEnabled(True)
        data = dict(res.get("result") or {})
        if not res.get("ok") or not data.get("qr_url"):
            self.qr_status.setText(str(res.get("error") or "Failed to generate QR code."))
            return
        self._qr_session_key = str(data.get("session_key") or "")
        qr_url = str(data.get("qr_url") or "")
        pix = _render_qr_pixmap(qr_url, 220)
        if pix is not None:
            self.qr_label.setPixmap(pix)
        else:
            self.qr_label.setText(qr_url)
        self.qr_status.setText("Waiting for scan in Telegram Mobile...")
        self._qr_timer.start()

    def _poll_qr(self) -> None:
        if not self._qr_session_key or self._qr_need_pwd:
            return
        self.bridge.submit(
            {"action": "telegram_qr_check", "session_key": self._qr_session_key},
            lambda res: self._signals.qr_poll_done.emit(res),
        )

    def _on_qr_poll(self, res: dict) -> None:
        data = dict(res.get("result") or {})
        if data.get("need_password"):
            self._qr_timer.stop()
            self._qr_need_pwd = True
            self.qr_pwd_input.setVisible(True)
            self.qr_pwd_btn.setVisible(True)
            self.qr_status.setText("2FA password required. Enter password below.")
            return
        if data.get("connected") and data.get("session"):
            self._qr_timer.stop()
            self.result_creds = {
                "api_id": str(data.get("api_id") or ""),
                "api_hash": str(data.get("api_hash") or ""),
                "session": str(data.get("session") or ""),
                "user": str(data.get("user") or "Telegram User"),
                "phone": str(data.get("phone") or ""),
            }
            self.accept()
            return
        if data.get("error"):
            self.qr_status.setText(str(data.get("error")))

    def _verify_qr_pwd(self) -> None:
        pwd = self.qr_pwd_input.text().strip()
        if not pwd:
            return
        self.qr_pwd_btn.setEnabled(False)
        self.bridge.submit(
            {"action": "telegram_password", "session_key": self._qr_session_key, "password": pwd},
            lambda res: self._signals.signed_in.emit(res),
        )

    def _otp_step(self) -> None:
        phone = self.phone_input.text().strip()
        if not phone:
            self.otp_status.setText("Enter phone number first.")
            return
        api_id, api_hash = self._api_creds()
        self.otp_btn.setEnabled(False)
        if self._phone_need_pwd:
            pwd = self.otp_pwd_input.text().strip()
            self.otp_status.setText("Checking 2FA password...")
            self.bridge.submit(
                {"action": "telegram_password", "phone": phone, "password": pwd},
                lambda res: self._signals.signed_in.emit(res),
            )
        elif self._phone_code_hash:
            code = self.code_input.text().strip()
            self.otp_status.setText("Signing in...")
            self.bridge.submit(
                {"action": "telegram_sign_in", "phone": phone, "code": code, "phone_code_hash": self._phone_code_hash},
                lambda res: self._signals.signed_in.emit(res),
            )
        else:
            self.otp_status.setText("Sending verification code...")
            self.bridge.submit(
                {"action": "telegram_send_code", "api_id": api_id, "api_hash": api_hash, "phone": phone},
                lambda res: self._signals.code_sent.emit(res),
            )

    def _on_code_sent(self, res: dict) -> None:
        self.otp_btn.setEnabled(True)
        data = dict(res.get("result") or {})
        if not res.get("ok") or not data.get("phone_code_hash"):
            self.otp_status.setText(str(res.get("error") or "Could not send code."))
            return
        self._phone_code_hash = str(data.get("phone_code_hash") or "")
        self.code_input.setVisible(True)
        self.otp_btn.setText("Verify Code")
        self.otp_status.setText("Code sent! Check Telegram messages on your phone.")

    def _verify_manual(self) -> None:
        api_id, api_hash = self._api_creds()
        sess = re.sub(r"\s+", "", self.man_sess.toPlainText())
        if not (api_id and api_hash and sess):
            self.man_status.setText("API ID, API Hash, and Session String are required.")
            return
        self.man_verify_btn.setEnabled(False)
        self.man_status.setText("Verifying session...")
        self.bridge.submit(
            {"action": "telegram_check", "api_id": api_id, "api_hash": api_hash, "session": sess},
            lambda res: self._signals.signed_in.emit(res),
        )

    def _on_signed_in(self, res: dict) -> None:
        self.otp_btn.setEnabled(True)
        self.qr_pwd_btn.setEnabled(True)
        self.man_verify_btn.setEnabled(True)
        data = dict(res.get("result") or {})
        if not res.get("ok"):
            err = str(res.get("error") or "Authentication failed.")
            self.otp_status.setText(err)
            self.qr_status.setText(err)
            self.man_status.setText(err)
            return
        if data.get("need_password"):
            self._phone_need_pwd = True
            self.otp_pwd_input.setVisible(True)
            self.otp_btn.setText("Verify 2FA")
            self.otp_status.setText("Account uses 2FA password. Enter password below.")
            return
        session_val = str(data.get("session") or "")
        if session_val or data.get("valid"):
            self.result_creds = {
                "api_id": str(data.get("api_id") or self.man_id.text().strip()),
                "api_hash": str(data.get("api_hash") or self.man_hash.text().strip()),
                "session": session_val or re.sub(r"\s+", "", self.man_sess.toPlainText()),
                "user": str(data.get("user") or "Telegram User"),
                "phone": str(data.get("phone") or ""),
            }
            self.accept()

    def closeEvent(self, event) -> None:  # noqa: N802
        self._qr_timer.stop()
        super().closeEvent(event)


class GoogleYouTubeDialog(QDialog):
    """YouTube cookies manager (Browser auto-extract, cookies.txt import, or paste)."""

    def __init__(self, current_email: str, current_cookies: str, parent=None) -> None:
        super().__init__(parent)
        self.result_email = current_email or ""
        self.result_cookies = current_cookies or ""
        self.setWindowTitle("YouTube Authentication (Optional)")
        self.setMinimumWidth(480)
        _dialog_style(self)

        layout = QVBoxLayout(self)
        layout.setContentsMargins(22, 20, 22, 20)
        layout.setSpacing(12)

        info = QLabel(
            "ARC downloads YouTube without login. Import session cookies only if YouTube "
            "blocks requests with a bot-check or 403 error. Everything stays locally on this PC."
        )
        info.setObjectName("Meta")
        info.setWordWrap(True)
        layout.addWidget(info)

        warn = QFrame()
        warn.setStyleSheet(
            "background-color: #1E1711; border: 1px solid #4A3015; border-radius: 10px;"
        )
        warn_layout = QHBoxLayout(warn)
        warn_layout.setContentsMargins(12, 8, 12, 8)
        w_lbl = QLabel("Use a secondary Google account, not your primary one.")
        w_lbl.setStyleSheet("color: #FFD59E; font-size: 12px;")
        w_lbl.setWordWrap(True)
        warn_layout.addWidget(w_lbl)
        layout.addWidget(warn)

        row_browsers = QHBoxLayout()
        row_browsers.setSpacing(8)
        for browser_key, label_txt in (("chrome", "Import Chrome"), ("edge", "Import Edge"), ("firefox", "Import Firefox")):
            btn = QPushButton(label_txt)
            btn.setObjectName("Chip")
            btn.setCursor(Qt.PointingHandCursor)
            btn.clicked.connect(lambda _=False, bk=browser_key: self._import_browser(bk))
            row_browsers.addWidget(btn)
        file_btn = QPushButton("Load cookies.txt")
        file_btn.setObjectName("Chip")
        file_btn.setCursor(Qt.PointingHandCursor)
        file_btn.clicked.connect(self._load_file)
        row_browsers.addWidget(file_btn)
        layout.addLayout(row_browsers)

        self.label_input = QLineEdit(current_email)
        self.label_input.setPlaceholderText("Label / Email (e.g. secondary@gmail.com)")
        layout.addWidget(self.label_input)

        self.cookies_edit = QPlainTextEdit(current_cookies)
        self.cookies_edit.setPlaceholderText("Paste Netscape cookies.txt or HTTP Cookie header...")
        self.cookies_edit.setMinimumHeight(130)
        layout.addWidget(self.cookies_edit)

        self.status_lbl = QLabel("")
        self.status_lbl.setObjectName("Meta")
        self.status_lbl.setWordWrap(True)
        layout.addWidget(self.status_lbl)

        btns = QHBoxLayout()
        clear_btn = QPushButton("Clear")
        clear_btn.setObjectName("Ghost")
        clear_btn.clicked.connect(self._clear_all)
        cancel_btn = QPushButton("Cancel")
        cancel_btn.setObjectName("Ghost")
        cancel_btn.clicked.connect(self.reject)
        save_btn = QPushButton("Save Cookies")
        save_btn.setObjectName("Primary")
        save_btn.clicked.connect(self._save)
        btns.addWidget(clear_btn)
        btns.addStretch(1)
        btns.addWidget(cancel_btn)
        btns.addWidget(save_btn)
        layout.addLayout(btns)

    def _import_browser(self, browser: str) -> None:
        ok, text, msg = platform_adapter.extract_browser_youtube_cookies(browser)
        self.status_lbl.setText(msg)
        if ok and text:
            self.cookies_edit.setPlainText(text)
            if not self.label_input.text().strip():
                self.label_input.setText(f"{browser.title()} Session")

    def _load_file(self) -> None:
        path, _ = QFileDialog.getOpenFileName(
            self, "Select cookies.txt", "", "Cookie Files (*.txt);;All Files (*)"
        )
        if not path:
            return
        try:
            content = Path(path).read_text(encoding="utf-8", errors="replace").strip()
            self.cookies_edit.setPlainText(content)
            if not self.label_input.text().strip():
                self.label_input.setText(Path(path).name)
            self.status_lbl.setText(f"Loaded {Path(path).name}")
        except Exception as error:
            self.status_lbl.setText(f"Could not read file: {error}")

    def _clear_all(self) -> None:
        self.label_input.clear()
        self.cookies_edit.clear()
        self.result_email = ""
        self.result_cookies = ""
        self.accept()

    def _save(self) -> None:
        raw = self.cookies_edit.toPlainText().strip()
        self.result_cookies = raw
        lbl = self.label_input.text().strip()
        self.result_email = lbl or ("Google Account" if raw else "")
        self.accept()
