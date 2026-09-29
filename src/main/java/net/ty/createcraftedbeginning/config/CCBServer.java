package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBServer extends ConfigBase {
    public final CCBStress kinetics = nested(0, CCBStress::new, "Kinetics");
    public final CCBGas gas = nested(0, CCBGas::new, "Gas");
    public final CCBMachines machines = nested(0, CCBMachines::new, "Machines");
    public final CCBEquipment equipment = nested(0, CCBEquipment::new, "Equipment");
    public final CCBStorage storage = nested(0, CCBStorage::new, "Storage");
    public final CCBOpticalPower opticalPower = nested(0, CCBOpticalPower::new, "Optical Power");

    @Override
    public String getName() {
        return "server";
    }
}
