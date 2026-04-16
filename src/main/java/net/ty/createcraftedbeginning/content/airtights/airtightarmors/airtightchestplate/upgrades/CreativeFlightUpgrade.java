package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades;

import com.simibubi.create.AllItems;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.AirtightChestplateElytraBoostPacket;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.AirtightChestplateItem;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum CreativeFlightUpgrade implements AirtightUpgrade {
    INSTANCE;

    public static void spawnParticles(@NotNull Player player, @NotNull Level level) {
        if (!CCBConfig.client().enableChestplateJetpackParticles.get() || player.isCreative() || player.isSpectator() || !player.getAbilities().flying) {
            return;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE) || !INSTANCE.isEnabled(chestplate) || !CanisterContainerSuppliers.isAnyContainerAvailable(player)) {
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

    public static boolean canEnable(@NotNull Player player) {
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE) || !INSTANCE.isEnabled(chestplate) || !CanisterContainerSuppliers.isAnyContainerAvailable(player)) {
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
        return CanisterContainerConsumers.interactContainer(player, gasContent.getGasType(), gasCost, () -> !level.isClientSide && player.getAbilities().flying && level.getGameTime() % 20 == 0);
    }

    public static void refreshModifiers(@NotNull Player player) {
        AttributeInstance instance = player.getAttributes().getInstance(NeoForgeMod.CREATIVE_FLIGHT);
        if (instance == null) {
            return;
        }

        boolean hasModifier = instance.hasModifier(AirtightChestplateItem.CREATIVE_FLIGHT_ID);
        boolean canEnable = canEnable(player);
        if (canEnable && !hasModifier) {
            instance.addTransientModifier(AirtightChestplateItem.CREATIVE_FLIGHT_MODIFIER);
        }
        else if (!canEnable && hasModifier) {
            instance.removeModifier(AirtightChestplateItem.CREATIVE_FLIGHT_MODIFIER);
        }
    }

    public static float getBoostMultiplier(@NotNull Player player) {
        if (!player.isFallFlying()) {
            return 0;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE) || !ElytraUpgrade.INSTANCE.isEnabled(chestplate) || !INSTANCE.isEnabled(chestplate) || player.getCooldowns().isOnCooldown(chestplate.getItem())) {
            return 0;
        }

        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            return 0;
        }

        Gas gasType = gasContent.getGasType();
        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasType);
        if (armorsHandler == null) {
            return 0;
        }

        int gasConsumption = Mth.ceil(CCBConfig.server().equipments.elytraGasCost.get() * 10 * armorsHandler.getConsumptionMultiplier()[1]);
        if (!CanisterContainerConsumers.interactContainer(player, gasType, gasConsumption, () -> !player.level().isClientSide)) {
            return 0;
        }

        return armorsHandler.getMultiplierForBoostingElytra();
    }

    public static void speedBoost(@NotNull Player player, float multiplier) {
        Vec3 pos = player.position();
        Vec3 lookAngle = player.getLookAngle().scale(0.85f * multiplier);
        Vec3 movement = player.getDeltaMovement().scale(0.5f * multiplier);
        player.setDeltaMovement(movement.add(lookAngle));
        player.playSound(CCBSoundEvents.AIRTIGHT_JETPACK_LAUNCH.getMainEvent(), 1, 0.8f);
        player.level().addParticle(ParticleTypes.GUST_EMITTER_SMALL, pos.x, pos.y, pos.z, 0, 0, 0);
        CatnipServices.NETWORK.sendToServer(AirtightChestplateElytraBoostPacket.INSTANCE);
    }

    @Override
    public int getIndex() {
        return 1;
    }

    @Override
    public @NotNull ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("creative_flight");
    }

    @Override
    public @NotNull Item getUpgradeItem() {
        return AllItems.NETHERITE_BACKTANK.asItem();
    }

    @Override
    public @NotNull Couple<Integer> getOffset() {
        return Couple.create(36, 55);
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_CREATIVE_FLIGHT;
    }

    @Override
    public boolean isRightIndicator() {
        return false;
    }

    @Override
    public @NotNull Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_chestplate.creative_flight_upgrade");
    }

    @Override
    public @NotNull Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_chestplate.creative_flight_upgrade.description");
    }

    @Override
    public @Nullable Component getGasCostComponent(Player player) {
        int gasCost = getGasCost(player);
        if (gasCost < 0) {
            return null;
        }

        return CCBLang.translateDirect("gui.gas_cost_per_second", gasCost);
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

        return Mth.ceil(CCBConfig.server().equipments.creativeFlightGasCost.get() * armorsHandler.getConsumptionMultiplier()[1]);
    }

    @Override
    public boolean canApply(@NotNull Player player) {
        return false;
    }

    @Override
    public void applyEffect(Player player) {
    }
}
