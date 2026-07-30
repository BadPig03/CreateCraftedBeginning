package net.ty.createcraftedbeginning.client;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackItem;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackOverrides.GasCanisterPackType;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class GasCanisterPackClientOverrides {
    private GasCanisterPackClientOverrides() {
    }

    public static void register(GasCanisterPackItem item) {
        ItemProperties.register(item, GasCanisterPackType.TYPE, (stack, level, entity, seed) -> GasCanisterPackType.getTypeFromFlags(stack.getOrDefault(CCBDataComponents.GAS_CANISTER_PACK_FLAGS, 0)).ordinal());
    }
}
