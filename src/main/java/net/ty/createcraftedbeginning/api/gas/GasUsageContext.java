package net.ty.createcraftedbeginning.api.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfile;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasUsageContext(GasStack gas, long sourcePressurePa) {
    public GasUsageContext {
        gas = gas.copy();
        if (gas.isEmpty()) {
            throw new IllegalArgumentException("Gas usage context requires a non-empty gas stack.");
        }

        if (sourcePressurePa < GasPressure.VACUUM_PA) {
            throw new IllegalArgumentException("Gas usage source pressure must be non-negative; got " + sourcePressurePa + " Pa.");
        }
    }

    @Override
    public GasStack gas() {
        return gas.copy();
    }

    public Gas gasType() {
        return gas.getGasType();
    }

    public GameplayPressureProfile pressureProfile() {
        return GameplayPressureProfiles.resolve(sourcePressurePa);
    }
}
