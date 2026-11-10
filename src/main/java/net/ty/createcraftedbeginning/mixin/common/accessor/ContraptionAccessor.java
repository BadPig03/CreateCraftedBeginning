package net.ty.createcraftedbeginning.mixin.common.accessor;

import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(Contraption.class)
public interface ContraptionAccessor {
    @Accessor("updateTags")
    Map<BlockPos, CompoundTag> ccb$getUpdateTags();
}
