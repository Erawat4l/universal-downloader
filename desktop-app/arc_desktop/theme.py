"""The APK's monochrome design system, expressed as Qt style sheets.

Tokens are copied straight from MainActivity.kt so the .exe and the APK look
like the same product:

    ArcBlack   #050505   window background
    ArcSurface #111111   cards
    ArcRaised  #191919   raised cards / inputs / chips
    ArcWhite   #F7F7F7   primary text and primary buttons
    ArcMuted   #B0B0B0   secondary text
    ArcSubtle  #777777   tertiary text
"""

from __future__ import annotations

ARC_BLACK = "#050505"
ARC_SURFACE = "#111111"
ARC_RAISED = "#191919"
ARC_WHITE = "#F7F7F7"
ARC_MUTED = "#B0B0B0"
ARC_SUBTLE = "#777777"
ARC_BORDER = "#222222"
ARC_BORDER_STRONG = "#2C2C2C"
ARC_ERROR = "#FF5252"
ARC_SUCCESS = "#4CAF50"
ARC_WARNING = "#FFB300"
ARC_CHIP_SELECTED_BG = "#F7F7F7"

RADIUS_CARD = 22
RADIUS_BUTTON = 16
RADIUS_CHIP = 999

FONT_FAMILY = "Roboto"
MONO_FAMILY = "Cascadia Mono, Consolas, 'Courier New', monospace"


