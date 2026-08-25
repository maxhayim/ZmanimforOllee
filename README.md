<p align="center">
  <img src="docs/assets/logo.png" alt="ZmanimforOllee Logo" width="200"/>
</p>

<p align="center">
  <a href="https://kotlinlang.org/">
    <img src="https://img.shields.io/badge/Kotlin-Android-blue" alt="Kotlin">
  </a>
  <a href="https://opensource.org/licenses/MIT">
    <img src="https://img.shields.io/badge/License-MIT-green" alt="License">
  </a>
  <img src="https://img.shields.io/badge/status-early%20WIP-orange" alt="Status">
</p>

# 🕯️ Zmanim for Ollee

**Zmanim for Ollee** pushes halachic times (zmanim) to an [**Ollee Watch**](https://www.olleewatch.com/) — candle lighting, Havdalah, and daily tefillah times — as a wrist complication, using the reverse-engineered Ollee BLE protocol.

No official Ollee SDK exists yet. This project builds on the community protocol work from
[FreeOllee](https://github.com/Arthur86000/FreeOllee) (Arthur86000) and
[FreeOllee-Faces / Super FreeOllee](https://github.com/kenblizzardcaron/FreeOllee-Faces) (kenblizzardcaron),
which reverse-engineered the Nordic UART BLE frame format, CRC, and command set.

Zmanim are computed from the [Hebcal](https://www.hebcal.com/) REST APIs.
No scraping, no shared credentials.

---

## What this does

- Pulls daily zmanim from the **Hebcal Zmanim API** (`hebcal.com/zmanim`) for your GPS location.
- Pulls candle lighting / Havdalah times (weekly Shabbat **and** Yom Tov) from the **Hebcal Calendar API** (`hebcal.com/hebcal`).
- Cycles the watch's 6-character nameplate between the next zman's name and its countdown/time
  (e.g. `MINCHA` → `4:12P`), the same way Super FreeOllee cycles Activity-mode metrics.
- Re-arms the watch's one-shot wall-clock alarm to whichever zman is next, so a single alarm
  slot is enough to always be "pointed at" the next relevant time.
- Gives candle lighting / Havdalah a distinct, more urgent chime — the one reminder you can't
  just check your phone for once Shabbat starts.

ZmanimforOllee focuses purely on **zmanim computation, scheduling, and nameplate/alarm formatting**.
It does not implement its own BLE stack from scratch — it reuses (or forks) the protocol
implementation already proven out by Super FreeOllee.

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

🚧 Early WIP — repo scaffolding only so far. Architecture decision pending:

- [ ] Fork Super FreeOllee and add zmanim as a new complication type, **or**
- [ ] Build a standalone app against the documented BLE protocol directly

Design notes so far live in [`docs/design-notes.md`](docs/design-notes.md).

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
* [FreeOllee](https://github.com/Arthur86000/FreeOllee) — original Ollee BLE protocol reverse engineering
* [FreeOllee-Faces / Super FreeOllee](https://github.com/kenblizzardcaron/FreeOllee-Faces) — full protocol
  documentation and reference implementation
