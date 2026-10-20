# Design notes

Captured from initial planning discussion. Updated now that an official SDK —
[Ollee Watch Forge](https://github.com/olleewatch/forge_emulator) — exists. This superseded the
original plan to fork Super FreeOllee / build against the reverse-engineered BLE protocol
directly; see "Superseded: pre-Forge plan" at the bottom for that history.

A first implementation of both pieces (`watchface/zmanim.js` and the `app/` phone app) now
exists; see the README's "Watchface" and "Phone app" sections for what's actually built. The
notes below are the design reasoning behind it.

## Architecture (current)

Two pieces:

- **Watchface** (`watchface/zmanim.js`) — a Forge script running on the watch itself (or in the
  [Forge emulator](https://github.com/olleewatch/forge_emulator) for now — see "Forge SDK status"
  below). It has no network access and does no zmanim math; it just displays text it receives
  over BLE and chimes.
- **Phone app** (`app/`, Android/Kotlin, no third-party dependencies) — does the Hebcal polling,
  next-zman scheduling, Shabbat/Yom Tov mode, and writes short text messages to the watch's
  Forge BLE service. `ZmanimForegroundService` owns the loop; `ZmanSchedule` is the pure
  next-zman/currently-in-a-window logic (unit tested, no Android dependency).

This is a real simplification over the old plan: Forge's BLE channel is a documented, standard
BLE GATT service, not a reverse-engineered Nordic UART protocol. The phone app no longer needs
to implement or fork anyone else's BLE stack — any BLE GATT client works.

## Forge SDK status

- **Beta 1**: emulator support only. On-watch deployment is **Beta 2**, not yet released.
- Test `watchface/zmanim.js` today by cloning `olleewatch/forge_emulator`, opening `index.html`,
  and using **Load file**.
- A Forge script is a single JS file, max **8158 bytes** (UTF-8). No `require`, no modules, no
  network, no storage — it's one self-contained file per the SDK's own constraints.
- The watch has 3 Forge slots (`FA`/`FB`/`FC`); the emulator runs one at a time as `FA`.

## BLE protocol (official, not reverse-engineered)

- Service `C6FB0001-A37D-40AC-89BE-887D25E1F5B3`.
- Write to RX `C6FB0002-...` (phone → watch), up to 20 bytes per write. Triggers the watchface's
  `ble_rx(data)` hook.
- Notify on TX `C6FB0003-...` (watch → phone) via `Ollee.send(data)`, up to 256 bytes (sent as
  20-byte notification chunks). Not currently used by `zmanim.js`, but available if the watch
  ever needs to report something back (e.g. battery, or an ack).
- Message format this project defines on top of that (see `watchface/zmanim.js` header comment):
  byte 0 is `'1'`/`'0'` (urgent / routine), remaining bytes are display text.

## Watch display

The Ollee Watch is a **7-segment LCD, not pixels**. Per Forge's `docs/concepts/display.md`:

- `Ollee.drawMain(text)` — 6 characters, the main clock digits (positions 4–9).
- `Ollee.drawDay(text)` — 2 characters, the day digits (positions 2–3); the first of these two
  positions doesn't have all seven segments, so only digits `1`–`3` render reliably there.
- Both keep the first N characters and right-align by left-padding with spaces.
- Digits render reliably; letters are hit-or-miss on a 7-segment display (no official glyph
  table fetched yet — check the emulator's "Character set" panel before assuming a specific
  letter renders as expected).
- **Conclusion: the phone app should pre-format display text**, favoring digits/time strings
  (`4:12P`) over letter abbreviations (`MINCHA`) wherever possible, since the watch does no
  formatting of its own and can't be relied on to render arbitrary letters well.

## Feedback: no alarm/bell access

Correction from the pre-Forge plan: Forge scripts **cannot** set the system bell (alarm) or
chime indicators — "Those belong to the system," per the SDK docs. There's no re-arming a
watch alarm from a custom face. The only feedback a Forge face has is `Ollee.beep()` (and
`Ollee.led_on()`/`led_off()`). `zmanim.js` uses a beep-chain (borrowed from the SDK's own
`examples/chime.js` pattern): one beep for a routine update, three for an urgent one (candle
lighting / Havdalah).

## Location

`Ollee.latitude()` / `Ollee.longitude()` read the location **the paired phone already set** (3
decimal places). This is read-only from the watchface's side — still useful to know it exists,
but the phone app doing the Hebcal calls will have its own, more precise location anyway.

## Shabbat / Yom Tov mode (halachic consideration — confirm with a rav, not a ruling)

Continuously polling Hebcal and writing new BLE messages to the watch throughout Shabbat/Yom Tov
itself is live wireless activity happening *during* the day — a meaningfully different case
from a pre-set Shabbat clock/timer, where everything is decided beforehand and nothing is
triggered live. Poskim differ specifically on Bluetooth/wireless transmission, separate from
the more settled question of pre-set electronic timers, so this should be confirmed with a rav
rather than assumed either way.

Proposed design to make the live-activity question moot rather than relying on a specific
leniency:

- All polling/scheduling decisions for the upcoming Shabbat or Yom Tov are made **before** it
  starts (fetch zmanim, compute candle lighting + all downstream display content in advance).
- At candle lighting, send the final urgent message and stop — **no further BLE writes, no
  further Hebcal polling**, until Havdalah.
- At Havdalah, send the Havdalah message and resume normal polling.

This mirrors how Shabbat clocks/timers already work (all deciding happens before, nothing is
triggered live during) rather than introducing a new category of live wireless use during
Shabbat.

## Hebcal API gotcha

Both endpoints need `latitude`/`longitude` (not `lat`/`lon` — Hebcal's own docs page doesn't
spell this out, confirmed against the live API) and a `tzid`. The app sends
`ZoneId.systemDefault().id` for `tzid` — fine as long as the phone's timezone matches where the
watch actually is, which won't hold for a traveler crossing timezones, but is a reasonable
default for now.

## Resolved (were open questions)

- **Phone app platform**: Android/Kotlin, no third-party dependencies (plain BLE/location/HTTP
  platform APIs, `org.json` for parsing) — matches the house style of other maxhayim Android
  projects (e.g. ShackScreen).
- **Candle-lighting lead time**: no special threshold. The app always just shows "whatever's
  next" (`ZmanSchedule.nextUpcoming`), so candle lighting naturally starts appearing once it
  becomes the soonest upcoming time — no separate "30–60 minutes before" logic needed.
- **Poll interval**: 5 minutes, re-checked against `OffsetDateTime.now()` each time (not a fixed
  wall-clock schedule) via a self-rescheduling `Handler.postDelayed` loop — no coroutines
  dependency.

## Open questions

- Support multiple halachic opinions (GRA / MGA / Baal HaTanya, all in the same Hebcal zmanim
  response) — currently hardcoded to GRA's fields; `HebcalClient.DAILY_ZMANIM` would need to
  become configurable.
- What should `Ollee.send()` (watch → phone) be used for, if anything — an ack? A "face is open"
  signal so the phone only pushes routine updates while someone's actually looking?
- No settings UI yet — location override and candle-lighting minutes are `SharedPreferences`
  only (`Prefs.kt`), set by nothing (no screen writes them yet).
- Timeline is gated on Forge Beta 2 for actual on-watch deployment; the phone app builds and
  passes its unit tests but has never talked to a real watch, only the protocol docs.
- If "today" rolls over to tomorrow with nothing left in `nextUpcoming` (e.g. late at night,
  after tonight's last zman), the app doesn't fall back to fetching tomorrow's date until the
  next poll naturally picks up the new `LocalDate` — a few hours with a stale display overnight,
  not addressed yet.

## Superseded: pre-Forge plan (kept for history)

Before the Forge SDK existed, the plan was to fork Super FreeOllee and use its reverse-engineered
BLE protocol: writing to the watch's "Name tag" field (nameplate), and re-arming a one-shot
wall-clock alarm for the next zman. Both ideas are superseded:

- The nameplate hack is replaced by `Ollee.drawMain`/`drawDay` — an official, documented display
  API instead of writing to a repurposed field.
- The alarm re-arming idea doesn't work under Forge at all — custom faces can't touch the system
  alarm/bell. Feedback is `Ollee.beep()` only (see "Feedback" above).
