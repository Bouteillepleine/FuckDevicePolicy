# DuckPolicy

[![CI](https://github.com/Bouteillepleine/FuckDevicePolicy/actions/workflows/build.yml/badge.svg)](https://github.com/Bouteillepleine/FuckDevicePolicy/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/Bouteillepleine/FuckDevicePolicy)](https://github.com/Bouteillepleine/FuckDevicePolicy/releases)
[![Downloads](https://img.shields.io/github/downloads/Bouteillepleine/FuckDevicePolicy/total)](https://github.com/Bouteillepleine/FuckDevicePolicy/releases)

An LSPosed / Xposed module that makes apps see **no device-policy restrictions**
on your own device. It hooks `DevicePolicyManager` and `UserManager` restriction
*checks* and returns the "no restriction" answer, per category, with a master
toggle.

> Fork and full rewrite of [liyafe1997/FuckDevicePolicy](https://github.com/liyafe1997/FuckDevicePolicy).
> The original neutralises `UserManager` policies (e.g. work-profile / Intune
> device-wide restrictions); this v3 rewrite generalises that into a per-category
> UI across `DevicePolicyManager` **and** `UserManager`.

## Features

- **Master toggle** + **per-category** switches, with **All / None** quick actions.
- Covers, per category: camera & screen-capture blocks, device-admin / owner /
  managed-profile checks, the full password/PIN policy set, keyguard feature
  limits, storage-encryption enforcement, managed app configuration, **user
  restrictions (`DISALLOW_*`, incl. `UserManager.hasUserRestriction`)**,
  auto-lock timeout, kiosk / lock-task, permitted IME & accessibility allow-lists,
  and assorted smaller restrictions.
- **Material You** dynamic colours (Android 12+) and an adaptive icon.
- Small: R8 + resource shrinking, ~1.8 MB.

## Install & scope

1. Install the APK from [Releases](https://github.com/Bouteillepleine/FuckDevicePolicy/releases)
   and enable **DuckPolicy** in LSPosed.
2. Set the module **scope**:
   - **System Framework** (`android`) for the broadest, system-wide effect — this
     is the recommended default for work-profile / user-restriction cases.
   - or specific target app(s) whose policy view you want to change.
3. Reboot (or force-stop) the scoped processes to apply.

   <img width="424" height="924" alt="uix_duckmypolicy" src="https://github.com/user-attachments/assets/c2c223e4-3e91-42a6-a740-b9ae99703760" />

> [!IMPORTANT]
> **Do not scope the MDM app itself** (e.g. Microsoft Intune / Company Portal) —
> hooking it can expose Xposed/root to detection.
>
> Because settings are shared cross-process via `XSharedPreferences`, the module
> should be **enabled (and the device rebooted once)** before you rely on the
> toggles — otherwise a freshly-installed, not-yet-hooked app can't read them.

To see which restrictions are actually applied on your device:
`adb shell dumpsys device_policy` (look under `userRestrictions:`).

## How it works (and its limits)

The hooks patch the **client-side** `DevicePolicyManager` / `UserManager` wrappers
inside each scoped process, so they change what an app (or the framework) *sees*
when it queries policy. They do **not** rewrite what `system_server` actually
*enforces* underneath. Intended for your own device.

## Build

```bash
./gradlew :app:assembleDebug      # debug
./gradlew :app:assembleRelease    # signed release (R8 + shrink)
```
Requires JDK 17. The Xposed API is pulled `compileOnly` from `api.xposed.info`.

### Release signing
The project ships a signing key (`app/duckpolicy.jks`) that is **intentionally not
a secret** — like an Android debug key. This lets CI and local builds produce a
consistently-signed, installable APK with zero secret setup, and lets users update
in place across versions. It is not a Play Store identity. Pushing a `v*` tag runs


`.github/workflows/release.yml`, which builds and publishes the signed APK.

## Credits

- Original module and concept: [liyafe1997/FuckDevicePolicy](https://github.com/liyafe1997/FuckDevicePolicy).
- See [LICENSE](LICENSE).
