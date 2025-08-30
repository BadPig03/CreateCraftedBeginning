package net.ty.createcraftedbeginning.content.airtighttank;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.simibubi.create.api.contraption.storage.fluid.WrapperMountedFluidStorage;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.ty.createcraftedbeginning.data.CCBTags;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AirtightTankMountedStorage extends WrapperMountedFluidStorage<AirtightTankMountedStorage.Handler> implements SyncedMountedStorage {
    public static final MapCodec<AirtightTankMountedStorage> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(ExtraCodecs.NON_NEGATIVE_INT.fieldOf("capacity").forGetter(AirtightTankMountedStorage::getCapacity), FluidStack.OPTIONAL_CODEC.fieldOf("fluid").forGetter(AirtightTankMountedStorage::getFluid)).apply(i, AirtightTankMountedStorage::new));

    private boolean dirty;

    protected AirtightTankMountedStorage(MountedFluidStorageType<AirtightTankMountedStorage> type, int capacity, FluidStack stack) {
        super(type, new AirtightTankMountedStorage.Handler(capacity, stack));
        this.wrapped.onChange = () -> this.dirty = true;
    }

    protected AirtightTankMountedStorage(int capacity, FluidStack stack) {
        this(CCBMountedStorage.AIRTIGHT_TANK.get(), capacity, stack);
    }

    public static AirtightTankMountedStorage fromTank(AirtightTankBlockEntity tank) {
        FluidTank inv = tank.getTankInventory();
        return new AirtightTankMountedStorage(inv.getCapacity(), inv.getFluid().copy());
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (!(be instanceof AirtightTankBlockEntity tank)) {
            return;
        }
        if (!tank.isController()) {
            return;
        }
        FluidTank inv = tank.getTankInventory();
        inv.setFluid(this.wrapped.getFluid());
    }

    public FluidStack getFluid() {
        return this.wrapped.getFluid();
    }

    public int getCapacity() {
        return this.wrapped.getCapacity();
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void markClean() {
        this.dirty = false;
    }

    @Override
    public void afterSync(Contraption contraption, BlockPos localPos) {
        BlockEntity be = contraption.presentBlockEntities.get(localPos);
        if (!(be instanceof AirtightTankBlockEntity tank)) {
            return;
        }

        FluidTank inv = tank.getTankInventory();
        inv.setFluid(this.getFluid());
    }

    public static final class Handler extends FluidTank {
        private Runnable onChange = () -> {
        };

        public Handler(int capacity, FluidStack stack) {
            super(capacity);
            this.setFluid(stack);
        }

        @Override
        protected void onContentsChanged() {
            this.onChange.run();
        }

        @Override
        public boolean isFluidValid(@NotNull FluidStack fluidStack) {
            return fluidStack.is(CCBTags.commonFluidTag("compressed_air"));
        }
    }
}
