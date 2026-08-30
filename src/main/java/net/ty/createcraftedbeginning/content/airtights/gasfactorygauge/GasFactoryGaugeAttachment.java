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
final class GasFactoryGaugeAttachment {
    private final GasFactoryGaugeBlockEntity blockEntity;

    GasFactoryGaugeAttachment(GasFactoryGaugeBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    Detection detectAttachedPackager() {
        Level level = blockEntity.getLevel();
        BlockState blockState = blockEntity.getBlockState();
        if (level == null || !(blockState.getBlock() instanceof GasFactoryGaugeBlock)) {
            return Detection.UNAVAILABLE;
        }

        BlockPos packagerPos = getAttachedPosition(blockState);
        if (!level.isLoaded(packagerPos)) {
            return Detection.UNAVAILABLE;
        }

        if (!(level.getBlockEntity(packagerPos) instanceof GasPackagerBlockEntity)) {
            return Detection.DETACHED;
        }
        return Detection.ATTACHED;
    }

    @Nullable GasPackagerBlockEntity findAttachedPackager() {
        Level level = blockEntity.getLevel();
        BlockState blockState = blockEntity.getBlockState();
        if (level == null || !(blockState.getBlock() instanceof GasFactoryGaugeBlock)) {
            return null;
        }

        BlockPos packagerPos = getAttachedPosition(blockState);
        if (!level.isLoaded(packagerPos) || !(level.getBlockEntity(packagerPos) instanceof GasPackagerBlockEntity packager)) {
            return null;
        }
        return packager;
    }

    private BlockPos getAttachedPosition(BlockState blockState) {
        Direction packagerDirection = FactoryPanelBlock.connectedDirection(blockState).getOpposite();
        return blockEntity.getBlockPos().relative(packagerDirection);
    }

    enum Detection {
        UNAVAILABLE,
        ATTACHED,
        DETACHED
    }
}
