# Design notes

Captured from initial planning discussion. Not final — revisit once the architecture
decision (fork Super FreeOllee vs. standalone) is made.

## Data sources

- `https://www.hebcal.com/zmanim?cfg=json&lat=<lat>&lon=<lon>` — daily zmanim. No API key.
  Returns multiple opinions in one response (`sofZmanShma` / `sofZmanShmaMGA` / `...BaalHatanya`,
  etc.) — worth letting the user pick one rather than hardcoding GRA.
- `https://www.hebcal.com/hebcal?cfg=json&latitude=<lat>&longitude=<lon>&c=on&b=18` — candle
  lighting / Havdalah, covers both weekly Shabbat and Yom Tov. `b=` sets candle-lighting minutes
  before sunset; `M=on` (nightfall) or `m=<minutes>` (fixed) controls Havdalah.

## Nameplate (6 ASCII chars)

Cycle two frames, same pattern as Super FreeOllee's Activity-mode MODE cycling:

1. Next zman's short name (`MINCHA`, `CANDLE`, `HAVDLH`, `MAARIV`)
2. Countdown or absolute time (`42M`, `4:12P`)

## Alarm

Protocol only exposes a single persistent Alarm-face slot, but Super FreeOllee's quick-timer
already supports a one-shot wall-clock countdown (fires at an absolute H:M target, independent
of the persistent alarm record). Re-arm this to the next zman every time one fires — avoids
needing multiple alarm slots since there's only ever one "next" zman.

Candle lighting / Havdalah should get a distinct chime pattern (different `playNow` byte /
alarm record) since it's the highest-stakes reminder — the one time people can't just check
their phone once Shabbat has started.

## Shabbat / Yom Tov mode (halachic consideration — confirm with a rav, not a ruling)

Continuously polling Hebcal and pushing new BLE writes to the watch throughout Shabbat/Yom Tov
itself is live wireless activity happening *during* the day — a meaningfully different case
from a pre-set Shabbat clock/timer, where everything is decided beforehand and nothing is
triggered live. Poskim differ specifically on Bluetooth/wireless transmission, separate from
the more settled question of pre-set electronic timers, so this should be confirmed with a rav
rather than assumed either way.

Proposed design to make the live-activity question moot rather than relying on a specific
leniency:

- All polling/scheduling decisions for the upcoming Shabbat or Yom Tov are made **before** it
  starts (fetch zmanim, compute candle lighting + all downstream alarm/nameplate content in
  advance).
- At candle lighting, push the final nameplate content and freeze — **no further BLE writes,
  no further Hebcal polling**, until Havdalah.
- At Havdalah, push the Havdalah time/notification and resume normal polling.

This mirrors how Shabbat clocks/timers already work (all deciding happens before, nothing is
triggered live during) rather than introducing a new category of live wireless use during
Shabbat.

## Open questions

- Fork Super FreeOllee (reuse BLE stack, connection manager, existing cycling UI pattern) vs.
  standalone app against the documented protocol directly?
- Support multiple halachic opinions (GRA / MGA / Baal HaTanya), or default to one and make it
  configurable later?
- How far in advance should the candle-lighting countdown start appearing on the nameplate
  (currently thinking last 30–60 minutes)?
