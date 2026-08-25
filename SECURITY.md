# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability, please open a private issue or contact the maintainer directly.

Do not publish exploit details publicly before coordination.

## Location Data Warning

ZmanimforOllee uses your GPS location to compute zmanim and requests it directly from the
Hebcal API. Location is sent only to Hebcal for time calculation — it is not stored,
logged, or transmitted anywhere else.

## Bluetooth Warning

The Ollee BLE protocol is unauthenticated and unencrypted at the application layer (standard
Nordic UART over BLE). Anyone within radio range with a paired connection to your watch can
read/write the same characteristics. Don't rely on it for anything sensitive.
