package net.ty.createcraftedbeginning.content.brimstone;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.ty.createcraftedbeginning.registry.CCBDamageSources;
import org.jetbrains.annotations.NotNull;

public class BrimstoneFluidBlock extends LiquidBlock {
    public BrimstoneFluidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(@NotNull BlockState blockState, @NotNull Level level, @NotNull BlockPos blockPos, @NotNull Entity entity) {
        switch (entity) {
            case LivingEntity livingEntity -> {
                if (livingEntity.fireImmune() || livingEntity.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                    livingEntity.hurt(CCBDamageSources.brimstone(level), 10);
                }
                else {
                    livingEntity.igniteForSeconds(15);
                    livingEntity.hurt(CCBDamageSources.brimstoneFire(level), 10);
                }
            }
            case ItemEntity itemEntity when !entity.fireImmune() -> {
                itemEntity.igniteForSeconds(15);
                itemEntity.hurt(CCBDamageSources.brimstoneFire(itemEntity.level()), 10);
            }
            default -> {
            }
        }
    }
}
