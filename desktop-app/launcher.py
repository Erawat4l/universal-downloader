"""PyInstaller entry point that imports Arc as a package.

Do not freeze arc_desktop/app.py directly: its relative imports require the
arc_desktop package context and fail when PyInstaller executes it as a script.
"""

from arc_desktop.app import main

if __name__ == "__main__":
    raise SystemExit(main())
