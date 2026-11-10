# UNIVERSAL DOWNLOADER — FRESH UI-ONLY PROMPT FOR CLAUDE

**Start fresh. Treat everything in this prompt as the only source of truth. Ignore any previous conversation, pasted screenshots, README-writing tasks, or assumptions.**

## Critical correction

I am **not** asking for a README, GitHub documentation, a local file-sharing app, a LAN file-transfer app, a server dashboard, a CLI redesign, code, or backend architecture.

I am asking for a **premium native Android APK UI/UX design brief only** for an app called **Universal Downloader**. The Android app will later become the visual frontend for an existing downloader CLI. Your job in this response is to describe **only what the Android app should look like and how its screens should behave**.

Do not mention or design “local file sharing.” Do not rewrite any README. Do not provide Kotlin, Jetpack Compose, XML, Flutter, React Native, API, database, shell commands, downloader logic, installation steps, or implementation instructions.

## Product facts — use these and only these

Repository: `Erawat4l/universal-downloader`

Current product: a Python command-line tool named `dl` / `dl.py`, called **Universal Downloader**.

The future Android app should make these existing capabilities feel simple and premium:

- Paste or type a URL.
- Search for a movie, series, video, or song by name when supported.
- Download direct files and supported online video/audio links.
- Download media from supported YouTube/video sources.
- Download Telegram media from a Telegram message link.
- Download supported educational PDFs from sources such as SelfStudys, StudiesToday, and Studocu.
- Resolve supported movie/series stream sources such as NetMirror.
- Choose the best available quality or select 4K, 2K, 1080p, 720p, 480p, 360p, 240p, 144p, or audio-only formats such as MP3/M4A when available.
- Choose an action when supported: download, stream instantly in an installed player such as VLC, stream while downloading, or copy a direct stream link for ADM/1DM.
- Show progress, percentage, downloaded/total size, speed, ETA, paused, retrying, network recovery, and completed states.
- Resume interrupted downloads and retry failed downloads.
- Save to the Android Downloads folder or a user-selected folder.
- Support optional cookies and Telegram credentials for authenticated sources. These are sensitive values and must be hidden by default.

The product is for mobile-first Gen Z users, but the UI must remain elegant, trustworthy, fast, accessible, and useful on ordinary Android phones.

## Core design direction

Create a latest-generation, premium Android experience:

- Dark-first, with a beautiful light theme too.
- Edge-to-edge layout and expressive Material 3-inspired components.
- Strong visual hierarchy, large friendly typography, rounded surfaces, subtle depth, and restrained gradients or blur.
- A distinctive accent color system, but **not** cheap neon cyberpunk.
- The app should feel fast, calm, and confident rather than like a terminal or a settings-heavy utility.
- Design for one-handed use; primary actions should be easy to reach.
- Do not copy any existing app or brand.

## Exact task

Write a **UI/UX design specification only** for this Android APK. Return the sections below in this order. Keep the writing visual and concrete: explain what is on screen, where it is, what the user sees, and what happens after each action.

### 1. Product concept
- One short tagline.
- One recommended visual direction, not three unrelated concepts.
- 5–8 brand personality words.
- A short explanation of why the direction feels premium and Gen Z-friendly.

### 2. Visual design system
Specify:
- Dark-theme and light-theme colors with example hex values.
- Background, surface, elevated surface, primary accent, secondary accent, text, muted text, success, warning, error, paused, offline, and progress colors.
- Typography hierarchy and a suitable modern Android type style.
- Spacing, corner radii, buttons, icon style, chips, cards, dividers, shadows, and touch-target guidance.
- How to use gradients, glass/blur, glow, and grain sparingly.
- How the design changes between dark and light themes.

### 3. Navigation and information architecture
Recommend the simplest Android navigation model and explain it briefly. Define the top-level destinations, such as:

- Home / New download
- Activity / Queue
- Library / Files
- Search, only if it improves the main flow
- Settings

Explain what should be a full screen, a bottom sheet, a dialog, or a small inline state. Keep the primary download flow short.

### 4. Screen-by-screen design
For every screen below, describe the layout from top to bottom, the visual hierarchy, components, primary and secondary actions, sample UI copy, and important states.

#### A. First launch and permissions
Include a minimal welcome screen, storage/media permission explanation, optional notification/background permission, sensitive credential explanation, skip paths, and the first successful download path.

#### B. Home / New download
Include:
- A prominent URL/search input.
- Paste-from-clipboard affordance that is helpful but not intrusive.
- A clear primary action.
- Recent downloads or quick actions without clutter.
- A premium first-use empty state.
- Download-folder access.

