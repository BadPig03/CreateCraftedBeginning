package net.ty.createcraftedbeginning.content.airtights.airvents;

import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlock.VentState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirVentBlockEntity extends SyncedBlockEntity {
    private final AirVentLouverState louvers = new AirVentLouverState();

    public AirVentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = getBlockState();
        BlockState connectedState = AirVentBlock.withConnections(state, level, worldPosition);
        if (connectedState == state) {
            return;
        }

        level.setBlockAndUpdate(worldPosition, connectedState);
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, Provider provider) {
        super.loadAdditional(compoundTag, provider);
        louvers.load(compoundTag);
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, Provider provider) {
        super.saveAdditional(compoundTag, provider);
        louvers.save(compoundTag);
    }

    public void setLouverState(Direction direction, VentState state) {
        if (!louvers.setLouverState(direction, state)) {
            return;
        }

        notifyUpdate();
    }

    VentState getLouverState(Direction direction) {
        return louvers.getLouverState(direction);
    }

    boolean hasLouver(Direction direction) {
        return louvers.hasLouver(direction);
    }

    boolean isLouverOpen(Direction direction) {
        return louvers.isLouverOpen(direction);
    }

    int getVisibleLouverMask() {
        return louvers.getVisibleLouverMask(AirVentBlock.getConnectionMask(getBlockState()));
    }

    int getOpenedLouverMask() {
        return louvers.getOpenedLouverMask();
    }

    void toggleLouver(Direction direction) {
        if (!louvers.toggleLouver(direction)) {
            return;
        }

        notifyUpdate();
    }

    void toggleLouverOpen(Direction direction) {
        if (!louvers.toggleLouverOpen(direction)) {
            return;
        }

        notifyUpdate();
    }
}
