package net.ty.createcraftedbeginning.api.armorhandlers.ethereal;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.EquipmentSlot;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizedEtherealAirArmorsHandler extends EtherealAirArmorsHandler {
    /**
     * {@inheritDoc}
     */
    @Override
    public float getConsumptionMultiplier(EquipmentSlot slot) {
        return 0.37f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getMultiplierForBoostingElytra() {
        return super.getMultiplierForBoostingElytra() * 3;
    }
}
