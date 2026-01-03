package net.ty.createcraftedbeginning.api.gas.gases;

import com.simibubi.create.foundation.ICapabilityProvider;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.config.CCBConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class OpenEndedGasPipe extends GasFlowSource {
    private static final String COMPOUND_KEY_LOCATION = "Location";
    private static final String COMPOUND_KEY_PULLING = "Pulling";
    private static final int MAX_CAPACITY = 1000;

    private final BlockPos pos;
    private final BlockPos outputPos;
    private final AABB area;
    private final Direction direction;
    private Level level;
    private OpenEndGasHandler gasHandler;
    private final ICapabilityProvider<IGasHandler> gasHandlerProvider = ICapabilityProvider.of(() -> gasHandler);
    private boolean wasPulling;

    public OpenEndedGasPipe(BlockFace face) {
        super(face);
        gasHandler = new OpenEndGasHandler();
        outputPos = face.getConnectedPos();
        pos = face.getPos();
        direction = face.getFace();
        area = new AABB(outputPos.relative(direction)).inflate(2);
    }

    public static @NotNull OpenEndedGasPipe read(@NotNull CompoundTag compoundTag, Provider registries, BlockPos blockEntityPos) {
        if (!compoundTag.contains(COMPOUND_KEY_LOCATION)) {
            throw new IllegalStateException("Invalid compound tag!");
        }

        BlockFace fromNBT = BlockFace.fromNBT(compoundTag.getCompound(COMPOUND_KEY_LOCATION));
        OpenEndedGasPipe pipe = new OpenEndedGasPipe(new BlockFace(blockEntityPos, fromNBT.getFace()));
        pipe.gasHandler.read(registries, compoundTag);
        pipe.wasPulling = compoundTag.contains(COMPOUND_KEY_PULLING) && compoundTag.getBoolean(COMPOUND_KEY_PULLING);
        return pipe;
    }

    public Level getLevel() {
        return level;
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
    @Nullable
    public ICapabilityProvider<IGasHandler> provideHandler() {
        return gasHandlerProvider;
    }

    @Override
    public boolean isEndpoint() {
        return true;
    }

    @Override
    public void manageSource(Level level, BlockEntity networkBE) {
        this.level = level;
    }

    public CompoundTag write(Provider registries) {
        CompoundTag compound = new CompoundTag();
        gasHandler.write(registries, compound);
        compound.putBoolean(COMPOUND_KEY_PULLING, wasPulling);
        compound.put(COMPOUND_KEY_LOCATION, location.serializeNBT());
        return compound;
    }

    private class OpenEndGasHandler extends GasTank {
        public OpenEndGasHandler() {
            super(MAX_CAPACITY);
        }

        @Override
        public long fill(@NotNull GasStack resource, @NotNull GasAction action) {
            if (level == null || !level.isLoaded(outputPos)) {
                return 0;
            }
            if (resource.isEmpty() || !isEmptySpace(resource)) {
                return 0;
            }

            GasStack containedGasStack = getGasStack();
            if (!containedGasStack.isEmpty() && !GasStack.isSameGas(containedGasStack, resource)) {
                setGasStack(GasStack.EMPTY);
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
                effectHandler.apply(level, area, resource.copy().getGas());
            }

            if (getGasAmount() == MAX_CAPACITY && isEmptySpace(containedGasStack)) {
                setGasStack(GasStack.EMPTY);
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
            if (level == null || !level.isLoaded(outputPos)) {
                return empty;
            }
            if (amount == 0) {
                return empty;
            }

            if (amount > MAX_CAPACITY) {
                amount = MAX_CAPACITY;
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

            GasStack drainedFromWorld = getGasFromSpace();
            if (drainedFromWorld.isEmpty()) {
                return GasStack.EMPTY;
            }
            if (filterPresent && !GasStack.isSameGas(drainedFromWorld, filter)) {
                return GasStack.EMPTY;
            }

            long remainder = drainedFromWorld.getAmount() - amount;
            drainedFromWorld.setAmount(amount);
            if (!action.simulate() && remainder > 0) {
                if (!getGasStack().isEmpty() && !GasStack.isSameGas(getGasStack(), drainedFromWorld)) {
                    setGasStack(GasStack.EMPTY);
                }
                super.fill(drainedFromWorld.copyWithAmount(remainder), GasAction.EXECUTE);
            }
            return drainedFromWorld;
        }

        private GasStack getGasFromSpace() {
            GasStack empty = GasStack.EMPTY;
            if (level == null || !level.isLoaded(outputPos)) {
                return empty;
            }
            if (!CCBConfig.server().gas.canExtractAirFromWorld.get()) {
                return empty;
            }

            BlockState currentState = level.getBlockState(pos);
            GasOpenPipeExtractHandler extractHandler = GasOpenPipeExtractHandler.REGISTRY.get(currentState.getBlock());
            return extractHandler != null ? extractHandler.extract(level, pos, currentState, direction) : empty;
        }

        private boolean isEmptySpace(GasStack gas) {
            return level != null && level.isLoaded(outputPos) && level.getBlockState(outputPos).isAir();
        }
    }
}
