# Publishing OChat to GitHub

Everything in this file has to be done by you — it needs credentials that are not present
on this machine (no `gh` CLI, no SSH keys, no git credential helper).

## 1. Install and authenticate the GitHub CLI

```bash
brew install gh
gh auth login
```

Choose HTTPS and let it configure git credentials when prompted.

## 2. Create the repository and push

```bash
cd /Users/otzua/CODE/O-CHAT
gh repo create O-CHAT --public --source=. --remote=origin --push
```

Use `--private` instead of `--public` if you would rather keep it closed for now.

> [!IMPORTANT]
> OChat is GPL-3.0 (inherited from upstream — see the Licence section of the README).
> GPL-3.0 requires that anyone you distribute a build to can obtain the corresponding
> source. Publishing APKs from a **private** repo does not satisfy that. If you release
> builds publicly, the source must be public too.

The local branch is `main`. Verify with `git branch --show-current` before pushing.

## 3. Repository "About" section

Set these in the repo sidebar (**About → gear icon**), or from the CLI:

```bash
gh repo edit --description "Off-grid Bluetooth mesh messaging with a familiar chat interface. A fork of bitchat with a rebuilt UI." \
             --homepage "" \
             --add-topic android --add-topic kotlin --add-topic jetpack-compose \
             --add-topic bluetooth-le --add-topic mesh-networking --add-topic messaging \
             --add-topic decentralized --add-topic end-to-end-encryption --add-topic bitchat
```

Suggested description (under GitHub's 350-character limit):

> Off-grid Bluetooth mesh messaging with a familiar chat interface. A fork of bitchat with a
> rebuilt UI — same protocol, so it still talks to bitchat clients.

## 4. Cutting a release

The workflow in `.github/workflows/release.yml` builds and publishes automatically when you
push a tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

It produces `ochat-arm64.apk`, `ochat-x86_64.apk` and `ochat-universal.apk`, and creates a
GitHub Release with a populated body.

### Signing (already configured)

CI signs every release APK automatically. The keystore lives at
`~/ochat-signing/ochat-release.jks` and these repository secrets are already set:
`SIGNING_KEY`, `ALIAS`, `KEY_STORE_PASSWORD`, `KEY_PASSWORD`.

Certificate SHA-256 fingerprint:

```
87:E2:6E:CC:64:F3:19:DB:6C:FB:03:48:30:02:4F:00:04:6C:63:BB:E1:53:84:52:88:4F:96:3F:80:55:C8:8F
```

The workflow zipaligns, signs with v1+v2+v3 schemes, and runs `apksigner verify` before
upload, so a signing failure fails the build rather than publishing a broken artifact.

> [!WARNING]
> Keep `ochat-release.jks` and its passwords backed up somewhere safe and **never commit
> them**. Losing the keystore means you can never ship an update to anyone who installed a
> build signed with it.

## 5. Remaining tasks

- [ ] Replace the placeholder launcher icon — see [BRANDING.md](BRANDING.md)
- [x] Test on a real device
- [x] Verify messaging against a real bitchat client — done, both directions, with each
      side validating the other's signed announce
- [ ] Back up `~/ochat-signing/ochat-release.jks` and its password off-machine. Losing it
      means never being able to ship an update to anyone who installed a signed build.
- [ ] Delete `~/ochat-signing/.pw` once the password is in a password manager

## What has not been done

- The ~30 non-English locales (`values-*/strings.xml`) still say "bitchat" and still contain
  emoji. Only `values/strings.xml` was rebranded.
- The legacy raster launcher icons in `mipmap-*dpi/ic_launcher.png` still contain the
  original bitchat artwork.
- Only the debug build has been exercised on hardware. The release build additionally runs
  R8/minification, which has not been tested.
- Media (images, voice notes, files) and private chats have not been tested against a real
  bitchat client — only the public mesh timeline.
