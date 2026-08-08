package net.ty.createcraftedbeginning.mixin.client.accessor;

import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterScreen;
import net.ty.createcraftedbeginning.platform.access.RedstoneRequesterScreenAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@FunctionalInterface
@Mixin(RedstoneRequesterScreen.class)
public interface RedstoneRequesterScreenAccessor extends RedstoneRequesterScreenAccess {
    @Override
    @Accessor("amounts")
    List<Integer> ccb$getAmounts();
}
