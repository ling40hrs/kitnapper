# Logging Out — Tokens & Credentials Runbook

> How to remove **every** stored GitHub credential from the Windows build machine (`C:\Users\CIIT`). Written 2026-11-14 after the setup in `decisions.md` §6; paths and tool names are machine-specific.

## 1. What is stored where

| Credential | Stored in | Created by |
| :--- | :--- | :--- |
| GitHub CLI token (`ling40hrs`) | Windows keyring | `gh auth login` |
| Git HTTPS push credential (`git:https://github.com`) | Windows Credential Manager | Git Credential Manager during first push / sign-in |
| SSH signing key | `C:\Users\CIIT\.ssh\id_ed25519` + `.pub`, and registered on the `ling40hrs` GitHub account as a **signing key** | `ssh-keygen` + `gh ssh-key add` |
| Repo-local signing config | `desktop-app` → actually the repo root `.git/config` (`commit.gpgsign`, `gpg.format`, `user.signingkey`) | `git config` |

The commit template (`C:\Users\CIIT\.gitmessage-kitnapper`) is **not** a credential — it holds no secrets and can stay.

## 2. Log out, in order

**Order matters:** remove the GitHub-side SSH key first, because it needs the `gh` token.

### 2.1 Remove the signing key from GitHub (while still logged in)

```powershell
& "C:\Program Files\GitHub CLI\gh.exe" ssh-key list
& "C:\Program Files\GitHub CLI\gh.exe" ssh-key delete <id>   # id from the list
```

Or by hand: github.com → Settings → SSH and GPG keys → **Signing keys** → delete "KitNapper build machine".

### 2.2 Log out of GitHub CLI

```powershell
& "C:\Program Files\GitHub CLI\gh.exe" auth logout --hostname github.com
& "C:\Program Files\GitHub CLI\gh.exe" auth status        # must report "not logged in"
```

This clears the keyring token.

### 2.3 Delete the Git HTTPS credential

```powershell
cmdkey /list | Select-String "git"
cmdkey /delete:"LegacyGeneric:target=git:https://github.com"
```

The next push will prompt for a sign-in again.

### 2.4 Delete the local SSH keypair

```powershell
Remove-Item "$env:USERPROFILE\.ssh\id_ed25519", "$env:USERPROFILE\.ssh\id_ed25519.pub"
```

### 2.5 Unset the repo's signing config

Without the key file, signed commits would fail:

```powershell
git config --unset commit.gpgsign
git config --unset gpg.format
git config --unset user.signingkey
```

### 2.6 Sign out of the browser

Sign out of github.com in the browser used for the device-flow authorizations (github.com/login/device sessions stay alive otherwise).

## 3. Logging back in

1. `gh auth login --hostname github.com --git-protocol https --web` (device flow; sign in as `ling40hrs`)
2. Regenerate the key if it was deleted: `ssh-keygen -t ed25519 -f "$env:USERPROFILE\.ssh\id_ed25519" -N '""'`
3. Re-upload as a **signing key**: `gh ssh-key add "$env:USERPROFILE\.ssh\id_ed25519.pub" --title "KitNapper build machine" --type signing`
4. Restore the repo config: `commit.gpgsign=true`, `gpg.format=ssh`, `user.signingkey=<the .pub path>` (values in `decisions.md` §6)
5. Push once to let Git Credential Manager store the HTTPS credential again.

## 4. Verification

After a full logout, all of these must fail or be absent:

- `gh auth status` → "not logged in"
- `cmdkey /list | Select-String "git"` → no `git:https://github.com` entry
- `Test-Path "$env:USERPROFILE\.ssh\id_ed25519"` → `False`
- `git push` → prompts for authentication instead of pushing silently
