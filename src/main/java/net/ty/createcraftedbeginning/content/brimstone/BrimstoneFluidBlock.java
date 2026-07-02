package net.ty.createcraftedbeginning.content.brimstone;

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
import net.ty.createcraftedbeginning.registry.CCBDamageSources;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BrimstoneFluidBlock extends LiquidBlock {
    public BrimstoneFluidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(BlockState blockState, Level level, BlockPos blockPos, Entity entity) {
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
            case ItemEntity itemEntity when !entity.fireImmune() -> itemEntity.igniteForSeconds(15);
            default -> {
            }
        }
    }
}
