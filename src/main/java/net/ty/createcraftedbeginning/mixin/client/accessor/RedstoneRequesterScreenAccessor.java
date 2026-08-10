package net.ty.createcraftedbeginning.mixin.client.accessor;

import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterScreen;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.platform.access.RedstoneRequesterScreenAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
@Mixin(RedstoneRequesterScreen.class)
public interface RedstoneRequesterScreenAccessor extends RedstoneRequesterScreenAccess {
    @Override
    @Accessor("amounts")
    List<Integer> ccb$getAmounts();
}
