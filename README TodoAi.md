# KillAura Standalone

A client-only Fabric mod for Minecraft **26.3**, using **Java 25** and Fabric Loader **0.19.5 or newer**. Install Fabric API **0.160.5+26.3** with it.

## Install and use

Copy `build/libs/killaura-standalone-1.0.0.jar` into the Minecraft instance's `mods` directory together with Fabric API. Launch that instance using Fabric Loader.

KillAura starts disabled and turns off when changing worlds. Press **R** while playing to toggle it; the binding is changeable in Minecraft's Controls menu. `/killaura toggle` also toggles it, `/killaura` shows its status, and `/killaura reload` reloads its config.

Settings are created at `config/killauraTodoAi.properties`. Defaults are a 3-block attack range, no attacks through walls, 8–12 CPS limited by vanilla attack cooldown, players and hostile mobs enabled, animals excluded, and automatic shield blocking disabled. Screen menus, pause, death, and spectator mode suspend attacks.

## Configuration

| Setting | Meaning |
| --- | --- |
| `range` | Eye-to-hitbox attack range, 0.1–6 blocks; server reach limits still apply |
| `wallRange` | Attack range through walls, capped at `range`; 0 disables it |
| `scanExtra` | Additional target scan range; does not extend attack range |
| `minCps`, `maxCps` | Random attack rate, 1–20 CPS; maximum is normalized to at least minimum |
| `turnSpeed` | Maximum rotation change per tick, 1–180 degrees |
| `cooldown` | Require vanilla attack cooldown to reach 90% |
| `players`, `hostileMobs`, `animals` | Target types |
| `invisible` | Allow invisible targets |
| `ignoreShield` | Include blocking targets; does not bypass shield mechanics |
| `autoBlock` | Use an offhand shield between attacks; release it before attacking |
| `pauseWhileUsing` | Pause while manually using an item |
| `requireWeapon` | Require a sword or axe in the main hand |
| `criticalOnly` | Attack only while falling, outside water and ladders |

Invalid numbers and booleans fall back to defaults; finite out-of-range numbers are clamped. Edit the file and use `/killaura reload`.

## Build and verify

With a JDK 25 installed, on Windows:

```powershell
.\gradlewTodoAi.bat build
.\gradlewTodoAi.bat runClient
.\gradlewTodoAi.bat runClientGameTest
```

On Linux/macOS use `./gradlewTodoAi` with the same task names. `build` runs 15 independent combat-math/configuration checks. The client game test creates a temporary world and checks disabled startup, key toggling, real attacks, range, disabling, and wall visibility. Its test mod is excluded from the production JAR.

## Scope and attribution

The runtime is a standalone Java adaptation of the core targeting, aiming, attack timing, requirements and shield-blocking behavior. It does not load the LiquidBounce framework, browser, UI, commands, or other modules.

The 11 original Kotlin KillAura files remain unchanged as reference under `src/main/kotlin`; they are not compiled or included in the production JAR. This is not a full feature-parity port: original FightBot movement, fake swings, visual indicators, silent/snap rotation modes, and integrations with other LiquidBounce modules are not implemented in this runtime.

Derived from LiquidBounce KillAura, copyright 2015–2026 CCBlueX. Distributed under GPL-3.0-or-later; see `LICENSE`.
