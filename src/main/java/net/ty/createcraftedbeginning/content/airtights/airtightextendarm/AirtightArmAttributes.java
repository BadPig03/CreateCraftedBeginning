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
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandler;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightArmAttributes {
    private static final ResourceLocation BLOCK_RANGE_MODIFIER_ID = CCBAPI.asResource("airtight_extend_arm_block_range");
    private static final ResourceLocation ENTITY_RANGE_MODIFIER_ID = CCBAPI.asResource("airtight_extend_arm_entity_range");
    private static final ResourceLocation KNOCKBACK_MODIFIER_ID = CCBAPI.asResource("airtight_extend_arm_knockback");

    private AirtightArmAttributes() {
    }

    static boolean requiresExtendedBlockRange(Player player, BlockPos blockPos) {
        AttributeInstance blockRangeAttribute = player.getAttributes().getInstance(Attributes.BLOCK_INTERACTION_RANGE);
        if (blockRangeAttribute == null) {
            return false;
        }

        double unpoweredRangeAdjustment = getAdjustmentWithoutModifier(blockRangeAttribute, BLOCK_RANGE_MODIFIER_ID);
        return unpoweredRangeAdjustment < 0 && !player.canInteractWithBlock(blockPos, unpoweredRangeAdjustment);
    }

    static boolean requiresExtendedEntityRange(Player player, Entity targetEntity) {
        AttributeInstance entityRangeAttribute = player.getAttributes().getInstance(Attributes.ENTITY_INTERACTION_RANGE);
        if (entityRangeAttribute == null) {
            return false;
        }

        double unpoweredRangeAdjustment = getAdjustmentWithoutModifier(entityRangeAttribute, ENTITY_RANGE_MODIFIER_ID);
        return unpoweredRangeAdjustment < 0 && !player.canInteractWithEntity(targetEntity, unpoweredRangeAdjustment);
    }

    static boolean requiresPoweredAttack(Player player, Entity targetEntity) {
        AttributeInstance knockbackAttribute = player.getAttributes().getInstance(Attributes.ATTACK_KNOCKBACK);
        return requiresExtendedEntityRange(player, targetEntity) || getModifierAmount(knockbackAttribute) > 0;
    }

    static void applyArmModifiers(Player player, AirtightArmHandler armHandler) {
        AttributeMap attributes = player.getAttributes();
        syncModifier(attributes.getInstance(Attributes.BLOCK_INTERACTION_RANGE), BLOCK_RANGE_MODIFIER_ID, armHandler.getIncreasedBlockInteractionRange());
        syncModifier(attributes.getInstance(Attributes.ENTITY_INTERACTION_RANGE), ENTITY_RANGE_MODIFIER_ID, armHandler.getIncreasedEntityInteractionRange());
        syncModifier(attributes.getInstance(Attributes.ATTACK_KNOCKBACK), KNOCKBACK_MODIFIER_ID, armHandler.getIncreasedKnockback());
    }

    static void removeArmModifiers(Player player) {
        AttributeMap attributes = player.getAttributes();
        removeModifier(attributes.getInstance(Attributes.BLOCK_INTERACTION_RANGE), BLOCK_RANGE_MODIFIER_ID);
        removeModifier(attributes.getInstance(Attributes.ENTITY_INTERACTION_RANGE), ENTITY_RANGE_MODIFIER_ID);
        removeModifier(attributes.getInstance(Attributes.ATTACK_KNOCKBACK), KNOCKBACK_MODIFIER_ID);
    }

    private static double getModifierAmount(@Nullable AttributeInstance attribute) {
        if (attribute == null) {
            return 0;
        }

        AttributeModifier knockbackModifier = attribute.getModifier(KNOCKBACK_MODIFIER_ID);
        if (knockbackModifier == null || knockbackModifier.operation() != Operation.ADD_VALUE) {
            return 0;
        }

        return knockbackModifier.amount();
    }

    private static double getAdjustmentWithoutModifier(AttributeInstance attribute, ResourceLocation modifierId) {
        AttributeModifier modifier = attribute.getModifier(modifierId);
        if (modifier == null || modifier.amount() <= 0) {
            return 0;
        }

        double currentAttributeValue = attribute.getValue();
        AttributeInstance attributeWithoutModifier = new AttributeInstance(attribute.getAttribute(), ignored -> {});
        attributeWithoutModifier.replaceFrom(attribute);
        attributeWithoutModifier.removeModifier(modifierId);
        return attributeWithoutModifier.getValue() - currentAttributeValue;
    }

    private static void removeModifier(@Nullable AttributeInstance attribute, ResourceLocation modifierId) {
        if (attribute == null) {
            return;
        }

        attribute.removeModifier(modifierId);
    }

    private static void syncModifier(@Nullable AttributeInstance attribute, ResourceLocation modifierId, double modifierAmount) {
        if (attribute == null) {
            return;
        }

        if (!GasConsumptionMath.isFinite(modifierAmount) || modifierAmount <= 0) {
            attribute.removeModifier(modifierId);
            return;
        }

        AttributeModifier currentModifier = attribute.getModifier(modifierId);
        if (currentModifier != null && currentModifier.amount() == modifierAmount && currentModifier.operation() == Operation.ADD_VALUE) {
            return;
        }

        attribute.addOrUpdateTransientModifier(new AttributeModifier(modifierId, modifierAmount, Operation.ADD_VALUE));
    }
}
