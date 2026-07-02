package net.ty.createcraftedbeginning.content.airtights.airtightextendarm;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.armhandlers.AirtightExtendArmHandler;
import net.ty.createcraftedbeginning.api.gas.canisters.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.api.gas.canisters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightExtendArmUtils {
    private static final ResourceLocation BLOCK_RANGE_MODIFIER_ID = CreateCraftedBeginning.asResource("airtight_extend_arm_block_range");
    private static final ResourceLocation ENTITY_RANGE_MODIFIER_ID = CreateCraftedBeginning.asResource("airtight_extend_arm_entity_range");
    private static final ResourceLocation KNOCKBACK_MODIFIER_ID = CreateCraftedBeginning.asResource("airtight_extend_arm_knockback");

    private AirtightExtendArmUtils() {
    }

    public static void refreshArmModifiers(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        if (!isHoldingArms(player)) {
            removeArmModifiers(player);
            return;
        }

        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            removeArmModifiers(player);
            return;
        }

        Gas gasType = gasContent.getGasType();
        AirtightExtendArmHandler armHandler = AirtightExtendArmHandler.REGISTRY.get(gasType);
        if (armHandler == null) {
            removeArmModifiers(player);
            return;
        }

        long gasConsumption = getGasConsumption(armHandler);
        if (!CanisterContainerConsumers.interactContainer(player, gasType, gasConsumption, () -> true, true)) {
            removeArmModifiers(player);
            return;
        }

        applyArmModifiers(player, armHandler);
    }

    public static boolean isHoldingArms(Player player) {
        return player.getMainHandItem().is(CCBItems.AIRTIGHT_EXTEND_ARM) || player.getOffhandItem().is(CCBItems.AIRTIGHT_EXTEND_ARM);
    }

    public static void consumeAndRefresh(Player player) {
        if (!updateOperationAbility(player)) {
            return;
        }

        refreshArmModifiers(player);
    }

    private static void removeArmModifiers(Player player) {
        AttributeInstance blockInstance = player.getAttributes().getInstance(Attributes.BLOCK_INTERACTION_RANGE);
        AttributeInstance entityInstance = player.getAttributes().getInstance(Attributes.ENTITY_INTERACTION_RANGE);
        AttributeInstance knockbackInstance = player.getAttributes().getInstance(Attributes.ATTACK_KNOCKBACK);
        if (blockInstance == null || entityInstance == null || knockbackInstance == null) {
            return;
        }

        blockInstance.removeModifier(BLOCK_RANGE_MODIFIER_ID);
        entityInstance.removeModifier(ENTITY_RANGE_MODIFIER_ID);
        knockbackInstance.removeModifier(KNOCKBACK_MODIFIER_ID);
    }

    private static boolean updateOperationAbility(Player player) {
        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            return false;
        }

        Gas gasType = gasContent.getGasType();
        AirtightExtendArmHandler armHandler = AirtightExtendArmHandler.REGISTRY.get(gasType);
        return armHandler != null && CanisterContainerConsumers.interactContainer(player, gasType, getGasConsumption(armHandler), () -> true, false);
    }

    private static long getGasConsumption(AirtightExtendArmHandler armHandler) {
        return Math.max(0, Mth.ceil(CCBConfig.server().equipments.perUseConsumption.get() * armHandler.getGasConsumptionMultiplier()));
    }

    private static void applyArmModifiers(Player player, AirtightExtendArmHandler armHandler) {
        AttributeInstance blockInstance = player.getAttributes().getInstance(Attributes.BLOCK_INTERACTION_RANGE);
        AttributeInstance entityInstance = player.getAttributes().getInstance(Attributes.ENTITY_INTERACTION_RANGE);
        AttributeInstance knockbackInstance = player.getAttributes().getInstance(Attributes.ATTACK_KNOCKBACK);
        if (blockInstance == null || entityInstance == null || knockbackInstance == null) {
            return;
        }

        syncModifier(blockInstance, BLOCK_RANGE_MODIFIER_ID, armHandler.getIncreasedBlockInteractionRange());
        syncModifier(entityInstance, ENTITY_RANGE_MODIFIER_ID, armHandler.getIncreasedEntityInteractionRange());
        syncModifier(knockbackInstance, KNOCKBACK_MODIFIER_ID, armHandler.getIncreasedKnockback());
    }

    private static void syncModifier(AttributeInstance instance, ResourceLocation id, double amount) {
        AttributeModifier currentModifier = instance.getModifier(id);
        if (currentModifier != null && Double.compare(currentModifier.amount(), amount) == 0 && currentModifier.operation() == Operation.ADD_VALUE) {
            return;
        }

        instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, Operation.ADD_VALUE));
    }
}