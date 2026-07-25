# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

Entries below `OChat 1.0.0` are inherited from upstream
[bitchat-android](https://github.com/permissionlesstech/bitchat-android).

## [OChat 1.0.1]

### Fixed
- **BLE scanning could stop permanently, preventing all peer discovery.** Leaving a
  duty-cycled power mode cancelled the duty-cycle timer without re-enabling scanning, so
  if the cycle was in its OFF phase the radio was left deaf with nothing left to wake it.
  In POWER_SAVER the cycle is off 28 of every 30 seconds, so simply bringing the app to
  the foreground had roughly a 93% chance of killing discovery until the process
  restarted. A second fault made the "restart scanning on power mode change" check
  compare a value against itself, so it never fired.

  Verified on hardware: OChat and an unmodified bitchat client now discover each other
  and exchange signed announces successfully.

  Both faults are inherited from upstream bitchat and affect it identically. The fix
  touches radio scheduling only, not the wire protocol.
- Release signing in CI was gated on a step-level `env` value, which is not available to
  that step's own `if` expression. The condition always saw an empty value, so signing
  would have silently skipped and produced unsigned APKs that Android refuses to install.

### Known hardware limitation
- Some devices report BLE support but provide no BLE *advertiser* (peripheral mode); on
  those, `getBluetoothLeAdvertiser()` returns null and the device can only ever scan, never
  be discovered. Observed on a Samsung Galaxy M10s. Two such devices cannot find each other,
  and at least one peer in range must be able to advertise. This is a hardware/firmware
  limitation and cannot be fixed in software.

---

## [OChat 1.0.0]
First release of the OChat fork. The interface is rebuilt; the networking,
protocol and cryptography are unchanged from upstream bitchat.

### Added
- Conversation list home screen with **Chats**, **Channels** and **People** tabs
- Full-screen conversation view, replacing the private-chat bottom sheet
- Message bubbles: own messages right on amber, others left on slate
- Delivery ticks drawn as vector paths, with a distinct read state
- Centered pills for system notices
- Amber-on-near-black dark theme and a matching light theme
- First-run welcome dialog, a credit line under the conversation list, and a
  credit block in the About sheet
- `docs/BRANDING.md` documenting the palette, icon replacement and the values
  that must not be renamed

### Changed
- Package renamed `com.bitchat.android` to `com.ochat.android`; applicationId
  `com.bitchat.droid` to `com.ochat.droid`. OChat therefore installs alongside
  bitchat rather than replacing it.
- Typography from monospace to sans-serif. Monospace is retained for
  fingerprints, geohashes, debug output and the Matrix animation.
- Back navigation is owned by the NavHost
- Release artifacts renamed to `ochat-*.apk`
- Issue, discussion and PR templates point at this fork

### Removed
- All emoji from the English interface strings, replaced with vector icons
  where an icon was warranted

### Fixed
- Back press no longer ends a private chat without leaving the screen. An
  activity-level `OnBackPressedCallback` ran before Navigation and consumed the
  press, so the nav stack never popped.
- Licence corrected to GPL-3.0. It had been described as MIT in the app credits
  and branding doc, which was wrong: `LICENSE.md` is the full GPL-3.0 text.

### Unchanged (deliberately)
- The `bitchat1:` Nostr DM wire prefix, the BLE service and characteristic
  UUIDs, the binary protocol, the Noise handshake, and all SharedPreferences
  file names. OChat remains protocol-compatible with unmodified bitchat clients.

---

## [1.4.0] - 2025-10-15
### Fixed
- fix: Resolve debug settings bottom sheet crash on some devices (Issue #472)
  - Fixed IllegalFormatConversionException in DebugSettingsSheet.kt when scrolling through debug settings
  - Corrected string formatting for debug_target_fpr_fmt and debug_derived_p_fmt string resources
  - Improved string resource parameter handling for numeric values

## [0.7.2] - 2025-07-20
### Fixed
- fix: battery optimization screen content scrollable with fixed buttons

## [0.7.1] - 2025-07-19

### Added
- feat(battery): add battery optimization management for background reliability

### Fixed
- fix: center align toolbar item in ChatHeader - passed modifier.fillmaxHeight so the content inside the row can actually be centered
- fix: update sidebar text to use string resources
- fix(chat): cursor location and enhance message input with slash command styling

### Changed
- refactor: remove context attribute at ChatViewModel.kt
- Refactor: Migrate MainViewModel to use StateFlow

### Improved
- Use HorizontalDivider instead of deprecated Divider
- Use contentPadding instead of padding so items remain fully visible


and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.7]

### Added
- Location services check during app startup with educational UI
- Message text selection functionality in chat interface
- Enhanced RSSI tracking and unread message indicators
- Major Bluetooth connection architecture refactoring with dedicated managers

### Fixed
- **Critical**: Android-iOS message fragmentation compatibility issues
  - Fixed fragment size (500→150 bytes) and ID generation for cross-platform messaging
  - Ensures Android can properly communicate with iOS devices
- DirectMessage notifications and text copying functionality
- Smart routing optimizations (no relay loops, targeted delivery)
- Build system compilation issues and null pointer exceptions

### Changed
- Comprehensive dependency updates (AGP 8.10.1, Kotlin 2.2.0, Compose 2025.06.01)
- Optimized BLE scan intervals for better battery performance
- Reduced excessive logging output

### Improved
- Cross-platform compatibility with iOS and Rust implementations
- Connection stability through architectural improvements
- Battery performance via scan duty cycling
- User onboarding with location services education

## [0.6]

### Added
- Channel password management with `/pass` command for channel owners
- Monochrome/themed launcher icon for Android 12+ dynamic theming support
- Unit tests package with initial testing infrastructure
- Production build optimization with code minification and shrinking
- Native back gesture/button handling for all app views

### Fixed
- Favorite peer functionality completely restored and improved
  - Enhanced favorite system with fallback mechanism for peers without key exchange
  - Fixed UI state updates for favorite stars in both header and sidebar
  - Improved favorite persistence across app sessions
- `/w` command now displays user nicknames instead of peer IDs
- Button styling and layout improvements across the app
  - Enhanced back button positioning and styling
  - Improved private chat and channel header button layouts
  - Fixed button padding and alignment issues
- Color scheme consistency updates
  - Updated orange color throughout the app to match iOS version
  - Consistent color usage for private messages and UI elements
- App startup reliability improvements
  - Better initialization sequence handling
  - Fixed null pointer exceptions during startup
  - Enhanced error handling and logging
- Input field styling and behavior improvements
- Sidebar user interaction enhancements
- Permission explanation screen layout fixes with proper vertical padding

### Changed
- Updated GitHub organization references in project files
- Improved README documentation with updated clone URLs
- Enhanced logging throughout the application for better debugging

## [0.5.1] - 2025-07-10

### Added
- Bluetooth startup check with user prompt to enable Bluetooth if disabled

### Fixed
- Improved Bluetooth initialization reliability on first app launch

## [0.5] - 2025-07-10

### Added
- New user onboarding screen with permission explanations
- Educational content explaining why each permission is required
- Privacy assurance messaging (no tracking, no servers, local-only data)

### Fixed
- Comprehensive permission validation - ensures all required permissions are granted
- Proper Bluetooth stack initialization on first app load
- Eliminated need for manual app restart after installation
- Enhanced permission request coordination and error handling

### Changed
- Improved first-time user experience with guided setup flow

## [0.4] - 2025-07-10

### Added
- Push notifications for direct messages
- Enhanced notification system with proper click handling and grouping

### Improved
- Direct message (DM) view with better user interface
- Enhanced private messaging experience

### Known Issues
- Favorite peer functionality currently broken

## [0.3] - 2025-07-09

### Added
- Battery-aware scanning policies for improved power management
- Dynamic scan behavior based on device battery state

### Fixed
- Android-to-Android Bluetooth Low Energy connections
- Peer discovery reliability between Android devices
- Connection stability improvements

## [0.2] - 2025-07-09

### Added
- Initial Android implementation of bitchat protocol
- Bluetooth Low Energy mesh networking
- End-to-end encryption for private messages
- Channel-based messaging with password protection
- Store-and-forward message delivery
- IRC-style commands (/msg, /join, /clear, etc.)
- RSSI-based signal quality indicators

### Fixed
- Various Bluetooth handling improvements
- User interface refinements
- Connection reliability enhancements

## [0.1] - 2025-07-08

### Added
- Initial release of bitchat Android client
- Basic mesh networking functionality
- Core messaging features
- Protocol compatibility with iOS bitchat client

[Unreleased]: https://github.com/permissionlesstech/bitchat-android/compare/0.5.1...HEAD
[0.5.1]: https://github.com/permissionlesstech/bitchat-android/compare/0.5...0.5.1
[0.5]: https://github.com/permissionlesstech/bitchat-android/compare/0.4...0.5
[0.4]: https://github.com/permissionlesstech/bitchat-android/compare/0.3...0.4
[0.3]: https://github.com/permissionlesstech/bitchat-android/compare/0.2...0.3
[0.2]: https://github.com/permissionlesstech/bitchat-android/compare/0.1...0.2
[0.1]: https://github.com/permissionlesstech/bitchat-android/releases/tag/0.1
