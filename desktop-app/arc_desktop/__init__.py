"""Arc Desktop - the Windows front end for the repository's dl.py engine.

The APK and this app are siblings: both are thin native shells around the same
`dl.py`, driven through its `--app-json` machine interface. Nothing here forks
or copies the engine, so a fix to dl.py improves the CLI, the APK and the .exe
at the same time.
"""

__version__ = "1.0.0"
APP_NAME = "Arc"
APP_LONG_NAME = "Arc - Universal Downloader"
