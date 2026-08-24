package net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IVentingGasSource;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SteamOutletGasHandler implements IGasHandler, IVentingGasSource {
    private final BoilerSteamOutletBlockEntity outlet;
    private long availableSteamThisTick;

    public SteamOutletGasHandler(BoilerSteamOutletBlockEntity outlet) {
        this.outlet = outlet;
    }

    void setAvailableSteamThisTick(long availableSteam) {
        availableSteamThisTick = Math.clamp(availableSteam, 0, BoilerSteamOutletProduction.getMaximumOutputCapacity());
    }

    @Override
    public boolean isGasValid(int tankIndex, GasStack stack) {
        outlet.ensureCurrentTick();
        return tankIndex == 0 && !stack.isEmpty() && stack.is(CCBGases.STEAM);
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        outlet.ensureCurrentTick();
        GasStack availableSteam = getAvailableSteamStack();
        if (resource.isEmpty() || availableSteam.isEmpty() || !GasStack.isSameGasSameComponents(resource, availableSteam)) {
            return GasStack.EMPTY;
        }

        GasStack drainedSteam = drainAvailable(resource.getAmount(), action);
        outlet.recordExtraction(drainedSteam, action);
        return drainedSteam;
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        outlet.ensureCurrentTick();
        GasStack drainedSteam = drainAvailable(maxDrain, action);
        outlet.recordExtraction(drainedSteam, action);
        return drainedSteam;
    }

    @Override
    public GasStack getGasInTank(int tankIndex) {
        outlet.ensureCurrentTick();
        if (tankIndex != 0) {
            return GasStack.EMPTY;
        }
        return getAvailableSteamStack();
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        return 0;
    }

    @Override
    public AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
        for (GasStack resource : resources) {
            if (resource == null || resource.isEmpty()) {
                continue;
            }

            return AtomicFillResult.REJECTED;
        }
        return AtomicFillResult.SUCCESS;
    }

    @Override
    public long getTankCapacity(int tankIndex) {
        outlet.ensureCurrentTick();
        return tankIndex == 0 ? BoilerSteamOutletProduction.getMaximumOutputCapacity() : 0;
    }

    private GasStack drainAvailable(long maxDrain, GasAction action) {
        long drainedAmount = Math.clamp(maxDrain, 0, availableSteamThisTick);
        if (drainedAmount <= 0) {
            return GasStack.EMPTY;
        }

        GasStack drainedSteam = new GasStack(CCBGases.STEAM.get(), drainedAmount);
        if (action.execute()) {
            availableSteamThisTick -= drainedAmount;
        }
        return drainedSteam;
    }

    private GasStack getAvailableSteamStack() {
        return availableSteamThisTick <= 0 ? GasStack.EMPTY : new GasStack(CCBGases.STEAM.get(), availableSteamThisTick);
    }
}
