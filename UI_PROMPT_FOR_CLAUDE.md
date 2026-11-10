# Copy/paste prompt for Claude — Universal Downloader Android UI only

You are a world-class mobile product designer and Android UI/UX art director. Design **only the UI/UX** for a premium Android APK companion to the existing **Universal Downloader** CLI. Do not write code.

## Product context

The repository currently contains a Python CLI named `dl` / `dl.py`. The Android app will eventually become a polished visual frontend for the same downloader engine; the backend and CLI integration will be implemented separately.

The product can:

- Accept a pasted URL, typed URL, or a movie/video/song search query.
- Download direct files and supported web video/audio links.
- Work with YouTube and other video platforms through an extractor, direct-file links, Telegram media links, educational PDF sources such as SelfStudys, StudiesToday, and Studocu, and supported movie/series stream sources such as NetMirror.
- Extract and download the best available quality or let the user choose 4K, 2K, 1080p, 720p, 480p, 360p, 240p, 144p, or audio-only formats such as MP3/M4A.
- Offer these actions when applicable: download, copy a direct stream link for ADM/1DM, stream instantly in VLC, or stream while downloading.
- Support resumable downloads, retries, network recovery, multi-connection acceleration, progress, speed, ETA, and background-style queue monitoring.
- Save to the Android Downloads folder, allow a custom destination, copy/share links, and expose downloaded files.
- Optionally use Telegram credentials/session cookies and cookies for authenticated sources. These must be treated as sensitive settings.

The app is for young, mobile-first, Gen Z users who want downloading to feel fast, clear, expressive, and trustworthy—not like a terminal utility.

## Main design goal

Create a **premium, modern, Gen Z-friendly downloader experience** that feels at home beside the best 2025–2026 Android apps. Make it visually memorable without becoming noisy, childish, or hard to use. It should feel fast before the download even starts.

Use a dark-first, edge-to-edge visual direction with a refined expressive accent system. You may explore subtle glass/blur surfaces, tonal depth, soft gradients, expressive typography, animated progress, chips, bottom sheets, and tactile micro-interactions, but avoid generic neon cyberpunk, excessive glassmorphism, bloated dashboards, or inaccessible low-contrast text. The UI should still look excellent in light theme and on mid-range Android phones.

Use contemporary Android patterns such as Material 3 / expressive components, adaptive layouts, large touch targets, dynamic color inspiration, system bars, predictive back, and clear permission flows. Do not blindly copy any named app or brand.

## Important constraints

1. **Return design direction and UI/UX specifications only.** Do not provide Kotlin, Jetpack Compose, XML, Flutter, React Native, API, database, downloader, or backend code.
2. Do not explain how to implement the CLI or connect to services. Assume a separate engineering team will do that later.
3. Do not invent unsupported downloader behavior. If a future-facing screen is useful, label it as a UI shell or placeholder and keep its behavior consistent with the product context above.
4. Keep the product legal and responsible: include a concise concept for an in-app note about downloading content the user is allowed to save, without making it the visual focus.
5. Avoid requiring account creation for the core download flow.
6. Design for one-handed use first. The primary action should be reachable near the lower half of the screen.
7. Every important action needs loading, success, empty, offline, permission-denied, unsupported-link, authentication-required, failed, paused, retrying, and completed states where relevant.
8. Do not use fake metrics, fake testimonials, or social-feed features.

## Please deliver the following, in this exact order

### 1. Product and visual concept
- Product name treatment and a short tagline.
- Three possible visual directions, then choose one recommended direction.
- A concise explanation of why the recommended direction fits Gen Z users while remaining premium and usable.
- Brand personality in 5–8 adjectives.

### 2. Design system
Specify, in plain language:
- Color roles for dark and light themes: background, elevated surface, primary accent, secondary accent, text, muted text, success, warning, error, paused, offline, and download progress.
- Suggested color values or ranges with enough contrast for readable text. Avoid using color alone to communicate status.
- Typography hierarchy: display, screen title, section title, body, label, metadata, monospace/log text. Suggest a modern Android-safe type pairing or style.
- Spacing rhythm, corner-radius family, elevation/blur treatment, icon style, button hierarchy, chips, cards, dividers, and touch target guidance.
- How gradients, grain, glow, or blur should be used sparingly so the UI does not look cheap.
- Light theme and dark theme behavior.

### 3. Information architecture and navigation
Define the simplest navigation model for the app. Recommend whether it should use a bottom navigation bar, a compact tab system, a floating action button, or another pattern. Include the purpose of each top-level destination. At minimum consider:
- Home / New download
- Activity / Queue
- Library / Files
- Search or Discover, if it genuinely improves the product
- Settings

Explain what belongs in a modal bottom sheet versus a full screen, and how a user returns to the main task without losing progress.

### 4. Screen-by-screen UI specification
For each screen, describe layout from top to bottom, hierarchy, components, copy, primary/secondary actions, gestures, and important states. Include these screens:

