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
- **Custom block models**: the 26.1 Fabric Renderer API moved (`fabric.api.client.renderer.v1`) and 1.20's
  `BakedModel`/`emitBlockQuads` is gone. Railways' models extend Create Fly's `WrapperBlockStateModel` (or
  `CopycatModel`/`FluidTankModel`) and build vanilla `BlockStateModelPart`s in `addPartsWithInfo`; Create Fly's
  mixins call that for world rendering with or without FRAPI. Registered through `CreateRegistrate.blockModel` →
  Create Fly `AllModels`. Per-quad `MaterialInfo` carries the render layer, so 1.20's material fix-up is gone.

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
- Ported, one commit each: track materials + mechanical renames; networking/events/platform glue; block method
  signatures, configs, coupler/switch client splits; edge points, util platform code (obsolete mixins dropped);
  palettes, paint fluid (data component), creative/registry generics; fuel tank, portable fuel interface, mounted
  toolbox, conductor cap, movement renders; casing effects, smokestack extender, copycat headstock/transform.
- End of session: 251 errors in 48 files.

### 2026-10-30
- Remaining at start (251 errors / 48 files): ConductorEntity (~57, `PlayerModel` generics, `spawnAtLocation`,
  overrides), the custom baked models (Fabric Renderer API `renderer.v1.model/mesh/material/render` is gone in 26.1:
  copycat headstock/bars/vent, buffer, generic crossing, sprite-copying casing, fuel tank), particle data codecs,
  `FabricValueInput.contains`, ~25 client mixins/renderers with 1-4 errors each.
- **The 251 was only javac's symbol-resolution pass.** Once the missing types resolved, attribution of method bodies
  ran and the count rose to ~960 across ~200 files (expected; not a regression).
- **Custom block models → Create Fly's `WrapperBlockStateModel`** (see Decisions). Ported: copycat headstock + bars,
  buffer (sprite swap + diagonal rotation), generic crossing, conductor vent, fuel tank (now just a Create Fly
  `FluidTankModel` with our CT shifts), casing `SpriteCopyingBakedModel` (precomputed `BlockStateModel`).
  `BufferModelUtils.addSwappedParts` replaces the per-model FRAPI sprite-replacing wrappers.
- Dropped `fabric.mixin.client.CopycatModelMixin` + `FilteredRenderAttachedBlockView` (they patched upstream Create's
  FRAPI copycat path; Create Fly's copycats don't filter connected textures by `canConnectTexturesToward` either) and
  `MixinItemModelShaper` (name-based conductor cap item models; see TODO).
- `AccessorSegmentAngles` invoker: Create Fly bakes track segments with `bc.getBakedSegments(SegmentAngles::new)` and
  the constructor is package-private.
- **Bogeys**: `port/create/bogey/CRBogeyStyleBuilder` (lazy block suppliers resolved in `postRegistrationInit`,
  renderers registered client side into Create Fly's `AllBogeyStyleRenders`). `UnifiedBogeyRenderer` extracts a
  render state of `SuperByteBuffer` parts; entity-aware displays updated around `CarriageBogeyRenderState.create`.
- **Mechanical batch**: Optional NBT getters (`getIntOr` etc.), `contains`/`getList` signatures, `NbtCompat`
  (removed UUID/BlockPos NBT helpers, 1.20 formats kept), `ChunkPos.x()`, `getSelectedSlot()`, `getRandom()`,
  `ResourceKey.identifier()`, `getUnitVec3i()`, `LevelRenderer.getLightCoords`, `Create.asResource` →
  `Identifier.fromNamespaceAndPath("create", …)`.
- **Particles**: data classes on `MapCodec`/`StreamCodec` + Fabric pending providers; particles `extract` into
  `QuadParticleRenderState` (`TextureAtlasSprite.getU/getV` now take 0–1, not 0–16).
- **Block entity renderers** (diesel stack, semaphore, fuel tank, track switch, coupler, whistle flag) on
  extract/submit. `CustomTrackOverlayRendering.extractOverlay` returns a render state with the track transform folded
  in; `MixinTrackTargetingClient` follows Create Fly's new `render` signature. Track casings: `MixinTrackRenderer`
  extracts block and curve casings at every return of `extractRenderState` (Create Fly returns early without curves)
  into `MixinTrackRenderState`/`IExtraRenderParts`, submitted after the track.
- **Track renderers registered for Railways tracks**: Create Fly's `AllTrackRenders` only had its own track, so signal/
  station/observer overlays never drew on Railways tracks. `TrackMaterialModels` registers
  `StandardTrackBlockRenderer` per factory-built material; `MixinStandardTrackBlockRenderer` (replaces
  `MixinTrackBlockClient` and the phantom/monorail overrides) applies `CustomTrackOverlayRendering.overlayShift`.
- **Schedule entries** split like Create Fly: data classes (Identifier ctor, ValueInput/Output) + client
  `IScheduleInput` renders registered by `CRSchedule.registerRenders()`.
- `MixinStationBlock` → `useItemOn`; station train limit is Railways' `StationLimitPacket` (Create Fly's
  `StationEditPacket` is a fixed record; `MixinStationEditPacket` removed).
