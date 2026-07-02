package net.ty.createcraftedbeginning.api.gas.gases;

import com.simibubi.create.foundation.ICapabilityProvider;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.effecthandlers.AirtightPipeEffectHandler;
import net.ty.createcraftedbeginning.api.gas.extracthandlers.AirtightPipeExtractHandler;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.GasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasExtractor;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.data.CCBGases;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class OpenEndedAirtightPipe extends GasFlowSource {
    private static final String COMPOUND_KEY_LOCATION = "Location";
    private static final int OPEN_END_BUFFER_CAPACITY = 1000;

    private final BlockPos pos;
    private final BlockPos outputPos;
    private final Direction direction;
    private Level level;
    private OpenEndGasHandler gasHandler;
    private final ICapabilityProvider<IGasHandler> gasHandlerProvider = ICapabilityProvider.of(() -> gasHandler);

    public OpenEndedAirtightPipe(BlockFace face) {
        super(face);
        gasHandler = new OpenEndGasHandler();
        outputPos = face.getConnectedPos();
        pos = face.getPos();
        direction = face.getFace();
    }

    public static OpenEndedAirtightPipe read(CompoundTag compoundTag, Provider provider, BlockPos blockPos) {
        OpenEndedAirtightPipe pipe = new OpenEndedAirtightPipe(new BlockFace(blockPos, BlockFace.fromNBT(compoundTag.getCompound(COMPOUND_KEY_LOCATION)).getFace()));
        pipe.gasHandler.read(provider, compoundTag);
        return pipe;
    }

    public Level getLevel() {
        return level;
    }

    public BlockPos getPos() {
        return pos;
    }

    @Override
    @Nullable
    public ICapabilityProvider<IGasHandler> getGasHandlerProvider() {
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
        compound.put(COMPOUND_KEY_LOCATION, location.serializeNBT());
        return compound;
    }

    private class OpenEndGasHandler extends GasTank {
        public OpenEndGasHandler() {
            super(OPEN_END_BUFFER_CAPACITY);
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            if (level == null || !level.isLoaded(outputPos) || resource.isEmpty() || !isOutputAir()) {
                return 0;
            }

            GasStack gasStack = getGasStack();
            boolean otherGas = !gasStack.isEmpty() && !GasStack.isSameGasSameComponents(gasStack, resource);
            AirtightPipeEffectHandler effectHandler = AirtightPipeEffectHandler.REGISTRY.get(resource.getGasType());
            GasStack toFill = effectHandler != null ? resource.copy() : resource;
            if (action.simulate()) {
                if (otherGas) {
                    return Math.min(OPEN_END_BUFFER_CAPACITY, toFill.getAmount());
                }

                return super.fill(toFill, GasAction.SIMULATE);
            }

            if (otherGas) {
                setGasStack(GasStack.EMPTY);
            }

            long filled = super.fill(toFill, GasAction.EXECUTE);
            if (filled <= 0) {
                return 0;
            }

            if (effectHandler != null) {
                effectHandler.apply(level, pos, direction, toFill.getGasType());
            }
            if (toFill.getGasType() == CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get() && level.getBlockEntity(pos) instanceof IGasExtractor extractor) {
                extractor.getAdvancementBehaviour().awardPlayer(CCBAdvancements.MINTY_FRESH);
            }
            if (getGasAmount() == OPEN_END_BUFFER_CAPACITY && isOutputAir()) {
                setGasStack(GasStack.EMPTY);
            }
            return filled;
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            return drainInner(resource.getAmount(), resource, action);
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            return drainInner(maxDrain, null, action);
        }

        private GasStack drainInner(long amount, @Nullable GasStack resource, GasAction action) {
            if (level == null || !level.isLoaded(outputPos) || amount <= 0) {
                return GasStack.EMPTY;
            }

            boolean emptyResource = resource == null;
            if (amount > OPEN_END_BUFFER_CAPACITY) {
                amount = OPEN_END_BUFFER_CAPACITY;
                if (!emptyResource) {
                    resource = resource.copyWithAmount(amount);
                }
            }

            GasStack drainedInternal = emptyResource ? super.drain(amount, action) : super.drain(resource, action);
            if (!drainedInternal.isEmpty()) {
                return drainedInternal;
            }

            GasStack drainedWorld = extractGasFromWorld();
            if (drainedWorld.isEmpty() || !emptyResource && !GasStack.isSameGasSameComponents(drainedWorld, resource)) {
                return GasStack.EMPTY;
            }

            long drainedAmount = Math.min(amount, drainedWorld.getAmount());
            long remainder = drainedWorld.getAmount() - drainedAmount;
            drainedWorld.setAmount(drainedAmount);
            if (action.simulate()) {
                return drainedWorld;
            }

            if (remainder > 0) {
                if (!getGasStack().isEmpty() && !GasStack.isSameGasSameComponents(getGasStack(), drainedWorld)) {
                    setGasStack(GasStack.EMPTY);
                }

                super.fill(drainedWorld.copyWithAmount(remainder), GasAction.EXECUTE);
            }
            if (drainedWorld.getGasType() == CCBGases.SPORE_AIR.get() && level.getBlockEntity(pos) instanceof IGasExtractor extractor) {
                extractor.getAdvancementBehaviour().awardPlayer(CCBAdvancements.GASEOUS_VARIATIONS);
            }
            return drainedWorld;
        }

        private GasStack extractGasFromWorld() {
            if (level == null || !level.isLoaded(outputPos) || !CCBConfig.server().airtights.canExtractAirFromWorld.get()) {
                return GasStack.EMPTY;
            }

            BlockState currentState = level.getBlockState(pos);
            if (!(level.getBlockEntity(pos) instanceof IGasExtractor extractor) || !extractor.canExtract(level, currentState, pos, direction)) {
                return GasStack.EMPTY;
            }

            BlockPos relativePos = pos.relative(direction);
            BlockState relativeState = level.getBlockState(relativePos);
            AirtightPipeExtractHandler extractHandler = AirtightPipeExtractHandler.REGISTRY.get(relativeState.getBlock());
            if (extractHandler == null) {
                return GasStack.EMPTY;
            }

            return new GasStack(extractHandler.extract(level, relativePos, relativeState), OPEN_END_BUFFER_CAPACITY);
        }

        private boolean isOutputAir() {
            return level != null && level.isLoaded(outputPos) && level.getBlockState(outputPos).isAir();
        }
    }
}
