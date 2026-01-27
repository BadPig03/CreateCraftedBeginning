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
    }

    public static @NotNull OpenEndedGasPipe read(@NotNull CompoundTag compoundTag, Provider provider, BlockPos blockPos) {
        OpenEndedGasPipe pipe = new OpenEndedGasPipe(new BlockFace(blockPos, BlockFace.fromNBT(compoundTag.getCompound(COMPOUND_KEY_LOCATION)).getFace()));
        pipe.gasHandler.read(provider, compoundTag);
        pipe.wasPulling = compoundTag.getBoolean(COMPOUND_KEY_PULLING);
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
    public void manageSource(Level level, BlockEntity blockEntity) {
        this.level = level;
    }

    public CompoundTag write(Provider provider) {
        CompoundTag compound = new CompoundTag();
        gasHandler.write(provider, compound);
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
            if (level == null || !level.isLoaded(outputPos) || resource.isEmpty() || !isEmptySpace(resource)) {
                return 0;
            }

            GasStack tankGasStack = getGasStack();
            if (!tankGasStack.isEmpty() && !GasStack.isSameGasSameComponents(tankGasStack, resource)) {
                setGasStack(GasStack.EMPTY);
            }
            if (wasPulling) {
                wasPulling = false;
            }

            GasOpenPipeEffectHandler effectHandler = GasOpenPipeEffectHandler.REGISTRY.get(resource.getGasType());
            if (effectHandler != null) {
                resource = resource.copy();
            }

            long filled = super.fill(resource, action);
            if (action.simulate()) {
                return filled;
            }

            if (effectHandler != null && !resource.isEmpty()) {
                effectHandler.apply(level, pos, direction, resource.copy().getGasType());
            }

            if (getGasAmount() == MAX_CAPACITY && isEmptySpace(tankGasStack)) {
                setGasStack(GasStack.EMPTY);
            }
            return filled;
        }

        @Override
        public @NotNull GasStack drain(@NotNull GasStack resource, @NotNull GasAction action) {
            return drainInner(resource.getAmount(), resource, action);
        }

        @Override
        public @NotNull GasStack drain(long maxDrain, @NotNull GasAction action) {
            return drainInner(maxDrain, null, action);
        }

        private GasStack drainInner(long amount, @Nullable GasStack resource, GasAction action) {
            if (level == null || !level.isLoaded(outputPos) || amount == 0) {
                return GasStack.EMPTY;
            }

            boolean emptyResource = resource == null;
            if (amount > MAX_CAPACITY) {
                amount = MAX_CAPACITY;
                if (!emptyResource) {
                    resource = resource.copyWithAmount(amount);
                }
            }

            if (!wasPulling) {
                wasPulling = true;
            }

            GasStack drainedFromInternal = emptyResource ? super.drain(amount, action) : super.drain(resource, action);
            if (!drainedFromInternal.isEmpty()) {
                return drainedFromInternal;
            }

            GasStack drainedFromWorld = getGasFromSpace();
            if (drainedFromWorld.isEmpty() || !emptyResource && !GasStack.isSameGasSameComponents(drainedFromWorld, resource)) {
                return GasStack.EMPTY;
            }

            long remainder = drainedFromWorld.getAmount() - amount;
            drainedFromWorld.setAmount(amount);
            if (!action.simulate() && remainder > 0) {
                if (!getGasStack().isEmpty() && !GasStack.isSameGasSameComponents(getGasStack(), drainedFromWorld)) {
                    setGasStack(GasStack.EMPTY);
                }
                super.fill(drainedFromWorld.copyWithAmount(remainder), GasAction.EXECUTE);
            }
            return drainedFromWorld;
        }

        private GasStack getGasFromSpace() {
            if (level == null || !level.isLoaded(outputPos) || !CCBConfig.server().airtights.canExtractAirFromWorld.get()) {
                return GasStack.EMPTY;
            }

            BlockState currentState = level.getBlockState(pos);
            if (!(level.getBlockEntity(pos) instanceof IGasExtractor extractor) || !extractor.canExtract(level, currentState, pos, direction)) {
                return GasStack.EMPTY;
            }

            BlockPos relativePos = pos.relative(direction);
            BlockState relativeState = level.getBlockState(relativePos);
            GasOpenPipeExtractHandler extractHandler = GasOpenPipeExtractHandler.REGISTRY.get(relativeState.getBlock());
            if (extractHandler == null) {
                return GasStack.EMPTY;
            }

            return new GasStack(extractHandler.extract(level, relativePos, relativeState), MAX_CAPACITY);
        }

        private boolean isEmptySpace(GasStack gas) {
            return level != null && level.isLoaded(outputPos) && level.getBlockState(outputPos).isAir();
        }
    }
}
