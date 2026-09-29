package net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel;

import com.simibubi.create.foundation.model.BakedModelWrapperWithData;
import com.simibubi.create.foundation.model.BakedQuadHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelData.Builder;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel.AmethystCollectorPanelLayout.Part;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystCollectorPanelModel extends BakedModelWrapperWithData {
    private static final ModelProperty<AmethystCollectorPanelLayout> LAYOUT = new ModelProperty<>();
    private final Map<AmethystCollectorPanelLayout, List<BakedQuad>> cache = new ConcurrentHashMap<>();

    public AmethystCollectorPanelModel(BakedModel originalModel) {
        super(originalModel);
    }

    private static @Unmodifiable List<BakedQuad> build(AmethystCollectorPanelLayout layout, List<BakedQuad> source) {
        Map<Direction, BakedQuad> templates = new EnumMap<>(Direction.class);
        for (BakedQuad quad : source) {
            templates.putIfAbsent(quad.getDirection(), quad);
        }
        List<BakedQuad> result = new ArrayList<>();
        for (Part part : layout.parts()) {
            for (Direction face : Direction.values()) {
                if (!part.panel() && face == Direction.UP) {
                    continue;
                }
                if (onInternalBoundary(layout, part, face)) {
                    continue;
                }
                BakedQuad template = templates.get(face);
                if (template == null) {
                    continue;
                }
                result.add(project(template, part, layout, face));
            }
        }
        return List.copyOf(result);
    }

    private static boolean onInternalBoundary(AmethystCollectorPanelLayout layout, Part part, Direction face) {
        return !layout.edge(face) && switch (face) {
            case WEST -> part.minX() == 0;
            case EAST -> part.maxX() == 16;
            case NORTH -> part.minZ() == 0;
            case SOUTH -> part.maxZ() == 16;
            default -> false;
        };
    }

    private static BakedQuad project(BakedQuad template, Part part, AmethystCollectorPanelLayout layout, Direction face) {
        BakedQuad quad = BakedQuadHelper.clone(template);
        int[] vertices = quad.getVertices();
        double[] us = new double[4];
        double[] vs = new double[4];
        int reversedZ = layout.depth() - 1 - layout.z();
        for (int i = 0; i < 4; i++) {
            Vec3 old = BakedQuadHelper.getXYZ(vertices, i);
            double x = old.x < 0.5 ? part.minX() : part.maxX();
            double y = old.y < 0.5625 ? part.minY() : part.maxY();
            double z = old.z < 0.5 ? part.minZ() : part.maxZ();
            BakedQuadHelper.setXYZ(vertices, i, new Vec3(x / 16, y / 16, z / 16));
            if (face == Direction.UP) {
                us[i] = AmethystCollectorPanelTextureMapping.topU(layout.x(), layout.width(), layout.depth(), x);
                vs[i] = AmethystCollectorPanelTextureMapping.topV(layout.z(), layout.width(), layout.depth(), z);
            }
            else if (face == Direction.DOWN) {
                us[i] = AmethystCollectorPanelTextureMapping.bottomU(layout.x(), layout.width(), layout.depth(), x);
                vs[i] = AmethystCollectorPanelTextureMapping.bottomV(reversedZ, layout.width(), layout.depth(), 16 - z);
            }
            else {
                int index = switch (face) {
                    case NORTH -> layout.width() - 1 - layout.x();
                    case SOUTH -> layout.x();
                    case WEST -> layout.z();
                    default -> reversedZ;
                };
                double horizontal = switch (face) {
                    case NORTH -> 16 - x;
                    case SOUTH -> x;
                    case WEST -> z;
                    default -> 16 - z;
                };
                us[i] = AmethystCollectorPanelTextureMapping.sideU(index, face.getAxis() == Axis.Z ? layout.width() : layout.depth(), horizontal);
                vs[i] = 64 - y;
            }
        }
        double centerU = (us[0] + us[1] + us[2] + us[3]) / 4;
        double centerV = (vs[0] + vs[1] + vs[2] + vs[3]) / 4;
        for (int i = 0; i < 4; i++) {
            BakedQuadHelper.setU(vertices, i, quad.getSprite().getU((float) ((us[i] * 0.9999 + centerU * 0.0001) / 128)));
            BakedQuadHelper.setV(vertices, i, quad.getSprite().getV((float) ((vs[i] * 0.9999 + centerV * 0.0001) / 128)));
        }
        return quad;
    }

    @Override
    protected Builder gatherModelData(Builder builder, BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData blockEntityData) {
        return builder.with(LAYOUT, AmethystCollectorPanelLayout.at(level, pos));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random, ModelData data, @Nullable RenderType renderType) {
        AmethystCollectorPanelLayout layout = data.get(LAYOUT);
        if (layout == null || layout.equals(AmethystCollectorPanelLayout.SINGLE)) {
            return super.getQuads(state, side, random, data, renderType);
        }
        if (side != null || state == null) {
            return List.of();
        }
        if (renderType != null && !getRenderTypes(state, random, data).contains(renderType)) {
            return List.of();
        }
        return cache.computeIfAbsent(layout, key -> build(key, super.getQuads(state, null, random, data, renderType)));
    }
}
