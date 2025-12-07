package net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge;

import net.minecraft.core.Holder;
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
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class AirtightCannonWindChargeItem extends Item {
    private static final int COOLDOWN = 10;
    private final Supplier<Gas> gasSupplier;

    public AirtightCannonWindChargeItem(Properties properties, Supplier<Gas> gasSupplier) {
        super(properties);
        this.gasSupplier = gasSupplier;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        if (!level.isClientSide) {
            shoot(level, player, itemStack.isEnchanted());
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WIND_CHARGE_THROW, SoundSource.NEUTRAL, 0.5f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
        player.getCooldowns().addCooldown(this, COOLDOWN);
        player.awardStat(Stats.ITEM_USED.get(this));

        itemStack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide);
    }

    private void shoot(@NotNull Level level, @NotNull Player player, boolean enchanted) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 barrelPos = eyePos.add(lookVec.scale(0.75));
        Vec3 motion = lookVec.normalize().scale(2);
        Gas pressurizedGas = gasSupplier.get().getPressurizedGas();
        Holder<Gas> holder = enchanted ? pressurizedGas.getHolder() : gasSupplier.get().getHolder();

        AirtightCannonWindChargeProjectileEntity windCharge = new AirtightCannonWindChargeProjectileEntity(level, holder, motion);
        windCharge.setPos(barrelPos);
        windCharge.setOwner(player);
        windCharge.setDeltaMovement(motion);
        windCharge.setMultiplier(1);
        windCharge.setKnockback(0.1f);
        level.addFreshEntity(windCharge);
    }
}
