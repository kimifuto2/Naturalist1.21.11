# Naturalist — Minecraft 1.21.11

Unofficial port of [Naturalist](https://github.com/crispytwig/Naturalist) to **Minecraft 1.21.11**, for **Fabric** and **NeoForge**.

Ported from the upstream `26.2-MultiLoader` branch (Minecraft 26.2, the release that follows 1.21.11).
All credit for the mod itself — code, models, textures, sounds, design — goes to **crispytwig / Starfish Studios**.

## What works

| Loader | Status |
|---|---|
| Fabric | Built and started in-game: 51 mods loaded, mixins applied, `naturalist 2.0.6` active |
| NeoForge | Builds and passes structural/access-transformer validation; not runtime-tested on this machine |

Everything the mod has is present: 50+ animals with their AI and variants, the bug net / knapsack / whistle items,
blocks and building sets, potions, advancements, loot tables, worldgen features, the built-in resource pack, and
a complete Simplified Chinese translation.

## Building

Requires **JDK 21**.

```bash
./gradlew build                 # both loaders
./gradlew :fabric:build         # fabric/build/libs/naturalist-<version>-fabric-1.21.11.jar
./gradlew :neoforge:build       # neoforge/build/libs/naturalist-<version>-neoforge-1.21.11.jar
```

Toolchain: Gradle 9.5.1 · Fabric Loom 1.17.19 · NeoForge moddev 2.0.112 · NeoForge 21.11.38-beta ·
official Mojang mappings · Fabric Loader 0.19.3 · Fabric API 0.141.5+1.21.11.

## Layout

```
common/    shared source (compiled into both loaders) + all assets and data
fabric/    Fabric entrypoints, config, platform services; compiles common in-place
neoforge/  NeoForge entrypoints, config, platform services; compiles common in-place
```

`common` is a plain source container, not a published artifact; each loader module includes
`../common/src/main/java` and `../common/src/main/resources` directly, so both jars are self-contained
and the Fabric jar needs no shadow/remap step for the shared code.

## Port notes (1.21.11 vs the 26.2 source)

Both versions use official Mojang mappings, so most of the port was API delta rather than a mapping rewrite.
The interesting parts, in case they help anyone porting something similar:

- **Only 26 classes** referenced by the mod do not exist in 1.21.11 (found by intersecting every
  `net.minecraft.*` import with the 1.21.11 class list). The rest was compile-driven work.
- **Namespace trap**: `configurations` that consume Minecraft types (Fabric API, Fabric Loader, LambDynamicLights)
  must use `modImplementation` / `modCompileOnly`. With plain `implementation` the jars stay in the intermediary
  namespace and javac reports bogus errors like `cannot access class_1299`.
- **Access widener**: source header must be `accessWidener v2 named` with Mojang member names; Loom rewrites it to
  `v2 intermediary` in the shipped jar. The three vanilla registration entry points
  (`MenuScreens.register`, `BlockEntityRenderers.register`, `SpawnPlacements.register`) are `private static` in
  1.21.11 and had to be widened.
- **Mixins**: annotation strings are invisible to javac. One 26.2-only injection target
  (`ParrotOnShoulderLayer.submitModel` took an `Identifier` in 26.2 and takes a `RenderType` in 1.21.11) would have
  crashed at startup under `defaultRequire: 1`.
- **`DANGER_*` vs `DAMAGE_*` path types**: both families exist in 1.21.11 with different costs, and 26.2 renamed
  them (`FIRE_IN_NEIGHBOR` → `DANGER_FIRE`, `FIRE` → `DAMAGE_FIRE`, …). Picking the wrong one silently changes
  mob pathfinding.
- **Knockback**: 26.2's 6-argument `knockback` does not exist; `getKnockback(Entity, DamageSource)` is the
  knockback the entity *deals*, so receiving-side scaling belongs in `knockback(double, double, double)`.

## Deliberate differences from upstream

- **Field Guide compatibility is not compiled.** Field Guide has no 1.21.11 build at all (it jumps 1.21.1 → 26.1.2),
  so `common/.../compat/fieldguide` is excluded and its mixin config ships inert (`required: false`).
- **The bundled resource pack is now registered.** Upstream ships `resourcepacks/custom_spawn_eggs` but never
  registers it, so Fabric never surfaces it. It is registered via `ResourceLoader.registerBuiltinPack(...)` with
  `PackActivationType.NORMAL` — it appears in the resource-pack screen and is off by default.
- **Enchanted banners carried by elephants do not render the glint.** 1.21.11's `BannerRenderer.submitPatterns`
  needs an explicit `Material` and a `withGlint` flag that 26.2's call site did not pass; the call now matches
  vanilla's own banner block-entity usage.
- **`BlockTags.SUBSTRATE_OVERWORLD` → `BlockTags.DIRT`** (the tag does not exist in 1.21.11). This matches what the
  mod's own 1.21.1 release did; mob spawning grounds are slightly narrower than in 26.2.
- **Localization**: `zh_cn.json` here is a complete rebuilt translation (531/531 keys). Upstream's file covered only
  133 keys and had 22 values still in English. Two music discs share the English name "Music Disc" upstream and
  therefore cannot be told apart in-game.

## License

See [LICENSE](LICENSE). The assets under `common/src/main/resources/` remain subject to the upstream
Starfish Studios license; this repository only ports the code to a different Minecraft version.
