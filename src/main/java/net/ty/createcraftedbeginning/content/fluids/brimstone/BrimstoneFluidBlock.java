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
        switch (entity) {
            case LivingEntity living -> {
                if (living.fireImmune() || living.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                    living.hurt(CCBDamageSources.brimstone(level), 10);
                }
                else {
                    living.igniteForSeconds(15);
                    living.hurt(CCBDamageSources.brimstoneFire(level), 10);
                }
            }
            case ItemEntity item when !item.fireImmune() -> item.igniteForSeconds(15);
            default -> {
            }
        }
    }
}
