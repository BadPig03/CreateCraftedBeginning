package net.ty.createcraftedbeginning.api.drillhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/** Omitted values and additional behavior delegate to the previous handler; supplied values are absolute. */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AirtightDrillHandlerPatch(@Nullable Integer damageBonus, @Nullable Double consumptionMultiplier) {
    public AirtightDrillHandlerPatch {
        if (damageBonus == null && consumptionMultiplier == null) {
            throw new IllegalArgumentException("An airtight drill handler patch must change at least one field.");
        }
        if (damageBonus != null && (damageBonus < 0 || damageBonus > Integer.MAX_VALUE - AirtightDrillHandler.BASE_DAMAGE_AMOUNT)) {
            throw new IllegalArgumentException("Airtight drill damage bonus must leave room for base damage in a non-negative integer, got " + damageBonus + '.');
        }
        if (consumptionMultiplier != null && !GasConsumptionMath.isNonNegativeFinite(consumptionMultiplier)) {
            throw new IllegalArgumentException("Airtight drill consumption multiplier must be finite and non-negative, got " + consumptionMultiplier + '.');
        }
    }

    AirtightDrillHandler applyTo(AirtightDrillHandler original) {
        try {
            if (original.getClass().getMethod("appendHoverText", ItemStack.class, TooltipContext.class, List.class, TooltipFlag.class).getDeclaringClass() != AirtightDrillHandler.class) {
                throw new IllegalArgumentException("Partial drill modifications require standard numeric tooltips; put custom tooltip additions in appendAdditionalHoverText instead of overriding appendHoverText.");
            }
        }
        catch (NoSuchMethodException failure) {
            throw new IllegalStateException("The original drill handler does not expose its tooltip contract.", failure);
        }

        return new AirtightDrillHandler() {
            @Override
            public int getDamageAddition() {
                if (damageBonus == null) {
                    return original.getDamageAddition();
                }

                return damageBonus;
            }

            @Override
            public double getConsumptionMultiplier() {
                if (consumptionMultiplier == null) {
                    return original.getConsumptionMultiplier();
                }

                return consumptionMultiplier;
            }

            @Override
            public void appendAdditionalHoverText(ItemStack drill, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                original.appendAdditionalHoverText(drill, context, tooltip, flag);
            }

            @Override
            public void extraBehaviour(LivingEntity entity, Player player, ServerLevel serverLevel) {
                original.extraBehaviour(entity, player, serverLevel);
            }
        };
    }
}
