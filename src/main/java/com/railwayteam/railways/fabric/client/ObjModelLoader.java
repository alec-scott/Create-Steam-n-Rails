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
package com.railwayteam.railways.fabric.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.Transparency;
import com.mojang.math.MatrixUtil;
import com.railwayteam.railways.Railways;
import net.fabricmc.fabric.api.client.model.loading.v1.UnbakedModelDeserializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads the Wavefront OBJ block models (boilers, bogeys, buffers, monorail and gauge track bases).
 *
 * <p>PORT-NOTE(26.1): 1.20 used Porting Lib's OBJ loader ({@code "loader": "forge:obj"}). The models now declare
 * {@code "fabric:type": "railways:obj"} and are baked here into plain quads. Only what Railways' files use is
 * supported: {@code v}/{@code vt}/{@code f} (triangles and quads, positive indices), {@code usemtl}, and
 * {@code mtllib} materials whose {@code map_Kd} names a texture slot ({@code #slot}).</p>
 */
public final class ObjModelLoader {
    public static final Identifier ID = Railways.asResource("obj");

    public static void register() {
        UnbakedModelDeserializer.register(ID, ObjModelLoader::deserialize);
    }

    private static UnbakedModel deserialize(JsonObject json, JsonDeserializationContext context) {
        String parent = GsonHelper.getAsString(json, "parent", "");
        Boolean ambientOcclusion = json.has("ambientocclusion") ? Boolean.valueOf(GsonHelper.getAsBoolean(json, "ambientocclusion"))
            : json.has("ambient_occlusion") ? Boolean.valueOf(GsonHelper.getAsBoolean(json, "ambient_occlusion")) : null;
        ItemTransforms transforms = json.has("display")
            ? context.deserialize(GsonHelper.getAsJsonObject(json, "display"), ItemTransforms.class) : null;
        TextureSlots.Data textures = json.has("textures")
            ? TextureSlots.parseTextureMap(GsonHelper.getAsJsonObject(json, "textures")) : TextureSlots.Data.EMPTY;
        // "model" is a full resource path, e.g. railways:models/block/palettes/boiler/boiler_x.obj
        ObjGeometry geometry = new ObjGeometry(Identifier.parse(GsonHelper.getAsString(json, "model")),
            GsonHelper.getAsBoolean(json, "flip_v", false));
        return new Model(parent.isEmpty() ? null : Identifier.parse(parent), ambientOcclusion, transforms, textures, geometry);
    }

    private record Model(@Nullable Identifier parent, @Nullable Boolean ambientOcclusion,
                         @Nullable ItemTransforms transforms, TextureSlots.Data textureSlots,
                         UnbakedGeometry geometry) implements UnbakedModel {}

    private record Face(String material, int[] positions, int[] uvs) {}

    private record ObjGeometry(Identifier location, boolean flipV) implements UnbakedGeometry {
        @Override
        public QuadCollection bake(TextureSlots textureSlots, ModelBaker baker, ModelState modelState, ModelDebugName debugName) {
            List<Vector3f> positions = new ArrayList<>();
            List<float[]> uvs = new ArrayList<>();
            List<Face> faces = new ArrayList<>();
            Map<String, String> materialSlots = new HashMap<>();
            try {
                read(positions, uvs, faces, materialSlots);
            } catch (IOException | RuntimeException e) {
                Railways.LOGGER.error("Failed to load OBJ model {} for {}", location, debugName.debugName(), e);
                return QuadCollection.EMPTY;
            }

            Matrix4fc transform = modelState.transformation().getMatrix();
            boolean identity = MatrixUtil.isIdentity(transform);
            ModelBaker.Interner interner = baker.interner();
            QuadCollection.Builder builder = new QuadCollection.Builder();
            for (Face face : faces) {
                String slot = materialSlots.getOrDefault(face.material(), face.material());
                Material.Baked material = baker.materials().resolveSlot(textureSlots, slot, debugName);
                TextureAtlasSprite sprite = material.sprite();

                Vector3fc[] quadPositions = new Vector3fc[4];
                long[] quadUvs = new long[4];
                float minU = 1, minV = 1, maxU = 0, maxV = 0;
                for (int i = 0; i < 4; i++) {
                    // triangles repeat their last vertex
                    int index = Math.min(i, face.positions().length - 1);
                    Vector3f pos = new Vector3f(positions.get(face.positions()[index]));
                    if (!identity) {
                        pos.sub(0.5f, 0.5f, 0.5f);
                        transform.transformPosition(pos);
                        pos.add(0.5f, 0.5f, 0.5f);
                    }
                    quadPositions[i] = interner.vector(pos);
                    float[] uv = face.uvs()[index] < 0 ? new float[] {0, 0} : uvs.get(face.uvs()[index]);
                    float u = uv[0];
                    float v = flipV ? 1 - uv[1] : uv[1];
                    minU = Math.min(minU, u);
                    maxU = Math.max(maxU, u);
                    minV = Math.min(minV, v);
                    maxV = Math.max(maxV, v);
                    quadUvs[i] = UVPair.pack(sprite.getU(u), sprite.getV(v));
                }

                Transparency transparency = material.forceTranslucent() ? Transparency.TRANSLUCENT
                    : sprite.contents().computeTransparency(clamp(minU), clamp(minV), clamp(maxU), clamp(maxV));
                BakedQuad.MaterialInfo info = interner.materialInfo(BakedQuad.MaterialInfo.of(material, transparency, -1, true, 0));
                builder.addUnculledFace(new BakedQuad(quadPositions[0], quadPositions[1], quadPositions[2], quadPositions[3],
                    quadUvs[0], quadUvs[1], quadUvs[2], quadUvs[3], facing(quadPositions), info));
            }
            return builder.build();
        }

        private void read(List<Vector3f> positions, List<float[]> uvs, List<Face> faces, Map<String, String> materialSlots) throws IOException {
            String material = "";
            try (BufferedReader reader = open(location)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.trim().split("\\s+");
                    switch (parts[0]) {
                        case "v" -> positions.add(new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])));
                        case "vt" -> uvs.add(new float[] {Float.parseFloat(parts[1]), Float.parseFloat(parts[2])});
                        case "usemtl" -> material = parts[1];
                        case "mtllib" -> readMaterials(sibling(parts[1]), materialSlots);
                        case "f" -> faces.addAll(parseFace(material, parts));
                        default -> {}
                    }
                }
            }
        }

        /** Triangles and quads are kept as they are; larger polygons are fanned into triangles. */
        private static List<Face> parseFace(String material, String[] parts) {
            int count = parts.length - 1;
            int[] pos = new int[count];
            int[] uv = new int[count];
            for (int i = 0; i < count; i++) {
                String[] indices = parts[i + 1].split("/");
                pos[i] = Integer.parseInt(indices[0]) - 1;
                uv[i] = indices.length > 1 && !indices[1].isEmpty() ? Integer.parseInt(indices[1]) - 1 : -1;
            }
            if (count <= 4)
                return List.of(new Face(material, pos, uv));
            List<Face> fan = new ArrayList<>();
            for (int i = 1; i < count - 1; i++)
                fan.add(new Face(material, new int[] {pos[0], pos[i], pos[i + 1]}, new int[] {uv[0], uv[i], uv[i + 1]}));
            return fan;
        }

        private static void readMaterials(Identifier mtl, Map<String, String> materialSlots) throws IOException {
            String material = null;
            try (BufferedReader reader = open(mtl)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts[0].equals("newmtl"))
                        material = parts[1];
                    else if (parts[0].equals("map_Kd") && material != null && parts[1].startsWith("#"))
                        materialSlots.put(material, parts[1].substring(1));
                }
            }
        }

        private Identifier sibling(String name) {
            String path = location.getPath();
            return location.withPath(path.substring(0, path.lastIndexOf('/') + 1) + name);
        }

        private static BufferedReader open(Identifier location) throws IOException {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isEmpty())
                throw new IOException("Missing resource " + location);
            return resource.get().openAsReader();
        }

        private static float clamp(float uv) {
            return Math.clamp(uv, 0f, 1f);
        }

        /** Counter-clockwise winding, as in OBJ; the first three vertices are enough for planar quads. */
        private static Direction facing(Vector3fc[] quad) {
            Vector3f normal = new Vector3f(quad[1]).sub(quad[0]).cross(new Vector3f(quad[2]).sub(quad[0]));
            return Direction.getApproximateNearest(normal.x, normal.y, normal.z);
        }
    }

    private ObjModelLoader() {}
}
