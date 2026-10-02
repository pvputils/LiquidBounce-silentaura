# KillAura source snapshot

This repository intentionally contains only the original LiquidBounce KillAura
Kotlin sources and the GPL-3.0 license. It is an incomplete mod, not a buildable
or runnable Fabric project.

The 11 files under
`src/main/kotlin/net/ccbluex/liquidbounce/features/module/modules/combat/killaura/`
are preserved unchanged, including their copyright and license notices.

The rest of LiquidBounce has been removed, including other modules, shared
utilities, configuration and event infrastructure, rendering, mixins, resources,
tests, the web theme, CI workflows, and Gradle build tooling. Imports and references
to those removed components remain intentionally unresolved. Minecraft and Fabric
dependencies are also no longer configured.

To turn these sources back into a working mod, provide the referenced dependencies,
a Fabric entry point and event hooks, resources, and build configuration. This
snapshot does not replace the original KillAura behavior with a standalone rewrite.
