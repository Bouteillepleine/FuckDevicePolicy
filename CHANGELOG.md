# Changelog

## 4.0
Migrated to the **modern Xposed API (libxposed)**. The APK is now a modern module,
not a legacy one — the two packagings are mutually exclusive, because the framework
picks the entry point from what the APK declares.

- **Requires a framework implementing API 101+** (`minApiVersion=101`,
  `targetApiVersion=102`) — the LSPosed 2.x forks. Mainline LSPosed 1.9.x will not
  load this build; 3.1 remains the last legacy release.
- Entry points are now declared in `META-INF/xposed/` (`module.prop`,
  `java_init.list`, `scope.list`) instead of `assets/xposed_init` plus `xposed*`
  manifest meta-data and `@array/xposed_scope`. The module description the manager
  shows comes from `android:description`.
- `IXposedHookLoadPackage.handleLoadPackage` is replaced by the two entry points the
  modern API separates: `onSystemServerStarting` (System Framework) and
  `onPackageReady` (each scoped app). Outlook's own private `DevicePolicy` rows are
  tagged with their package and installed only in Outlook's process, instead of being
  attempted and silently failing everywhere else.
- `XC_MethodHook`'s before/after pair is replaced by a single `Hooker.intercept`;
  returning without `chain.proceed()` is what `param.result = ...` used to mean.
  `XposedHelpers` is gone: classes resolve through `Class.forName` and methods through
  an explicit superclass walk that reproduces `findAndHookMethod`.
- **Settings moved off `XSharedPreferences`.** They now live in the framework's remote
  preferences, which system_server can read and which the framework pushes on change —
  so toggles apply live and no `reload()` is needed in the hook. The UI falls back to a
  local file when no framework is bound, and says so.
- ⚠️ **Legacy settings do not carry over.** A modern module gets no world-readable
  redirect, so the 3.x preference file is usually unreadable; a one-shot best-effort
  import runs, and otherwise you start from defaults (everything on, as shipped).
- The UI can now read **its own LSPosed scope** and reports it, instead of only giving
  generic advice — the legacy API could not see it at all.
- `minSdk` 21 -> 26 (libxposed's floor), `compileSdk` 36, AGP 8.13.2 / Kotlin 2.3.0 /
  Gradle 8.14.3. R8 stays on, with libxposed's published rules — including
  `-adaptresourcefilecontents META-INF/xposed/java_init.list`, so the entry class can
  still be obfuscated without the list going stale.

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
