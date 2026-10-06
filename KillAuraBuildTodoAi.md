# KillAura Fabric extraction

This client mod retains upstream KillAura and its direct support modules: AutoWeapon, AntiBot, Teams, TargetLock, Debug, SwordBlock, Criticals, ElytraTarget and MultiActions. Other module implementations, browser UI, remote control server, commands, account services, cosmetics and theme sources have been removed. Shared targeting, rotation, inventory, rendering, configuration and event code remains where required by those modules.

## Build and install

Use Java 25 and `./gradlew build` (Windows: `./gradlew.bat build`). The mod targets Minecraft 26.3. Install the jar from `build/libs` alongside Fabric Loader, Fabric API and Fabric Language Kotlin. Mod Menu is optional and provides a second way to open the configuration screen. ViaFabricPlus is optional; it is only a compile dependency for guarded protocol compatibility.

For development, use `./gradlew runClient`. Run `./gradlew runClientGameTest` for the automated Minecraft integration test.

## Configuration

Press **Right Shift** in a world, or choose Configure in Mod Menu. The native Minecraft screen exposes KillAura, every nested group and rotation mode, global target filters and retained support modules. Settings change live and save when the screen closes. Numeric and range inputs validate bounds; curves and complex values have text editors. Enable KillAura with its Enabled control or configure its keybind. Attacks now follow real presses of your configured Minecraft attack button. Each press can cause at most one aura attack; holding it never repeats attacks. There is no CPS setting, click scheduler, automatic cooldown click or delayed attack queue. Ordinary Minecraft weapon cooldown still determines damage. If range, criticals or blocking requirements reject a click, it is discarded rather than saved for later.

Settings are stored separately under `<game directory>/LiquidBounceKillAura`. The upstream resource namespace and mod ID remain `liquidbounce`; install this extraction in place of the full client.

AI rotations initialize asynchronously. If the native inference engine cannot load, ordinary rotations remain available. Included model and render assets are retained for KillAura features.

## Verification

The client integration test checks startup, the exact module registry, the native settings screen, configuration persistence and KillAura hits on a stationary zombie in a local test world. It checks zero attacks while idle, one attack for a held press, no repeats after release, and one attack per subsequent physical press, including rapid presses. Unit tests cover retained shared utilities and invalid config input. Build runs Detekt, unit tests and access widener validation.

New source filenames use the requested TodoAi suffix. Changes to existing code carry codex provenance comments; imports and deletions need none. JSON metadata retains valid JSON syntax. File naming suppressions preserve upstream event type names, and line-length suppressions permit the requested old-code comments.

Upstream GPL-3.0 licensing and attribution are preserved.
Validation completed: Gradle build passed; 332 unit tests, zero failures, one skipped. The Minecraft 26.3 client integration test passed startup, settings-screen opening, config save/load and manual-input KillAura attacks with only Fabric dependencies and Mod Menu loaded.
