package net.ty.createcraftedbeginning.mixin.common.accessor;

import com.simibubi.create.content.trains.entity.Carriage;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(Carriage.class)
public interface CarriageAccessor {
    @Accessor("serialisedEntity")
    CompoundTag ccb$getSerialisedEntity();
}
