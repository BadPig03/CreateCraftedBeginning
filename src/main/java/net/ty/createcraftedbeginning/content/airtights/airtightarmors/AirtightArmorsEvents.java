package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class AirtightArmorsEvents {
    @SubscribeEvent
    public static void onAirtightArmorFireImmune(@NotNull LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !AirtightArmorsUtils.isEntireArmoredUp(player)) {
            return;
        }

        DamageSource damageSource = event.getSource();
        if (damageSource.type().effects() != DamageEffects.BURNING || !(damageSource.is(DamageTypes.LAVA) || damageSource.is(DamageTypes.IN_FIRE) || damageSource.is(DamageTypes.ON_FIRE))) {
            return;
        }

        if (player.getRemainingFireTicks() > 0) {
            player.setRemainingFireTicks(0);
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(@NotNull Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }

        AirtightArmorsUtils.applyResistance(player);
    }
}
