package net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightCannonWindChargeItem extends Item {
    private static final int COOLDOWN = 10;

    private final Supplier<Gas> gasSupplier;

    public AirtightCannonWindChargeItem(Properties properties, Supplier<Gas> gasSupplier) {
        super(properties);
        this.gasSupplier = gasSupplier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack windChargeStack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            shoot(level, player);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WIND_CHARGE_THROW, SoundSource.NEUTRAL, 0.5f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
        player.getCooldowns().addCooldown(this, COOLDOWN);
        player.awardStat(Stats.ITEM_USED.get(this));
        windChargeStack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(windChargeStack, level.isClientSide);
    }

    private void shoot(Level level, Player player) {
        Vec3 lookDirection = player.getLookAngle();
        Vec3 barrelPos = player.getEyePosition().add(lookDirection.scale(0.75));
        Vec3 launchMotion = lookDirection.normalize().scale(2);

        AirtightCannonWindChargeProjectileEntity windCharge = new AirtightCannonWindChargeProjectileEntity(level, gasSupplier.get().getHolder(), launchMotion);
        windCharge.setPos(barrelPos);
        windCharge.setOwner(player);
        windCharge.setDeltaMovement(launchMotion);
        windCharge.setMultiplier(1);
        windCharge.setKnockback(0.1f);

        level.addFreshEntity(windCharge);
    }
}
