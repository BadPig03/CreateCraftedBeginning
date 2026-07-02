package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent.RenderFog;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.Result;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.AirtightHelmetUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.EffectsProtectionUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.HelmetResistanceUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.VisionUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.WaterBreathingUpgrade;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class AirtightHelmetEvents {
    @SubscribeEvent
    public static void onMobEffectApplicable(Applicable event) {
        if (!(event.getEntity() instanceof Player player) || !EffectsProtectionUpgrade.INSTANCE.canApply(player, event.getEffectInstance())) {
            return;
        }

        event.setResult(Result.DO_NOT_APPLY);
    }

    @SubscribeEvent
    public static void onPlayerBreathe(LivingBreatheEvent event) {
        if (event.canBreathe() || !(event.getEntity() instanceof Player player) || !WaterBreathingUpgrade.INSTANCE.canApply(player)) {
            return;
        }

        event.setConsumeAirAmount(0);
        event.setCanBreathe(true);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onRenderFog(RenderFog event) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        Camera camera = event.getCamera();
        BlockPos blockPos = camera.getBlockPosition();
        FluidState fluidState = level.getFluidState(blockPos);
        if (camera.getPosition().y >= blockPos.getY() + fluidState.getHeight(level, blockPos)) {
            return;
        }

        FluidType fluidType = fluidState.getType().getFluidType();
        if (fluidType == Fluids.EMPTY.getFluidType()) {
            return;
        }

        Entity entity = camera.getEntity();
        if (!(entity instanceof Player player) || player.isSpectator() || !VisionUpgrade.INSTANCE.canApply(player)) {
            return;
        }

        event.setNearPlaneDistance(-8);
        event.setFarPlaneDistance(128);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerPreTakeDamage(Pre event) {
        if (!(event.getEntity() instanceof Player player) || event.getSource().is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            return;
        }

        HelmetResistanceUpgrade.INSTANCE.canApply(player, event.getOriginalDamage());
    }

    @SubscribeEvent
    public static void onPlayerTick(Post event) {
        Player player = event.getEntity();
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.is(CCBItems.AIRTIGHT_HELMET) || player.level().isClientSide) {
            return;
        }

        AirtightHelmetUpgradeRegistry.forEach(upgrade -> {
            if (!upgrade.canApply(player)) {
                return;
            }

            upgrade.applyEffect(player);
        });
    }
}
