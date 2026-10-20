// Zmanim for Ollee: a thin Forge display face. All the real work - Hebcal
// polling, next-zman scheduling, Shabbat/Yom Tov mode - happens in the
// phone app; this script has no network access (Forge scripts can't reach
// one) and does no zmanim math of its own. It just shows whatever text it's
// sent over BLE, and chimes.
//
// Message format written to RX (see forge_emulator's examples/bleEcho.js for
// the BLE service/characteristic UUIDs):
//   byte 0:   '1' for an urgent alert (candle lighting / Havdalah), '0' for
//             a routine update (next zman name or countdown).
//   byte 1-6: text to show on the clock digits. Ollee.drawMain keeps the
//             first 6 characters and right-pads/truncates automatically, so
//             the phone app should pre-format this (e.g. a time like
//             "4:12P") rather than relying on the watch to do it.
//
// Background subscription (Ollee.subscribe(E_BLE_RX) in init()) means a
// message still updates the display and chimes even while another face is
// on screen - the same pattern as forge_emulator's examples/bleInbox.js.
// An urgent alert gets three beeps instead of one, since candle lighting is
// the one reminder you can't just check your phone for once Shabbat starts
// (see forge_emulator's examples/chime.js for the beep-chain pattern this
// borrows).

var lastText = '';
var urgent = false;
var beepsLeft = 0;

const subscribe = E_A_SHORT;

function show() {
  Ollee.drawMain(lastText || 'zman  ');
  Ollee.drawLap(urgent);
}

function ring() {
  Ollee.beep();
  if (--beepsLeft > 0) Ollee.setTimeout(ring, 400);
}

// Runs at upload and every boot, so messages are received before the face
// has ever been opened.
function init() {
  Ollee.subscribe(E_BLE_RX);
}

function enter() {
  Ollee.clear();
  show();
}

// Runs for every message, on screen or not (background-subscribed above).
function ble_rx(data) {
  urgent = data.charAt(0) === '1';
  lastText = data.slice(1);
  if (Ollee.active()) show();
  beepsLeft = urgent ? 3 : 1;
  ring();
}

// A: acknowledge an urgent alert (clears the LAP indicator).
function a_short() {
  urgent = false;
  show();
}
