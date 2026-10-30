/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2026 The Railways Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.railwayteam.railways.content.custom_tracks.casing;

import com.railwayteam.railways.port.create.track.TrackMaterialData;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.railwayteam.railways.Railways;
import com.railwayteam.railways.mixin.client.AccessorSegmentAngles;
import com.railwayteam.railways.mixin_interfaces.IHasTrackCasing;
import com.railwayteam.railways.registry.CRBlockPartials;
import com.zurrtum.create.content.trains.track.BezierConnection;
import com.zurrtum.create.client.content.trains.track.TrackRenderer.SegmentAngles;
import com.railwayteam.railways.port.create.track.TrackType;
import com.zurrtum.create.client.flywheel.api.instance.InstancerProvider;
import com.zurrtum.create.client.flywheel.api.material.CardinalLightingMode;
import com.zurrtum.create.client.flywheel.lib.instance.InstanceTypes;
import com.zurrtum.create.client.flywheel.lib.instance.TransformedInstance;
import com.zurrtum.create.client.flywheel.lib.material.LightShaders;
import com.zurrtum.create.client.flywheel.lib.material.SimpleMaterial;
import com.zurrtum.create.client.flywheel.lib.model.ModelUtil;
import com.zurrtum.create.client.flywheel.lib.model.SimpleModel;
import com.zurrtum.create.client.flywheel.lib.model.baked.BakedModelBuilder;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import com.zurrtum.create.catnip.data.Iterate;
import com.zurrtum.create.catnip.data.Pair;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.railwayteam.railways.registry.CRTrackMaterials.CRTrackType.NARROW_GAUGE;
import static com.railwayteam.railways.registry.CRTrackMaterials.CRTrackType.WIDE_GAUGE;
import static com.railwayteam.railways.util.MathUtils.copy;

public abstract class CasingRenderUtils {

    private static final HashMap<Pair<PartialModel, Block>, PartialModel> reTexturedModels = new HashMap<>();

    public static void clearModelCache() {
        reTexturedModels.clear();
        CRBlockPartials.registerCasingSpecs();
        Minecraft.getInstance().levelRenderer.allChanged();
    }

    public static PartialModel reTexture(PartialModel model, Block block) {
        Pair<PartialModel, Block> key = Pair.of(model, block);
        if (!reTexturedModels.containsKey(key)) {
            BlockStateModel blockModel = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(block.defaultBlockState());
            BlockStateModel texturedCasing = new SpriteCopyingBakedModel(model.get(), blockModel);
            PartialModel texturedPartial = RuntimeFakePartialModel.make(Railways.asResource("runtime_casing"), texturedCasing);
            reTexturedModels.put(key, texturedPartial);
            return texturedPartial;
        }

        return reTexturedModels.get(key);
    }

