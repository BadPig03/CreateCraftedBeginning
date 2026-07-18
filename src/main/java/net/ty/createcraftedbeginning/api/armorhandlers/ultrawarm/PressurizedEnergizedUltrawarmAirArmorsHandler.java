package net.ty.createcraftedbeginning.api.armorhandlers.ultrawarm;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.EquipmentSlot;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizedEnergizedUltrawarmAirArmorsHandler extends UltrawarmAirArmorsHandler {
    /**
     * {@inheritDoc}
     */
    @Override
    public float getConsumptionMultiplier(EquipmentSlot slot) {
        return 0.3f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getMultiplierForBoostingElytra() {
        return super.getMultiplierForBoostingElytra() * 3;
    }
}
