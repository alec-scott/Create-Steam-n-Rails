/*
 * Steam 'n' Rails
 * Copyright (c) 2026 The Railways Team
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

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * PORT-NOTE(26.1): entity rendering is split into extraction and submission, so everything the conductor renderer
 * and its layers used to read from the entity is copied here.
 */
public class ConductorRenderState extends HumanoidRenderState {
  public Identifier texture = ConductorRenderer.TEXTURE;
  public DyeColor color = ConductorEntity.defaultColor();
  public ConductorEntity.Job job = ConductorEntity.Job.DEFAULT;
  public boolean holdingSchedules;

  /** The secondary head item, extracted the same way vanilla extracts the worn head item. */
  public final LivingEntityRenderState secondaryHead = new LivingEntityRenderState();

  public boolean carryingToolbox;
  public DyeColor toolboxColor = DyeColor.BROWN;
  public float toolboxLidAngle;
  public float toolboxDrawerOffset;
  public BlockState toolboxBlockState = Blocks.AIR.defaultBlockState();

  /** Set when this conductor is the disguise of a player; rotations are delegated to the player's renderer. */
  public @Nullable LivingEntityRenderState visualBaseState;
  public @Nullable LivingEntityRenderer<?, ?, ?> visualBaseRenderer;
}
