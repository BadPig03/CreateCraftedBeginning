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
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBMobEffects;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum CreativeFlightUpgrade implements AirtightUpgrade {
    INSTANCE;

    private static final int DURATION_THRESHOLD = 30;

    public static void spawnParticles(Player player, Level level) {
        if (!CCBConfig.client().enableChestplateJetpackParticles.get() || player.isCreative() || player.isSpectator() || !INSTANCE.canApply(player) || !player.getAbilities().flying || player.getEffect(CCBMobEffects.JETPACK_FLIGHT) == null) {
            return;
        }

        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();
        double angle = -player.yBodyRot * Math.PI / 180;
        double finalY = player.getEyeHeight() * 0.4;
        level.addParticle(CCBParticleTypes.AIRTIGHT_JETPACK.getParticleOptions(), playerX + -0.48 * Math.sin(angle) - Math.cos(angle) * 0.24, playerY + finalY, playerZ + -0.48 * Math.cos(angle) + Math.sin(angle) * 0.24, 0, -0.24, 0);
        level.addParticle(CCBParticleTypes.AIRTIGHT_JETPACK.getParticleOptions(), playerX + -0.48 * Math.sin(angle) + Math.cos(angle) * 0.24, playerY + finalY, playerZ + -0.48 * Math.cos(angle) - Math.sin(angle) * 0.24, 0, -0.24, 0);
    }

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
        MobEffectInstance existingEffect = player.getEffect(CCBMobEffects.JETPACK_FLIGHT);
        return existingEffect == null || existingEffect.getAmplifier() == 0 && existingEffect.endsWithin(DURATION_THRESHOLD);
    }

    @Override
    public boolean isRightIndicator() {
        return false;
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_CREATIVE_FLIGHT;
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
        return Couple.create(36, 55);
    }

    @Override
    public int getGasConsumptionPerSecond(Player player, ItemStack item) {
        if (player.getAbilities().flying) {
            return CCBConfig.server().equipments.creativeFlightConsumption.get();
        }
        return 0;
    }

    @Override
    public int getIndex() {
        return 1;
    }

    @Override
    public Item getUpgradeItem() {
        return AllItems.NETHERITE_BACKTANK.asItem();
    }

    @Override
    public ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("creative_flight");
    }

    @Override
    public void applyEffect(Player player) {
        player.addEffect(new MobEffectInstance(CCBMobEffects.JETPACK_FLIGHT, DURATION_THRESHOLD, 0, true, false));
    }

    @Override
    public boolean isActive(Player player, ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_CHESTPLATE) && AirtightUpgrade.super.isActive(player, item);
    }
}
