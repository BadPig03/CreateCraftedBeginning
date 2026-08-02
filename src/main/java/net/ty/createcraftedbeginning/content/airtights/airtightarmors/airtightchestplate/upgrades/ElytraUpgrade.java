package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades;

import net.createmod.catnip.data.Couple;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandlerUtils;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerConsumers.AffordableFuel;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradePowerMode;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum ElytraUpgrade implements AirtightUpgrade {
    INSTANCE;

    private static final ResourceLocation ID = CreateCraftedBeginning.asResource("elytra");
    private static final Couple<Integer> OFFSET = Couple.create(36, 31);

    public static boolean canRequestBoost(Player player) {
        if (!player.isFallFlying()) {
            return false;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        return isBoostReady(player, chestplate) && findBoostFuel(player, chestplate).isPresent();
    }

    private static boolean isBoostReady(Player player, ItemStack chestplate) {
        return chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE) && INSTANCE.isEnabled(chestplate) && !player.getCooldowns().isOnCooldown(chestplate.getItem());
    }

    private static Optional<AffordableFuel> findBoostFuel(Player player, ItemStack chestplate) {
        int baseCost = INSTANCE.getGasConsumptionPerSecond(player, chestplate);
        return CanisterContainerConsumers.findAffordableFuel(player, gasType -> {
            AirtightArmorsHandler armorsHandler = AirtightArmorsHandlerUtils.of(gasType);
            float boostMultiplier = armorsHandler.getMultiplierForBoostingElytra();
            if (!Float.isFinite(boostMultiplier) || boostMultiplier <= 0) {
                return -1;
            }
            return baseCost * armorsHandler.getConsumptionMultiplier(EquipmentSlot.CHEST);
        });
    }

    public static boolean applyClientSpeedBoost(Player player) {
        if (!player.level().isClientSide || !player.isFallFlying()) {
            return false;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!isBoostReady(player, chestplate)) {
            return false;
        }

        Optional<AffordableFuel> fuel = findBoostFuel(player, chestplate);
        if (fuel.isEmpty()) {
            return false;
        }

        float multiplier = AirtightArmorsHandlerUtils.of(fuel.get().gasType()).getMultiplierForBoostingElytra();
        return applySpeedBoost(player, multiplier);
    }

    public static boolean tryApplySpeedBoost(Player player) {
        if (player.level().isClientSide || !player.isFallFlying()) {
            return false;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!isBoostReady(player, chestplate)) {
            return false;
        }

        Optional<AffordableFuel> fuel = findBoostFuel(player, chestplate);
        if (fuel.isEmpty()) {
            GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.insufficient_gas");
            return false;
        }

        AffordableFuel selectedFuel = fuel.get();
        float multiplier = AirtightArmorsHandlerUtils.of(selectedFuel.gasType()).getMultiplierForBoostingElytra();
        if (!Float.isFinite(multiplier) || multiplier <= 0) {
            return false;
        }
        if (!CanisterContainerConsumers.interactContainer(player, selectedFuel.gasType(), selectedFuel.amount(), () -> true, false)) {
            GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.insufficient_gas", selectedFuel.gasContent().getHoverName());
            return false;
        }

        return applySpeedBoost(player, multiplier);
    }

    private static boolean applySpeedBoost(Player player, float multiplier) {
        if (!Float.isFinite(multiplier) || multiplier <= 0) {
            return false;
        }

        Vec3 position = player.position();
        Vec3 boost = player.getLookAngle().scale(0.85 * multiplier);
        Vec3 movement = player.getDeltaMovement().scale(0.75 * multiplier);
        player.setDeltaMovement(movement.add(boost));
        player.hasImpulse = true;
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, position.x, position.y, position.z, 1, 0, 0, 0, 0);
            CCBSoundEvents.AIRTIGHT_JETPACK_LAUNCH.playOnServer(serverLevel, BlockPos.containing(position));
        }
        return true;
    }

    @Override
    public @Unmodifiable List<Component> getComponents(Player player, ItemStack item) {
        int gasCost = getGasConsumptionPerSecond(player, item);
        if (gasCost == 0) {
            return List.of(CCBLang.translateDirect("gui.gas_consumption.supply_require_only"));
        }
        return List.of(CCBLang.translateDirect("gui.gas_consumption_per_boost", gasCost));
    }

    @Override
    public boolean canApply(Player player) {
        return isActive(player, player.getItemBySlot(EquipmentSlot.CHEST));
    }

    @Override
    public boolean meetsConditions(Player player, ItemStack item) {
        return player.isFallFlying();
    }

    @Override
    public boolean isRightIndicator() {
        return false;
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_ELYTRA;
    }

    @Override
    public Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_chestplate.elytra_upgrade.description");
    }

    @Override
    public Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_chestplate.elytra_upgrade");
    }

    @Override
    public Couple<Integer> getOffset() {
        return OFFSET;
    }

    @Override
    public AirtightUpgradePowerMode getPowerMode() {
        return AirtightUpgradePowerMode.ON_DEMAND;
    }

    @Override
    public int getGasConsumptionPerSecond(Player player, ItemStack item) {
        return CCBConfig.server().equipments.elytraConsumption.get();
    }

    @Override
    public Item getUpgradeItem() {
        return Items.ELYTRA;
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public void applyEffect(Player player) {
    }

    @Override
    public boolean isActive(Player player, ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_CHESTPLATE) && AirtightUpgrade.super.isActive(player, item);
    }

    public boolean canFly(Player player, ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_CHESTPLATE) && isEnabled(item) && CanisterContainerConsumers.findAffordableFuel(player, gasType -> 0).isPresent();
    }
}
