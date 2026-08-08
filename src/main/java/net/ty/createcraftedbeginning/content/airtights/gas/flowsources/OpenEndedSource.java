package net.ty.createcraftedbeginning.content.airtights.gas.flowsources;

import com.simibubi.create.foundation.ICapabilityProvider;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.drainagehandlers.AirtightDrainageHandler;
import net.ty.createcraftedbeginning.api.drainagehandlers.AirtightDrainageHandlerUtils;
import net.ty.createcraftedbeginning.api.fillhandlers.AirtightFillHandler;
import net.ty.createcraftedbeginning.api.fillhandlers.AirtightFillHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IGasTransporter;
import net.ty.createcraftedbeginning.data.CCBGases;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class OpenEndedSource extends GasFlowSource {
    private static final String COMPOUND_KEY_EFFECT_PROGRESS = "EffectProgress";
    private static final String COMPOUND_KEY_EFFECT_GAS = "EffectGas";
    private static final long EFFECT_INTERVAL = 1000;

    private final BlockPos pos;
    private final BlockPos outputPos;
    private final Direction direction;
    private final ICapabilityProvider<IGasHandler> gasHandlerProvider;
    private final OpenEndGasHandler gasHandler;

    private Level level;
    private long effectProgress;
    private long lastFeedbackTick = Long.MIN_VALUE;
    private GasStack effectGas;

    /**
     * Creates a new {@code OpenEndedSource} instance.
     *
     * @param face the face to render or highlight
     */
    public OpenEndedSource(BlockFace face) {
        super(face);
        gasHandler = new OpenEndGasHandler();
        outputPos = face.getConnectedPos();
        pos = face.getPos();
        direction = face.getFace();
        gasHandlerProvider = ICapabilityProvider.of(() -> gasHandler);
        effectGas = GasStack.EMPTY;
    }

    /**
     * Reads this object's state from the supplied serialized data.
     *
     * @param compoundTag the NBT compound to read from or write to
     * @param provider    the provider used to resolve the requested value
     * @param location    the resource location identifying the target value
     * @return this instance
     */
    public static OpenEndedSource read(CompoundTag compoundTag, Provider provider, BlockFace location) {
        OpenEndedSource pipe = new OpenEndedSource(location);
        if (compoundTag.contains(COMPOUND_KEY_EFFECT_PROGRESS, Tag.TAG_LONG)) {
            pipe.effectProgress = Math.clamp(compoundTag.getLong(COMPOUND_KEY_EFFECT_PROGRESS), 0, EFFECT_INTERVAL - 1);
        }
        if (!compoundTag.contains(COMPOUND_KEY_EFFECT_GAS, Tag.TAG_COMPOUND)) {
            return pipe;
        }

        pipe.effectGas = GasStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_EFFECT_GAS));
        if (pipe.effectGas.isEmpty()) {
            return pipe;
        }

        pipe.effectGas = pipe.effectGas.copyWithAmount(1);
        return pipe;
    }

    /**
     * Writes this object's state to the supplied serialized data.
     *
     * @param provider the provider used to resolve the requested value
     * @return the resulting compound tag
     */
    public CompoundTag write(Provider provider) {
        CompoundTag compound = new CompoundTag();
        if (effectProgress <= 0 || effectGas.isEmpty()) {
            return compound;
        }

        compound.putLong(COMPOUND_KEY_EFFECT_PROGRESS, effectProgress);
        compound.put(COMPOUND_KEY_EFFECT_GAS, effectGas.saveOptional(provider));
        return compound;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEndpoint() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void manageSource(Level level, BlockEntity blockEntity) {
        this.level = level;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICapabilityProvider<IGasHandler> getGasHandlerProvider() {
        return gasHandlerProvider;
    }

    private class OpenEndGasHandler implements IGasHandler {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isGasValid(int tank, GasStack stack) {
            return tank == 0 && !stack.isEmpty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            if (resource.isEmpty()) {
                return GasStack.EMPTY;
            }
            return drainWorld(resource.getAmount(), resource, action);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            return drainWorld(maxDrain, null, action);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public GasStack getGasInTank(int tank) {
            return tank == 0 ? getWorldGas() : GasStack.EMPTY;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getTanks() {
            return 1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long fill(GasStack resource, GasAction action) {
            if (level == null || !level.isLoaded(outputPos) || resource.isEmpty()) {
                return 0;
            }

            long accepted = Math.min(EFFECT_INTERVAL, resource.getAmount());
            if (accepted <= 0 || action.simulate()) {
                return accepted;
            }

            AirtightDrainageHandler drainageHandler = AirtightDrainageHandlerUtils.of(resource.getGasType());
            showOutputFeedback(resource, drainageHandler);
            recordOutput(resource, accepted, drainageHandler);
            return accepted;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getTankCapacity(int tank) {
            return tank == 0 ? EFFECT_INTERVAL : 0;
        }

        private GasStack drainWorld(long amount, @Nullable GasStack requested, GasAction action) {
            if (amount <= 0) {
                return GasStack.EMPTY;
            }

            GasStack worldGas = getWorldGas();
            if (worldGas.isEmpty() || requested != null && !GasStack.isSameGasSameComponents(worldGas, requested)) {
                return GasStack.EMPTY;
            }

            long drainedAmount = Math.min(Math.min(amount, EFFECT_INTERVAL), worldGas.getAmount());
            if (drainedAmount <= 0) {
                return GasStack.EMPTY;
            }

            GasStack drained = worldGas.copyWithAmount(drainedAmount);
            if (!action.execute() || !drained.is(CCBGases.SPORE_AIR) || !(level.getBlockEntity(pos) instanceof IGasTransporter transporter)) {
                return drained;
            }

            transporter.getAdvancementBehaviour().awardPlayer(CCBAdvancements.GASEOUS_VARIATIONS);
            return drained;
        }

        private GasStack getWorldGas() {
            if (level == null || !level.isLoaded(outputPos) || !CCBConfig.server().airtights.canExtractAirFromWorld.get()) {
                return GasStack.EMPTY;
            }

            BlockState currentState = level.getBlockState(pos);
            if (!(level.getBlockEntity(pos) instanceof IGasTransporter transporter) || !transporter.canTransport(level, currentState, pos, direction)) {
                return GasStack.EMPTY;
            }

            BlockState targetState = level.getBlockState(outputPos);
            AirtightFillHandler fillHandler = AirtightFillHandlerUtils.of(targetState.getBlock());
            return new GasStack(fillHandler.apply(level, outputPos, targetState), EFFECT_INTERVAL);
        }

        private void showOutputFeedback(GasStack resource, AirtightDrainageHandler drainageHandler) {
            long gameTime = level.getGameTime();
            if (lastFeedbackTick == gameTime) {
                return;
            }

            lastFeedbackTick = gameTime;

            if (drainageHandler.shouldShowOutline()) {
                drainageHandler.showOutline(level, pos, direction, drainageHandler.getInflation(), resource.getGasType().getTint());
            }
            if (gameTime % 20 != 10) {
                return;
            }

            CCBSoundEvents.GAS_DRAINAGE.playOnServer(level, pos, 1, 1);
        }

        private void recordOutput(GasStack resource, long amount, AirtightDrainageHandler drainageHandler) {
            if (effectGas.isEmpty() || !GasStack.isSameGasSameComponents(effectGas, resource)) {
                effectGas = resource.copyWithAmount(1);
                effectProgress = 0;
            }
            effectProgress += amount;
            if (effectProgress < EFFECT_INTERVAL) {
                return;
            }

            effectProgress -= EFFECT_INTERVAL;
            drainageHandler.apply(level, pos, direction, resource.getGasType());
            if (!resource.is(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR) || !(level.getBlockEntity(pos) instanceof IGasTransporter extractor)) {
                return;
            }

            extractor.getAdvancementBehaviour().awardPlayer(CCBAdvancements.MINTY_FRESH);
        }
    }
}
