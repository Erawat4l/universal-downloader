"""Allow `python -m arc_desktop` to launch the desktop app."""

import sys

from .app import main

if __name__ == "__main__":
    sys.exit(main())
