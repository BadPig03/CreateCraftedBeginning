package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades;

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
public enum VisionUpgrade implements AirtightUpgrade {
    INSTANCE;

    private static final int DURATION_THRESHOLD = 230;

    @Override
    public @Unmodifiable List<Component> getComponents(Player player, ItemStack item) {
        int gasCost = getGasConsumptionPerSecond(player, item);
        if (gasCost == 0) {
            return List.of(CCBLang.translateDirect("gui.gas_consumption.supply_require_only"));
        }
        return List.of(CCBLang.translateDirect("gui.gas_consumption_per_second", gasCost));
    }

    @Override
    public boolean canApply(Player player) {
        return isActive(player, player.getItemBySlot(EquipmentSlot.HEAD));
    }

    @Override
    public boolean meetsConditions(Player player, ItemStack item) {
        MobEffectInstance effectInstance = player.getEffect(MobEffects.NIGHT_VISION);
        return effectInstance == null || effectInstance.getAmplifier() == 0 && effectInstance.endsWithin(DURATION_THRESHOLD);
    }

    @Override
    public boolean isRightIndicator() {
        return true;
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_VISION;
    }

    @Override
    public Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_helmet.vision_upgrade.description");
    }

    @Override
    public Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_helmet.vision_upgrade");
    }

    @Override
    public Couple<Integer> getOffset() {
        return Couple.create(132, 31);
    }

    @Override
    public int getGasConsumptionPerSecond(Player player, ItemStack item) {
        return CCBConfig.server().equipments.visionConsumption.get();
    }

    @Override
    public int getIndex() {
        return 3;
    }

    @Override
    public Item getUpgradeItem() {
        return CCBItems.AMETHYST_CRYSTAL_SHEET.asItem();
    }

    @Override
    public ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("vision");
    }

    @Override
    public void applyEffect(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, DURATION_THRESHOLD, 0, true, false));
    }

    @Override
    public boolean isActive(Player player, ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_HELMET) && AirtightUpgrade.super.isActive(player, item);
    }
}
