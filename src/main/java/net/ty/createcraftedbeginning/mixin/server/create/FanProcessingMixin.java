package net.ty.createcraftedbeginning.mixin.server.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessing;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = FanProcessing.class, remap = false)
public abstract class FanProcessingMixin {
    @WrapOperation(method = "canProcess(Lnet/minecraft/world/entity/item/ItemEntity;Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;)Z", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;canProcess(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;)Z"))
    private static boolean ccb$canProcess(FanProcessingType type, ItemStack stack, Level level, Operation<Boolean> original) {
        boolean canProcess = original.call(type, stack, level);
        return canProcess || GasInjectionChamberUtils.isFilter(stack);
    }

    @WrapOperation(method = "applyProcessing(Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;Lnet/minecraft/world/level/Level;Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;)Lcom/simibubi/create/content/kinetics/belt/behaviour/TransportedItemStackHandlerBehaviour$TransportedResult;", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;canProcess(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;)Z"))
    private static boolean ccb$applyProcessing(FanProcessingType type, ItemStack stack, Level level, Operation<Boolean> original) {
        boolean canProcess = original.call(type, stack, level);
        return canProcess || GasInjectionChamberUtils.isFilter(stack);
    }

    @WrapOperation(method = "applyProcessing(Lnet/minecraft/world/entity/item/ItemEntity;Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;)Z", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;process(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;)Ljava/util/List;"))
    @Nullable
    private static List<ItemStack> ccb$processFilterEntity(FanProcessingType type, ItemStack stack, Level level, Operation<List<ItemStack>> original) {
        return ccb$processFilter(type, stack, level, original);
    }

    @WrapOperation(method = "applyProcessing(Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;Lnet/minecraft/world/level/Level;Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;)Lcom/simibubi/create/content/kinetics/belt/behaviour/TransportedItemStackHandlerBehaviour$TransportedResult;", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;process(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;)Ljava/util/List;"))
    @Nullable
    private static List<ItemStack> ccb$processFilterOnBelt(FanProcessingType type, ItemStack stack, Level level, Operation<List<ItemStack>> original) {
        return ccb$processFilter(type, stack, level, original);
    }

    @Nullable
    private static List<ItemStack> ccb$processFilter(FanProcessingType type, ItemStack stack, Level level, Operation<List<ItemStack>> original) {
        if (!GasInjectionChamberUtils.isFilter(stack)) {
            return original.call(type, stack, level);
        }

        List<ItemStack> results = new ArrayList<>(1);
        results.add(GasInjectionChamberUtils.create(stack, type));
        return results;
    }
}
