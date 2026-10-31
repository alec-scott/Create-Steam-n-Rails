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

package com.railwayteam.railways.content.conductor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.railwayteam.railways.Railways;
import com.railwayteam.railways.registry.CRBlockPartials;
import com.zurrtum.create.catnip.data.Pair;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * PORT-NOTE(26.1): models are posed from a state when they are drawn, after the submit, and armor renderers submit
 * rather than draw. The cap model is shared between entities, so its pose (copied from the wearer's head, or the
 * conductor's tilted cap anchor) travels in a {@link CapState} with each submit. Custom caps are partial models, and
 * 26.1 block state models have no item display transforms, so their {@code display.head} transform is read from the
 * model JSON (following parents) and cached.
 */
public class ConductorCapModel extends Model<ConductorCapModel.CapState> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Railways.asResource("conductor_cap"), "main");
	private final ModelPart cap;
	@Nullable
	private final PartialModel override;
	private final boolean doNotTilt;

	public record CapState(PartPose pose, boolean onPlayer) {}

	public ConductorCapModel(ModelPart root, @Nullable PartialModel override, boolean doNotTilt) {
		super(root, RenderTypes::armorCutoutNoCull);
		this.cap = root.getChild("cap");
		this.override = override;
		this.doNotTilt = doNotTilt;
	}

	private static ConductorCapModel defaultModel = null;
	private static final Map<Pair<String, Boolean>, ConductorCapModel> customModels = new HashMap<>();
	private static final Map<Identifier, ItemTransform> headTransforms = new HashMap<>();

	public static void clearModelCache() {
		defaultModel = null;
		customModels.clear();
		headTransforms.clear();
	}

	public static ConductorCapModel of(ItemStack stack, boolean onConductor) {
		if (defaultModel == null) {
			EntityModelSet set = Minecraft.getInstance().getEntityModels();
			ModelPart root = set.bakeLayer(ConductorCapModel.LAYER_LOCATION);
			CRBlockPartials.CUSTOM_CONDUCTOR_CAPS.forEach((name, partial) -> {
				ConductorCapModel model = new ConductorCapModel(root, partial, CRBlockPartials.shouldPreventTiltingCap(name));
				customModels.put(Pair.of(name, false), model); // normal caps should apply whether they are on a conductor
				customModels.put(Pair.of(name, true), model);
			});
			CRBlockPartials.CUSTOM_CONDUCTOR_ONLY_CAPS.forEach((name, partial) -> {
				ConductorCapModel model = new ConductorCapModel(root, partial, CRBlockPartials.shouldPreventTiltingCap(name));
				Pair<String, Boolean> key = Pair.of(name, true); // for conductors, override conductor model
				customModels.put(key, model);
			});
			defaultModel = new ConductorCapModel(root, null, false);
		}
		String name = stack.getHoverName().getString();
		if (name.startsWith("[sus]"))
			name = name.substring(5);
		return customModels.getOrDefault(Pair.of(name, onConductor), defaultModel);
	}

	/** Submits the cap worn on the head of {@code base}, which has already been posed for this entity. */
	public static void submit(PoseStack poseStack, SubmitNodeCollector queue, ItemStack stack, HumanoidRenderState state,
							  int packedLight, HumanoidModel<?> base, Identifier texture) {
		ConductorCapModel model = of(stack, state instanceof ConductorRenderState);
		ModelPart source = !model.doNotTilt && base instanceof ConductorEntityModel conductorModel && conductorModel.capAnchor != null
			? conductorModel.capAnchor
			: base.head;

		if (model.override != null) {
			poseStack.pushPose();
			source.translateAndRotate(poseStack);
			poseStack.translate(0, -0.25d, 0);
			poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
			poseStack.scale(0.625F, -0.625F, -0.625F);
			getHeadTransform(model.override.modelLocation()).apply(false, poseStack.last());
			poseStack.translate(-0.5, -0.5, -0.5);
			CachedBuffers.partial(model.override, Blocks.AIR.defaultBlockState())
				.light(packedLight)
				.overlay(OverlayTexture.NO_OVERLAY)
				.submit(poseStack, queue);
			poseStack.popPose();
		} else {
			CapState capState = new CapState(source.storePose(), base instanceof PlayerModel);
			queue.submitModel(model, capState, poseStack, model.renderType(texture), packedLight,
				OverlayTexture.NO_OVERLAY, state.outlineColor, null);
		}
	}

	@Override
	public void setupAnim(CapState state) {
		this.cap.loadPose(state.pose());
		if (state.onPlayer()) {
			float s = 1 / 16f;
			this.cap.offsetScale(new Vector3f(s, s, s));
			this.cap.offsetPos(new Vector3f(0, 0, -10 / 16f));
		}
	}

	private static ItemTransform getHeadTransform(Identifier modelLocation) {
		return headTransforms.computeIfAbsent(modelLocation, location -> {
			Identifier current = location;
			for (int depth = 0; current != null && depth < 16; depth++) {
				Optional<Resource> resource = Minecraft.getInstance().getResourceManager()
					.getResource(current.withPath(path -> "models/" + path + ".json"));
				if (resource.isEmpty())
					break;
				CuboidModel cuboidModel;
				try (Reader reader = resource.get().openAsReader()) {
					cuboidModel = CuboidModel.fromStream(reader);
				} catch (Exception e) {
					Railways.LOGGER.warn("Failed to read the display transforms of conductor cap model {}", current, e);
					break;
				}
				ItemTransforms transforms = cuboidModel.transforms();
				if (transforms != null && transforms.head() != ItemTransform.NO_TRANSFORM)
					return transforms.head();
				current = cuboidModel.parent();
			}
			return ItemTransform.NO_TRANSFORM;
		});
	}

	public static LayerDefinition createBodyLayer () {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition hat = partdefinition.addOrReplaceChild("cap",
			CubeListBuilder.create().texOffs(0, 0)
				.addBox(-4.5F, -9.0F, -4.0F, 9.0F, 3.0F, 9.0F, new CubeDeformation(0.0F)),
			PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition brim = hat.addOrReplaceChild("brim",
			CubeListBuilder.create().texOffs(6, 12)
				.addBox(-4.5F, 3.8F, -3.0F, 9.0F, 0.02F, 3.0F, new CubeDeformation(0.0F)),
			PartPose.offsetAndRotation(0.0F, -10.0F, -4.0F, 0.2618F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}
}
