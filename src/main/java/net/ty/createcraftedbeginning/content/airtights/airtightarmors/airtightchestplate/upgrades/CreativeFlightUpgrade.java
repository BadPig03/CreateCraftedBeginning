package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades;

import com.simibubi.create.AllItems;
import net.createmod.catnip.data.Couple;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeIcon;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradePowerMode;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.TickingAirtightUpgrade;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBMobEffects;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum CreativeFlightUpgrade implements TickingAirtightUpgrade {
    INSTANCE;

    private static final ResourceLocation ID = CreateCraftedBeginning.asResource("creative_flight");
    private static final Couple<Integer> OFFSET = Couple.create(36, 55);

    private static final int EFFECT_DURATION = 40;
    private static final int REFRESH_THRESHOLD = 20;

    @Override
    public @Unmodifiable List<Component> getComponents(Player player, ItemStack item) {
        int consumption = CCBConfig.server().equipments.creativeFlightConsumption.get();
        if (consumption == 0) {
            return List.of(CCBLang.translateDirect("gui.gas_consumption.supply_require_only"));
        }
        return List.of(CCBLang.translateDirect("gui.airtight_chestplate.creative_flight_upgrade.gas_cost.flying", consumption));
    }

    @Override
    public boolean canApply(Player player) {
        return isActive(player, player.getItemBySlot(EquipmentSlot.CHEST));
    }

    @Override
    public boolean meetsConditions(Player player, ItemStack item) {
        return true;
    }

    @Override
    public boolean isRightIndicator() {
        return false;
    }

    @Override
    public AirtightUpgradeIcon getIcon() {
        return AirtightUpgradeIcon.CREATIVE_FLIGHT;
    }

    @Override
    public Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_chestplate.creative_flight_upgrade.description");
    }

    @Override
    public Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_chestplate.creative_flight_upgrade");
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
        if (!player.getAbilities().flying) {
            return 0;
        }
        return CCBConfig.server().equipments.creativeFlightConsumption.get();
    }

    @Override
    public Item getUpgradeItem() {
        return AllItems.NETHERITE_BACKTANK.asItem();
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public void applyEffect(Player player) {
        player.addEffect(new MobEffectInstance(CCBMobEffects.JETPACK_FLIGHT, EFFECT_DURATION, 0, true, false));
    }

    @Override
    public boolean isActive(Player player, ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_CHESTPLATE) && TickingAirtightUpgrade.super.isActive(player, item);
    }

    @Override
    public boolean shouldApplyEffect(Player player, ItemStack item) {
        MobEffectInstance effect = player.getEffect(CCBMobEffects.JETPACK_FLIGHT);
        return effect == null || effect.getAmplifier() == 0 && effect.endsWithin(REFRESH_THRESHOLD);
    }
}
