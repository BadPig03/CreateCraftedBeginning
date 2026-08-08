package net.ty.createcraftedbeginning.content.fanprocessing;

import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import net.ty.createcraftedbeginning.recipe.CCBRecipeTypes;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ChillingFanProcessingType implements FanProcessingType {
    @Override
    public boolean isValidAt(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BreezeCoolerBlockEntity cooler && cooler.getFrostLevel().isAtLeast(FrostLevel.CHILLED);
    }

    @Override
    public int getPriority() {
        return 550;
    }

    @Override
    public boolean canProcess(ItemStack stack, Level level) {
        return level.getRecipeManager().getRecipeFor(CCBRecipeTypes.CHILLING.getType(), new SingleRecipeInput(stack), level).isPresent();
    }

    @Override
    public @Nullable List<ItemStack> process(ItemStack stack, Level level) {
        return level.getRecipeManager().getRecipeFor(CCBRecipeTypes.CHILLING.getType(), new SingleRecipeInput(stack), level).map(recipe -> RecipeApplier.applyRecipeOn(level, stack, recipe.value(), false)).orElse(null);
    }

    @Override
    public void spawnProcessingParticles(Level level, Vec3 pos) {
        if (level.random.nextInt(8) != 0) {
            return;
        }

        level.addParticle(ParticleTypes.SNOWFLAKE, pos.x + (level.random.nextFloat() - 0.5) * 0.5, pos.y + 0.5, pos.z + (level.random.nextFloat() - 0.5) * 0.5, 0, 0.125, 0);
    }

    @Override
    public void morphAirFlow(AirFlowParticleAccess particleAccess, RandomSource random) {
        particleAccess.setColor(0xEBF6FF);
        particleAccess.setAlpha(1);
        if (random.nextDouble() >= 0.03125) {
            return;
        }

        particleAccess.spawnExtraParticle(ParticleTypes.SNOWFLAKE, 0.125f);
    }

    @Override
    public void affectEntity(Entity entity, Level level) {
        if (level.isClientSide) {
            return;
        }

        if (entity.canFreeze()) {
            int ticks = Math.min(entity.getTicksRequiredToFreeze(), entity.getTicksFrozen() + 7);
            entity.setTicksFrozen(ticks);
        }
        entity.extinguishFire();
    }
}
