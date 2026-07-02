package net.ty.createcraftedbeginning.content.airtights.airtightcannon;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge.AirtightCannonWindChargeProjectileEntity;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class AirtightCannonEvents {
    @SubscribeEvent
    public static void onAirtightCannonKillEntity(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }

        DamageSource damageSource = event.getSource();
        Entity directEntity = damageSource.getDirectEntity();
        if (!(directEntity instanceof AirtightCannonWindChargeProjectileEntity projectile)) {
            return;
        }
        if (!(projectile.getOwner() instanceof Player player)) {
            return;
        }
        if (entity.getType().getCategory() != MobCategory.MONSTER) {
            return;
        }

        CCBAdvancements.WIND_CHARGED.awardTo(player);
        if (!(entity instanceof Breeze)) {
            return;
        }

        CCBAdvancements.WHO_IS_THE_BREEZE_NOW.awardTo(player);
    }

    @SubscribeEvent
    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        ItemStack usingItem = player.getUseItem();
        if (!usingItem.is(CCBItems.AIRTIGHT_CANNON) || !player.isUsingItem()) {
            return;
        }

        float fovModifier = 1 - Math.min((float) (usingItem.getUseDuration(player) - player.getUseItemRemainingTicks()) / (AirtightCannonUtils.getEfficientUseTime(usingItem) * 2), 1) * 0.15f;
        event.setNewFovModifier(event.getFovModifier() * fovModifier);
    }
}