def build_stylesheet() -> str:
    return f"""
* {{
    font-family: "{FONT_FAMILY}";
    outline: none;
}}

QMainWindow, QWidget#RootPane {{
    background-color: {ARC_BLACK};
    color: {ARC_WHITE};
}}

QWidget {{
    background-color: transparent;
    color: {ARC_WHITE};
    font-size: 13px;
}}

/* ---------------------------------------------------------- navigation rail */
QFrame#NavRail {{
    background-color: {ARC_SURFACE};
    border: none;
    border-right: 1px solid {ARC_BORDER};
}}

QPushButton#NavButton {{
    background-color: transparent;
    color: {ARC_MUTED};
    border: none;
    border-radius: {RADIUS_BUTTON}px;
    padding: 11px 14px;
    text-align: left;
    font-size: 13px;
    font-weight: 600;
}}
QPushButton#NavButton:hover {{
    background-color: {ARC_RAISED};
    color: {ARC_WHITE};
}}
QPushButton#NavButton:checked {{
    background-color: {ARC_WHITE};
    color: {ARC_BLACK};
}}
QPushButton#NavButton:disabled {{
    color: {ARC_SUBTLE};
}}

QLabel#NavBrand {{
    color: {ARC_WHITE};
    font-size: 17px;
    font-weight: 800;
    letter-spacing: 1px;
}}
QLabel#NavBrandSub {{
    color: {ARC_SUBTLE};
    font-size: 10px;
    font-weight: 600;
}}

/* ------------------------------------------------------------------ surfaces */
QFrame#Card {{
    background-color: {ARC_SURFACE};
    border: 1px solid {ARC_BORDER};
    border-radius: {RADIUS_CARD}px;
}}
QFrame#CardRaised {{
    background-color: {ARC_RAISED};
    border: 1px solid {ARC_BORDER};
    border-radius: {RADIUS_CARD}px;
}}
QFrame#CardClickable {{
    background-color: {ARC_SURFACE};
    border: 1px solid {ARC_BORDER};
    border-radius: {RADIUS_CARD}px;
}}
QFrame#CardClickable:hover {{
    border: 1px solid #3A3A3A;
    background-color: #151515;
}}
QFrame#CardSelected {{
    background-color: {ARC_RAISED};
    border: 1px solid {ARC_WHITE};
    border-radius: {RADIUS_CARD}px;
}}
QFrame#LogBox {{
    background-color: #070707;
    border: 1px solid {ARC_BORDER};
    border-radius: 12px;
}}
QFrame#Divider {{
    background-color: {ARC_BORDER};
    border: none;
    max-height: 1px;
    min-height: 1px;
}}

QLabel#ScreenTitle {{
    color: {ARC_WHITE};
    font-size: 24px;
    font-weight: 800;
}}
QLabel#ScreenSubtitle {{
    color: {ARC_MUTED};
    font-size: 12px;
}}
QLabel#SectionHeader {{
    color: {ARC_WHITE};
    font-size: 14px;
    font-weight: 700;
}}
QLabel#Meta {{
    color: {ARC_MUTED};
    font-size: 11px;
}}
QLabel#MetaDim {{
    color: {ARC_SUBTLE};
    font-size: 10px;
}}
QLabel#Badge {{
    color: {ARC_MUTED};
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 0.6px;
}}
QLabel#LogText {{
    color: #AAAAAA;
    font-family: {MONO_FAMILY};
    font-size: 11px;
}}
QLabel#LogStep {{
    color: #D4D4D4;
    font-family: {MONO_FAMILY};
    font-size: 11px;
}}
QLabel#ErrorText {{
    color: {ARC_ERROR};
    font-size: 12px;
    font-weight: 600;
}}
QLabel#SuccessText {{
    color: {ARC_SUCCESS};
    font-size: 12px;
    font-weight: 600;
}}

/* -------------------------------------------------------------------- inputs */
QLineEdit, QPlainTextEdit, QTextEdit, QSpinBox, QComboBox {{
    background-color: {ARC_RAISED};
    color: {ARC_WHITE};
    border: 1px solid #2B2B2B;
    border-radius: 12px;
    padding: 11px 13px;
    selection-background-color: {ARC_WHITE};
    selection-color: {ARC_BLACK};
    font-size: 13px;
}}
QLineEdit:focus, QPlainTextEdit:focus, QTextEdit:focus, QSpinBox:focus, QComboBox:focus {{
    border: 1px solid #4A4A4A;
}}
QLineEdit:disabled, QComboBox:disabled {{
    color: {ARC_SUBTLE};
    background-color: #141414;
}}
QPlainTextEdit, QTextEdit {{
    font-family: {MONO_FAMILY};
    font-size: 11px;
}}

QComboBox::drop-down {{
    border: none;
    width: 26px;
}}
QComboBox::down-arrow {{
    image: none;
    border-left: 4px solid transparent;
    border-right: 4px solid transparent;
    border-top: 5px solid {ARC_MUTED};
    margin-right: 10px;
}}
QComboBox QAbstractItemView {{
    background-color: {ARC_SURFACE};
    color: {ARC_WHITE};
    border: 1px solid {ARC_BORDER_STRONG};
    border-radius: 10px;
    padding: 6px;
    selection-background-color: {ARC_RAISED};
    selection-color: {ARC_WHITE};
    outline: none;
}}

/* ------------------------------------------------------------------- buttons */
QPushButton {{
    background-color: {ARC_RAISED};
    color: {ARC_WHITE};
    border: 1px solid {ARC_BORDER_STRONG};
    border-radius: {RADIUS_BUTTON}px;
    padding: 10px 16px;
    font-size: 13px;
    font-weight: 600;
}}
QPushButton:hover {{ background-color: #202020; }}
QPushButton:pressed {{ background-color: #161616; }}
QPushButton:disabled {{ color: {ARC_SUBTLE}; background-color: #131313; border-color: {ARC_BORDER}; }}

QPushButton#Primary {{
    background-color: {ARC_WHITE};
    color: {ARC_BLACK};
    border: none;
    border-radius: 15px;
    padding: 13px 18px;
    font-size: 14px;
    font-weight: 800;
}}
QPushButton#Primary:hover {{ background-color: #FFFFFF; }}
QPushButton#Primary:pressed {{ background-color: #DDDDDD; }}
QPushButton#Primary:disabled {{ background-color: #2A2A2A; color: {ARC_SUBTLE}; }}

QPushButton#Ghost {{
    background-color: transparent;
    color: {ARC_MUTED};
    border: none;
    padding: 6px 10px;
    font-size: 12px;
    font-weight: 600;
}}
QPushButton#Ghost:hover {{ color: {ARC_WHITE}; background-color: {ARC_RAISED}; }}

QPushButton#Chip {{
    background-color: {ARC_RAISED};
    color: {ARC_MUTED};
    border: 1px solid {ARC_BORDER_STRONG};
    border-radius: {RADIUS_CHIP}px;
    padding: 7px 15px;
    font-size: 12px;
    font-weight: 600;
}}
QPushButton#Chip:hover {{ color: {ARC_WHITE}; border-color: #3A3A3A; }}
QPushButton#Chip:checked {{
    background-color: {ARC_CHIP_SELECTED_BG};
    color: {ARC_BLACK};
    border-color: {ARC_CHIP_SELECTED_BG};
    font-weight: 800;
}}

/* ------------------------------------------------------------------ progress */
QProgressBar {{
    background-color: #262626;
    border: none;
    border-radius: 4px;
    max-height: 8px;
    min-height: 8px;
    text-align: center;
    color: transparent;
}}
QProgressBar::chunk {{
    background-color: {ARC_WHITE};
    border-radius: 4px;
}}
QProgressBar#Failed::chunk {{ background-color: {ARC_ERROR}; }}
QProgressBar:indeterminate {{ color: transparent; }}

/* --------------------------------------------------------------------- lists */
QScrollArea {{ border: none; background-color: transparent; }}
QListWidget {{
    background-color: transparent;
    border: none;
    outline: none;
}}
QListWidget::item {{ background-color: transparent; }}

QScrollBar:vertical {{
    background: transparent;
    width: 9px;
    margin: 2px;
}}
QScrollBar::handle:vertical {{
    background: #2E2E2E;
    border-radius: 4px;
    min-height: 32px;
}}
QScrollBar::handle:vertical:hover {{ background: #454444; }}
QScrollBar::add-line:vertical, QScrollBar::sub-line:vertical {{ height: 0; }}
QScrollBar::add-page:vertical, QScrollBar::sub-page:vertical {{ background: transparent; }}
QScrollBar:horizontal {{ background: transparent; height: 9px; margin: 2px; }}
QScrollBar::handle:horizontal {{ background: #2E2E2E; border-radius: 4px; min-width: 32px; }}
QScrollBar::add-line:horizontal, QScrollBar::sub-line:horizontal {{ width: 0; }}
QScrollBar::add-page:horizontal, QScrollBar::sub-page:horizontal {{ background: transparent; }}

/* -------------------------------------------------------------------- dialogs */
QMessageBox, QFileDialog, QInputDialog {{
    background-color: {ARC_SURFACE};
}}
QMessageBox QLabel {{ color: {ARC_WHITE}; font-size: 13px; }}
QToolTip {{
    background-color: {ARC_RAISED};
    color: {ARC_WHITE};
    border: 1px solid {ARC_BORDER_STRONG};
    padding: 5px 8px;
    border-radius: 6px;
}}

QCheckBox {{ color: {ARC_WHITE}; spacing: 9px; font-size: 12px; }}
QCheckBox::indicator {{
    width: 17px; height: 17px;
    border-radius: 5px;
    border: 1px solid #3A3A3A;
    background: {ARC_RAISED};
}}
QCheckBox::indicator:checked {{ background: {ARC_WHITE}; border-color: {ARC_WHITE}; }}

QStatusBar {{ background-color: {ARC_SURFACE}; color: {ARC_MUTED}; font-size: 11px; }}
QStatusBar::item {{ border: none; }}
"""


def nav_icon_glyph(label: str) -> str:
    """Compact monochrome glyphs for the nav rail.

    The APK uses Material outlined icons. Shipping a full icon font would bloat
    the exe, so the rail uses geometric characters that render in Segoe UI on
    every supported Windows version.
    """
    return {
        "Home": "\u2302",
        "Activity": "\u2193",
        "Library": "\u25A4",
        "Settings": "\u2699",
    }.get(label, "\u25CF")
