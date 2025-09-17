package net.ty.createcraftedbeginning.api.gas;

import com.simibubi.create.foundation.ICapabilityProvider;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.api.gas.interfaces.GasOpenPipeEffectHandler;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBGases;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class OpenEndedGasPipe extends GasFlowSource {
    private final BlockPos pos;
    private final BlockPos outputPos;
    private final AABB area;
    private Level world;
    private OpenEndGasHandler gasHandler;
    private final ICapabilityProvider<IGasHandler> gasHandlerProvider = ICapabilityProvider.of(() -> gasHandler);
    private boolean wasPulling;

    public OpenEndedGasPipe(BlockFace face) {
        super(face);
        gasHandler = new OpenEndGasHandler();
        outputPos = face.getConnectedPos();
        pos = face.getPos();
        area = new AABB(outputPos.relative(face.getFace())).inflate(2);
    }

    public static @NotNull OpenEndedGasPipe fromNBT(@NotNull CompoundTag compound, HolderLookup.Provider registries, BlockPos blockEntityPos) {
        BlockFace fromNBT = BlockFace.fromNBT(compound.getCompound("Location"));
        OpenEndedGasPipe pipe = new OpenEndedGasPipe(new BlockFace(blockEntityPos, fromNBT.getFace()));

        pipe.gasHandler.readFromNBT(registries, compound);
        pipe.wasPulling = compound.getBoolean("Pulling");
        return pipe;
    }

    public Level getWorld() {
        return world;
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockPos getOutputPos() {
        return outputPos;
    }

    public AABB getArea() {
        return area;
    }

    @Override
    public boolean isEndpoint() {
        return true;
    }

    @Override
    public void manageSource(Level world, BlockEntity networkBE) {
        this.world = world;
    }

    @Override
    @Nullable
    public ICapabilityProvider<IGasHandler> provideHandler() {
        return gasHandlerProvider;
    }

    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag compound = new CompoundTag();
        gasHandler.writeToNBT(registries, compound);
        compound.putBoolean("Pulling", wasPulling);
        compound.put("Location", location.serializeNBT());
        return compound;
    }

    private GasStack getGasFromSpace(boolean simulate) {
        GasStack empty = GasStack.EMPTY;
        if (world == null || !world.isLoaded(outputPos)) {
            return empty;
        }

        BlockState state = world.getBlockState(outputPos);
        BlockState currentState = world.getBlockState(pos);
        if (!currentState.is(CCBBlocks.AIRTIGHT_PIPE_BLOCK) || !state.isAir()) {
            return empty;
        }

        if (!CCBConfig.server().compressedAir.canAbsorbAirFromWorld.get()) {
            return empty;
        }

        DimensionType dimensionType = world.dimensionType();
        GasType<Gas> gas;
        if (dimensionType.ultraWarm()) {
            gas = CCBGases.ULTRAWARM_AIR;
        } else if (dimensionType.natural()) {
            gas = CCBGases.NATURAL_AIR;
        } else {
            gas = CCBGases.ETHEREAL_AIR;
        }

        return new GasStack(gas, 1000);
    }

    private boolean isEmptySpace(GasStack gas) {
        if (world == null || !world.isLoaded(outputPos)) {
            return false;
        }

        return world.getBlockState(outputPos).isAir();
    }

    private class OpenEndGasHandler extends GasTank {
        public OpenEndGasHandler() {
            super(1000);
        }

        @Override
        public long fill(@NotNull GasStack resource, @NotNull GasAction action) {
            if (world == null || !world.isLoaded(outputPos)) {
                return 0;
            }
            if (resource.isEmpty() || !isEmptySpace(resource)) {
                return 0;
            }

            GasStack containedGasStack = getGas();
            if (!containedGasStack.isEmpty() && !GasStack.isSameGas(containedGasStack, resource)) {
                setGas(GasStack.EMPTY);
            }
            if (wasPulling) {
                wasPulling = false;
            }

            GasOpenPipeEffectHandler effectHandler = GasOpenPipeEffectHandler.REGISTRY.get(resource.getGas());
            if (effectHandler != null) {
                resource = resource.copyWithAmount(1);
            }

            long fill = super.fill(resource, action);
            if (action.simulate()) {
                return fill;
            }

            if (effectHandler != null && !resource.isEmpty()) {
                effectHandler.apply(world, area, resource.copy().getGas());
            }

            if (getGasAmount() == 1000 && isEmptySpace(containedGasStack)) {
                setGas(GasStack.EMPTY);
            }
            return fill;
        }

        @Override
        public @NotNull GasStack drain(@NotNull GasStack resource, @NotNull GasAction action) {
            return drainInner(resource.getAmount(), resource, action);
        }

        @Override
        public @NotNull GasStack drain(long maxDrain, @NotNull GasAction action) {
            return drainInner(maxDrain, null, action);
        }

        private GasStack drainInner(long amount, @Nullable GasStack filter, GasAction action) {
            GasStack empty = GasStack.EMPTY;
            boolean filterPresent = filter != null;

            if (world == null || !world.isLoaded(outputPos)) {
                return empty;
            }

            if (amount == 0) {
                return empty;
            }

            if (amount > 1000) {
                amount = 1000;
                if (filterPresent) {
                    filter = filter.copyWithAmount(amount);
                }
            }

            if (!wasPulling) {
                wasPulling = true;
            }

            GasStack drainedFromInternal = filterPresent ? super.drain(filter, action) : super.drain(amount, action);
            if (!drainedFromInternal.isEmpty()) {
                return drainedFromInternal;
            }

            GasStack drainedFromWorld = getGasFromSpace(action.simulate());
            if (drainedFromWorld.isEmpty()) {
                return GasStack.EMPTY;
            }
            if (filterPresent && !GasStack.isSameGas(drainedFromWorld, filter)) {
                return GasStack.EMPTY;
            }

            long remainder = drainedFromWorld.getAmount() - amount;
            drainedFromWorld.setAmount(amount);

            if (!action.simulate() && remainder > 0) {
                if (!getGas().isEmpty() && !GasStack.isSameGas(getGas(), drainedFromWorld)) {
                    setGas(GasStack.EMPTY);
                }
                super.fill(drainedFromWorld.copyWithAmount(remainder), GasAction.EXECUTE);
            }
            return drainedFromWorld;
        }
    }
}
