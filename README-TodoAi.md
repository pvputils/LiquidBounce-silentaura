# KillAura with a native Fabric configuration screen

Open **Mods > LiquidBounce > Configure** through Mod Menu, or press **Right Shift**
while in game. The menu uses ordinary Minecraft buttons and text fields.

- Click a section to browse its settings. Use Search and page arrows within a section.
- Click a boolean to toggle it. Click a choice to select an option; multi-choice
  entries can be toggled independently.
- Open a mode group to select any mode and edit its nested settings.
- Numeric and range editors validate the allowed bounds. Ranges use `minimum..maximum`.
- Colors accept `#RRGGBB` or `#AARRGGBB`. Curves accept `x,y; x,y` points with axis bounds.
- Keybinds support keyboard/mouse names, Toggle/Hold/Smart actions, and modifiers.
- Texture paths and all original rotation, AI, targeting, blocking, rendering,
  and FightBot settings use the existing configuration objects.
- Apply changes a value; Cancel discards the current text edit; Reset restores that
  value. Done/Escape saves through the existing configuration system.

The web theme, browser/JCEF backends, GUI HTTP server, ClickGUI, HUD editor,
custom backgrounds, branded splash screen, and their build steps are removed.
Vanilla game menus remain in place. Shared rendering required by KillAura target
visuals and the AI engine are retained. KillAura is the only registered module.

Build with Java 25 and `gradlew.bat build`. No Node.js or browser runtime is required.
The original GPL license and attribution still apply.
