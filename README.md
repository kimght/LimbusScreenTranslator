<div align="center">
  <a href="https://github.com/kimght/LimbusScreenTranslator">
    <img src="docs/assets/logo_full.webp" width="160" height="160" alt="Limbus Screen Translator logo" />
  </a>

<sub>**English** · [Русский](docs/readme/ru.md)</sub>

# Limbus Screen Translator

**Community translations, directly over _Limbus Company_.**

A movable Android overlay for reading localization packs while you play.

[![Latest release](https://img.shields.io/github/v/release/kimght/LimbusScreenTranslator?style=flat-square&label=Latest&labelColor=%23352b24&color=%23c88719)](../../releases/latest)
[![Total downloads](https://img.shields.io/github/downloads/kimght/LimbusScreenTranslator/total?style=flat-square&label=Downloads&labelColor=%23352b24&color=%23c88719)](../../releases)
[![Android 9+](https://img.shields.io/badge/Android-9%2B-c88719?style=flat-square&labelColor=%23352b24)](#installation)

**[Download the latest APK](../../releases/latest)** · [Installation](#installation) · [Usage](#usage)
</div>

<p align="center">
  <img src="docs/assets/story.webp" width="900" alt="Translated dialogue displayed over Limbus Company" />
</p>

## At a glance

- **Community localization packs** — browse translations and install the one you want.
- **Shared localization catalog** — use the same community localizations available in the desktop [Limbus Localization Manager](https://github.com/kimght/LimbusLocalizationManager).
- **Separate from the game** — translations are displayed on top of *Limbus Company*; the app does not modify its files.

## Installation

Limbus Screen Translator requires **Android 9 or newer**.

1. Download the APK from the [latest release](../../releases/latest).
2. Open the downloaded file and allow installation from this source if Android asks.
3. Install and open **Limbus Screen Translator**.

> [!WARNING]
> **Google Play Protect may warn about an APK installed outside Google Play.** Only continue if you downloaded it from this repository and trust the file. Expand **More details**, then choose **Install anyway**. The wording may differ between devices.

> [!IMPORTANT]
> Android may apply **Restricted Settings** to sideloaded apps, preventing you from enabling **Display over other apps**. If the permission is unavailable, open **Settings → Apps → Limbus Screen Translator → More → Allow restricted settings**, then try again. See [Google's Restricted Settings guide](https://support.google.com/android/answer/12623953) for details.

## Usage

### 1. Choose a localization

Open **Library**, select a source, then choose the localization pack you want to use.

> [!TIP]
> Some sources may be unavailable in certain countries. If a source cannot be reached, choose another one from the source selector in **Library**. You can add or remove sources under **Settings → Localization sources**. All sources provide the same localization packs.

### 2. Install the pack

Tap **Install** on the pack details screen. After the download finishes, tap **Set active**.

### 3. Start the overlay

Tap **Open overlay**, grant notification permission when requested, and enable **Display over other apps**. Launch *Limbus Company* when the overlay controls appear.

<table>
  <tr>
    <td align="center" width="33%">
      <strong>01 · Choose</strong><br /><br />
      <img src="docs/assets/library.webp" width="100%" alt="Localization library with available community packs" />
    </td>
    <td align="center" width="33%">
      <strong>02 · Install</strong><br /><br />
      <img src="docs/assets/localization.webp" width="100%" alt="Localization details with the Install button" />
    </td>
    <td align="center" width="33%">
      <strong>03 · Play</strong><br /><br />
      <img src="docs/assets/installed.webp" width="100%" alt="Active localization with the Open overlay button" />
    </td>
  </tr>
</table>

### Overlay controls

- Drag the overlay header to place it where it does not cover important game UI; resize the window as needed.
- Open the chapter selector and choose the episode that matches the scene you are playing; use the quick navigation to select the next or previous episode.
- Swipe to move to the next or previous dialogue line, or tap to advance to the next line.
- Minimize the overlay during gameplay.

<table>
  <tr>
    <td align="center" width="50%">
      <img src="docs/assets/chapters.webp" width="100%" alt="Chapter and episode selector displayed over the game" /><br />
      <sub><strong>Match the chapter and episode</strong></sub>
    </td>
    <td align="center" width="50%">
      <img src="docs/assets/resize.webp" width="100%" alt="Overlay resize controls displayed over the game" /><br />
      <sub><strong>Move, resize, or minimize the overlay</strong></sub>
    </td>
  </tr>
</table>

## Contributors

<a href="https://github.com/kimght/LimbusScreenTranslator/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=kimght/LimbusScreenTranslator" alt="Project contributors" />
</a>

## License

Distributed under the terms in [LICENSE](LICENSE).
