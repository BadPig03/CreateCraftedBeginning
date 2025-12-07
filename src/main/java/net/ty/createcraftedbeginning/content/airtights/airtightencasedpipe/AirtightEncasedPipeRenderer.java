package net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class AirtightEncasedPipeRenderer extends SafeBlockEntityRenderer<AirtightEncasedPipeBlockEntity> {
    private static final Map<Direction, PartialModel> MODEL_CACHE = Arrays.stream(Direction.values()).collect(Collectors.toMap(dir -> dir, dir -> switch (dir) {
        case DOWN -> CCBPartialModels.ENCASED_DOWN;
        case UP -> CCBPartialModels.ENCASED_UP;
        case NORTH -> CCBPartialModels.ENCASED_NORTH;
        case SOUTH -> CCBPartialModels.ENCASED_SOUTH;
        case WEST -> CCBPartialModels.ENCASED_WEST;
        case EAST -> CCBPartialModels.ENCASED_EAST;
    }));

    public AirtightEncasedPipeRenderer(Context ignored) {
    }

    @Override
    protected void renderSafe(@NotNull AirtightEncasedPipeBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        BlockState state = be.getBlockState();
        for (Direction direction : Iterate.directions) {
            if (AirtightEncasedPipeBlock.isOpenAt(state, direction)) {
                continue;
            }

            CachedBuffers.partial(MODEL_CACHE.get(direction), state).light(light).renderInto(ms, bufferSource.getBuffer(RenderType.solid()));
        }
    }
}
