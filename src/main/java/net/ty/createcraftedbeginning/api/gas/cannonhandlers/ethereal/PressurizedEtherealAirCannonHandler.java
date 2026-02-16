package net.ty.createcraftedbeginning.api.gas.cannonhandlers.ethereal;

import net.minecraft.ChatFormatting;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonUtils;
import net.ty.createcraftedbeginning.data.CCBLang;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PressurizedEtherealAirCannonHandler extends EtherealAirCannonHandler {
    @Override
    public ItemStack getRenderIcon(@NotNull Level level) {
        ItemStack icon = super.getRenderIcon(level);
        RegistryLookup<Enchantment> enchantmentRegistryLookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        icon.enchant(enchantmentRegistryLookup.getOrThrow(Enchantments.MENDING), 1);
        return icon;
    }

    @Override
    public void renderTrailParticles(@NotNull Level level, @NotNull Vec3 pos) {
        super.renderTrailParticles(level, pos);
    }

    @Override
    public void explode(@NotNull Level level, @NotNull Vec3 pos, Entity source, float multiplier) {
        super.explode(level, pos, source, multiplier);

        List<LivingEntity> entities = AirtightCannonUtils.getNearbyEntities(level, pos, DEFAULT_RADIUS * multiplier, source);
        if (entities.isEmpty()) {
            return;
        }

        MobEffectInstance effectInstance = new MobEffectInstance(MobEffects.LEVITATION, Math.round(DEFAULT_DURATION * 2 * multiplier), 1);
        for (LivingEntity entity : entities) {
            entity.addEffect(effectInstance);
        }
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return super.getTextureLocation();
    }

    @Override
    public LayerDefinition getLayerDefinition() {
        return super.getLayerDefinition();
    }

    @Override
    public float[] getSetupAnim(float ageInTicks) {
        return super.getSetupAnim(ageInTicks);
    }

    @Override
    public float getGasConsumptionMultiplier() {
        return 0.37f;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack cannon, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(CCBLang.translate("gui.tooltips.airtight_cannon.pressurized_ethereal_air").style(ChatFormatting.DARK_GREEN).component());
    }
}
