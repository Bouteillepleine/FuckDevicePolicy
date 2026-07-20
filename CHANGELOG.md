# Changelog

## 3.1
- **New category: Outlook enrollment gate.** Hooks Outlook's own
  `olmcore.managers.mdm.DevicePolicy` (`requiresDeviceManagement` → false,
  `isPolicyApplied` → true) so the app never shows the "your organization
  requires device management" enrollment screen. This is a different layer
  than the DPM/UserManager rows above — it's Outlook's private compliance
  gate, not a framework or Intune-MAM-SDK check — so it only installs when
  the module is scoped into `com.microsoft.office.outlook`, added to the
  default scope suggestion.
- Note: this does not touch Intune MAM app-protection restrictions
  (screenshot block, copy/paste, PIN) inside Outlook — those are enforced
  independently via `com.microsoft.intune.mam` and need a separate hook.

## 3.0
Full rewrite (Kotlin).

- **Toggles actually work.** v2 read the setting from the *hooked app's* prefs via
  `ActivityThread.currentApplication()`, so it always defaulted to on. State is
  now shared cross-process with `XSharedPreferences`.
- **Master + per-category** toggles with All / None quick actions, replacing the
  single all-or-nothing switch.
- **Much broader coverage** — from 8 methods to ~35 across `DevicePolicyManager`
  and `UserManager` (incl. `UserManager.hasUserRestriction`, the full password
  policy set, lock-task, permitted IME/accessibility, and more), via a
  data-driven hook table with per-hook error isolation.
- **Material You** UI with an adaptive icon; edge-to-edge inset handling.
- Hook once per process; removed unused storage permissions; `targetSdk 35`;
  R8 + resource shrinking (~1.8 MB).
- Clean Gradle build + CI (`build.yml` for checks, `release.yml` for signed
  releases), replacing the workflow that regenerated the project inline.
