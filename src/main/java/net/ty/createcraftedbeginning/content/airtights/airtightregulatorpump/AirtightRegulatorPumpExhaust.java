package net.ty.createcraftedbeginning.content.airtights.airtightregulatorpump;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseCause;
import net.ty.createcraftedbeginning.gas.atmosphere.AtmosphereStateResolver;
import net.ty.createcraftedbeginning.gas.network.solver.routing.OutletSideRelief;
import net.ty.createcraftedbeginning.gas.release.GasReleaseRequest;
import net.ty.createcraftedbeginning.gas.release.GasReleaseService;
import net.ty.createcraftedbeginning.gas.release.GasReleaseState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightRegulatorPumpExhaust implements OutletSideRelief {
    private final AirtightRegulatorPumpBlockEntity pump;
    private GasReleaseState releaseState = new GasReleaseState();

    AirtightRegulatorPumpExhaust(AirtightRegulatorPumpBlockEntity pump) {
        this.pump = pump;
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        return tank == 0 && !stack.isEmpty();
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        return GasStack.EMPTY;
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        return GasStack.EMPTY;
    }

    @Override
    public GasStack getGasInTank(int tank) {
        return GasStack.EMPTY;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public long getFillPressurePa(int tank, GasStack gas) {
        Level level = pump.getLevel();
        if (level == null) {
            return pump.getOutletSetPressurePa();
        }

        long atmospherePressurePa = AtmosphereStateResolver.resolve(level, pump.getBlockPos()).pressurePa();
        return Math.max(pump.getOutletSetPressurePa(), Math.max(GasPressure.VACUUM_PA, atmospherePressurePa - pump.getMaxPressureRisePa()));
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        return fillFromPressure(resource, getFillPressurePa(0, resource), action);
    }

    @Override
    public long fillFromPressure(GasStack resource, long sourcePressurePa, GasAction action) {
        Level level = pump.getLevel();
        if (level == null || level.isClientSide || resource.isEmpty()) {
            return 0;
        }

        long accepted = Math.min(resource.getAmount(), pump.getFlowRateLimit());
        if (accepted <= 0 || action.simulate()) {
            return accepted;
        }

        BlockPos pos = pump.getBlockPos();
        long exhaustPressurePa = Math.max(sourcePressurePa, AtmosphereStateResolver.resolve(level, pos).pressurePa());
        GasReleaseService.release(level, GasReleaseRequest.radial(resource.copyWithAmount(accepted), pos, GasReleaseCause.ATMOSPHERIC_OUTLET, exhaustPressurePa), releaseState);
        pump.setChanged();
        return accepted;
    }

    void read(CompoundTag tag, Provider provider) {
        releaseState = GasReleaseState.read(tag, provider);
    }

    CompoundTag write(Provider provider) {
        return releaseState.write(provider);
    }
}
