# SophisQuickTorio

NeoForge 1.21.1 bridge mod: makes **QuickShulker** work with **Inventorio** rows and
**Sophisticated Storage** shulkers, and adds **ToolBelt** tools to **Toggle Enchantments**.

## What it fixes

1. **QuickShulker × Inventorio** — QuickShulker only knows the vanilla 36-slot
   inventory. Shulkers stored in Inventorio rows (Deep Pockets, ToolBelt, …) can now be
   quick-opened (`K` / right-click), including insert/extract/drag bundling.
2. **QuickShulker × Sophisticated Storage** — QuickShulker hardcodes the 17 vanilla
   shulkers. All 6 Soph shulkers (basic → netherite) open with the full Soph UI:
   - in hand (right-click, `Shift` still places the block),
   - hovered in any inventory (`K` / right-click),
   - in Inventorio rows,
   - nested inside open Soph screens (backpacks, storages).
3. **QuickShulker in Soph screens** — vanilla openables (crafting table, stonecutter,
   ender chest, anvil, vanilla shulkers) can be quick-opened from slots inside
   Sophisticated screens.
4. **Toggle Enchantments × ToolBelt** — tools in the Inventorio ToolBelt show up in
   the toggle list (the screen also grows past its hardcoded 6 rows).

Everything is fail-open (`require = 0` mixins + `try/catch`): if a target mod is
missing or changes, vanilla behavior is preserved.

## Install

Put the same jar in **client and server** `mods/` (needs QuickShulker, Inventorio,
Sophisticated Core/Storage/Backpacks, Toggle Enchantments present to activate each
bridge — every bridge enables independently).

## Build

```bash
./gradlew build --offline
```

`compileOnly` references live in `libs/` (present at runtime). Java 21, NeoForge 21.1.249.

## Credits

- QuickShulker NeoForged, Inventorio, Sophisticated mods, Toggle Enchantments authors.
- Bridge by ItsYusei99.