    /**
     * Casing parts along a curve, relative to the curve's first block entity.
     *
     * <p>PORT-NOTE(26.1): was {@code renderBezierCasings}, drawing into a buffer from Create's removed
     * {@code TrackRenderer.renderBezierTurn}; parts are extracted for {@code MixinTrackRenderer} to submit.</p>
     */
    public static void extractBezierCasings(Level level, PartialModel texturedPartial, BlockState state, BezierConnection bc,
                                            @Nullable CardinalLighting cardinalLighting, List<SuperByteBufferRenderState> out) {
        int heightDiff = Math.abs(bc.bePositions.get(false).getY() - bc.bePositions.get(true).getY());
        double shiftDown = ((IHasTrackCasing) bc).railways$isAlternate() && heightDiff > 0 ? -0.25 : 0;
        if (heightDiff / bc.getLength() <= 4 / 30d) {
            for (Vec3 pos : casingPositions(bc)) {
                BlockPos tePosition = bc.bePositions.getFirst();
                int light = LevelRenderer.getLightCoords(level, BlockPos.containing(pos).offset(tePosition));

                out.add(CachedBuffers.partial(texturedPartial, state)
                        .translate(pos.x, pos.y, pos.z)
                        .translate(0, shiftDown, 0)
                        .scale(1.001f)
                        .cardinalLighting(cardinalLighting)
                        .light(light)
                        .extractRenderState());
            }
        } else {
            BlockPos tePosition = bc.bePositions.getFirst();
            SegmentAngles segments = bc.getBakedSegments(AccessorSegmentAngles::railways$create);

            PoseStack nudge = new PoseStack();
            TransformStack.of(nudge)
                    .nudge((int) tePosition.asLong());

            for (int i = 1; i < segments.length; i++) {
                if (i % 2 == 0) continue;
                int light = LevelRenderer.getLightCoords(level, segments.lightPosition[i].offset(tePosition));
                Matrix4f pose = copy(segments.tieTransform[i].pose());
                pose.translate(new Vector3f(0, (i % 4) * 0.001f, 0));
                out.add(CachedBuffers.partial(texturedPartial, state)
                        .transform(nudge.last())
                        .mulPose(pose)
                        .mulNormal(segments.tieTransform[i].normal())
                        .translate(0, shiftDown, 0)
                        .scale(1.02f)
                        .cardinalLighting(cardinalLighting)
                        .light(light)
                        .extractRenderState());

                TrackType trackType = TrackMaterialData.of(bc.getMaterial()).trackType;
                if (trackType == WIDE_GAUGE) {
                    for (boolean first : Iterate.trueAndFalse) {
                        for (boolean inner : Iterate.trueAndFalse) {
                            Pose transform = segments.railTransforms[i].get(first);
                            Matrix4f pose2 = copy(transform.pose());
                            pose2.translate(new Vector3f(0, (i % 4) * 0.001f, 0));
                            out.add(CachedBuffers.partial(texturedPartial, state)
                                    .transform(nudge.last())
                                    .mulPose(pose2)
                                    .mulNormal(transform.normal())
                                    .translate((first ? -(61 / 64.) : -(1 / 32.)) + (inner ? 0 : (first ? 1 : -1)), shiftDown, 0)
                                    .cardinalLighting(cardinalLighting)
                                    .light(light)
                                    .extractRenderState());
                        }
                    }
                } else {
                    for (boolean first : Iterate.trueAndFalse) {
                        Pose transform = segments.railTransforms[i].get(first);
                        Matrix4f pose2 = copy(transform.pose());
                        pose2.translate(new Vector3f(0, (i % 4) * 0.001f, 0));
                        out.add(CachedBuffers.partial(texturedPartial, state)
                                .transform(nudge.last())
                                .mulPose(pose2)
                                .mulNormal(transform.normal())
                                .translate(-0.5 + (trackType == NARROW_GAUGE ? (first ? 0.5 : -0.5) : 0), shiftDown, 0)
                                .cardinalLighting(cardinalLighting)
                                .light(light)
                                .extractRenderState());
                    }
                }
            }
        }
    }

    public static List<Vec3> casingPositions(BezierConnection bc) {
        List<Vec3> positions = new ArrayList<>();
        List<int[]> takenPositions = new ArrayList<>();
        for (BezierConnection.Segment segment : bc) {
            double factor = 1.3;
            if (TrackMaterialData.of(bc.getMaterial()).trackType == WIDE_GAUGE)
                factor += 0.5;
            else if (TrackMaterialData.of(bc.getMaterial()).trackType == NARROW_GAUGE)
                factor -= 7 / 16.;
            Vec3 pos1 = segment.position.add(segment.normal.scale(factor));
            Vec3 pos2 = segment.position.add(segment.normal.scale(-factor));

            float steps = 4;
            Vec3 stepVec = pos1.vectorTo(pos2).scale(1 / steps);

            Vec3 curPos = pos1;

            for (int i = 0; i <= steps; i++) {
                int x = (int) Math.floor(curPos.x);
                int z = (int) Math.floor(curPos.z);
                if (takenPositions.stream().noneMatch(pos -> pos[0] == x && pos[1] == z)) {
                    takenPositions.add(new int[]{x, z});
                    positions.add(new Vec3(x, curPos.y - (3 / 16d), z));
                }
                curPos = curPos.add(stepVec);
            }
        }
        return positions.stream().toList();
    }

    public static TransformedInstance makeCasingInstance(PartialModel baseModel, Block casingBlock, InstancerProvider instancerProvider) {
        PartialModel texturedPartial = reTexture(baseModel, casingBlock);
        SimpleModel model = new BakedModelBuilder(texturedPartial.get())
                .materialFunc((ChunkSectionLayer layer, Boolean shaded) ->
					SimpleMaterial.builderOf(ModelUtil.getMaterial(ChunkSectionLayer.CUTOUT, shaded, false))
						.light(LightShaders.FLAT)
						.cardinalLightingMode(shaded ? CardinalLightingMode.CHUNK : CardinalLightingMode.OFF)
						.build()
				)
                .build();
        return instancerProvider.instancer(InstanceTypes.TRANSFORMED, model)
                .createInstance();
    }
}
