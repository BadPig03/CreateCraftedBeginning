package net.ty.createcraftedbeginning.content.airtights.airtightextendarm;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandler;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandlerUtils;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerConsumers.AffordableFuel;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightExtendArmUtils {
    private static final ResourceLocation BLOCK_RANGE_MODIFIER_ID = CreateCraftedBeginning.asResource("airtight_extend_arm_block_range");
    private static final ResourceLocation ENTITY_RANGE_MODIFIER_ID = CreateCraftedBeginning.asResource("airtight_extend_arm_entity_range");
    private static final ResourceLocation KNOCKBACK_MODIFIER_ID = CreateCraftedBeginning.asResource("airtight_extend_arm_knockback");
    private static final int POWER_REFRESH_INTERVAL = 5;
    private static final Map<Player, AffordableFuel> ACTIVE_FUELS = new WeakHashMap<>();

    private AirtightExtendArmUtils() {
    }

    public static void tick(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        if (!isHoldingArms(player)) {
            removeArmModifiers(player);
            return;
        }

        if (player.tickCount % POWER_REFRESH_INTERVAL != 0) {
            return;
        }

        refreshArmModifiers(player);
    }

    public static void refreshArmModifiers(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        if (!isHoldingArms(player)) {
            removeArmModifiers(player);
            return;
        }

        Optional<AffordableFuel> fuel = findAffordableFuel(player);
        if (fuel.isEmpty()) {
            removeArmModifiers(player);
            return;
        }

        AffordableFuel selectedFuel = fuel.get();
        ACTIVE_FUELS.put(player, selectedFuel);
        applyArmModifiers(player, AirtightArmHandlerUtils.of(selectedFuel.gasType()));
    }

    public static Optional<AffordableFuel> findAffordableFuel(Player player) {
        return CanisterContainerConsumers.findAffordableFuel(player, gasType -> getRawGasConsumption(AirtightArmHandlerUtils.of(gasType)));
    }

    public static Optional<AffordableFuel> getCurrentFuelSelection(Player player) {
        if (!isHoldingArms(player)) {
            return Optional.empty();
        }

        if (player.level().isClientSide) {
            return findAffordableFuel(player);
        }
        return Optional.ofNullable(ACTIVE_FUELS.get(player));
    }

    public static boolean isHoldingArms(Player player) {
        return player.getMainHandItem().is(CCBItems.AIRTIGHT_EXTEND_ARM) || player.getOffhandItem().is(CCBItems.AIRTIGHT_EXTEND_ARM);
    }

    public static boolean isArmPowered(Player player) {
        return isBlockInteractionPowered(player) || isEntityInteractionPowered(player);
    }

    public static boolean isBlockInteractionPowered(Player player) {
        return hasModifier(player.getAttributes().getInstance(Attributes.BLOCK_INTERACTION_RANGE), BLOCK_RANGE_MODIFIER_ID);
    }

    public static boolean isEntityInteractionPowered(Player player) {
        AttributeMap attributes = player.getAttributes();
        return hasModifier(attributes.getInstance(Attributes.ENTITY_INTERACTION_RANGE), ENTITY_RANGE_MODIFIER_ID) || hasModifier(attributes.getInstance(Attributes.ATTACK_KNOCKBACK), KNOCKBACK_MODIFIER_ID);
    }

    public static boolean requiresExtendedBlockRange(Player player, BlockPos pos) {
        AttributeInstance instance = player.getAttributes().getInstance(Attributes.BLOCK_INTERACTION_RANGE);
        if (instance == null) {
            return false;
        }

        double rangeAdjustment = getAdjustmentWithoutModifier(instance, BLOCK_RANGE_MODIFIER_ID);
        return rangeAdjustment < 0 && !player.canInteractWithBlock(pos, rangeAdjustment);
    }

    public static boolean requiresExtendedEntityRange(Player player, Entity target) {
        AttributeInstance instance = player.getAttributes().getInstance(Attributes.ENTITY_INTERACTION_RANGE);
        if (instance == null) {
            return false;
        }

        double rangeAdjustment = getAdjustmentWithoutModifier(instance, ENTITY_RANGE_MODIFIER_ID);
        return rangeAdjustment < 0 && !player.canInteractWithEntity(target, rangeAdjustment);
    }

    public static boolean requiresPoweredAttack(Player player, Entity target) {
        AttributeInstance knockback = player.getAttributes().getInstance(Attributes.ATTACK_KNOCKBACK);
        return requiresExtendedEntityRange(player, target) || getModifierAmount(knockback) > 0;
    }

    public static boolean tryConsumeAndRefresh(Player player) {
        if (player.level().isClientSide || !isHoldingArms(player) || !isArmPowered(player)) {
            return false;
        }

        AffordableFuel selectedFuel = ACTIVE_FUELS.get(player);
        if (selectedFuel == null) {
            refreshArmModifiers(player);
            return false;
        }

        boolean consumed = CanisterContainerConsumers.interactContainer(player, selectedFuel.gasType(), selectedFuel.amount(), () -> true, false);
        refreshArmModifiers(player);
        return consumed;
    }

    private static double getRawGasConsumption(AirtightArmHandler armHandler) {
        return (double) CCBConfig.server().equipments.perUseConsumption.get() * armHandler.getGasConsumptionMultiplier();
    }

    private static void removeArmModifiers(Player player) {
        ACTIVE_FUELS.remove(player);

        AttributeMap attributes = player.getAttributes();
        removeModifier(attributes.getInstance(Attributes.BLOCK_INTERACTION_RANGE), BLOCK_RANGE_MODIFIER_ID);
        removeModifier(attributes.getInstance(Attributes.ENTITY_INTERACTION_RANGE), ENTITY_RANGE_MODIFIER_ID);
        removeModifier(attributes.getInstance(Attributes.ATTACK_KNOCKBACK), KNOCKBACK_MODIFIER_ID);
    }

    private static void applyArmModifiers(Player player, AirtightArmHandler armHandler) {
        AttributeMap attributes = player.getAttributes();
        syncModifier(attributes.getInstance(Attributes.BLOCK_INTERACTION_RANGE), BLOCK_RANGE_MODIFIER_ID, armHandler.getIncreasedBlockInteractionRange());
        syncModifier(attributes.getInstance(Attributes.ENTITY_INTERACTION_RANGE), ENTITY_RANGE_MODIFIER_ID, armHandler.getIncreasedEntityInteractionRange());
        syncModifier(attributes.getInstance(Attributes.ATTACK_KNOCKBACK), KNOCKBACK_MODIFIER_ID, armHandler.getIncreasedKnockback());
    }

    private static boolean hasModifier(@Nullable AttributeInstance instance, ResourceLocation id) {
        return instance != null && instance.getModifier(id) != null;
    }

    private static double getModifierAmount(@Nullable AttributeInstance instance) {
        if (instance == null) {
            return 0;
        }

        AttributeModifier modifier = instance.getModifier(KNOCKBACK_MODIFIER_ID);
        return modifier == null || modifier.operation() != Operation.ADD_VALUE ? 0 : modifier.amount();
    }

    private static double getAdjustmentWithoutModifier(AttributeInstance instance, ResourceLocation id) {
        AttributeModifier modifier = instance.getModifier(id);
        if (modifier == null || modifier.amount() <= 0) {
            return 0;
        }

        double currentValue = instance.getValue();
        AttributeInstance copy = new AttributeInstance(instance.getAttribute(), ignored -> {});
        copy.replaceFrom(instance);
        copy.removeModifier(id);
        return copy.getValue() - currentValue;
    }

    private static void removeModifier(@Nullable AttributeInstance instance, ResourceLocation id) {
        if (instance == null) {
            return;
        }

        instance.removeModifier(id);
    }

    private static void syncModifier(@Nullable AttributeInstance instance, ResourceLocation id, double amount) {
        if (instance == null) {
            return;
        }

        if (!Double.isFinite(amount) || amount <= 0) {
            instance.removeModifier(id);
            return;
        }

        AttributeModifier currentModifier = instance.getModifier(id);
        if (currentModifier != null && Double.compare(currentModifier.amount(), amount) == 0 && currentModifier.operation() == Operation.ADD_VALUE) {
            return;
        }

        instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, Operation.ADD_VALUE));
    }
}