- Custom block outlines: Fabric `LevelRenderEvents.BEFORE_BLOCK_OUTLINE` (`CustomOutlineRendering`, replaces
  `MixinLevelRenderer`). Item NBT → `ItemUtils.getCustomData/setCustomData` (`custom_data`).
- `CRExtraRegistration` attaches to Create Fly's eagerly registered `AllBlockEntityTypes` after Railways' registration
  (`MultiRegistryCallback` removed). `CopycatTypeOverride` + `MixinCopycatBlockEntity` let the copycat headstock BE
  keep its own type (Create Fly's `CopycatBlockEntity` hardwires `COPYCAT`, and 26.1 validates in the constructor).
- Mixins retargeted: `CarriageParticles`/`CarriageSounds` → Create Fly's `CarriageParticleBehaviour`/
  `CarriageAudioBehaviour`; `EntityHats` → `HatFeatureRenderer.submit`; `BlockElement.Deserializer` →
  `CuboidModelElement$Deserializer.getPosition`; train relocation → `AllHandle.onTrainRelocation`.
  Removed: `AllKeysMixin` (uses `AllKeys.ALL`), `MixinParticleEngine`, `CreateCodecsMixin` (Porting Lib),
  `AccessorIngredient$TagValue`, `MixinBasingRecipe` (see TODO).
- `CRCTTypes` extends Create Fly's class-based `CTType` (bit-mask contexts); `BoilerCTBehaviour` likewise.

## TODO (not compile errors)
- **Generated data is still in 1.20 form**: `src/generated/resources/data/railways` uses `recipes`/`loot_tables`/
  `advancements` (26.1: `recipe`/`loot_table`/`advancement`), and the 1,063 recipe files use 1.20 JSON (`fluidTag`,
  NBT strings, Railways' "mangled" `railways:amount` fluid units that `MixinProcessingRecipeSerializer` converted;
  that mixin is gone). Needs a conversion script to Create Fly's 26.1 recipe formats.
- **Bogey menu preview** shows the bogey block statically (`GuiGameElement`). The animated preview needs a
  picture-in-picture renderer that can draw a `BogeyRenderState` (which only submits to a `SubmitNodeCollector`).
- `fabric.mixin.client.LevelRendererMixin` (conductor camera entity override) targets `Camera.entity()` ordinal 3 in
  `renderLevel`, unverified against 26.1 (`require = 0`, so it silently no-ops if wrong).
- **Item model definitions** (`assets/railways/items/*.json`, 1.21.4+): none exist yet, so items will render as
  missing models. Generate them from the committed `models/item` files.
- Custom conductor caps by item name (was `MixinItemModelShaper`): a `minecraft:select` on `minecraft:custom_name` in
  the cap item definitions, or a custom `ItemModel`.
- Buffer / copycat headstock **items** no longer show the stack's material/colour (1.20 wrapped the item baked model);
  needs a custom `ItemModel.Unbaked` reading `block_entity_data`. Copycat headstock item tint likewise.
- Copycat headstock: connected-texture filtering via `canConnectTexturesToward` (needs a filtering
  `BlockAndTintGetter`), matching upstream rather than Create Fly.
- `MixinBasingRecipe` (stopped Railways basin recipes consuming an ingredient identical to their output, e.g. dyeing)
  was dropped: Create Fly's basin matching is restructured and 26.1 recipes have no id. Re-derive.
- Compat track rail ingredients are a single tag now (iron nuggets / wrought iron rods; was "or zinc"). Datagen only.
- Creative tab ordering treats block items as "3D" (no baked item model `isGui3d` in 26.1).
