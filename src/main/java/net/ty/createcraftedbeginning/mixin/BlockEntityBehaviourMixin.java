package net.ty.createcraftedbeginning.mixin;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockEntityBehaviour.class)
public abstract class BlockEntityBehaviourMixin {
    @Shadow
    public abstract Level getWorld();

    @Shadow
    public abstract BlockPos getPos();
}
