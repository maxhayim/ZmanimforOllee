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

## Open questions

- Fork Super FreeOllee (reuse BLE stack, connection manager, existing cycling UI pattern) vs.
  standalone app against the documented protocol directly?
- Support multiple halachic opinions (GRA / MGA / Baal HaTanya), or default to one and make it
  configurable later?
- How far in advance should the candle-lighting countdown start appearing on the nameplate
  (currently thinking last 30–60 minutes)?
