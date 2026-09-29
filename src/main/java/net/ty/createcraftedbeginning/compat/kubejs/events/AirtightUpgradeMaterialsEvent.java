package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeMaterials;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightUpgradeMaterialsEvent implements KubeEvent {
    public void set(ResourceLocation upgradeId, ResourceLocation itemId) {
        try {
            AirtightUpgradeMaterials.set(upgradeId, itemId);
        }
        catch (IllegalArgumentException exception) {
            CCBAPI.LOGGER.error("Failed to set material '{}' for airtight upgrade '{}'; ignoring this assignment: {}", itemId, upgradeId, exception.getMessage());
        }
    }
}
