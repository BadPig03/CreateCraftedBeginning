package net.ty.createcraftedbeginning.content.airtights.airtightcannon;

import com.simibubi.create.content.equipment.zapper.ShootableGadgetItemMethods;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerClients;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge.AirtightCannonWindChargeProjectileEntity;
import net.ty.createcraftedbeginning.content.airtights.weatherflares.WeatherFlareProjectileEntity;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

public final class AirtightCannonUtils {
    private static final int MIN_USE_TIME = 5;
    private static final int EFFICIENT_USE_TIME = 15;

    private AirtightCannonUtils() {
    }

    public static @NotNull ExplosionDamageCalculator createDamageCalculator(float knockbackMultiplier) {
        return new SimpleExplosionDamageCalculator(true, false, Optional.of(knockbackMultiplier), BuiltInRegistries.BLOCK.getTag(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS).map(Function.identity()));
    }

    public static @NotNull List<LivingEntity> getNearbyEntities(@NotNull Level level, @NotNull Vec3 pos, float radius) {
        float affectRadius = radius * 2;
        AABB aabb = new AABB(pos, pos).inflate(affectRadius, affectRadius, affectRadius);
        return level.getEntitiesOfClass(LivingEntity.class, aabb);
    }

    public static float getChargedRatio(@NotNull ItemStack cannon, Player player, int timeCharged) {
        if (!CanisterContainerSuppliers.isAnyContainerAvailable(player) || timeCharged < MIN_USE_TIME) {
            return -1;
        }

        return Mth.clamp((float) timeCharged / getEfficientUseTime(cannon), 0, 2);
    }

    public static int getEfficientUseTime(ItemStack cannon) {
        int quickChargeLevel = getEnchantmentQuickChargeLevel(cannon);
        return Math.max(EFFICIENT_USE_TIME - quickChargeLevel * 3, 0);
    }

    public static int getEnchantmentMultiShotLevel(@NotNull ItemStack cannon) {
        if (!cannon.is(CCBItems.AIRTIGHT_CANNON)) {
            return 0;
        }

        return cannon.getTagEnchantments().entrySet().stream().filter(entry -> entry.getKey().is(Enchantments.MULTISHOT)).findFirst().map(Entry::getValue).orElse(0);
    }

    public static int getEnchantmentPunchLevel(@NotNull ItemStack cannon) {
        if (!cannon.is(CCBItems.AIRTIGHT_CANNON)) {
            return 0;
        }

        return cannon.getTagEnchantments().entrySet().stream().filter(entry -> entry.getKey().is(Enchantments.PUNCH)).findFirst().map(Entry::getValue).orElse(0);
    }

    public static int getEnchantmentQuickChargeLevel(@NotNull ItemStack cannon) {
        if (!cannon.is(CCBItems.AIRTIGHT_CANNON)) {
            return 0;
        }

        return cannon.getTagEnchantments().entrySet().stream().filter(entry -> entry.getKey().is(Enchantments.QUICK_CHARGE)).findFirst().map(Entry::getValue).orElse(0);
    }

    public static int getGasConsumption(ItemStack cannon, float chargedRatio) {
        int multiShotMultiplier = 2 * getEnchantmentMultiShotLevel(cannon) + 1;
        float ratio = chargedRatio >= 1 ? Mth.square(chargedRatio) : Mth.sqrt(chargedRatio);
        int gasConsumption = CCBConfig.server().equipments.gasCostPerShot.get();
        return Math.round(gasConsumption * multiShotMultiplier * ratio);
    }

