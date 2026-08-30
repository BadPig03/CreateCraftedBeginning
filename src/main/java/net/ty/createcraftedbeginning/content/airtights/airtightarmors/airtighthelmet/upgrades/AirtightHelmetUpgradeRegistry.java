package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades;

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
public final class AirtightHelmetUpgradeRegistry {
    private static final AirtightUpgradeRegistry REGISTRY = new AirtightUpgradeRegistry("airtight_helmet");

    private AirtightHelmetUpgradeRegistry() {
    }

    public static @Nullable AirtightUpgrade getById(ResourceLocation id) {
        return REGISTRY.getById(id);
    }

    public static @Nullable AirtightUpgrade getByStack(ItemStack stack) {
        return REGISTRY.getByStack(stack);
    }

    public static boolean allUpgradesEnabled(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        return helmet.is(CCBItems.AIRTIGHT_HELMET) && REGISTRY.allUpgradesEnabled(helmet);
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
        REGISTRY.registerAll(EffectsProtectionUpgrade.INSTANCE, WaterBreathingUpgrade.INSTANCE, GogglesUpgrade.INSTANCE, VisionUpgrade.INSTANCE, SpectralUpgrade.INSTANCE, HelmetResistanceUpgrade.INSTANCE);
    }
}
