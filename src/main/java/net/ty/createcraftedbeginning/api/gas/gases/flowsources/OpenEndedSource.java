package net.ty.createcraftedbeginning.api.gas.gases.flowsources;

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
import net.ty.createcraftedbeginning.api.drainagehandlers.AirtightDrainageHandler;
import net.ty.createcraftedbeginning.api.drainagehandlers.AirtightDrainageHandlerUtils;
import net.ty.createcraftedbeginning.api.fillhandlers.AirtightFillHandler;
import net.ty.createcraftedbeginning.api.fillhandlers.AirtightFillHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.GasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasTransporter;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.data.CCBGases;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class OpenEndedSource extends GasFlowSource {
    private static final String COMPOUND_KEY_LOCATION = "Location";
    private static final int BUFFER_CAPACITY = 1000;

    private final BlockPos pos;
    private final BlockPos outputPos;
    private final Direction direction;
    private final ICapabilityProvider<IGasHandler> gasHandlerProvider;
    private final OpenEndGasHandler gasHandler;

    private Level level;

    public OpenEndedSource(BlockFace face) {
        super(face);
        gasHandler = new OpenEndGasHandler();
        outputPos = face.getConnectedPos();
        pos = face.getPos();
        direction = face.getFace();
        gasHandlerProvider = ICapabilityProvider.of(() -> gasHandler);
    }

    public static OpenEndedSource read(CompoundTag compoundTag, Provider provider, BlockPos blockPos) {
        OpenEndedSource pipe = new OpenEndedSource(new BlockFace(blockPos, BlockFace.fromNBT(compoundTag.getCompound(COMPOUND_KEY_LOCATION)).getFace()));
        pipe.gasHandler.read(provider, compoundTag);
        return pipe;
    }

    public CompoundTag write(Provider provider) {
        CompoundTag compound = new CompoundTag();
        gasHandler.write(provider, compound);
        compound.put(COMPOUND_KEY_LOCATION, location.serializeNBT());
        return compound;
    }

    @Override
    public boolean isEndpoint() {
        return true;
    }

    @Override
    public void manageSource(Level level, BlockEntity blockEntity) {
        this.level = level;
    }

    @Override
    public ICapabilityProvider<IGasHandler> getGasHandlerProvider() {
        return gasHandlerProvider;
    }

    private class OpenEndGasHandler extends GasTank {
        public OpenEndGasHandler() {
            super(BUFFER_CAPACITY);
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            if (level == null || !level.isLoaded(outputPos) || resource.isEmpty()) {
                return 0;
            }

            GasStack gasStack = getGasStack();
            boolean isSameGas = gasStack.isEmpty() || GasStack.isSameGasSameComponents(gasStack, resource);
            GasStack toFill = resource.copy();
            if (action.simulate()) {
                if (!isSameGas) {
                    return Math.min(BUFFER_CAPACITY, toFill.getAmount());
                }

                return super.fill(toFill, GasAction.SIMULATE);
            }

            if (!isSameGas) {
                setGasStack(GasStack.EMPTY);
            }
            long filled = super.fill(toFill, GasAction.EXECUTE);
            if (filled <= 0) {
                return 0;
            }

            AirtightDrainageHandler drainageHandler = AirtightDrainageHandlerUtils.of(resource.getGasType());
            drainageHandler.apply(level, pos, direction, toFill.getGasType());
            if (toFill.is(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR) && level.getBlockEntity(pos) instanceof IGasTransporter extractor) {
                extractor.getAdvancementBehaviour().awardPlayer(CCBAdvancements.MINTY_FRESH);
            }
            if (getGasAmount() == BUFFER_CAPACITY) {
                setGasStack(GasStack.EMPTY);
            }
            return filled;
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            return drain(resource.getAmount(), resource, action);
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            return drain(maxDrain, null, action);
        }

        private GasStack drain(long amount, @Nullable GasStack resource, GasAction action) {
            if (level == null || !level.isLoaded(outputPos) || amount <= 0) {
                return GasStack.EMPTY;
            }

            boolean isEmpty = resource == null;
            if (amount > BUFFER_CAPACITY) {
                amount = BUFFER_CAPACITY;
                if (!isEmpty) {
                    resource = resource.copyWithAmount(amount);
                }
            }

            GasStack drainedInternal = isEmpty ? super.drain(amount, action) : super.drain(resource, action);
            if (!drainedInternal.isEmpty()) {
                return drainedInternal;
            }

            GasStack drainedFromWorld = GasStack.EMPTY;
            if (CCBConfig.server().airtights.canExtractAirFromWorld.get()) {
                BlockState currentState = level.getBlockState(pos);
                if (level.getBlockEntity(pos) instanceof IGasTransporter transporter && transporter.canTransport(level, currentState, pos, direction)) {
                    BlockPos targetPos = pos.relative(direction);
                    BlockState targetState = level.getBlockState(targetPos);
                    AirtightFillHandler fillHandler = AirtightFillHandlerUtils.of(targetState.getBlock());
                    drainedFromWorld = new GasStack(fillHandler.apply(level, targetPos, targetState), BUFFER_CAPACITY);
                }
            }

            if (drainedFromWorld.isEmpty() || !isEmpty && !GasStack.isSameGasSameComponents(drainedFromWorld, resource)) {
                return GasStack.EMPTY;
            }

            long drainedAmount = Math.min(amount, drainedFromWorld.getAmount());
            long remainder = drainedFromWorld.getAmount() - drainedAmount;
            drainedFromWorld.setAmount(drainedAmount);
            if (action.simulate()) {
                return drainedFromWorld;
            }

            if (remainder > 0) {
                GasStack gasStack = getGasStack();
                if (!gasStack.isEmpty() && !GasStack.isSameGasSameComponents(gasStack, drainedFromWorld)) {
                    setGasStack(GasStack.EMPTY);
                }
                super.fill(drainedFromWorld.copyWithAmount(remainder), GasAction.EXECUTE);
            }
            if (drainedFromWorld.is(CCBGases.SPORE_AIR) && level.getBlockEntity(pos) instanceof IGasTransporter transporter) {
                transporter.getAdvancementBehaviour().awardPlayer(CCBAdvancements.GASEOUS_VARIATIONS);
            }
            return drainedFromWorld;
        }
    }
}
