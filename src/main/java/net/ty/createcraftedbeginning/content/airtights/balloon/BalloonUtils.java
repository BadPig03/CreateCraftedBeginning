package net.ty.createcraftedbeginning.content.airtights.balloon;

import com.simibubi.create.content.logistics.box.PackageEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BalloonUtils {
    private BalloonUtils() {
    }

    public static boolean containsGasContents(ItemStack stack) {
        return isBalloon(stack) && !stack.getOrDefault(CCBDataComponents.BALLOON_GAS_CONTENT, GasStack.EMPTY).isEmpty();
    }

    public static boolean isBalloon(ItemStack stack) {
        return stack.getItem() instanceof BalloonItem;
    }

    public static boolean isBalloonPackage(PackageEntity entity) {
        return isBalloon(entity.getBox());
    }

    public static boolean isInWater(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER) || state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED);
    }

    public static GasStack getGasContents(ItemStack stack) {
        GasStack gas = stack.getOrDefault(CCBDataComponents.BALLOON_GAS_CONTENT, GasStack.EMPTY);
        if (gas.isEmpty()) {
            return GasStack.EMPTY;
        }
        return gas.copy();
    }

    public static ItemStack containing(GasStack gas) {
        if (gas.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack balloon = BalloonStyleUtils.getRandomBalloon();
        setGasContents(balloon, gas);
        return balloon;
    }

    public static long getCapacity() {
        return CCBConfig.server().airtights.maxGasPerBalloon.get();
    }

    public static void setGasContents(ItemStack stack, GasStack gas) {
        if (!isBalloon(stack)) {
            return;
        }

        if (gas.isEmpty()) {
            stack.remove(CCBDataComponents.BALLOON_GAS_CONTENT);
            return;
        }

        stack.set(CCBDataComponents.BALLOON_GAS_CONTENT, gas.copy());
    }

    public static void tickInWater(PackageEntity entity) {
        if (!isBalloonPackage(entity) || entity.isPassenger()) {
            return;
        }

        double waterHeight = entity.getFluidTypeHeight(Fluids.WATER.getFluidType());
        if (waterHeight <= 0) {
            return;
        }

        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x * 0.85, motion.y + 0.01, motion.z * 0.85);
        entity.setOnGround(false);
        entity.hasImpulse = true;
    }

    public static void windBurst(PackageEntity entity) {
        if (!containsGasContents(entity.getBox())) {
            return;
        }

        ItemStack box = entity.getBox();
        GasStack gas = getGasContents(box);
        if (gas.isEmpty()) {
            return;
        }

        float multiplier = Mth.clamp(Mth.sqrt((float) gas.getAmount() / getCapacity()), 0.5f, 2);
        AirtightCannonHandler cannonHandler = AirtightCannonHandlerUtils.of(gas.getGasType());
        cannonHandler.explode(entity.level(), entity.position(), entity, multiplier);
    }
}