    @OnlyIn(Dist.CLIENT)
    public static void appendHoverText(ItemStack cannon, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(CCBLang.gasName(gasContent).add(CCBLang.translate("gui.tooltips.gas_tools.content")).style(ChatFormatting.GRAY).component());
        AirtightCannonHandler cannonHandler = AirtightCannonHandler.REGISTRY.get(gasContent.getGasType());
        if (cannonHandler == null) {
            return;
        }

        cannonHandler.appendHoverText(cannon, context, tooltip, tooltipFlag);
        float consumptionMultiplier = cannonHandler.getGasConsumptionMultiplier();
        MutableComponent advancedConsumptionMultiplier = tooltipFlag.isAdvanced() ? CCBLang.text(" [x" + cannonHandler.getRenderStr(consumptionMultiplier) + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.tooltips.gas_tools.gas_consumption", cannonHandler.getRenderStr(consumptionMultiplier * 100)).add(advancedConsumptionMultiplier.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());
    }

    public static void fireFlares(Level level, @NotNull Player player, ItemStack flareItemStack, float chargedRatio) {
        if (!CanisterContainerSuppliers.isAnyContainerAvailable(player)) {
            return;
        }

        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        Gas gasType = gasContent.getGasType();
        AirtightCannonHandler cannonHandler = AirtightCannonHandler.REGISTRY.get(gasType);
        if (cannonHandler == null) {
            return;
        }

        InteractionHand hand = player.getUsedItemHand();
        ItemStack cannon = player.getItemInHand(hand);
        int gasConsumption = Mth.ceil(getGasConsumption(cannon, chargedRatio) * cannonHandler.getGasConsumptionMultiplier());
        if (!CanisterContainerConsumers.interactContainer(player, gasType, gasConsumption, () -> !player.level().isClientSide)) {
            CanisterContainerClients.displayCustomWarningHint(player, "gui.warnings.insufficient_gas", gasContent.getHoverName());
            return;
        }

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 barrelPos = eyePos.add(lookVec.scale(0.75));
        Vec3 motion = lookVec.normalize().scale(chargedRatio);

        WeatherFlareProjectileEntity flare = new WeatherFlareProjectileEntity(level, flareItemStack.getItem(), barrelPos.y);
        flare.setPos(barrelPos);
        flare.setOwner(player);
        flare.setDeltaMovement(motion);
        level.addFreshEntity(flare);
        if (!player.isCreative()) {
            flareItemStack.shrink(1);
        }
        ShootableGadgetItemMethods.applyCooldown(player, cannon, hand, s -> s.getItem() instanceof AirtightCannonItem, getEfficientUseTime(cannon));
        ShootableGadgetItemMethods.sendPackets(player, b -> new AirtightCannonPacket(player.getEyePosition().add(player.getLookAngle().scale(0.75)), player.getLookAngle().normalize(), ItemStack.EMPTY, hand, 1, b));
    }

    public static void spawnWindCharges(Level level, @NotNull Player player, float chargedRatio) {
        if (!CanisterContainerSuppliers.isAnyContainerAvailable(player)) {
            return;
        }

        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        Gas gasType = gasContent.getGasType();
        AirtightCannonHandler cannonHandler = AirtightCannonHandler.REGISTRY.get(gasType);
        if (cannonHandler == null) {
            return;
        }

        InteractionHand hand = player.getUsedItemHand();
        ItemStack cannon = player.getItemInHand(hand);
        int gasConsumption = Mth.ceil(getGasConsumption(cannon, chargedRatio) * cannonHandler.getGasConsumptionMultiplier());
        if (!CanisterContainerConsumers.interactContainer(player, gasType, gasConsumption, () -> !player.level().isClientSide)) {
            CanisterContainerClients.displayCustomWarningHint(player, "gui.warnings.insufficient_gas", gasContent.getHoverName());
            return;
        }

        RandomSource random = level.getRandom();
        Vec3 sprayBase = VecHelper.rotate(new Vec3(0, 0.1, 0), 360 * random.nextFloat(), Axis.Z);
        int multiShotLevel = getEnchantmentMultiShotLevel(cannon);
        int punchLevel = getEnchantmentPunchLevel(cannon);
        int windChargesCount = multiShotLevel * 2 + 1;
        float sprayChange = 360.0f / windChargesCount;
        Holder<Gas> gasHolder = gasContent.getGasHolder();
        for (int i = 0; i < windChargesCount; i++) {
            Vec3 eyePos = player.getEyePosition();
            Vec3 lookVec = player.getLookAngle();
            Vec3 barrelPos = eyePos.add(lookVec.scale(0.75));
            Vec3 motion = lookVec.normalize().scale(2);
            Vec3 splitMotion = motion;
            if (windChargesCount > 1) {
                float imperfection = 45 * (random.nextFloat() - 0.5f);
                Vec3 sprayOffset = VecHelper.rotate(sprayBase, i * sprayChange + imperfection, Axis.Z);
                splitMotion = splitMotion.add(VecHelper.lookAt(sprayOffset, motion));
            }

            AirtightCannonWindChargeProjectileEntity windCharge = new AirtightCannonWindChargeProjectileEntity(level, gasHolder, splitMotion);
            windCharge.setPos(barrelPos);
            windCharge.setOwner(player);
            windCharge.setDeltaMovement(splitMotion);
            windCharge.setMultiplier(chargedRatio);
            windCharge.setKnockback(0.1f + punchLevel * 0.2f);
            level.addFreshEntity(windCharge);
        }

        ShootableGadgetItemMethods.applyCooldown(player, cannon, hand, s -> s.getItem() instanceof AirtightCannonItem, getEfficientUseTime(cannon));
        ShootableGadgetItemMethods.sendPackets(player, b -> new AirtightCannonPacket(player.getEyePosition().add(player.getLookAngle().scale(0.75)), player.getLookAngle().normalize(), ItemStack.EMPTY, hand, 1, b));
    }
}
