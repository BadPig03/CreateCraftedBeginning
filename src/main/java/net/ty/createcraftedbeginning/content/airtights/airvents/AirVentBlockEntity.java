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
    protected final AirVentLouverState louvers = new AirVentLouverState();

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
    protected void loadAdditional(CompoundTag tag, Provider provider) {
        super.loadAdditional(tag, provider);
        louvers.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, Provider provider) {
        super.saveAdditional(tag, provider);
        louvers.save(tag);
    }

    public VentState getLouverState(Direction direction) {
        return louvers.getLouverState(direction);
    }

    public boolean hasLouver(Direction direction) {
        return louvers.hasLouver(direction);
    }

    public boolean isLouverOpen(Direction direction) {
        return louvers.isLouverOpen(direction);
    }

    public int getVisibleLouverMask() {
        return louvers.getVisibleLouverMask(AirVentBlock.getConnectionMask(getBlockState()));
    }

    public int getOpenedLouverMask() {
        return louvers.getOpenedLouverMask();
    }

    public void toggleLouver(Direction direction) {
        if (!louvers.toggleLouver(direction)) {
            return;
        }

        notifyUpdate();
    }

    public void toggleLouverOpen(Direction direction) {
        if (!louvers.toggleLouverOpen(direction)) {
            return;
        }

        notifyUpdate();
    }

    public void setLouverState(Direction direction, VentState state) {
        if (!louvers.setLouverState(direction, state)) {
            return;
        }

        notifyUpdate();
    }
}
