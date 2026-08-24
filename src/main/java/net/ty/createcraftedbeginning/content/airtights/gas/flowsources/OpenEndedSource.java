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
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
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

    public OpenEndedSource(BlockFace face) {
        super(face);
        gasHandler = new OpenEndGasHandler();
        outputPos = face.getConnectedPos();
        pos = face.getPos();
        direction = face.getFace();
        gasHandlerProvider = ICapabilityProvider.of(() -> gasHandler);
        effectGas = GasStack.EMPTY;
    }

    public static OpenEndedSource read(CompoundTag sourceTag, Provider provider, BlockFace location) {
        OpenEndedSource source = new OpenEndedSource(location);
        if (sourceTag.contains(COMPOUND_KEY_EFFECT_PROGRESS, Tag.TAG_LONG)) {
            source.effectProgress = Math.clamp(sourceTag.getLong(COMPOUND_KEY_EFFECT_PROGRESS), 0, EFFECT_INTERVAL - 1);
        }
        if (!sourceTag.contains(COMPOUND_KEY_EFFECT_GAS, Tag.TAG_COMPOUND)) {
            return source;
        }

        source.effectGas = GasStack.parseOptional(provider, sourceTag.getCompound(COMPOUND_KEY_EFFECT_GAS));
        if (source.effectGas.isEmpty()) {
            return source;
        }

        source.effectGas = source.effectGas.copyWithAmount(1);
        return source;
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

    public CompoundTag write(Provider provider) {
        CompoundTag sourceTag = new CompoundTag();
        if (effectProgress <= 0 || effectGas.isEmpty()) {
            return sourceTag;
        }

        sourceTag.putLong(COMPOUND_KEY_EFFECT_PROGRESS, effectProgress);
        sourceTag.put(COMPOUND_KEY_EFFECT_GAS, effectGas.saveOptional(provider));
        return sourceTag;
    }

    private class OpenEndGasHandler implements IGasHandler {
        @Override
        public boolean isGasValid(int tank, GasStack stack) {
            return tank == 0 && !stack.isEmpty();
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            if (resource.isEmpty()) {
                return GasStack.EMPTY;
            }
            return drainWorld(resource.getAmount(), resource, action);
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            return drainWorld(maxDrain, null, action);
        }

        @Override
        public GasStack getGasInTank(int tank) {
            return tank == 0 ? getWorldGas() : GasStack.EMPTY;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            if (level == null || !level.isLoaded(outputPos) || resource.isEmpty()) {
                return 0;
            }

            long acceptedAmount = Math.min(EFFECT_INTERVAL, resource.getAmount());
            if (acceptedAmount <= 0 || action.simulate()) {
                return acceptedAmount;
            }

            AirtightDrainageHandler drainageHandler = AirtightDrainageHandlerUtils.of(resource.getGasType());
            showOutputFeedback(resource, drainageHandler);
            recordOutput(resource, acceptedAmount, drainageHandler);
            return acceptedAmount;
        }

        @Override
        public long getTankCapacity(int tank) {
            return tank == 0 ? EFFECT_INTERVAL : 0;
        }

        private GasStack drainWorld(long maxDrainAmount, @Nullable GasStack requestedGas, GasAction action) {
            if (maxDrainAmount <= 0) {
                return GasStack.EMPTY;
            }

            GasStack worldGas = getWorldGas();
            if (worldGas.isEmpty() || requestedGas != null && !GasStack.isSameGasSameComponents(worldGas, requestedGas)) {
                return GasStack.EMPTY;
            }

            long drainedAmount = Math.min(Math.min(maxDrainAmount, EFFECT_INTERVAL), worldGas.getAmount());
            if (drainedAmount <= 0) {
                return GasStack.EMPTY;
            }

            GasStack drainedGas = worldGas.copyWithAmount(drainedAmount);
            if (!action.execute() || !drainedGas.is(CCBGases.SPORE_AIR) || !(level.getBlockEntity(pos) instanceof IGasTransporter transporter)) {
                return drainedGas;
            }

            transporter.getAdvancementBehaviour().awardPlayer(CCBAdvancements.GASEOUS_VARIATIONS);
            return drainedGas;
        }

        private GasStack getWorldGas() {
            if (level == null || !level.isLoaded(outputPos) || !CCBConfig.server().airtights.canExtractAirFromWorld.get()) {
                return GasStack.EMPTY;
            }

            BlockState sourceState = level.getBlockState(pos);
            if (!(level.getBlockEntity(pos) instanceof IGasTransporter transporter) || !transporter.canTransport(level, sourceState, pos, direction)) {
                return GasStack.EMPTY;
            }

            BlockState outputState = level.getBlockState(outputPos);
            AirtightFillHandler fillHandler = AirtightFillHandlerUtils.of(outputState.getBlock());
            return new GasStack(fillHandler.apply(level, outputPos, outputState), EFFECT_INTERVAL);
        }

        private void showOutputFeedback(GasStack outputGas, AirtightDrainageHandler drainageHandler) {
            long gameTime = level.getGameTime();
            if (lastFeedbackTick == gameTime) {
                return;
            }

            lastFeedbackTick = gameTime;
            if (drainageHandler.shouldShowOutline()) {
                drainageHandler.showOutline(level, pos, direction, drainageHandler.getInflation(), outputGas.getGasType().getTint());
            }
            if (gameTime % 20 != 10) {
                return;
            }

            CCBSoundEvents.GAS_DRAINAGE.playOnServer(level, pos, 1, 1);
        }

        private void recordOutput(GasStack outputGas, long outputAmount, AirtightDrainageHandler drainageHandler) {
            if (effectGas.isEmpty() || !GasStack.isSameGasSameComponents(effectGas, outputGas)) {
                effectGas = outputGas.copyWithAmount(1);
                effectProgress = 0;
            }
            effectProgress += outputAmount;
            if (effectProgress < EFFECT_INTERVAL) {
                return;
            }

            effectProgress -= EFFECT_INTERVAL;
            drainageHandler.apply(level, pos, direction, outputGas.getGasType());
            if (!outputGas.is(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR) || !(level.getBlockEntity(pos) instanceof IGasTransporter extractor)) {
                return;
            }

            extractor.getAdvancementBehaviour().awardPlayer(CCBAdvancements.MINTY_FRESH);
        }
    }
}
