package net.ty.createcraftedbeginning.api.drillhandlers;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AirtightDrillHandler {
    int BASE_DAMAGE_AMOUNT = 1;

    int getDamageAddition();

    double getConsumptionMultiplier();

    default void appendHoverText(ItemStack drill, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        double consumptionMultiplier = getConsumptionMultiplier();
        boolean advanced = flag.isAdvanced();
        MutableComponent advancedGasConsumption = advanced ? Component.literal(" [x" + GasConsumptionMath.format(consumptionMultiplier) + ']') : Component.empty();
        tooltip.add(Component.translatable(CCBAPI.MOD_ID + ".gui.gas_tools.gas_consumption", GasConsumptionMath.formatPercent(consumptionMultiplier)).append(advancedGasConsumption.withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GREEN));

        int additionDamage = getDamageAddition();
        int damage = BASE_DAMAGE_AMOUNT + additionDamage;
        MutableComponent advancedAttackDamage = Component.empty();
        if (advanced) {
            String damageDetails;
            if (additionDamage == 0) {
                damageDetails = '+' + String.valueOf(BASE_DAMAGE_AMOUNT);
            }
            else {
                damageDetails = BASE_DAMAGE_AMOUNT + " + " + additionDamage;
            }
            advancedAttackDamage = Component.literal(" [" + damageDetails + ']');
        }
        tooltip.add(Component.translatable(CCBAPI.MOD_ID + ".gui.airtight_handheld_drill.attack_damage", damage).append(advancedAttackDamage.withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GREEN));
        appendAdditionalHoverText(drill, context, tooltip, flag);
    }

    /** Appends gas-specific information after the current handler's standard consumption and damage lines. */
    default void appendAdditionalHoverText(ItemStack drill, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    }

    default void extraBehaviour(LivingEntity entity, Player player, ServerLevel serverLevel) {
        Vec3 position = entity.position();
        serverLevel.sendParticles(ParticleTypes.CRIT, position.x, position.y + entity.getBbHeight() / 2, position.z, serverLevel.random.nextInt(5, 15), 0.4, 0.25, 0.4, 0);
    }
}
