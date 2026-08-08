package net.ty.createcraftedbeginning.content.airtights.handlers.armor.ethereal;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.EquipmentSlot;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EnergizedEtherealAirArmorsHandler extends EtherealAirArmorsHandler {
    /**
     * {@inheritDoc}
     */
    @Override
    public float getConsumptionMultiplier(EquipmentSlot slot) {
        return 0.64f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getMultiplierForBoostingElytra() {
        return super.getMultiplierForBoostingElytra() * 1.5f;
    }
}
