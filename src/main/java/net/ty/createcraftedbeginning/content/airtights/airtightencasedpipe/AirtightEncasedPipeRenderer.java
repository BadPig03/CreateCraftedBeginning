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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class AirtightEncasedPipeRenderer extends SafeBlockEntityRenderer<AirtightEncasedPipeBlockEntity> {
    public AirtightEncasedPipeRenderer(Context ignored) {
        super();
    }

    @Override
    protected void renderSafe(@NotNull AirtightEncasedPipeBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        BlockState state = be.getBlockState();

        for (Direction direction : Iterate.directions) {
            if (!state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(direction))) {
                PartialModel model = getModel(direction);
                CachedBuffers.partial(model, state).light(light).renderInto(ms, bufferSource.getBuffer(RenderType.solid()));
            }
        }
    }

    @Contract(pure = true)
    private PartialModel getModel(@NotNull Direction direction) {
        return switch (direction) {
            case UP -> CCBPartialModels.ENCASED_UP;
            case DOWN -> CCBPartialModels.ENCASED_DOWN;
            case NORTH -> CCBPartialModels.ENCASED_NORTH;
            case EAST -> CCBPartialModels.ENCASED_EAST;
            case SOUTH -> CCBPartialModels.ENCASED_SOUTH;
            case WEST -> CCBPartialModels.ENCASED_WEST;
        };
    }
}
