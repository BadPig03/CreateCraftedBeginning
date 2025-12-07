package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.cansiters.GasCanisterExecuteUtils;
import net.ty.createcraftedbeginning.api.gas.cansiters.GasCanisterSupplierUtils;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public final class AirtightArmorsUtils {
    private AirtightArmorsUtils() {
    }

    @OnlyIn(Dist.CLIENT)
    public static void appendHelmetHoverText(ItemStack helmet, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || GasCanisterSupplierUtils.noUsableGasAvailable(player)) {
            return;
        }

        if (isEntireArmoredUp(player) && tooltipFlag.hasShiftDown()) {
            tooltip.add(CCBLang.translate("gui.tooltips.airtight_armors.fire_immune_condition").style(ChatFormatting.GRAY).component());
            tooltip.addAll(TooltipHelper.cutTextComponent(CCBLang.translateDirect("gui.tooltips.airtight_armors.fire_immune_behaviour"), Palette.STANDARD_CREATE));
        }

        GasStack gasStack = GasCanisterSupplierUtils.getFirstNonEmptyGasContent(player);
        if (gasStack.isEmpty()) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(CCBLang.gasName(gasStack).add(CCBLang.translate("gui.tooltips.gas_tools.content")).style(ChatFormatting.GRAY).component());

        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasStack.getGas());
        if (armorsHandler == null) {
            return;
        }

        armorsHandler.appendHelmetHoverText(helmet, context, tooltip, tooltipFlag);
    }

    @OnlyIn(Dist.CLIENT)
    public static void appendChestplateHoverText(ItemStack chestplate, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || GasCanisterSupplierUtils.noUsableGasAvailable(player)) {
            return;
        }

        if (isEntireArmoredUp(player) && tooltipFlag.hasShiftDown()) {
            tooltip.add(CCBLang.translate("gui.tooltips.airtight_armors.fire_immune_condition").style(ChatFormatting.GRAY).component());
            tooltip.addAll(TooltipHelper.cutTextComponent(CCBLang.translateDirect("gui.tooltips.airtight_armors.fire_immune_behaviour"), Palette.STANDARD_CREATE));
        }

        GasStack gasStack = GasCanisterSupplierUtils.getFirstNonEmptyGasContent(player);
        if (gasStack.isEmpty()) {
            return;
        }

        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasStack.getGas());
        if (armorsHandler == null) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(CCBLang.gasName(gasStack).add(CCBLang.translate("gui.tooltips.gas_tools.content")).style(ChatFormatting.GRAY).component());
        armorsHandler.appendChestplateHoverText(chestplate, context, tooltip, tooltipFlag);
    }

    @OnlyIn(Dist.CLIENT)
    public static void appendLeggingsHoverText(ItemStack leggings, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || GasCanisterSupplierUtils.noUsableGasAvailable(player)) {
            return;
        }

        if (isEntireArmoredUp(player) && tooltipFlag.hasShiftDown()) {
            tooltip.add(CCBLang.translate("gui.tooltips.airtight_armors.fire_immune_condition").style(ChatFormatting.GRAY).component());
            tooltip.addAll(TooltipHelper.cutTextComponent(CCBLang.translateDirect("gui.tooltips.airtight_armors.fire_immune_behaviour"), Palette.STANDARD_CREATE));
        }

        GasStack gasStack = GasCanisterSupplierUtils.getFirstNonEmptyGasContent(player);
        if (gasStack.isEmpty()) {
            return;
        }

        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasStack.getGas());
        if (armorsHandler == null) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(CCBLang.gasName(gasStack).add(CCBLang.translate("gui.tooltips.gas_tools.content")).style(ChatFormatting.GRAY).component());
        armorsHandler.appendLeggingsHoverText(leggings, context, tooltip, tooltipFlag);
    }

    @OnlyIn(Dist.CLIENT)
    public static void appendBootsHoverText(ItemStack boots, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || GasCanisterSupplierUtils.noUsableGasAvailable(player)) {
            return;
        }

        if (isEntireArmoredUp(player) && tooltipFlag.hasShiftDown()) {
            tooltip.add(CCBLang.translate("gui.tooltips.airtight_armors.fire_immune_condition").style(ChatFormatting.GRAY).component());
            tooltip.addAll(TooltipHelper.cutTextComponent(CCBLang.translateDirect("gui.tooltips.airtight_armors.fire_immune_behaviour"), Palette.STANDARD_CREATE));
        }

        GasStack gasStack = GasCanisterSupplierUtils.getFirstNonEmptyGasContent(player);
        if (gasStack.isEmpty()) {
            return;
        }

        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasStack.getGas());
        if (armorsHandler == null) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(CCBLang.gasName(gasStack).add(CCBLang.translate("gui.tooltips.gas_tools.content")).style(ChatFormatting.GRAY).component());
        armorsHandler.appendBootsHoverText(boots, context, tooltip, tooltipFlag);
    }

    public static boolean canPreventMobEffects(@NotNull Player player, MobEffectInstance effectInstance) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.is(CCBItems.AIRTIGHT_HELMET)) {
            return false;
        }
        if (GasCanisterSupplierUtils.noUsableGasAvailable(player)) {
            return false;
        }

        GasStack gasStack = GasCanisterSupplierUtils.getTotalGasStack(player);
        if (gasStack.isEmpty()) {
            return false;
        }

        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasStack.getGas());
        if (armorsHandler == null || !armorsHandler.canCureEffect(effectInstance)) {
            return false;
        }

        int gasConsumption = Mth.ceil((effectInstance.getAmplifier() + 1) * effectInstance.getDuration() / 20.0f * armorsHandler.getConsumptionMultiplier()[0]);
        return gasStack.getAmount() >= gasConsumption && GasCanisterExecuteUtils.tryGasConsumption(player, gasStack.getGas(), gasConsumption);
    }

    public static boolean canBreatheUnderwater(@NotNull Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.is(CCBItems.AIRTIGHT_HELMET)) {
            return false;
        }
        if (GasCanisterSupplierUtils.noUsableGasAvailable(player)) {
            return false;
        }

        GasStack gasStack = GasCanisterSupplierUtils.getTotalGasStack(player);
        if (gasStack.isEmpty()) {
            return false;
        }

        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasStack.getGas());
        if (armorsHandler == null) {
            return false;
        }

        int underwaterBreathingConsumption = CCBConfig.server().equipments.helmetUnderwaterBreathing.get();
        int gasConsumption = Mth.ceil(underwaterBreathingConsumption * armorsHandler.getConsumptionMultiplier()[0]);
        return gasStack.getAmount() >= gasConsumption && GasCanisterExecuteUtils.tryGasConsumption(player, gasStack.getGas(), gasConsumption, () -> player.level().getGameTime() % 20 == 10);
    }

    public static void refreshChestplateFlight(@NotNull Player player) {
        AttributeInstance instance = player.getAttributes().getInstance(NeoForgeMod.CREATIVE_FLIGHT);
        if (instance == null) {
            return;
        }

        boolean isFlightEnabled = instance.hasModifier(AirtightChestplateItem.CREATIVE_FLIGHT_ID);
        boolean canPlayerFly = canPlayerFly(player);
        if (canPlayerFly && !isFlightEnabled) {
            instance.addTransientModifier(AirtightChestplateItem.CREATIVE_FLIGHT);
        }
        else if (!canPlayerFly && isFlightEnabled) {
            instance.removeModifier(AirtightChestplateItem.CREATIVE_FLIGHT);
        }
    }

    public static boolean canPlayerFly(@NotNull Player player) {
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE)) {
            return false;
        }
        if (GasCanisterSupplierUtils.noUsableGasAvailable(player)) {
            return false;
        }

        GasStack gasStack = GasCanisterSupplierUtils.getTotalGasStack(player);
        if (gasStack.isEmpty()) {
            return false;
        }

        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasStack.getGas());
        if (armorsHandler == null) {
            return false;
        }

        int creativeFlightConsumption = CCBConfig.server().equipments.chestplateCreativeFlight.get();
        int gasConsumption = Mth.ceil(creativeFlightConsumption * armorsHandler.getConsumptionMultiplier()[1]);
        return gasStack.getAmount() >= gasConsumption && GasCanisterExecuteUtils.tryGasConsumption(player, gasStack.getGas(), gasConsumption, () -> player.getAbilities().flying && player.level().getGameTime() % 20 == 10);
    }

    public static void spawnFlightParticles(@NotNull Player player, @NotNull Level level) {
        if (player.isCreative() || player.isSpectator() || !player.getAbilities().flying) {
            return;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE)) {
            return;
        }
        if (GasCanisterSupplierUtils.noUsableGasAvailable(player)) {
            return;
        }

        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();
        double angle = -player.yBodyRot * Math.PI / 180;
        double leftFinalX = -0.48 * Math.sin(angle) - Math.cos(angle) * 0.24;
        double leftFinalZ = -0.48 * Math.cos(angle) + Math.sin(angle) * 0.24;
        double rightFinalX = -0.48 * Math.sin(angle) + Math.cos(angle) * 0.24;
        double rightFinalZ = -0.48 * Math.cos(angle) - Math.sin(angle) * 0.24;
        double finalY = player.getEyeHeight() * 0.4;
        level.addParticle(CCBParticleTypes.AIRTIGHT_JETPACK.getParticleOptions(), playerX + leftFinalX, playerY + finalY, playerZ + leftFinalZ, 0, -0.24, 0);
        level.addParticle(CCBParticleTypes.AIRTIGHT_JETPACK.getParticleOptions(), playerX + rightFinalX, playerY + finalY, playerZ + rightFinalZ, 0, -0.24, 0);
    }

    public static float getBoostMultiplier(@NotNull Player player) {
        if (!player.isFallFlying()) {
            return 0;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE)) {
            return 0;
        }
        if (player.getCooldowns().isOnCooldown(chestplate.getItem())) {
            return 0;
        }

        GasStack gasStack = GasCanisterSupplierUtils.getTotalGasStack(player);
        if (gasStack.isEmpty()) {
            return 0;
        }

        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasStack.getGas());
        if (armorsHandler == null) {
            return 0;
        }

        int boostElytraConsumption = CCBConfig.server().equipments.chestplateElytraBoost.get();
        int gasConsumption = Mth.ceil(boostElytraConsumption * armorsHandler.getConsumptionMultiplier()[1]);
        if (gasStack.getAmount() < gasConsumption) {
            return 0;
        }

        float multiplier = armorsHandler.getMultiplierForBoostingElytra();
        return GasCanisterExecuteUtils.tryGasConsumption(player, gasStack.getGas(), gasConsumption) ? multiplier : 0;
    }

    public static void boostElytra(@NotNull Player player, float multiplier) {
        Vec3 lookAngle = player.getLookAngle().scale(0.85f * multiplier);
        Vec3 movement = player.getDeltaMovement().scale(0.5f * multiplier);
        player.setDeltaMovement(movement.add(lookAngle));

        Vec3 pos = player.position();
        player.level().addParticle(ParticleTypes.GUST_EMITTER_SMALL, pos.x, pos.y, pos.z, 0, 0, 0);
        player.playSound(CCBSoundEvents.AIRTIGHT_JETPACK_LAUNCH.getMainEvent(), 1, 0.8f);
        CatnipServices.NETWORK.sendToServer(AirtightBoostElytraPacket.INSTANCE);
    }

    public static boolean canElytraFlightTick(@NotNull Player player, int flightTicks) {
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE)) {
            return false;
        }
        if (GasCanisterSupplierUtils.noUsableGasAvailable(player)) {
            return false;
        }

        GasStack gasStack = GasCanisterSupplierUtils.getTotalGasStack(player);
        if (gasStack.isEmpty()) {
            return false;
        }

        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasStack.getGas());
        if (armorsHandler == null) {
            return false;
        }

        int elytraGlidingConsumption = CCBConfig.server().equipments.chestplateElytraGliding.get();
        int gasConsumption = Mth.ceil(elytraGlidingConsumption * armorsHandler.getConsumptionMultiplier()[1]);
        return gasStack.getAmount() >= gasConsumption && GasCanisterExecuteUtils.tryGasConsumption(player, gasStack.getGas(), gasConsumption, () -> flightTicks % 20 == 10);
    }

    public static boolean canDeflectProjectile(@NotNull Player player) {
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!leggings.is(CCBItems.AIRTIGHT_LEGGINGS)) {
            return false;
        }

        GasStack gasStack = GasCanisterSupplierUtils.getTotalGasStack(player);
        if (gasStack.isEmpty()) {
            return false;
        }

        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasStack.getGas());
        if (armorsHandler == null) {
            return false;
        }

        int projectileDeflectionConsumption = CCBConfig.server().equipments.leggingsProjectileDeflection.get();
        int gasConsumption = Mth.ceil(projectileDeflectionConsumption * armorsHandler.getConsumptionMultiplier()[2]);
        return gasStack.getAmount() >= gasConsumption && GasCanisterExecuteUtils.tryGasConsumption(player, gasStack.getGas(), gasConsumption);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean canInvalidateDamage(@NotNull Player player, float amount, Supplier<Boolean> supplier) {
        if (GasCanisterSupplierUtils.noUsableGasAvailable(player)) {
            return false;
        }

        GasStack gasStack = GasCanisterSupplierUtils.getTotalGasStack(player);
        if (gasStack.isEmpty()) {
            return false;
        }

        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasStack.getGas());
        if (armorsHandler == null) {
            return false;
        }

        int damageConsumption = CCBConfig.server().equipments.armorsInvalidateDamage.get();
        int gasConsumption = Mth.ceil(damageConsumption * amount * armorsHandler.getConsumptionMultiplier()[3]);
        return gasStack.getAmount() >= gasConsumption && GasCanisterExecuteUtils.tryGasConsumption(player, gasStack.getGas(), gasConsumption, supplier);
    }

    public static boolean isEntireArmoredUp(@NotNull Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.is(CCBItems.AIRTIGHT_HELMET)) {
            return false;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE)) {
            return false;
        }

        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!leggings.is(CCBItems.AIRTIGHT_LEGGINGS)) {
            return false;
        }

        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        return boots.is(CCBItems.AIRTIGHT_BOOTS);
    }
}
