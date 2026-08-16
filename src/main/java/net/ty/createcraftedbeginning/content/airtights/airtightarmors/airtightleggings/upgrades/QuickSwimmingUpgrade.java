package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades;

import net.createmod.catnip.data.Couple;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradePowerMode;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.TickingAirtightUpgrade;
import net.ty.createcraftedbeginning.foundation.gui.AirtightUpgradeIcon;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum QuickSwimmingUpgrade implements TickingAirtightUpgrade {
    INSTANCE;

    private static final ResourceLocation ID = CCBAPI.asResource("quick_swimming");
    private static final Couple<Integer> OFFSET = Couple.create(36, 55);

    private static final int EFFECT_DURATION = 40;
    private static final int REFRESH_THRESHOLD = 20;

    @Override
    public @Unmodifiable List<Component> getComponents(Player player, ItemStack item) {
        int gasCost = getGasConsumptionPerSecond(player, item);
        if (gasCost == 0) {
            return List.of(CCBLang.translateDirect("gui.gas_consumption.supply_require_only"));
        }
        return List.of(CCBLang.translateDirect("gui.airtight_leggings.quick_swimming_upgrade.gas_cost", gasCost));
    }

    @Override
    public boolean canApply(Player player) {
        return isActive(player, player.getItemBySlot(EquipmentSlot.LEGS));
    }

    @Override
    public boolean meetsConditions(Player player, ItemStack item) {
        if (!player.isInWaterRainOrBubble()) {
            return false;
        }

        MobEffectInstance dolphinsGraceEffect = player.getEffect(MobEffects.DOLPHINS_GRACE);
        return dolphinsGraceEffect == null || dolphinsGraceEffect.getAmplifier() == 0;
    }

    @Override
    public boolean isRightIndicator() {
        return false;
    }

    @Override
    public AirtightUpgradeIcon getIcon() {
        return AirtightUpgradeIcon.QUICK_SWIMMING;
    }

    @Override
    public Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_leggings.quick_swimming_upgrade.description");
    }

    @Override
    public Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_leggings.quick_swimming_upgrade");
    }

    @Override
    public Couple<Integer> getOffset() {
        return OFFSET;
    }

    @Override
    public AirtightUpgradePowerMode getPowerMode() {
        return AirtightUpgradePowerMode.CONTINUOUS;
    }

    @Override
    public int getGasConsumptionPerSecond(Player player, ItemStack item) {
        return CCBConfig.server().equipments.quickSwimmingConsumption.get();
    }

    @Override
    public Item getUpgradeItem() {
        return Items.AXOLOTL_BUCKET;
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public void applyEffect(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, EFFECT_DURATION, 0, true, false));
    }

    @Override
    public boolean isActive(Player player, ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_LEGGINGS) && TickingAirtightUpgrade.super.isActive(player, item);
    }

    @Override
    public boolean shouldApplyEffect(Player player, ItemStack item) {
        MobEffectInstance dolphinsGraceEffect = player.getEffect(MobEffects.DOLPHINS_GRACE);
        return dolphinsGraceEffect == null || dolphinsGraceEffect.getAmplifier() == 0 && dolphinsGraceEffect.endsWithin(REFRESH_THRESHOLD);
    }
}
