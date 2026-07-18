package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeStatus;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightHandheldDrillUpgradeRegistry {
    private static final AirtightUpgradeRegistry REGISTRY = new AirtightUpgradeRegistry("airtight_handheld_drill");

    private AirtightHandheldDrillUpgradeRegistry() {
    }

    public static @Nullable AirtightUpgrade getByID(ResourceLocation id) {
        return REGISTRY.getById(id);
    }

    public static @Nullable AirtightUpgrade getByStack(ItemStack stack) {
        return REGISTRY.getByStack(stack);
    }

    public static List<AirtightUpgrade> getAll() {
        return REGISTRY.getAll();
    }

    public static List<AirtightUpgradeStatus> getDefaultUpgradeList() {
        return REGISTRY.getDefaultStatuses();
    }

    public static void forEach(Consumer<AirtightUpgrade> action) {
        REGISTRY.forEach(action);
    }

    public static void registerUpgrades() {
        REGISTRY.registerAll(SilkTouchUpgrade.INSTANCE, MagnetUpgrade.INSTANCE, ExperienceConversionUpgrade.INSTANCE, LiquidReplacementUpgrade.INSTANCE, HandheldDrillFilterButton.INSTANCE, HandheldDrillContainerProtectionButton.INSTANCE, HandheldDrillOutlineDisplayButton.INSTANCE, HandheldDrillAttackModeButton.INSTANCE);
    }
}
