package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades;

import com.simibubi.create.AllItems;
import net.createmod.catnip.data.Couple;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum WaterBreathingUpgrade implements AirtightUpgrade {
    INSTANCE;

    public static boolean canApply(@NotNull Player player, boolean canBreathe) {
        if (canBreathe) {
            return true;
        }

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.is(CCBItems.AIRTIGHT_HELMET) || !INSTANCE.isEnabled(helmet) || !CanisterContainerSuppliers.isAnyContainerAvailable(player)) {
            return false;
        }

        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            return false;
        }

        int gasCost = INSTANCE.getGasCost(player);
        if (gasCost < 0) {
            return false;
        }

        Level level = player.level();
        return CanisterContainerConsumers.interactContainer(player, gasContent.getGasType(), gasCost, () -> !level.isClientSide && level.getGameTime() % 20 == 0);
    }

    @Override
    public int getIndex() {
        return 1;
    }

    @Override
    public @NotNull ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("water_breathing");
    }

    @Override
    public @NotNull Item getUpgradeItem() {
        return AllItems.COPPER_DIVING_HELMET.asItem();
    }

    @Override
    public @NotNull Couple<Integer> getOffset() {
        return Couple.create(36, 55);
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_WATER_BREATHING;
    }

    @Override
    public boolean isRightIndicator() {
        return false;
    }

    @Override
    public @NotNull Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_helmet.water_breathing_upgrade");
    }

    @Override
    public @NotNull Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_helmet.water_breathing_upgrade.description");
    }

    @Override
    public @Nullable Component getGasCostComponent(Player player) {
        int gasCost = getGasCost(player);
        if (gasCost < 0) {
            return null;
        }

        return CCBLang.translateDirect("gui.airtight_helmet.water_breathing_upgrade.gas_cost", gasCost);
    }

    @Override
    public int getGasCost(Player player) {
        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            return -1;
        }

        Gas gasType = gasContent.getGasType();
        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasType);
        if (armorsHandler == null) {
            return -1;
        }

        return Mth.ceil(CCBConfig.server().equipments.waterBreathingGasCost.get() * armorsHandler.getConsumptionMultiplier()[0]);
    }

    @Override
    public boolean canApply(@NotNull Player player) {
        return false;
    }

    @Override
    public void applyEffect(Player player) {
    }
}
