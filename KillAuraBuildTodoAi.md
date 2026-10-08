# KillAura Fabric extraction

This client mod retains upstream KillAura and its direct support modules: Debug. Other module implementations, browser UI, remote control server, commands, account services, cosmetics and theme sources have been removed. Shared targeting, rotation, inventory, rendering, configuration and event code remains where required by those modules.

Teams, TargetLock, SwordBlock, ElytraTarget, Criticals, AutoWeapon, AntiBot, FightBot, FailSwing KeepSprint, MultiActions, AutoBlocking, IgnoreOpenInventory and SimulateInventoryClosing are removed, along with their hooks, settings and unused dependencies. KillAura uses the weapon you actually hold.

## Build and install

Use Java 25 and `./gradlew build` (Windows: `./gradlew.bat build`). The mod targets Minecraft 26.3. Install the jar from `build/libs` alongside Fabric Loader, Fabric API and Fabric Language Kotlin. Mod Menu is optional and provides a second way to open the configuration screen. ViaFabricPlus is optional; it is only a compile dependency for guarded protocol compatibility.

For development, use `./gradlew runClient`. Run `./gradlew runClientGameTest` for the automated Minecraft integration test.

## Configuration

Press **Right Shift** in a world, or choose Configure in Mod Menu. The native Minecraft screen exposes KillAura, every nested group and rotation mode, global target filters and retained support modules. Settings change live and save when the screen closes. Numeric and range inputs validate bounds; curves and complex values have text editors. Enable KillAura with its Enabled control or configure its keybind. KillAura only supplies aiming rotations. Your configured Minecraft attack button runs the complete vanilla input pipeline, including picking the actual entity or block, weapon-specific reach checks, misses, item-use restrictions, cooldown, swing and block breaking. KillAura never substitutes its tracked target for the vanilla hit result or cancels the click. There is no CPS setting, click scheduler, automatic cooldown click or delayed attack queue. Holding the button follows vanilla behavior.

Criticals selection, waiting for a crit, sprint cancellation, artificial ground flags, crit packet modes and fake crit effects are removed. Attacks use the complete vanilla Minecraft click pipeline, including native target selection, crit eligibility/damage, weapon cooldown and sprint slowdown. KeepSprint is removed because it bypassed that implementation.

KillAura uses the held item's vanilla attack range and line of sight to choose where to aim; Minecraft's own picker and attack routine decide what your click actually hits. Reach increases, extended scanning, through-wall aiming/attacks and legacy range migration are removed. The range indicator displays native reach.

KillAura pauses while an inventory is open. It never simulates closing/reopening inventories, automatically blocks/unblocks, or enables simultaneous actions. Manual item use follows vanilla behavior.

Settings are stored separately under `<game directory>/LiquidBounceKillAura`. The upstream resource namespace and mod ID remain `liquidbounce`; install this extraction in place of the full client.

AI rotations initialize asynchronously. If the native inference engine cannot load, ordinary rotations remain available. Included model and render assets are retained for KillAura features.

Requires and AimPoint are removed. Target aiming ray-traces the entity's current bounding box directly, without aim-point prediction, exemptions, delay, lazy tracking or Gaussian offsets. Client game tests use a silent OpenAL backend.

## Verification

The client integration test checks startup, the exact module registry, the native settings screen, configuration persistence and KillAura hits on a stationary zombie in a local test world. It checks zero attacks while idle, one attack for a held press, no repeats after release, and one attack per subsequent physical press, including rapid presses. Real input presses against forced out-of-range targets and targets behind a solid stone wall cannot hit those targets. Regression checks compare rotated picking with unmodified vanilla picking at the same angle, including a near miss. Grounded and naturally falling attack damage are compared with vanilla attacks for Normal, Snap and OnTick rotations. Clicking a wall while an aura target is tracked also verifies vanilla block-breaking input. Unit tests cover retained shared utilities and invalid config input. Build runs Detekt, unit tests and access widener validation.

New source filenames use the requested TodoAi suffix. Changes to existing code carry codex provenance comments; imports and deletions need none. JSON metadata retains valid JSON syntax. File naming suppressions preserve upstream event type names, and line-length suppressions permit the requested old-code comments.

Upstream GPL-3.0 licensing and attribution are preserved.
Rotation-only validation completed: Gradle build passed with 328 unit tests, zero failures and one skipped. The Minecraft 26.3 client integration test passed native picking parity (including a miss), real attack input with the camera pointed away, out-of-range and solid-wall rejection, vanilla block breaking, and vanilla grounded/critical damage parity in Normal, Snap and OnTick modes.
