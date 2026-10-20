<p align="center">
  <img src="docs/assets/logo.png" alt="Zmanim for Ollee Logo" width="200"/>
</p>

<p align="center">
  <a href="https://github.com/olleewatch/forge_emulator">
    <img src="https://img.shields.io/badge/Ollee%20Watch-Forge-blueviolet" alt="Ollee Watch Forge">
  </a>
  <a href="https://kotlinlang.org/">
    <img src="https://img.shields.io/badge/Kotlin-Android-blue" alt="Kotlin">
  </a>
  <a href="https://opensource.org/licenses/MIT">
    <img src="https://img.shields.io/badge/License-MIT-green" alt="License">
  </a>
  <img src="https://img.shields.io/badge/status-early%20WIP-orange" alt="Status">
</p>

# 🕯️ Zmanim for Ollee

**Zmanim for Ollee** pushes halachic times (zmanim) to an [**Ollee Watch**](https://www.olleewatch.com/) — candle lighting, Havdalah, and daily tefillah times — as a custom watchface, built on the official **[Ollee Watch Forge](https://github.com/olleewatch/forge_emulator)** SDK.

Ollee Watch Forge lets you write custom watchfaces in plain JavaScript and talk to the
watch over a documented BLE GATT service — no more reverse engineering required. (It's
currently **Beta 1**: emulator support only, with on-watch deployment coming in Beta 2 —
see [Status](#status).) Earlier design work in this repo leaned on the community protocol
reverse-engineered by [FreeOllee](https://github.com/Arthur86000/FreeOllee) (Arthur86000) and
[FreeOllee-Faces / Super FreeOllee](https://github.com/kenblizzardcaron/FreeOllee-Faces)
(kenblizzardcaron); that effort is credited in Acknowledgments but is no longer the
dependency now that an official SDK exists.

Zmanim are computed from the [Hebcal](https://www.hebcal.com/) REST APIs.
No scraping, no shared credentials.

---

## What this does

- Pulls daily zmanim from the **Hebcal Zmanim API** (`hebcal.com/zmanim`) for your GPS location.
- Pulls candle lighting / Havdalah times (weekly Shabbat **and** Yom Tov) from the **Hebcal Calendar API** (`hebcal.com/hebcal`).
- A phone-side app computes the next zman and writes a short text message to the watch over
  Forge's BLE GATT service; the [`watchface/zmanim.js`](watchface/zmanim.js) Forge face just
  displays whatever it receives on the clock digits (`Ollee.drawMain`) and chimes.
- Candle lighting / Havdalah messages are flagged **urgent** and get three beeps instead of
  one — the one reminder you can't just check your phone for once Shabbat starts.
- The watchface keeps listening for messages in the background (`Ollee.subscribe(E_BLE_RX)`),
  so an urgent alert still buzzes your wrist even while another face is on screen.

Two pieces, split the way Forge intends: the **watchface** (`watchface/zmanim.js`) is a thin,
dumb display — it has no network access and does no zmanim math of its own, by design (Forge
scripts can't reach the network). The **phone app** (not yet built) does all the Hebcal
computation and scheduling, and just writes short strings to the watch.

---

## Screenshots

### App Screenshots

_Coming soon — no app build yet (see Status below)._

<!--
| Home | Settings |
|:---:|:---:|
| <img src="docs/assets/screenshots/app/home.png" width="220" alt="App home screen"> | <img src="docs/assets/screenshots/app/settings.png" width="220" alt="App settings screen"> |
-->

### Watch Screenshots

_Coming soon — no app build yet (see Status below)._

<!--
| Nameplate | Alarm |
|:---:|:---:|
| <img src="docs/assets/screenshots/watch/nameplate.png" width="220" alt="Watch nameplate cycling next zman"> | <img src="docs/assets/screenshots/watch/alarm.png" width="220" alt="Watch alarm face"> |
-->

---

## Status

🚧 Early WIP.

- [x] Forge watchface script (`watchface/zmanim.js`) — receives text + an urgent flag over BLE,
      displays it, chimes. Runs today in the [Forge emulator](https://github.com/olleewatch/forge_emulator).
- [ ] Phone app: Hebcal polling, next-zman scheduling, Shabbat/Yom Tov mode, BLE writes to the watch.
- [ ] On-watch deployment — blocked on Forge **Beta 2** (Beta 1 is emulator-only).

Design notes so far live in [`docs/design-notes.md`](docs/design-notes.md).

---

## Watchface

[`watchface/zmanim.js`](watchface/zmanim.js) is a Forge watchface: a single self-contained
JavaScript file, no phone-app code baked in. Test it by cloning
[olleewatch/forge_emulator](https://github.com/olleewatch/forge_emulator), opening `index.html`,
and using **Load file** to load `zmanim.js`.

**BLE message protocol** (phone → watch, over Forge's BLE service
`C6FB0001-A37D-40AC-89BE-887D25E1F5B3`, write to RX `C6FB0002-...`):

- Byte 0: `'1'` for an urgent alert (candle lighting / Havdalah), `'0'` for a routine update.
- Remaining bytes: text to show (`Ollee.drawMain` keeps the first 6 characters and
  right-pads/truncates automatically).

The watch display is a 7-segment LCD, not pixels — stick to characters that render cleanly on
it (digits render reliably; letters are hit-or-miss, see
[`docs/concepts/display.md`](https://github.com/olleewatch/forge_emulator/blob/main/docs/concepts/display.md)
in the Forge repo). The phone app is expected to pre-format text (e.g. a time like `4:12P`)
rather than relying on the watch to do any formatting.

---

## Configuration (planned)

- `location` — lat/lon, reused from the phone's `LocationManager` (no Google Play Services
  dependency, same as Super FreeOllee's GrapheneOS-friendly location handling).
- `calc_method` — which halachic opinion to use for zman calculation (GRA / Magen Avraham /
  Baal HaTanya — all returned by the Hebcal API in a single response).
- `candle_lighting_minutes` — minutes before sunset for candle lighting (Hebcal `b=` param,
  default 18).
- `havdalah_mode` — fixed-minutes after sundown vs. nightfall (Hebcal `m=` / `M=on`).

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

## Contributing

Pull requests are welcome. Open an issue first to discuss ideas or report bugs — see
[CONTRIBUTING.md](CONTRIBUTING.md).

---

## Acknowledgments

* [Hebcal](https://www.hebcal.com/) — Jewish calendar and zmanim data
* [Ollee Watch Forge](https://github.com/olleewatch/forge_emulator) — the official watchface SDK this project is built on
* [FreeOllee](https://github.com/Arthur86000/FreeOllee) — original Ollee BLE protocol reverse engineering (historical reference, no longer a dependency)
* [FreeOllee-Faces / Super FreeOllee](https://github.com/kenblizzardcaron/FreeOllee-Faces) — full protocol
  documentation and reference implementation (historical reference, no longer a dependency)
