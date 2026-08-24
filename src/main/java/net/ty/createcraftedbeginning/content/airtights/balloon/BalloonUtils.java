package net.ty.createcraftedbeginning.content.airtights.balloon;

import com.simibubi.create.content.logistics.box.PackageEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandlerUtils;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonShotContext;
import net.ty.createcraftedbeginning.api.cannonhandlers.visual.AirtightCannonVisualHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonGasContents.GasEntry;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BalloonUtils {
    private BalloonUtils() {
    }

    public static boolean containsGasContents(ItemStack stack) {
        return !getGasContents(stack).isEmpty();
    }

    public static boolean isBalloon(ItemStack stack) {
        return stack.getItem() instanceof BalloonItem;
    }

    public static boolean isBalloonPackage(PackageEntity balloon) {
        return isBalloon(balloon.getBox());
    }

    public static boolean isInWater(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER) || state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED);
    }

    public static BalloonGasContents getGasContents(ItemStack stack) {
        if (!isBalloon(stack)) {
            return BalloonGasContents.EMPTY;
        }

        BalloonGasContents contents = stack.getOrDefault(CCBDataComponents.BALLOON_GAS_CONTENTS, BalloonGasContents.EMPTY);
        return contents.isEmpty() ? BalloonGasContents.EMPTY : contents;
    }

    public static ItemStack containing(BalloonGasContents contents) {
        BalloonGasContents fitted = fitToBalloon(contents);
        if (fitted.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack balloon = BalloonStyleUtils.getRandomBalloon();
        setGasContents(balloon, fitted);
        return balloon;
    }

    public static ItemStack containingLike(ItemStack template, BalloonGasContents contents) {
        BalloonGasContents fitted = fitToBalloon(contents);
        if (fitted.isEmpty() || !isBalloon(template)) {
            return ItemStack.EMPTY;
        }

        ItemStack balloon = new ItemStack(template.getItem());
        setGasContents(balloon, fitted);
        return balloon;
    }

    public static long getCapacity() {
        return CCBConfig.server().airtights.maxGasPerBalloon.get() * GasAmounts.MILLIBUCKETS_PER_BUCKET;
    }

    public static boolean fitsInBalloon(BalloonGasContents contents) {
        long capacity = getCapacity();
        return capacity > 0 && contents.gasTypeCount() <= BalloonGasContents.MAX_GAS_TYPES && contents.totalAmount() <= capacity;
    }

    public static void setGasContents(ItemStack stack, BalloonGasContents contents) {
        if (!isBalloon(stack)) {
            return;
        }

        BalloonGasContents fitted = fitToBalloon(contents);
        if (fitted.isEmpty()) {
            stack.remove(CCBDataComponents.BALLOON_GAS_CONTENTS);
            return;
        }

        stack.set(CCBDataComponents.BALLOON_GAS_CONTENTS, fitted);
    }

    public static void tickInWater(PackageEntity balloon) {
        if (!isBalloonPackage(balloon) || balloon.isPassenger()) {
            return;
        }

        double waterHeight = balloon.getFluidTypeHeight(Fluids.WATER.getFluidType());
        if (waterHeight <= 0) {
            return;
        }

        BalloonGasContents contents = getGasContents(balloon.getBox());
        long capacity = getCapacity();
        double fillRatio = capacity <= 0 ? 0 : Mth.clamp(contents.totalAmount() / (double) capacity, 0, 1);
        Vec3 currentMovement = balloon.getDeltaMovement();
        balloon.setDeltaMovement(currentMovement.x * 0.85, currentMovement.y + 0.003 + 0.007 * Math.sqrt(fillRatio), currentMovement.z * 0.85);
        balloon.setOnGround(false);
        balloon.hasImpulse = true;
    }

    public static void renderGasEffects(PackageEntity balloon) {
        if (!balloon.level().isClientSide) {
            return;
        }

        if (!containsGasContents(balloon.getBox())) {
            return;
        }

        if (balloon.getDeltaMovement().lengthSqr() < 1.0E-4 || (balloon.tickCount & 1) != 0) {
            return;
        }

        List<GasEntry> gases = getGasContents(balloon.getBox()).gases();
        if (gases.isEmpty()) {
            return;
        }

        int gasIndex = Math.floorMod(balloon.tickCount / 2 + balloon.getId(), gases.size());
        AirtightCannonVisualHandlerUtils.of(gases.get(gasIndex).getGasType()).renderTrailParticles(balloon.level(), balloon.position().add(0, 0.25, 0));
    }

    public static void windBurst(PackageEntity balloon) {
        BalloonGasContents contents = getGasContents(balloon.getBox());
        if (contents.isEmpty()) {
            return;
        }

        long totalAmount = contents.totalAmount();
        long capacity = getCapacity();
        if (totalAmount <= 0 || capacity <= 0) {
            return;
        }

        float burstMultiplier = Mth.clamp((float) totalAmount / capacity, 0, 1) * 2;
        Level level = balloon.level();
        Vec3 burstPosition = balloon.position();
        for (GasEntry gas : contents.gases()) {
            float gasBurstMultiplier = burstMultiplier * ((float) gas.getAmount() / totalAmount);
            if (gasBurstMultiplier <= 0) {
                continue;
            }

            AirtightCannonHandler gasHandler = AirtightCannonHandlerUtils.of(gas.getGasType());
            gasHandler.explode(level, burstPosition, AirtightCannonShotContext.external(balloon, gas.getGasHolder(), gasBurstMultiplier));
        }
    }

    private static BalloonGasContents fitToBalloon(BalloonGasContents contents) {
        return contents.normalized().limitedTo(Math.max(0, getCapacity()));
    }
}
