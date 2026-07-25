# OChat branding and UI notes

OChat is a fork of [bitchat](https://github.com/permissionlesstech/bitchat) (MIT). The
networking, protocol, crypto and mesh layers are unchanged from upstream; the fork replaces
the presentation layer and the app identity.

## Supplying your own launcher icon

Three vector layers make up the adaptive icon. All three currently hold an amber "O"
placeholder and are meant to be replaced:

| File | Layer | Notes |
|------|-------|-------|
| `app/src/main/res/drawable/ic_launcher_background.xml` | Background | Flat colour or pattern. Fills the whole 108x108 viewport. |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Foreground | The mark itself, in brand colour. |
| `app/src/main/res/drawable/ic_launcher_monochrome.xml` | Themed icon (Android 13+) | Single-colour silhouette. The system recolours it, so do not bake in brand colours. |

Rules that matter:

- The viewport is **108x108 dp**. Launchers mask the icon to a circle, squircle or rounded
  square, so keep everything meaningful inside the **centre 72x72** safe zone.
- The three layers are referenced by `mipmap-anydpi-v26/ic_launcher.xml` and
  `ic_launcher_round.xml`. If you keep the same filenames, no other change is needed.
- To use raster art instead, drop `ic_launcher.png` into each `mipmap-*dpi/` folder and
  point `android:icon` in `AndroidManifest.xml` at it.

The legacy `mipmap-*dpi/ic_launcher.png` files still contain the original bitchat artwork
and should be replaced too if you ship to a store.

## Palette

Defined in `app/src/main/java/com/ochat/android/ui/theme/Color.kt`.

| Role | Dark | Light |
|------|------|-------|
| Background / chat wallpaper | `#0F0D0A` | `#F7F2EA` |
| Surface (top bar, rows, input) | `#1A1713` | `#FFFFFF` |
| Accent | `#FFA726` | `#B26A00` |
| Sent bubble | `#4A3316` | `#FFE3B8` |
| Received bubble | `#23201B` | `#FFFFFF` |
| Primary text | `#F2EDE6` | `#1B1813` |
| Muted text | `#9A8F80` | `#7A7065` |

Material's `ColorScheme` has no slots for bubbles or the chat wallpaper, so those live in
`OChatColors` and are read through `LocalOChatColors.current`. The light accent is darker
than the dark-theme accent so it keeps contrast on white.

## UI structure

- `ui/home/OChatApp.kt` - `NavHost` with two routes: `home` and `conversation/{id}`. Also
  hosts the sheets and dialogs inherited from bitchat.
- `ui/home/HomeScreen.kt` - top bar plus **Chats / Channels / People** tabs.
- `ui/home/ConversationScreen.kt` - full-screen chat. Replaces bitchat's private-chat
  bottom sheet.
- `ui/home/ConversationModels.kt` - `ConversationId` and `ConversationRow`. These are a
  **projection over existing `ChatState` flows**, not a new source of truth.
- `ui/home/MessageBubble.kt` - bubble shape, delivery ticks, system-message pill.

Back navigation is owned by the `NavHost`. Do not add an activity-level
`OnBackPressedCallback` for chat state: it runs before Navigation and will swallow the
press, ending a private chat without leaving the screen. Overlays that sit outside the nav
graph use a `BackHandler` inside `OChatApp`.

## Things deliberately left alone

Renaming any of these breaks interoperability with real bitchat clients, or destroys
existing user data:

| Value | Where | Why |
|-------|-------|-----|
| `"bitchat1:"` | `nostr/NostrEmbeddedBitChat.kt`, `NostrDirectMessageHandler.kt` | Wire prefix for Nostr DMs. Changing it breaks DMs with bitchat users. |
| `SERVICE_UUID`, `CHARACTERISTIC_UUID` | `util/AppConstants.kt` | BLE mesh identity. Package-independent, so the rename did not affect mesh interop. |
| `bitchat_identity`, `bitchat_crypto*`, `bitchat_prefs`, `bitchat_settings`, ... | SharedPreferences names | Renaming discards existing identity keys and settings. |
| `BitchatMessage`, `BitchatFilePacket`, `NostrEmbeddedBitChat` | `model/`, `nostr/` | Mirror the cross-platform spec; keeping the names makes upstream merges tractable. |

Resource **keys** in `strings.xml` were also left untouched (only the values were
rebranded) so `R.string.*` references keep resolving.

## Known gaps

- Only `values/strings.xml` was rebranded. The ~30 other `values-*` locales still say
  "bitchat" and still contain emoji.
- The old `ui/ChatScreen.kt` is no longer a screen, but is retained because it still
  provides `ChatInputSection`, which the new conversation screen reuses.

## Building

The system JDK must be 17 or 21 - Gradle 8.13 cannot run on JDK 24. If Android Studio is
installed, its bundled runtime works:

```
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug
```
