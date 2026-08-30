package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeStatus;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightChestplateUpgradeRegistry {
    private static final AirtightUpgradeRegistry REGISTRY = new AirtightUpgradeRegistry("airtight_chestplate");

    private AirtightChestplateUpgradeRegistry() {
    }

    public static @Nullable AirtightUpgrade getById(ResourceLocation id) {
        return REGISTRY.getById(id);
    }

    public static @Nullable AirtightUpgrade getByStack(ItemStack stack) {
        return REGISTRY.getByStack(stack);
    }

    public static boolean allUpgradesEnabled(Player player) {
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        return chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE) && REGISTRY.allUpgradesEnabled(chestplate);
    }

    public static List<AirtightUpgrade> getAll() {
        return REGISTRY.getAll();
    }

    public static List<AirtightUpgradeStatus> getDefaultUpgradeList() {
        return REGISTRY.getDefaultStatuses();
    }

    public static void tick(Player player, ItemStack item) {
        REGISTRY.tick(player, item);
    }

    public static void registerUpgrades() {
        REGISTRY.registerAll(ElytraUpgrade.INSTANCE, CreativeFlightUpgrade.INSTANCE, InvisibilityUpgrade.INSTANCE, RegenerationUpgrade.INSTANCE, HasteUpgrade.INSTANCE, ChestplateResistanceUpgrade.INSTANCE);
    }
}
