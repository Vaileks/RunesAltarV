# RunesAltarV

16 legendary weapons for Paper 1.21.11. Maintained by Vaileks.

Install `RunesAltarV.jar` in `plugins/` and restart the server. No other plugins are required. WorldGuard support is optional.

Set `lang: en_US` or `lang: ru_RU` in `plugins/RunesAltarV/config.yml`. Each weapon has one editable file in `plugins/RunesAltarV/items/`, containing its name, descriptions, models and ability settings. Reload with `/rav reload`. Existing administrator settings are preserved.

## Commands

- `/rav` — view the weapon menu; administrators can click an item to receive it.
- `/rav give <player> <weapon_id>` — give a weapon. Tab completion lists all 16 IDs.
- `/rav reload` — reload configuration and refresh held items.
- `/rav cooldown reset <player>` — reset ability cooldowns.
- `/friendlist add <player>`, `/friendlist remove <player>`, `/friendlist list` — manage up to two friends who are protected from your abilities. Aliases: `/fl`, `/trust`.

Permissions: `runesaltarv.menu` (everyone), `runesaltarv.admin` (operators).

Controls are shown in each weapon's lore. F means the swap-hands key; Shift + F means using it while sneaking. Active effect indicators use the action bar, with chat messages when attempting an ability on cooldown and when its cooldown ends. Passive effects do not announce cooldowns.

## Weapons

Bone Blade, Bloodlust, Nightpiercer, Vulcan's Crossbow, Pale Gun, Frost Scythe, Pure Blade, Knightfall, Shadow Blade, Windweaver, Hyperion, Withered Bone Blade, Earth Gauntlet, Cutlass, Paladin's Battle Axe and Crazy Slots.

Weapons have their own persistent tags, so an unrelated item with the same custom model is not treated as legendary. Weapon ability damage follows the server damage pipeline, including absorption, event cancellation and totem resurrection.

## Resource pack

Custom models and tooltip textures require a compatible resource pack, distributed separately through [Discord](https://discord.gg/ssXctkBkQD). No resource pack assets are included in this source repository or JAR. Mechanics work server-side without a client mod. Without the pack, items use vanilla appearances.

## Build

JDK 21 or newer and Maven: `mvn package`. The resulting file is `target/RunesAltarV.jar`. Only the provided Paper API and its transitive libraries are needed at compile time.

Alternatively set `ALTAR_COMPILE_CLASSPATH` to the Paper 1.21.11 API and its transitive dependencies and run `python3 build.py`; this produces `RunesAltarV.jar`.

The `tests/` directory contains integration checks intended for a disposable Paper server with this plugin and the test helper. They cover weapon creation, both lore languages, configuration reload, ability cooldown guards, active effect rendering, obstruction checks and native damage/resurrection behavior. They do not replace multiplayer visual testing.

## Attribution

RunesAltarV is a standalone derivative of [AltarLegendaryWH by WHersss](https://modrinth.com/plugin/altarlegendarywh), published under the MIT license. This version removes the framework and pedestal integrations, adds independent configuration and administration, and adapts presentation and cooldown feedback. It is maintained separately and is not an official release of the original project. See `LICENSE` and `NOTICE`.
