package net.ty.createcraftedbeginning.content.airtights.airtightcannon;

import com.simibubi.create.content.equipment.zapper.ShootableGadgetItemMethods;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.canisters.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.api.gas.canisters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge.AirtightCannonWindChargeProjectileEntity;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.content.airtights.weatherflares.projectile.WeatherFlareProjectileEntity;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightCannonUtils {
    private static final int MIN_USE_TIME = 5;
    private static final int EFFICIENT_USE_TIME = 15;

    private AirtightCannonUtils() {
    }

    public static ExplosionDamageCalculator createDamageCalculator(float knockbackMultiplier) {
        return new SimpleExplosionDamageCalculator(true, false, Optional.of(knockbackMultiplier), BuiltInRegistries.BLOCK.getTag(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS).map(Function.identity()));
    }

    public static List<LivingEntity> getNearbyEntities(Level level, Vec3 pos, float radius, Entity source) {
        float affectRadius = radius * 2;
        float affectRadiusSqr = affectRadius * affectRadius;
        AABB aabb = new AABB(pos, pos).inflate(affectRadius, affectRadius, affectRadius);
        return level.getEntitiesOfClass(LivingEntity.class, aabb, entity -> (!(source instanceof AirtightCannonWindChargeProjectileEntity projectile) || !(projectile.getOwner() instanceof Player player) || entity != player) && entity.getBoundingBox().getCenter().distanceToSqr(pos) <= affectRadiusSqr);
    }

    public static float getChargedRatio(ItemStack cannon, Player player, int timeCharged) {
        if (!CanisterContainerSuppliers.isAnyContainerAvailable(player) || timeCharged < MIN_USE_TIME) {
            return -1;
        }

        return Mth.clamp((float) timeCharged / getEfficientUseTime(cannon), 0, 2);
    }

    public static int getEfficientUseTime(ItemStack cannon) {
        int quickChargeLevel = getEnchantmentLevel(cannon, Enchantments.QUICK_CHARGE);
        return Math.max(EFFICIENT_USE_TIME - quickChargeLevel * 3, 0);
    }

    public static int getEnchantmentLevel(ItemStack cannon, ResourceKey<Enchantment> enchantment) {
        if (!cannon.is(CCBItems.AIRTIGHT_CANNON)) {
            return 0;
        }

        return cannon.getTagEnchantments().entrySet().stream().filter(entry -> entry.getKey().is(enchantment)).findFirst().map(Entry::getValue).orElse(0);
    }

    public static int getGasConsumption(ItemStack cannon, float chargedRatio) {
        int multiShotMultiplier = 2 * getEnchantmentLevel(cannon, Enchantments.MULTISHOT) + 1;
        float ratio = chargedRatio >= 1 ? Mth.square(chargedRatio) : Mth.sqrt(chargedRatio);
        int gasConsumption = CCBConfig.server().equipments.perShotConsumption.get();
        return Math.round(gasConsumption * multiShotMultiplier * ratio);
    }

    public static void fireFlares(Level level, Player player, ItemStack flareItemStack, float chargedRatio) {
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
        if (!CanisterContainerConsumers.interactContainer(player, gasType, gasConsumption, () -> true, false)) {
            GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.insufficient_gas", gasContent.getHoverName());
            return;
        }

        int infinityLevel = getEnchantmentLevel(cannon, Enchantments.INFINITY);
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 barrelPos = eyePos.add(lookVec.scale(0.75));
        Vec3 motion = lookVec.normalize().scale(chargedRatio);

        WeatherFlareProjectileEntity flare = new WeatherFlareProjectileEntity(level, flareItemStack.getItem(), barrelPos.y);
        flare.setPos(barrelPos);
        flare.setOwner(player);
        flare.setDeltaMovement(motion);
        flare.setCopied(infinityLevel > 0);
        level.addFreshEntity(flare);
        if (!player.isCreative() && infinityLevel == 0) {
            flareItemStack.shrink(1);
        }
        ShootableGadgetItemMethods.applyCooldown(player, cannon, hand, s -> s.getItem() instanceof AirtightCannonItem, getEfficientUseTime(cannon));
        ShootableGadgetItemMethods.sendPackets(player, b -> new AirtightCannonPacket(player.getEyePosition().add(player.getLookAngle().scale(0.75)), player.getLookAngle().normalize(), ItemStack.EMPTY, hand, 1, b));
    }

    public static void spawnWindCharges(Level level, Player player, float chargedRatio) {
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
        if (!CanisterContainerConsumers.interactContainer(player, gasType, gasConsumption, () -> true, false)) {
            GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.insufficient_gas", gasContent.getHoverName());
            return;
        }

        RandomSource random = level.getRandom();
        Vec3 sprayBase = VecHelper.rotate(new Vec3(0, 0.1, 0), 360 * random.nextFloat(), Axis.Z);
        int multiShotLevel = getEnchantmentLevel(cannon, Enchantments.MULTISHOT);
        int punchLevel = getEnchantmentLevel(cannon, Enchantments.PUNCH);
        int powerLevel = getEnchantmentLevel(cannon, Enchantments.POWER);
        int flameLevel = getEnchantmentLevel(cannon, Enchantments.FLAME);
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
            windCharge.setMultiplier(chargedRatio + powerLevel * 0.125f);
            windCharge.setKnockback(0.1f + punchLevel * 0.25f);
            windCharge.setFlame(flameLevel > 0);
            level.addFreshEntity(windCharge);
        }

        ShootableGadgetItemMethods.applyCooldown(player, cannon, hand, s -> s.getItem() instanceof AirtightCannonItem, getEfficientUseTime(cannon));
        ShootableGadgetItemMethods.sendPackets(player, b -> new AirtightCannonPacket(player.getEyePosition().add(player.getLookAngle().scale(0.75)), player.getLookAngle().normalize(), ItemStack.EMPTY, hand, 1, b));
    }
}
