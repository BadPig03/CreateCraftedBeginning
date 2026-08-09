package net.ty.createcraftedbeginning.content.airtights.handlers.armor.ethereal;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.EquipmentSlot;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizedEnergizedEtherealAirArmorsHandler extends EtherealAirArmorsHandler {
    @Override
    public float getConsumptionMultiplier(EquipmentSlot slot) {
        return 0.1f;
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return super.getMultiplierForBoostingElytra() * 3;
    }
}
