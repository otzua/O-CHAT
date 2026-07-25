# OChat

**Decentralized, off-grid messaging that looks and feels like a normal chat app.**

OChat is a fork of [bitchat-android](https://github.com/permissionlesstech/bitchat-android)
with a rebuilt interface. The networking is untouched — messages still travel peer-to-peer
over Bluetooth LE mesh with no servers, no accounts and no phone numbers — but the terminal
style interface has been replaced with a conventional messaging layout.

Made by Krish · [github.com/otzua](https://github.com/otzua)

> [!WARNING]
> This software has not received external security review and may contain vulnerabilities,
> and may not necessarily meet its stated security goals. Do not use it for sensitive
> purposes and do not rely on its security until it has been reviewed. Work in progress.

---

## Why this fork exists

bitchat is excellent software with an interface aimed at people comfortable in a terminal.
Messages render as a single scrolling log, private chats open as bottom sheets, and most
navigation happens through IRC-style slash commands.

That is a real barrier for anyone who has only ever used a mainstream messenger. OChat keeps
every capability and rearranges the presentation:

| | bitchat | OChat |
|---|---|---|
| Home screen | Single message timeline | Conversation list with Chats / Channels / People tabs |
| Private chats | Bottom sheet | Full screen with back navigation |
| Messages | One-line log entries, monospace | Left/right bubbles, sans-serif |
| Delivery status | Check characters in a text overlay | Drawn ticks inside the bubble |
| Colours | Terminal green | Amber on near-black, with a light theme |
| Slash commands | Primary interface | Still work, now with UI equivalents |

**Nothing about the protocol changed.** OChat talks to unmodified bitchat clients on Android
and iOS.

## Compatibility

Interoperability is preserved deliberately. These values were left exactly as upstream
defines them:

- The `bitchat1:` prefix used for Nostr direct messages
- The BLE service and characteristic UUIDs
- The binary packet format, Noise handshake and all crypto
- Every `SharedPreferences` file name, so an existing install keeps its identity keys

Renaming the Android package (`com.bitchat.android` → `com.ochat.android`) has no effect on
any of this — mesh identity lives in the BLE UUIDs, not the package name.

## Device requirements for mesh

Peer discovery needs at least one device in range that can **advertise** over BLE
(peripheral mode). Some phones — typically older or budget models — report
`android.hardware.bluetooth_le` support but return no advertiser, so they can only scan and
can never be discovered themselves.

If two such devices are put together, they will never find each other no matter how long you
wait. Confirmed on a Samsung Galaxy M10s (Android 11), where the log shows:

```
W/BluetoothAdapter: getBluetoothLeAdvertiser() ble not available
E/BluetoothGattServerManager: BLE advertiser not available
```

This is a hardware/firmware limitation and cannot be fixed in software. Such a device still
works fine as long as the peer it is talking to can advertise.

## Features

Inherited from bitchat and fully working:

- Decentralized BLE mesh with automatic peer discovery and multi-hop relay
- End-to-end encryption (X25519 key exchange, AES-256-GCM, Noise protocol)
- Named channels with optional password protection
- Geohash location channels over Nostr relays
- Store and forward for offline peers
- Voice notes, images and file transfer
- Tor / Arti routing
- No accounts, no phone numbers, no persistent identifiers
- Emergency wipe (triple tap) to clear all data
- IRC-style commands (`/join`, `/msg`, `/who`, `/block`, ...)

Added or changed in OChat:

- Conversation list home screen with three tabs
- Full-screen chats with message bubbles and drawn delivery ticks
- Amber dark theme plus a matching light theme
- Sans-serif typography (monospace retained for fingerprints, geohashes and debug output)
- No emoji anywhere in the interface; vector icons instead
- First-run welcome dialog

## Install

Download an APK from the [Releases page](https://github.com/otzua/O-CHAT/releases) and open
it on your device. You may need to allow installation from unknown sources under
**Settings → Security** or **Settings → Apps → Special app access**.

Pick `ochat-arm64.apk` for essentially any modern phone. Use `ochat-universal.apk` if you
are unsure.

OChat installs alongside bitchat rather than replacing it — the two use different
application IDs, so you can run both and message between them.

## Building

### Requirements

- Android Studio Ladybug or newer
- Android SDK, compile target API 36, minimum API 26 (Android 8.0)
- **JDK 17 or 21**

> [!IMPORTANT]
> Gradle 8.13 cannot run on JDK 24. If `./gradlew` fails with
> `Could not create task ':outgoingVariants'` or `Type T not present`, you are on too new a
> JDK. Android Studio ships a compatible runtime you can point at:
>
> ```bash
> export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"   # macOS
> ```

### Build

```bash
git clone https://github.com/otzua/O-CHAT.git
cd O-CHAT
./gradlew assembleDebug
```

APKs land in `app/build/outputs/apk/debug/`.

```bash
./gradlew installDebug     # install to a connected device
./gradlew testDebugUnitTest # run unit tests
./gradlew assembleRelease   # unsigned release build
```

`local.properties` is generated on first build in Android Studio. Building from the command
line on a fresh clone may require creating it:

```
sdk.dir=/Users/you/Library/Android/sdk
```

## Permissions

| Permission | Reason |
|---|---|
| Bluetooth (scan, advertise, connect) | Core mesh networking |
| Location | Required by Android for BLE scanning; also used by geohash channels |
| Notifications | Message alerts |
| Microphone | Voice notes |
| Camera / photos | Sending images |
| Network | Geohash channels and Nostr relays |

OChat does not track your location. Location permission is an Android requirement for
Bluetooth scanning.

## Usage

Tap a row in **Chats** to open a conversation, **Channels** for group and location chats, or
**People** to start a new private chat with someone nearby.

Slash commands still work if you prefer them:

| Command | Effect |
|---|---|
| `/j #channel` | Join or create a channel |
| `/m @name message` | Send a private message |
| `/w` | List who is online |
| `/block @name` | Block a user |
| `/clear` | Clear the current chat |

## Project layout

| Path | Purpose |
|---|---|
| `ui/home/` | The OChat interface: navigation, tabs, conversation list, bubbles, credits |
| `ui/` | Screens and components inherited from bitchat |
| `ui/theme/` | Palette, typography, theme |
| `mesh/` | Peer discovery, routing, BLE transport |
| `protocol/` | Wire format |
| `crypto/`, `noise/`, `identity/` | Encryption and key management |
| `nostr/`, `geohash/` | Relay integration and location channels |

See [docs/BRANDING.md](docs/BRANDING.md) for the palette, icon replacement instructions and
the full list of values that must not be renamed.

## Contributing

Issues and pull requests are welcome. Two rules specific to this fork:

1. **Do not touch the interop-critical values** listed in `docs/BRANDING.md`. Changing them
   silently breaks compatibility with bitchat, and no test will catch it.
2. **Keep UI work above the ViewModel line.** The interface is a projection over the flows
   `ChatState` already exposes; it should not introduce a second source of truth for chat
   data.

## Licence

OChat is licensed under the **GNU General Public License v3.0**. See [LICENSE.md](LICENSE.md).

This is inherited, not chosen: upstream bitchat-android is GPL-3.0, and GPL-3.0 is a
copyleft licence, so any derivative work must also be GPL-3.0 and must make its source
available.

> [!NOTE]
> Upstream's README states the project is "released into the public domain", which
> contradicts the GPL-3.0 text in its own LICENSE file and GitHub's own classification of
> the repository. This fork follows the LICENSE file, which is the more conservative and
> more defensible reading. If you intend to redistribute OChat commercially or under
> different terms, get the licence position clarified with the upstream maintainers first.

## Credits

- **[bitchat](https://github.com/permissionlesstech/bitchat-android)** by permissionlesstech
  — all of the mesh networking, protocol and cryptography, which is the hard part.
- **[bitchat iOS](https://github.com/jackjackbits/bitchat)** — the original implementation
  this protocol comes from.
- Interface rework by Krish — [github.com/otzua](https://github.com/otzua).
