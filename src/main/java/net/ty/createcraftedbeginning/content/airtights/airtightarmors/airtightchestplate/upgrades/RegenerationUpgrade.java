package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades;

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
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum RegenerationUpgrade implements AirtightUpgrade {
    INSTANCE;

    private static final int DURATION_THRESHOLD = 30;
    private static final int DURATION_LIMIT = 10;
    private static final int AMPLIFIER = 1;

    @Override
    public @Unmodifiable List<Component> getComponents(Player player, ItemStack item) {
        int gasCost = getGasConsumptionPerSecond(player, item);
        if (gasCost == 0) {
            return List.of(CCBLang.translateDirect("gui.gas_consumption.supply_require_only"));
        }
        return List.of(CCBLang.translateDirect("gui.airtight_chestplate.regeneration_upgrade.gas_cost", gasCost));
    }

    @Override
    public boolean canApply(Player player) {
        return isActive(player, player.getItemBySlot(EquipmentSlot.CHEST));
    }

    @Override
    public boolean meetsConditions(Player player, ItemStack item) {
        MobEffectInstance effectInstance = player.getEffect(MobEffects.REGENERATION);
        return (effectInstance == null || effectInstance.getAmplifier() <= AMPLIFIER && effectInstance.endsWithin(DURATION_LIMIT)) && player.getHealth() < player.getMaxHealth();
    }

    @Override
    public boolean isRightIndicator() {
        return true;
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_REGENERATION;
    }

    @Override
    public Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_chestplate.regeneration_upgrade.description");
    }

    @Override
    public Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_chestplate.regeneration_upgrade");
    }

    @Override
    public Couple<Integer> getOffset() {
        return Couple.create(132, 31);
    }

    @Override
    public int getGasConsumptionPerSecond(Player player, ItemStack item) {
        return CCBConfig.server().equipments.regenerationConsumption.get();
    }

    @Override
    public int getIndex() {
        return 3;
    }

    @Override
    public Item getUpgradeItem() {
        return Items.ENCHANTED_GOLDEN_APPLE;
    }

    @Override
    public ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("regeneration");
    }

    @Override
    public void applyEffect(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DURATION_THRESHOLD, AMPLIFIER, true, false));
    }

    @Override
    public boolean isActive(Player player, ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_CHESTPLATE) && AirtightUpgrade.super.isActive(player, item);
    }
}
