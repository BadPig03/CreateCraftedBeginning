package net.ty.createcraftedbeginning.data;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.ty.createcraftedbeginning.api.airtightcannon.AirtightCannonProjectileType;
import net.ty.createcraftedbeginning.registry.CCBRegistries;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("removal")
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class CCBRegistriesImpl {
    @ApiStatus.Internal
    @SubscribeEvent
    public static void registerDatapackRegistries(DataPackRegistryEvent.@NotNull NewRegistry event) {
        event.dataPackRegistry(CCBRegistries.AIRTIGHT_CANNON_PROJECTILE_TYPE, AirtightCannonProjectileType.CODEC, AirtightCannonProjectileType.CODEC);
    }
}
