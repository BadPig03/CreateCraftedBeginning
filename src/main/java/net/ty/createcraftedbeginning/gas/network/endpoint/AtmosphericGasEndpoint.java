package net.ty.createcraftedbeginning.gas.network.endpoint;

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
import net.ty.createcraftedbeginning.api.atmosphere.AtmosphereState;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseCause;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.gas.atmosphere.AtmosphereStateResolver;
import net.ty.createcraftedbeginning.gas.network.GasTransportNode;
import net.ty.createcraftedbeginning.gas.release.GasReleaseRequest;
import net.ty.createcraftedbeginning.gas.release.GasReleaseService;
import net.ty.createcraftedbeginning.gas.release.GasReleaseState;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.OptionalLong;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AtmosphericGasEndpoint extends GasConnectionEndpoint {
    private final BlockPos pos;
    private final BlockPos outputPos;
    private final Direction direction;
    private final ICapabilityProvider<GasHandler> gasHandlerProvider;
    private final AtmosphericGasHandler gasHandler;

    private Level level;
    private GasReleaseState releaseState;

    public AtmosphericGasEndpoint(BlockFace face) {
        super(face);
        gasHandler = new AtmosphericGasHandler();
        outputPos = face.getConnectedPos();
        pos = face.getPos();
        direction = face.getFace();
        gasHandlerProvider = ICapabilityProvider.of(() -> gasHandler);
        releaseState = new GasReleaseState();
    }

    @Override
    public void bind(Level level, BlockEntity networkBlockEntity) {
        this.level = level;
    }

    @Override
    public ICapabilityProvider<GasHandler> getGasHandlerProvider() {
        return gasHandlerProvider;
    }

    public static AtmosphericGasEndpoint read(CompoundTag endpointTag, Provider provider, BlockFace location) {
        AtmosphericGasEndpoint endpoint = new AtmosphericGasEndpoint(location);
        endpoint.releaseState = GasReleaseState.read(endpointTag, provider);
        return endpoint;
    }

    public AtmosphereState getAtmosphereState() {
        if (level == null) {
            return new AtmosphereState(Gas.EMPTY_GAS_HOLDER.value(), GasPressure.VACUUM_PA);
        }

        return AtmosphereStateResolver.resolve(level, outputPos);
    }

    public boolean canExtractAtmosphericGas() {
        if (level == null || !level.isLoaded(outputPos) || !CCBConfig.server().gas.atmosphere.allowEnvironmentAirExtraction.get()) {
            return false;
        }

        BlockState sourceState = level.getBlockState(pos);
        return level.getBlockEntity(pos) instanceof GasTransportNode transporter && transporter.allowsGasTransport(level, sourceState, pos, direction);
    }

    public CompoundTag write(Provider provider) {
        return releaseState.write(provider);
    }

    private class AtmosphericGasHandler implements GasPressureBoundary {
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
            if (tank != 0) {
                return GasStack.EMPTY;
            }

            return getWorldGas();
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            return fill(resource, OptionalLong.empty(), action);
        }

        @Override
        public long fillFromPressure(GasStack resource, long sourcePressurePa, GasAction action) {
            return fill(resource, OptionalLong.of(Math.max(GasPressure.VACUUM_PA, sourcePressurePa)), action);
        }

        @Override
        public boolean supportsExactDrainRecovery(int tank) {
            return tank == 0;
        }

        @Override
        public long restoreDrainedGas(int tank, GasStack resource, GasAction action) {
            if (tank != 0 || resource.isEmpty()) {
                return 0;
            }

            return resource.getAmount();
        }

        private long fill(GasStack resource, OptionalLong sourcePressurePa, GasAction action) {
            if (level == null || !level.isLoaded(outputPos) || resource.isEmpty()) {
                return 0;
            }

            long acceptedAmount = resource.getAmount();
            if (acceptedAmount <= 0 || action.simulate()) {
                return acceptedAmount;
            }

            GasStack releasedGas = resource.copyWithAmount(acceptedAmount);
            GasReleaseRequest releaseRequest = sourcePressurePa.isPresent() ? GasReleaseRequest.directional(releasedGas, pos, direction, GasReleaseCause.ATMOSPHERIC_OUTLET, sourcePressurePa.getAsLong()) : GasReleaseRequest.directional(releasedGas, pos, direction, GasReleaseCause.ATMOSPHERIC_OUTLET);
            GasReleaseService.release(level, releaseRequest, releaseState);
            return acceptedAmount;
        }

        private GasStack drainWorld(long maxDrainAmount, @Nullable GasStack requestedGas, GasAction action) {
            if (maxDrainAmount <= 0) {
                return GasStack.EMPTY;
            }

            GasStack worldGas = getWorldGas();
            if (worldGas.isEmpty() || requestedGas != null && !GasStack.isSameGasSameComponents(worldGas, requestedGas)) {
                return GasStack.EMPTY;
            }

            long drainedAmount = Math.min(maxDrainAmount, worldGas.getAmount());
            if (drainedAmount <= 0) {
                return GasStack.EMPTY;
            }

            GasStack drainedGas = worldGas.copyWithAmount(drainedAmount);
            if (!action.execute() || !drainedGas.is(CCBGases.SPORE_AIR) || !(level.getBlockEntity(pos) instanceof GasTransportNode transporter)) {
                return drainedGas;
            }

            transporter.getAdvancementBehaviour().awardPlayer(CCBAdvancements.GASEOUS_VARIATIONS);
            return drainedGas;
        }

        private GasStack getWorldGas() {
            if (!canExtractAtmosphericGas()) {
                return GasStack.EMPTY;
            }

            AtmosphereState atmosphereState = getAtmosphereState();
            if (atmosphereState.gas().isEmpty()) {
                return GasStack.EMPTY;
            }

            return new GasStack(atmosphereState.gas(), Long.MAX_VALUE);
        }
    }
}
