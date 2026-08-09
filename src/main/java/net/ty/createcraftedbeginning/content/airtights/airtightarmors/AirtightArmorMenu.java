package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradableMenu;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeStatus;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AirtightArmorMenu extends AirtightUpgradableMenu {
    private final UpgradeRegistryAccess upgradeRegistry;

    protected AirtightArmorMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData, UpgradeRegistryAccess upgradeRegistry) {
        super(type, id, inv, extraData);
        this.upgradeRegistry = upgradeRegistry;
    }

    protected AirtightArmorMenu(MenuType<?> type, int id, Inventory inv, ItemStack contentHolder, InteractionHand sourceHand, UpgradeRegistryAccess upgradeRegistry) {
        super(type, id, inv, contentHolder, sourceHand);
        this.upgradeRegistry = upgradeRegistry;
    }

    protected static UpgradeRegistryAccess upgradeRegistry(Function<ResourceLocation, AirtightUpgrade> byId, Function<ItemStack, AirtightUpgrade> byStack, Supplier<List<AirtightUpgradeStatus>> defaultStatuses, Supplier<List<AirtightUpgrade>> upgrades) {
        return new UpgradeRegistryAccess(byId, byStack, defaultStatuses, upgrades);
    }

    @Override
    protected @Nullable AirtightUpgrade getUpgradeById(ResourceLocation id) {
        return upgradeRegistry.byId().apply(id);
    }

    @Override
    protected boolean isValidUpgrade(ItemStack stack) {
        AirtightUpgrade upgrade = upgradeRegistry.byStack().apply(stack);
        return upgrade != null && !getStatus(upgrade).isInstalled();
    }

    @Override
    public void updateStatus(ItemStack stack) {
        List<AirtightUpgrade> upgrades = upgradeRegistry.upgrades().get();
        currentStatusList = normalizeStatusList(stack.getOrDefault(CCBDataComponents.AIRTIGHT_UPGRADE_STATUS, upgradeRegistry.defaultStatuses().get()), upgrades);
    }

    void forEachUpgrade(Consumer<AirtightUpgrade> action) {
        upgradeRegistry.upgrades().get().forEach(action);
    }

    protected record UpgradeRegistryAccess(Function<ResourceLocation, AirtightUpgrade> byId, Function<ItemStack, AirtightUpgrade> byStack, Supplier<List<AirtightUpgradeStatus>> defaultStatuses, Supplier<List<AirtightUpgrade>> upgrades) {}
}
