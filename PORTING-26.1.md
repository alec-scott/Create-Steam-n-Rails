# 26.1.2 Fabric Port — Working Log

Branch `26.1-fabric`, from `1.20/create-v6` (Steam 'n' Rails 1.7.3, Create 6.0.8, Minecraft 1.20.1, Architectury
Forge + Fabric). Goal: Steam 'n' Rails on **Fabric 26.1.2**, running on **Create Fly** (our fork at `../Create-Fly`),
alongside the Create Aeronautics and Create Big Cannons ports and the full testing modpack.

This file tracks the port's state so work can resume across sessions. Update it as you go.

## Starting point

| | |
|---|---|
| Upstream | `Layers-of-Railways/Railway`; fork `alec-scott/Create-Steam-n-Rails` (`origin`) |
| Base branch | `1.20/create-v6`: Create 6.0.8 on 1.20.1, Architectury `common`/`fabric`/`forge` (656 + 99 + 91 Java files, 213 mixins) |
| Newer upstream | None. Upstream has no 1.21 branch, so this is the closest base. `1.20/dev` is still on Create 0.5.1. |

Three ports at once, as for CBC (see `../createbigcannons/PORTING-26.1.md`):
1. **Architectury → plain Fabric.** The `fabric` module already had Fabric implementations of everything.
2. **Minecraft 1.20.1 → 26.1.2.** A bigger gap than the other ports (they started from 1.21.x): data components,
   1.21 `Identifier`/registry changes, the 1.21.2 recipe and interaction rewrites, 1.21.4 item model definitions,
   1.21.5+ render-state rendering, `ValueInput`/`ValueOutput`, etc.
3. **Create → Create Fly** (`com.zurrtum.create`, client code under `com.zurrtum.create.client.*`, Flywheel/Catnip/
   Ponder bundled inside it, no Registrate or datagen helpers).

## Building

```sh
export JAVA_HOME=/c/Users/akrsc/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon   # Git Bash, JDK 25
./gradlew compileJava --console=plain
```

Create Fly comes from the local checkout's jar (`create_fly_jar` in `gradle.properties`); rebuild it there first.

## Decisions

- **Base on `1.20/create-v6`.** Create 6 matches Create Fly 6.0.9; the 1.20/dev branch is Create 0.5.1.
- **Single Fabric module.** `common` + `fabric` merged into `src/main`, `forge` deleted. Each `@ExpectPlatform`
  stub now calls its `fabric.<Class>Impl` directly (Architectury's naming: `pkg.Outer.Inner` →
  `pkg.fabric.OuterInnerImpl`). The `fabric` sub-packages are kept so history stays readable.
- **Registrate:** CBC's relocated Fabric 26.1 Registrate (from the Aeronautics Simulated module) copied into
  `com.railwayteam.railways.port.registrate`, with the `CreateRegistrate` shim and the Porting Lib datagen stubs.
  Same reason as CBC: two mods shipping the same class names is fragile on Fabric.
- **Datagen stubbed**: the committed `src/generated/resources` are used (and hand-converted to 26.1 formats).
- **Build info**: the Blossom template is replaced by a small `generateBuildInfo` Gradle task. `@DevEnvMixin`
  stripping (release builds only) is not ported.
- **Compat excluded for the first pass**: EMI, Tweakeroo/malilib, Simple Voice Chat mixins. Fabric-ASM (no 26.1
  build) only added the roller's `TRACK_REPLACE` mode; that becomes an enum mixin.
- **Track materials (Create Fly gap).** Create Fly removed upstream's extensible track API: no `TrackType`, no
  `TrackMaterialFactory`/builder, no per-material lang/particle/sleeper/rails data, and it compares the *material id*
  where upstream compared the *track type* (`AbstractBogeyBlock.isOnIncompatibleTrack`, `Navigation` pathfinding).
  Taken as is, a standard bogey would be "incompatible" with every Railways wooden/stone track. Fix, kept inside
  Railways (as CBC does with Create Fly gaps, so the shared Create Fly jar doesn't change):
  `port/create/track` recreates `TrackType` and `TrackMaterialFactory`, a mixin attaches the extra data to Create
  Fly's `TrackMaterial`, and two small mixins make Create Fly compare track types. Materials Railways doesn't
  extend keep Create Fly's behaviour (their type is their id). Candidate for the Create Fly fork later.

## Port mechanics (how things map)

| 1.20.1 Architectury | 26.1.2 Fabric here |
|---|---|
| `@ExpectPlatform` | direct call to `pkg.fabric.XImpl` |
| `ResourceLocation` | `Identifier` (`fromNamespaceAndPath` / `parse`) |
| `javax.annotation.Nullable`, `@MethodsReturnNonnullByDefault` | JetBrains `@Nullable`; the package-default annotations are dropped |
| Porting Lib `FluidStack` / `FluidTank` | Create Fly `infrastructure.fluids.FluidStack` / `foundation.fluid.FluidTank` (droplets, 81 per mB) |
| Create `TrackMaterialFactory` / `TrackType` | `port/create/track` (see Decisions) |

## Session log

### 2026-10-24
- Created `26.1-fabric`; flattened the Architectury build; build migrated to non-remap Loom 1.17 / Java 25 /
  Fabric API 0.155.3 / Create Fly 26.1.2-6.0.9-4 / Forge Config API Port 26.1.5.
- Import remap with the CBC tooling: 380 Create/Catnip/Ponder/Flywheel imports resolved, 47 classes Create Fly lacks.
- First compile: 3918 errors.
