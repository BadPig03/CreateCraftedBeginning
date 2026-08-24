package net.ty.createcraftedbeginning.content.airtights.gasfactorygauge;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerBlockEntity;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasFactoryGaugeAttachment {
    private final GasFactoryGaugeBlockEntity blockEntity;

    public GasFactoryGaugeAttachment(GasFactoryGaugeBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    public Detection detectAttachedPackager() {
        Level level = blockEntity.getLevel();
        BlockState blockState = blockEntity.getBlockState();
        if (level == null || !(blockState.getBlock() instanceof GasFactoryGaugeBlock)) {
            return Detection.UNAVAILABLE;
        }

        BlockPos packagerPos = getAttachedPosition(blockState);
        if (!level.isLoaded(packagerPos)) {
            return Detection.UNAVAILABLE;
        }
        return level.getBlockEntity(packagerPos) instanceof GasPackagerBlockEntity ? Detection.ATTACHED : Detection.DETACHED;
    }

    @Nullable public GasPackagerBlockEntity findAttachedPackager() {
        Level level = blockEntity.getLevel();
        BlockState blockState = blockEntity.getBlockState();
        if (level == null || !(blockState.getBlock() instanceof GasFactoryGaugeBlock)) {
            return null;
        }

        BlockPos packagerPos = getAttachedPosition(blockState);
        if (!level.isLoaded(packagerPos)) {
            return null;
        }
        return level.getBlockEntity(packagerPos) instanceof GasPackagerBlockEntity packager ? packager : null;
    }

    private BlockPos getAttachedPosition(BlockState blockState) {
        Direction packagerDirection = FactoryPanelBlock.connectedDirection(blockState).getOpposite();
        return blockEntity.getBlockPos().relative(packagerDirection);
    }

    public enum Detection {
        UNAVAILABLE,
        ATTACHED,
        DETACHED
    }
}
