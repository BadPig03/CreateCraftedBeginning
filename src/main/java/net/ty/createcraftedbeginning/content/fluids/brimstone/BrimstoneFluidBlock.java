package net.ty.createcraftedbeginning.content.fluids.brimstone;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.registry.CCBDamageSources;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BrimstoneFluidBlock extends LiquidBlock {
    public BrimstoneFluidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        float damage = Math.max(0, CCBConfig.server().fluids.contactDamage.getF());
        int burnDuration = Math.max(0, CCBConfig.server().fluids.burnDuration.get());
        switch (entity) {
            case LivingEntity living -> {
                if (living.fireImmune() || living.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                    if (damage > 0) {
                        living.hurt(CCBDamageSources.brimstone(level), damage);
                    }
                }
                else {
                    if (burnDuration > 0) {
                        living.igniteForSeconds(burnDuration);
                    }
                    if (damage > 0) {
                        living.hurt(CCBDamageSources.brimstoneFire(level), damage);
                    }
                }
            }
            case ItemEntity item when !item.fireImmune() && burnDuration > 0 -> item.igniteForSeconds(burnDuration);
            default -> {
            }
        }
    }
}
