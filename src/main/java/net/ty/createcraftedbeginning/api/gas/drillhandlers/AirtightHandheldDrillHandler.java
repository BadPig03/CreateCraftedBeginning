package net.ty.createcraftedbeginning.api.gas.drillhandlers;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.data.CCBLang;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface AirtightHandheldDrillHandler {
    SimpleRegistry<Gas, AirtightHandheldDrillHandler> REGISTRY = SimpleRegistry.create();

    int BASE_DAMAGE_AMOUNT = 1;

    int getDamageAddition();

    float getConsumptionMultiplier();

    default String getRenderStr(float n) {
        return String.format("%.2f", n).replaceAll("\\.?0+$", "");
    }

    default void appendHoverText(ItemStack drill, TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        float consumptionMultiplier = getConsumptionMultiplier();
        MutableComponent advancedGasConsumption = flag.isAdvanced() ? CCBLang.text(" [x" + getRenderStr(consumptionMultiplier) + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.tooltips.gas_tools.gas_consumption", getRenderStr(consumptionMultiplier * 100)).add(advancedGasConsumption.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());

        int additionDamage = getDamageAddition();
        int damage = BASE_DAMAGE_AMOUNT + additionDamage;
        MutableComponent advancedAttackDamage = flag.isAdvanced() ? CCBLang.text(" [" + (additionDamage != 0 ? "" : "+") + BASE_DAMAGE_AMOUNT + (additionDamage != 0 ? " + " + additionDamage : "") + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.tooltips.airtight_handheld_drill.attack_damage", damage).add(advancedAttackDamage.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());
    }

    default void extraBehaviour(@NotNull LivingEntity entity, Player player, @NotNull ServerLevel serverLevel) {
        Vec3 position = entity.position();
        RandomSource random = serverLevel.random;
        for (int i = 0; i < random.nextInt(5, 15); i++) {
            double offsetX = (random.nextDouble() - 0.5) * 0.8;
            double offsetY = random.nextDouble() * 0.5;
            double offsetZ = (random.nextDouble() - 0.5) * 0.8;
            serverLevel.sendParticles(ParticleTypes.CRIT, position.x, position.y + entity.getBbHeight() / 2, position.z, 1, offsetX, offsetY, offsetZ, 0);
        }
    }
}
