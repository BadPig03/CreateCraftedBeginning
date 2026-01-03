package net.ty.createcraftedbeginning.content.fanprocessing;

import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.foundation.recipe.RecipeApplier;
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
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChillingFanProcessingType implements FanProcessingType {
    private static final int COLOR = 0xEBF6FF;

    @Override
    public boolean isValidAt(@NotNull Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BreezeCoolerBlockEntity cooler && cooler.getFrostLevel().isAtLeast(FrostLevel.CHILLED);
    }

    @Override
    public int getPriority() {
        return 550;
    }

    @Override
    public boolean canProcess(ItemStack stack, @NotNull Level level) {
        return level.getRecipeManager().getRecipeFor(CCBRecipeTypes.CHILLING.getType(), new SingleRecipeInput(stack), level).isPresent();
    }

    @Override
    public @Nullable List<ItemStack> process(ItemStack stack, @NotNull Level level) {
        return level.getRecipeManager().getRecipeFor(CCBRecipeTypes.CHILLING.getType(), new SingleRecipeInput(stack), level).map(recipe -> RecipeApplier.applyRecipeOn(level, stack, recipe.value(), true)).orElse(null);
    }

    @Override
    public void spawnProcessingParticles(@NotNull Level level, Vec3 pos) {
        if (level.random.nextInt(8) != 0) {
            return;
        }

        level.addParticle(ParticleTypes.SNOWFLAKE, pos.x + (level.random.nextFloat() - 0.5f) * 0.5f, pos.y + 0.5f, pos.z + (level.random.nextFloat() - 0.5f) * 0.5f, 0, 0.125f, 0);
    }

    @Override
    public void morphAirFlow(@NotNull AirFlowParticleAccess particleAccess, @NotNull RandomSource random) {
        particleAccess.setColor(COLOR);
        particleAccess.setAlpha(1);
        if (random.nextFloat() >= 0.03125f) {
            return;
        }

        particleAccess.spawnExtraParticle(ParticleTypes.SNOWFLAKE, 0.125f);
    }

    @Override
    public void affectEntity(Entity entity, @NotNull Level level) {
        if (level.isClientSide) {
            return;
        }

        if (entity.canFreeze()) {
            entity.setTicksFrozen(Math.min(entity.getTicksRequiredToFreeze(), entity.getTicksFrozen()) + 7);
        }
        entity.extinguishFire();
    }
}