#### A. First launch and permissions
- Minimal welcome moment.
- Storage/media permission rationale only when necessary.
- Optional notifications/background download permission.
- Privacy-respecting explanation of cookies and Telegram credentials.
- Skip paths and the first successful download path.

#### B. Home / New download
- Paste-from-clipboard affordance that does not feel intrusive.
- URL/search input and submit action.
- Recent links or quick actions without clutter.
- A premium empty state for a first-time user.
- A clear way to select download folder and open settings.

#### C. Link analysis / media preview
- Extracting state with meaningful progress feedback.
- Content thumbnail or neutral placeholder, title, source/domain, duration, size estimate, and available media type.
- Handling a raw direct file versus a video, audio, PDF, Telegram post, or movie/series result.
- Trust cues showing what is happening without exposing terminal noise.

#### D. Format and action selection
- Quality selector for 4K, 2K, 1080p, 720p, 480p, 360p, 240p, 144p, and audio-only where available.
- Download, stream, stream-and-download, and copy-link actions where supported.
- Explain when a high-quality video needs video/audio merging, without technical overload.
- Show file type, resolution, estimated size, and audio language when relevant.
- Design the interaction as a bottom sheet or focused selection screen and explain the choice.

#### E. Download queue / activity
- Multiple downloads, queued, active, paused, retrying, waiting for network, failed, and completed states.
- Progress bar/ring with percent, downloaded/total, speed, ETA, source, and file name.
- Pause, resume, cancel, retry, move priority, and overflow actions.
- A calm, readable way to expose detailed process logs for advanced users without making logs the default experience.

#### F. Download detail
- Hero file information, progress, destination, action buttons, speed/ETA, retry history, and safe error explanation.
- Copy/share/open-in-player/open-folder actions.
- Handling incomplete files and resumed downloads.

#### G. Library / downloaded files
- File cards or list rows for video, audio, PDF, archive, and unknown types.
- Search, sort, filter, multi-select, rename, share, delete, open, and reveal location.
- Storage summary that is useful but not overwhelming.
- Empty, permission-lost, and inaccessible-file states.

#### H. Search and result selection
- Search UI for movie/series/video queries only if it fits the IA.
- Clean result cards with title, year, type, audio language, provider/source label, and thumbnail treatment.
- Quality/action selection after a result is chosen.
- No misleading streaming-service imitation and no fake availability promises.

#### I. Streaming and direct-link handoff
- Instant playback state, VLC/installed-player handoff, copied-link confirmation, headers/cookies warning only when necessary, and fallback if no compatible player exists.
- Explain how the user gets back to the downloader without losing an active task.

#### J. Settings
Organize settings into focused groups:
- Appearance and theme
- Download location and file naming
- Default quality and audio format
- Connection/acceleration and retry preferences
- Player preference
- Clipboard behavior and notifications
- Cookies/authentication, Telegram configuration, and sensitive-data visibility
- About, legal note, diagnostics, and clear local data

Use progressive disclosure for advanced settings. Include a safe reveal/hide pattern for secrets and a clear “never show credentials in logs” rule in the UI copy.

#### K. Errors, offline, and unsupported links
Design polished states for:
- No internet / reconnecting
- Link unsupported
- Private or authentication-required content
- Service rate-limited or temporarily unavailable
- Permission denied
- Not enough storage
- Download interrupted and resumable
- Extraction succeeded but no downloadable stream was found
- Invalid URL

For each, provide empathetic copy, what the user can do next, and the least destructive recovery action.

### 5. Motion and interaction language
Describe:
- Launch and transition motion.
- Paste/analyze feedback.
- Progress animation that remains useful and battery-conscious.
- Success, pause, retry, network recovery, and failure motion.
- Haptics and sound guidance, including reduced-motion behavior.
- Swipe actions and long-press behavior.

### 6. Content and microcopy
Provide concise example copy for:
- Home headline, input placeholder, CTA, and paste confirmation.
- Analyze, quality selection, download, pause, resume, retry, completed, copied, opened, and failed states.
- Empty states and permission prompts.
- Error messages that are human, specific, and not terminal-like.

Use sentence case. Avoid vague phrases such as “Something went wrong” when a more useful message is possible.

### 7. Accessibility and device adaptation
Cover:
- Contrast, text scaling, TalkBack labels, focus order, semantic status announcements, color-blind-safe status treatment, minimum touch targets, and reduced motion.
- Small Android phones, large phones, tablets/foldables, portrait and landscape.
- Gesture navigation and edge-to-edge safe areas.
- One-handed use and thumb reach.

### 8. Final handoff summary
End with:
- A concise screen inventory.
- The recommended visual direction in one paragraph.
- The five most important UI principles.
- A short “do not do” list of common design mistakes for this app.

Remember: this response is a **UI/UX design brief only**. Do not output source code, architecture, API contracts, implementation steps, or backend details.
