<div align="center">

<img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
<img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
<img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white"/>
<img src="https://img.shields.io/badge/Architecture-MVVM-FF6F00?style=for-the-badge"/>
<img src="https://img.shields.io/badge/License-Personal-lightgrey?style=for-the-badge"/>

<br/>
<br/>

# 🎵 Muzik Player

**A visually immersive local music player for Android — built entirely with Kotlin and Jetpack Compose.**

Muzik Player is a UI/UX exploration project focused on delivering a premium, glassmorphic audio experience with a custom theming engine, fluid player transitions, and dynamic album-art-driven accent colors.

[Features](#-features) · [Screenshots](#-screenshots) · [Tech Stack](#%EF%B8%8F-tech-stack) · [Architecture](#-architecture) · [Getting Started](#-getting-started) · [Roadmap](#-roadmap)

</div>

---

## ✨ Features

### 🎨 Liquid Glass Theming Engine
Four fully immersive visual palettes — **Sunset Gold**, **Ice Blue**, **Neon Pink**, and **Forest Green** — that dynamically repaint the entire UI, from radial background blobs to control capsules, on a single tap. No app restart required.

### 🪟 Glassmorphic UI
Translucent control capsules, action pills, and floating action buttons with precision-tuned frosted-glass borders. Each surface is crafted to blend with the ambient background without optical color-bleeding.

### 🔄 Fluid Player Transitions
Seamless, physics-aware animations between three player states:
- **Mini-player** — compact bottom bar
- **Full-screen immersive view** — album art and controls
- **Control capsule** — persistent playback strip

Transitions use Compose `AnimatedVisibility`, `animateFloatAsState`, and custom gesture-driven overscroll suppression via `LocalOverscrollConfiguration`.

### 🖼️ Dynamic Accent Colors
The UI resolves accent tints directly from the currently playing track's album artwork, dynamically tinting icons, backgrounds, and interactive elements in real time.

### 📋 Rich Playback Experience
- Swipeable card view and ambient immersive view
- One-tap access to Lyrics and Play Queue
- Favorites system with live UI accent tinting
- Interactive background style preview selectors

---

## 📸 Screenshots

> Screenshots coming soon. Clone and run the project on a physical device for the full experience.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI Toolkit | Jetpack Compose |
| Architecture | MVVM (Model-View-ViewModel) |
| Media Playback | Custom Android Service / Controller |
| Animation | `AnimatedVisibility`, `animateFloatAsState`, Compose gestures |
| Build System | Gradle (AGP 8.1.1) |
| Min SDK | Android (see `app/build.gradle`) |

---

## 🏗️ Architecture

The project follows **MVVM** with a unidirectional data flow:

```
UI Layer (Compose Screens)
        ↕  state / events
ViewModel Layer
        ↕  playback commands / media state
Media Service (Custom Controller)
        ↕  MediaStore queries
Data Layer (Local audio files)
```

- **UI layer** — Compose screens observe `StateFlow`/`LiveData` from ViewModels. All UI state (current track, theme selection, favorites) is held here.
- **ViewModel layer** — Manages player state, album art extraction, accent color resolution, and favorites persistence.
- **Media Service** — A custom Android `Service` handles audio focus, playback lifecycle, and background operation.
- **Data layer** — Audio files are sourced from the device via Android `MediaStore`.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK (API level specified in `app/build.gradle`)
- A physical Android device or emulator (physical device recommended for the full visual experience)

### Clone & Run

```bash
git clone https://github.com/rajsriv/muzik-Player.git
```

1. Open the project root in **Android Studio**.
2. Wait for Gradle to sync and download dependencies.
3. Select your target device (physical device strongly recommended for accurate glassmorphic rendering).
4. Click **Run ▶** or press `Shift + F10`.

> **Note:** The first Gradle build may take a few minutes. Subsequent builds will be significantly faster.

---

## 📁 Project Structure

```
muzik-Player/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/          # Kotlin source — ViewModels, Screens, Service
│   │       ├── res/           # Resources — drawables, values, fonts
│   │       └── AndroidManifest.xml
│   └── build.gradle           # Module-level dependencies
├── build.gradle               # Project-level build config (AGP 8.1.1 / Kotlin 1.8.20)
├── gradle.properties          # Gradle JVM args, AndroidX flag
├── settings.gradle            # Module includes
└── README.md
```

---

## 🗺️ Roadmap

- [ ] Add screenshots and screen recordings to README
- [ ] Migrate build scripts to Kotlin DSL (`.kts`)
- [ ] Upgrade to AGP 8.x latest + Kotlin 2.x
- [ ] Migrate media playback to `androidx.media3` / ExoPlayer
- [ ] Add equalizer support
- [ ] Sleep timer
- [ ] Lock screen / notification media controls
- [ ] Playlist management
- [ ] Widget support

---

## 🤝 Contributing

This project is primarily a personal UI/UX exploration. However, if you have suggestions, feel free to open an issue or submit a pull request.

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m 'Add your feature'`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request

---

## 📄 License

This project is intended for personal exploration and UI/UX experimentation. No formal open-source license is applied. Please reach out before using substantial portions of this code in other projects.

---

<div align="center">

Made with ❤️ and Jetpack Compose by [@rajsriv](https://github.com/rajsriv)

</div>
