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

### The APKs are unsigned

The signing step in the workflow is commented out. Unsigned APKs install with a warning and
**cannot be updated in place** by a later signed build — users would have to uninstall
first. Before your first real release, generate a keystore:

```bash
keytool -genkey -v -keystore ochat-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias ochat
```

Then add these repository secrets (**Settings → Secrets and variables → Actions**):

| Secret | Value |
|---|---|
| `SIGNING_KEY` | `base64 -i ochat-release.jks` output |
| `ALIAS` | `ochat` |
| `KEY_STORE_PASSWORD` | keystore password |
| `KEY_PASSWORD` | key password |

and uncomment the "Sign APKs" step in `release.yml`.

> [!WARNING]
> Keep `ochat-release.jks` and its passwords backed up somewhere safe and **never commit
> them**. Losing the keystore means you can never ship an update to anyone who installed a
> build signed with it.

## 5. Before making the repo public

- [ ] Replace the placeholder launcher icon — see [BRANDING.md](BRANDING.md)
- [ ] Test the APK on a real device; nothing in this fork has been run yet, only compiled
- [ ] Verify messaging against a real bitchat client, which is the whole compatibility claim
- [ ] Decide whether to keep the security warning in the README (recommended: yes)
- [ ] Confirm you are comfortable with GPL-3.0 obligations

## What has not been done

- The ~30 non-English locales (`values-*/strings.xml`) still say "bitchat" and still contain
  emoji. Only `values/strings.xml` was rebranded.
- The legacy raster launcher icons in `mipmap-*dpi/ic_launcher.png` still contain the
  original bitchat artwork.
- No release has been built or tested; only `assembleDebug` has been verified locally.
