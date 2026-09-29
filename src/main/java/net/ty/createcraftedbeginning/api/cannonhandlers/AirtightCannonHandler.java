package net.ty.createcraftedbeginning.api.cannonhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AirtightCannonHandler {
    void explode(Level level, Vec3 pos, AirtightCannonShotContext context);

    float getGasConsumptionMultiplier();

    void appendHoverText(ItemStack cannon, TooltipContext context, List<Component> tooltip, TooltipFlag flag);

    default float getDirectHitDamage() {
        return 1;
    }

    default DamageSource createDirectHitDamageSource(Entity projectile, @Nullable LivingEntity owner) {
        return projectile.damageSources().windCharge(projectile, owner);
    }

    default void applyAdditionalEffects(Level level, List<LivingEntity> entities, DamageSource explosionDamageSource, AirtightCannonShotContext context) {
    }
}
