# KillAura with its original supporting framework

This branch preserves the complete original KillAura implementation and its
configuration dependencies. It is a buildable Fabric client, not a source-only
snapshot or a rewritten standalone aura.

## Preserved functionality

- Every KillAura source file, local setting, range option, attack requirement,
  raycast mode, critical-hit selection, and inventory-handling option.
- All inherited click scheduling, cooldown, target-tracking, and aim-point options.
- Rotation timing, movement correction, reset behavior, Linear, Sigmoid,
  Interpolation, Acceleration, and AI smoothing, plus their nested settings.
- AutoBlock, FailSwing, FightBot (including inherited movement assistance),
  range indication, failed-hit handling, and all eight target-rendering modes.
- The original configuration framework, settings UI, persistence, event hooks,
  mixins, rendering resources, AI models, and build dependencies.

KillAura is the only registered built-in module. Separate modules are not exposed
as additional toggles; referenced module classes remain where shared code needs
them. This preserves KillAura's own option tree without claiming that every
separate LiquidBounce module is enabled. Further framework removal must preserve
these dependencies and their runtime initialization.

## Validation

The KillAura sources and shared configuration, aiming, clicking, combat,
navigation, rendering, deep-learning, integration/UI, theme, and resource trees
match the original upstream base at 0a80fecbdd3dedff68235454bd34ef71cd7596d3.
The restored branch passes `gradlew.bat build --offline` with Java 25, including
unit tests, Detekt, ABI checks, access-widener validation, and JAR packaging.
This does not substitute for testing every combat setting against a live server.
