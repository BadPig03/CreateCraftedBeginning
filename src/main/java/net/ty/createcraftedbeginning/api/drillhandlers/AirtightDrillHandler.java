package net.ty.createcraftedbeginning.api.drillhandlers;

import com.simibubi.create.api.registry.SimpleRegistry;
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
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptionUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AirtightDrillHandler {
    SimpleRegistry<Gas, AirtightDrillHandler> REGISTRY = SimpleRegistry.create();

    int BASE_DAMAGE_AMOUNT = 1;

    /**
     * Returns the damage addition.
     *
     * @return the damage addition
     */
    int getDamageAddition();

    /**
     * Returns the consumption multiplier.
     *
     * @return the consumption multiplier
     */
    float getConsumptionMultiplier();

    /**
     * Appends this object's contextual information to the supplied tooltip.
     *
     * @param drill   the airtight drill item stack
     * @param context the context for the operation
     * @param tooltip the tooltip entries to append to
     * @param flag    the tooltip display flags
     */
    default void appendHoverText(ItemStack drill, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        float consumptionMultiplier = getConsumptionMultiplier();
        MutableComponent advancedGasConsumption = flag.isAdvanced() ? Component.literal(" [x" + GasConsumptionUtils.format(consumptionMultiplier) + ']') : Component.empty();
        tooltip.add(Component.translatable(CCBAPI.MOD_ID + ".gui.gas_tools.gas_consumption", GasConsumptionUtils.formatPercent(consumptionMultiplier)).append(advancedGasConsumption.withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GREEN));

        int additionDamage = getDamageAddition();
        int damage = BASE_DAMAGE_AMOUNT + additionDamage;
        MutableComponent advancedAttackDamage = flag.isAdvanced() ? Component.literal(" [" + (additionDamage != 0 ? "" : "+") + BASE_DAMAGE_AMOUNT + (additionDamage != 0 ? " + " + additionDamage : "") + ']') : Component.empty();
        tooltip.add(Component.translatable(CCBAPI.MOD_ID + ".gui.airtight_handheld_drill.attack_damage", damage).append(advancedAttackDamage.withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GREEN));
    }

    /**
     * Updates state by performing extra behaviour.
     *
     * @param entity      the entity associated with the operation
     * @param player      the player performing the operation
     * @param serverLevel the server level to use
     */
    default void extraBehaviour(LivingEntity entity, Player player, ServerLevel serverLevel) {
        Vec3 position = entity.position();
        serverLevel.sendParticles(ParticleTypes.CRIT, position.x, position.y + entity.getBbHeight() / 2, position.z, serverLevel.random.nextInt(5, 15), 0.4, 0.25, 0.4, 0);
    }
}
