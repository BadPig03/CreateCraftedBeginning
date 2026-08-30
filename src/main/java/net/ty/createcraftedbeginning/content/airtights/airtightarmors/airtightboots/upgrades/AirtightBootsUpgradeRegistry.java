package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades;

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
public final class AirtightBootsUpgradeRegistry {
    private static final AirtightUpgradeRegistry REGISTRY = new AirtightUpgradeRegistry("airtight_boots");

    private AirtightBootsUpgradeRegistry() {
    }

    public static @Nullable AirtightUpgrade getById(ResourceLocation id) {
        return REGISTRY.getById(id);
    }

    public static @Nullable AirtightUpgrade getByStack(ItemStack stack) {
        return REGISTRY.getByStack(stack);
    }

    public static boolean allUpgradesEnabled(Player player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        return boots.is(CCBItems.AIRTIGHT_BOOTS) && REGISTRY.allUpgradesEnabled(boots);
    }

    public static List<AirtightUpgrade> getAll() {
        return REGISTRY.getAll();
    }

    public static List<AirtightUpgradeStatus> getDefaultUpgradeList() {
        return REGISTRY.getDefaultStatuses();
    }

    public static void registerUpgrades() {
        REGISTRY.registerAll(MovementEfficiencyUpgrade.INSTANCE, JumpStrengthUpgrade.INSTANCE, StepHeightUpgrade.INSTANCE, EnvironmentalDamageProtectionUpgrade.INSTANCE, FallProtectionUpgrade.INSTANCE, BootsResistanceUpgrade.INSTANCE);
    }
}
