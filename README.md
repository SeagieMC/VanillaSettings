# Vanilla Settings

A client-side Fabric mod for **Minecraft 26.2** that switches your game between plain vanilla and a
crystal-PvP visual preset with a single key.

Everything it changes is a client-side rendering or option setting. It never touches gameplay,
packets or the server.

## What the preset does

While the mod is **on**:

| Feature | Effect |
| --- | --- |
| **Particles** | Blocks every particle from spawning, explosions included (vanilla's "Minimal" setting still shows those). |
| **No Fog** | Removes all fog: atmospheric, water, lava, powder snow, blindness and darkness. |
| **Smooth Fullbright** | Raises the light level with a smooth fade in *and* out. Strength 1-15 (1 = vanilla), fade time 1-5 s. Switching back mid-fade turns around smoothly from wherever the brightness currently is. |
| **Render Distance Switch** | Uses one render distance while the mod is on and another while it is off (2-32 chunks each). |
| **Fire/Shield Offsets** | Lowers the first-person fire overlay and the shield. |
| **Armor HUD** | Shows your worn armor next to the hotbar. It steps aside for the offhand slot and the attack indicator, and dims together with the hotbar when a menu is open. |
| **Totem Resize** | Resizes the totem in your hands (**Totem Size**) and the pop animation (**Totem Pop Size**). `1.00` is vanilla, `0.40` is 40% of vanilla. The pop is also kept in the centre of the screen. |
| **Alert Toast** | Small on-screen message when the mode changes. |

Every feature has its own on/off switch, so the preset does exactly what you want and nothing else.

## Usage

1. Open the settings with the command `/vanillasettings`.
2. Click **Activate bind** and press the key that should toggle the preset.
3. Click **Open menu bind** and press the key that should open the settings from now on.

While a bind button says "Press a key...", press **Esc** to clear that bind.
Binding a key that the other bind already uses moves it to the new bind.

Both binds work only while no menu is open.

## Settings screen

The settings are grouped into five sections, separated by thin lines:

1. Particles, No Fog, Fire/Shield Offsets, Armor HUD
2. Smooth Fullbright, Fullbright Strength, Brightness Transition
3. Render Distance Switch, Render Distance (mod ON), Render Distance (mod OFF)
4. Totem Resize, Totem Size, Totem Pop Size
5. Activate bind, Open menu bind, Alert Toast, Language

### Languages

The **Language** button cycles through **English** (default), **Russian** and **Mixed Russian**.
All text lives in one file, [`Text.java`](src/main/java/net/vanillasettings/lang/Text.java), one line per
label with one column per language, so translations are easy to adjust.

## Configuration

Settings are saved to `config/vanillasettings.json` when the settings screen is closed.
If you used this mod under its old name (Better Vanilla), your `config/bettervanilla.json` is imported
automatically on the first launch.

Brightness and render-distance settings are applied when the preset is switched on or off; everything else takes effect immediately.

## Installation

Requires Minecraft 26.2, [Fabric Loader](https://fabricmc.net/use/) 0.19.3 or newer and
[Fabric API](https://modrinth.com/mod/fabric-api).

Put the mod jar into your `mods` folder. If you have an older `bettervanilla-*.jar` there, delete it:
the mod was renamed, so the old jar would otherwise load next to the new one.

## Building

Requires JDK 25.

```
gradle build
```

The jar is written to `build/libs/`.

## Credits

- Armor HUD layout and its offhand / attack indicator behaviour follow
  [uku's Armor HUD](https://github.com/uku3lig/armor-hud).

## License

[CC0 1.0 Universal](LICENSE)
