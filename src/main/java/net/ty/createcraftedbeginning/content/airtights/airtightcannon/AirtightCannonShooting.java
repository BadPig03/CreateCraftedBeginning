package net.ty.createcraftedbeginning.content.airtights.airtightcannon;

import com.simibubi.create.content.equipment.zapper.ShootableGadgetItemMethods;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandlers;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge.AirtightCannonWindChargeProjectileEntity;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClients;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers.AffordableFuel;
import net.ty.createcraftedbeginning.content.airtights.weatherflares.projectile.WeatherFlareProjectileEntity;
import net.ty.createcraftedbeginning.gas.interaction.GasInteractionFeedback;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightCannonShooting {
    private static final int SHOT_COOLDOWN = 15;
    private static final float POWER_MULTIPLIER_PER_LEVEL = 0.125F;

    private AirtightCannonShooting() {
    }

    static void fireFlares(Level level, Player player, ItemStack flareStack, float chargedRatio) {
        InteractionHand hand = player.getUsedItemHand();
        ItemStack cannon = player.getItemInHand(hand);
        if (consumeShotFuel(player, chargedRatio, 1).isEmpty()) {
            return;
        }

        int infinityLevel = AirtightCannonCharge.getEnchantmentLevel(cannon, Enchantments.INFINITY);
        Vec3 lookDirection = player.getLookAngle().normalize();
        Vec3 barrelPos = player.getEyePosition().add(lookDirection.scale(0.75));
        Vec3 flareMotion = lookDirection.scale(chargedRatio);

        WeatherFlareProjectileEntity flareProjectile = new WeatherFlareProjectileEntity(level, flareStack.getItem(), barrelPos.y);
        flareProjectile.setPos(barrelPos);
        flareProjectile.setOwner(player);
        flareProjectile.setDeltaMovement(flareMotion);
        flareProjectile.setCopied(infinityLevel > 0);
        level.addFreshEntity(flareProjectile);
        if (!player.isCreative() && infinityLevel == 0) {
            flareStack.shrink(1);
        }

        finishShot(player, cannon, hand, barrelPos, lookDirection);
    }

    static void spawnWindCharges(Level level, Player player, float chargedRatio) {
        InteractionHand hand = player.getUsedItemHand();
        ItemStack cannon = player.getItemInHand(hand);
        int windChargeCount = (2 * AirtightCannonCharge.getEnchantmentLevel(cannon, Enchantments.MULTISHOT) + 1);
        Optional<ShotFuel> fuel = consumeShotFuel(player, chargedRatio, windChargeCount);
        if (fuel.isEmpty()) {
            return;
        }

        ShotFuel selectedFuel = fuel.get();
        int punchLevel = AirtightCannonCharge.getEnchantmentLevel(cannon, Enchantments.PUNCH);
        int powerLevel = AirtightCannonCharge.getEnchantmentLevel(cannon, Enchantments.POWER);
        boolean hasFlame = AirtightCannonCharge.getEnchantmentLevel(cannon, Enchantments.FLAME) > 0;
        float powerMultiplier = 1 + powerLevel * POWER_MULTIPLIER_PER_LEVEL;
        float effectMultiplier = chargedRatio * powerMultiplier;
        float knockbackMultiplier = 0.1F + punchLevel * 0.25F;

        Vec3 lookDirection = player.getLookAngle().normalize();
        Vec3 barrelPos = player.getEyePosition().add(lookDirection.scale(0.75));
        Vec3 baseMotion = lookDirection.scale(2);
        RandomSource random = level.getRandom();
        Vec3 spreadBase = windChargeCount > 1 ? VecHelper.rotate(new Vec3(0, 0.1, 0), 360 * random.nextFloat(), Axis.Z) : Vec3.ZERO;
        float spreadStepDegrees = 360.0F / windChargeCount;
        Holder<Gas> gasHolder = selectedFuel.gasType().getHolder();
        for (int projectileIndex = 0; projectileIndex < windChargeCount; projectileIndex++) {
            Vec3 projectileMotion = baseMotion;
            if (windChargeCount > 1) {
                float spreadJitterDegrees = 45 * (random.nextFloat() - 0.5F);
                Vec3 spreadOffset = VecHelper.rotate(spreadBase, projectileIndex * spreadStepDegrees + spreadJitterDegrees, Axis.Z);
                projectileMotion = projectileMotion.add(VecHelper.lookAt(spreadOffset, baseMotion));
            }

            AirtightCannonWindChargeProjectileEntity windCharge = new AirtightCannonWindChargeProjectileEntity(level, gasHolder, projectileMotion);
            windCharge.setPos(barrelPos);
            windCharge.setOwner(player);
            windCharge.setDeltaMovement(projectileMotion);
            windCharge.setSourcePressurePa(selectedFuel.sourcePressurePa());
            windCharge.setMultiplier(effectMultiplier);
            windCharge.setKnockback(knockbackMultiplier);
            windCharge.setFlame(hasFlame);
            level.addFreshEntity(windCharge);
        }

        finishShot(player, cannon, hand, barrelPos, lookDirection);
    }

    private static double getRawGasConsumption(float chargedRatio, int projectileCount) {
        float chargeConsumptionMultiplier = chargedRatio >= 1 ? Mth.square(chargedRatio) : Mth.sqrt(chargedRatio);
        int perShotConsumption = CCBConfig.server().equipment.airtightCannon.gasPerShot.get();
        return (double) perShotConsumption * projectileCount * chargeConsumptionMultiplier;
    }

    private static Optional<ShotFuel> resolveShotFuel(Player player, float chargedRatio, int projectileCount) {
        Gas selectedGas = CanisterContainerClients.getStoredGasType(player);
        if (selectedGas.isEmpty()) {
            return Optional.empty();
        }

        double rawGasConsumption = getRawGasConsumption(chargedRatio, projectileCount);
        Optional<AffordableFuel> affordableFuel = CanisterContainerConsumers.findAffordableFuel(player, selectedGas, context -> {
            AirtightCannonHandler cannonHandler = AirtightCannonHandlers.resolveForEquipment(context.gasType());
            return rawGasConsumption * cannonHandler.getGasConsumptionMultiplier();
        });
        return affordableFuel.map(ShotFuel::new);
    }

    private static Optional<ShotFuel> consumeShotFuel(Player player, float chargedRatio, int projectileCount) {
        Optional<ShotFuel> fuel = resolveShotFuel(player, chargedRatio, projectileCount);
        if (fuel.isEmpty()) {
            GasInteractionFeedback.sendWarningFeedback(player, "gui.warnings.no_gas");
            return Optional.empty();
        }

        ShotFuel selectedFuel = fuel.get();
        if (CanisterContainerConsumers.interactContainer(player, selectedFuel.fuel(), () -> true, false)) {
            return fuel;
        }

        GasInteractionFeedback.sendWarningFeedback(player, "gui.warnings.insufficient_gas", Component.translatable(selectedFuel.gasType().getTranslationKey()));
        return Optional.empty();
    }

    private static void finishShot(Player player, ItemStack cannon, InteractionHand hand, Vec3 barrelPos, Vec3 lookDirection) {
        ShootableGadgetItemMethods.applyCooldown(player, cannon, hand, stack -> stack.getItem() instanceof AirtightCannonItem, SHOT_COOLDOWN);
        ShootableGadgetItemMethods.sendPackets(player, isSelf -> new AirtightCannonPacket(barrelPos, lookDirection, ItemStack.EMPTY, hand, 1, isSelf));
    }

    private record ShotFuel(AffordableFuel fuel) {
        private Gas gasType() {
            return fuel.gasType();
        }

        private long sourcePressurePa() {
            return fuel.sourcePressurePa();
        }
    }
}