#### C. Link analysis / media preview
Include:
- An extracting/loading state.
- Thumbnail or neutral placeholder.
- Title, source/domain, duration, file type, estimated size, and available media type.
- Different visual treatment for a video, audio file, PDF, Telegram media, movie, and series.
- Trustworthy progress feedback without terminal logs.

#### D. Format and action selection
Include:
- Quality options from 4K down to 144p and audio-only when available.
- Resolution, format, audio language, estimated size, and quality badges.
- Download, stream, stream-and-download, and copy-link actions where supported.
- A simple explanation when video and audio must be merged.
- Your recommendation for a bottom sheet versus a focused screen.

#### E. Activity / download queue
Include:
- Multiple queued and active downloads.
- Queued, downloading, paused, retrying, waiting for network, failed, and completed states.
- Progress, percentage, downloaded/total, speed, ETA, source, and file name.
- Pause, resume, cancel, retry, reorder, and overflow actions.
- A calm advanced-log drawer or detail view, while keeping logs hidden from the default experience.

#### F. Download detail
Include:
- File hero area, progress, destination, speed, ETA, retry history, and a human-readable error area.
- Open, play, share, copy link, open folder, rename, retry, and delete actions as appropriate.
- Clear handling of partial and resumable files.

#### G. Library / downloaded files
Include:
- Video, audio, PDF, archive, and unknown-file treatments.
- Search, sort, filter, multi-select, rename, share, open, and delete.
- Useful storage summary.
- Empty, inaccessible-file, and permission-lost states.

#### H. Search and result selection
Design search results for supported movie/series/video/song queries:
- Result cards with title, year, type, audio language, source label, and restrained thumbnail treatment.
- A clear transition from result to format/action selection.
- No fake streaming-service imitation or false availability promises.

#### I. Streaming and direct-link handoff
Include:
- Instant playback state.
- Handoff to VLC or another installed player.
- Copy-link success state.
- A non-technical warning only when cookies/headers or another player is required.
- A safe way to return to the app without losing the active task.

#### J. Settings
Group settings into:
- Appearance and theme.
- Download folder and file naming.
- Default quality and audio format.
- Retry, connection, and acceleration preferences.
- Player preference.
- Clipboard, notifications, and background behavior.
- Cookies/authentication and Telegram configuration.
- About, legal note, diagnostics, and local-data controls.

Use progressive disclosure for advanced settings. Describe hidden/reveal controls for credentials and make it clear that secrets never appear in visible logs.

#### K. Error, offline, and recovery states
Give UI and sample copy for:
- Invalid URL.
- No internet / reconnecting.
- Unsupported link.
- Private or authentication-required content.
- Rate-limited or temporarily unavailable source.
- Permission denied.
- Not enough storage.
- Interrupted but resumable download.
- Extraction succeeded but no downloadable stream was found.

Each state must say what the user can do next and prefer non-destructive recovery.

### 5. Flows
Describe these flows in short numbered steps:

1. Paste URL → analyze → choose quality → download → completed file.
2. Search → choose result → choose quality/action → download or stream.
3. Failed download → retry/resume.
4. Copy direct link → hand off to ADM/1DM.
5. Configure Telegram or cookie-based access without exposing secrets.

### 6. Motion and interaction
Describe launch, analyze, progress, completion, pause, retry, network recovery, swipe actions, long press, haptics, sound, and reduced-motion behavior. Keep animations useful and battery-conscious.

### 7. Microcopy
Provide short, friendly, sentence-case examples for the home screen, paste confirmation, analyze state, quality selection, download, pause, resume, retry, completion, copy-link confirmation, empty states, permissions, and errors. Avoid terminal language and vague messages like “Something went wrong.”

### 8. Accessibility and responsive behavior
Cover contrast, text scaling, TalkBack labels, focus order, status announcements, color-blind-safe status design, minimum touch targets, reduced motion, small phones, large phones, tablets/foldables, landscape, gesture navigation, edge-to-edge safe areas, and one-handed reach.

### 9. Final handoff
End with:
- A concise screen inventory.
- Five non-negotiable UI principles.
- A short list of visual mistakes to avoid.

## Final output rules

Your entire response must be a **UI/UX design brief for the Universal Downloader Android APK**.

Do not output:

- A README.
- A GitHub-style document for another app.
- A local file-sharing design.
- Code of any kind.
- Backend, API, database, or CLI implementation details.
- Installation instructions.
- Benchmark claims.
- A security audit.

Begin your response with exactly:

**Universal Downloader — Premium Android UI/UX Specification**
