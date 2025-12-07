package net.ty.createcraftedbeginning.registry;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.Builder;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import org.jetbrains.annotations.NotNull;

public class CCBEnchantments {
    public static final ResourceKey<Enchantment> ECONOMIZE = key("economize");

    public static void bootstrap(@NotNull BootstrapContext<Enchantment> context) {
        register(context, ECONOMIZE, Enchantment.enchantment(Enchantment.definition(HolderSet.direct(CCBItems.GAS_CANISTER), 2, 3, Enchantment.dynamicCost(16, 16), Enchantment.dynamicCost(48, 16), 2, EquipmentSlotGroup.MAINHAND)));
    }

    @SuppressWarnings("SameParameterValue")
    private static void register(@NotNull BootstrapContext<Enchantment> context, ResourceKey<Enchantment> key, @NotNull Builder builder) {
        context.register(key, builder.build(key.location()));
    }

    @SuppressWarnings("SameParameterValue")
    private static @NotNull ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, CreateCraftedBeginning.asResource(name));
    }
}
