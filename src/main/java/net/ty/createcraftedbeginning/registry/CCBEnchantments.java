package net.ty.createcraftedbeginning.registry;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.Builder;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBEnchantments {
    public static final ResourceKey<Enchantment> ECONOMIZE = key("economize");

    public static void bootstrap(BootstrapContext<Enchantment> context) {
        register(context, ECONOMIZE, Enchantment.enchantment(Enchantment.definition(context.lookup(Registries.ITEM).getOrThrow(CCBItemTags.GAS_CANISTER_ENCHANTABLE.tag), 10, GasCanisterContainerContents.ECONOMIZE_MAX_LEVEL, Enchantment.dynamicCost(15, 15), Enchantment.dynamicCost(45, 15), 1, EquipmentSlotGroup.MAINHAND)));
    }

    @SuppressWarnings("SameParameterValue")
    private static void register(BootstrapContext<Enchantment> context, ResourceKey<Enchantment> key, Builder builder) {
        context.register(key, builder.build(key.location()));
    }

    @SuppressWarnings("SameParameterValue")
    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, CreateCraftedBeginning.asResource(name));
    }
}
