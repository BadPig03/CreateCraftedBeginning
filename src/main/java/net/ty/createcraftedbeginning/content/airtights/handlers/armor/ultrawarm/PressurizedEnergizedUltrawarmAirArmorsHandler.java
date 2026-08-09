package net.ty.createcraftedbeginning.content.airtights.handlers.armor.ultrawarm;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.EquipmentSlot;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizedEnergizedUltrawarmAirArmorsHandler extends UltrawarmAirArmorsHandler {
    @Override
    public float getConsumptionMultiplier(EquipmentSlot slot) {
        return 0.3f;
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return super.getMultiplierForBoostingElytra() * 3;
    }
}
