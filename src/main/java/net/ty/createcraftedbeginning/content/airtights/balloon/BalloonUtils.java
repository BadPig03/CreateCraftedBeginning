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
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
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

    public static boolean isBalloonPackage(PackageEntity entity) {
        return isBalloon(entity.getBox());
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
        return CCBConfig.server().airtights.maxGasPerBalloon.get() * GasAmountUtils.MILLIBUCKETS_PER_BUCKET;
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

    private static BalloonGasContents fitToBalloon(BalloonGasContents contents) {
        return contents.normalized().limitedTo(Math.max(0, getCapacity()), BalloonGasContents.MAX_GAS_TYPES);
    }

    public static int getDisplayColor(BalloonGasContents contents) {
        if (contents.isEmpty()) {
            return 0xFFFFFF;
        }

        long total = contents.totalAmount();
        if (total <= 0) {
            return 0xFFFFFF;
        }

        double red = 0;
        double green = 0;
        double blue = 0;
        for (GasEntry gas : contents.gases()) {
            int tint = gas.getGasType().getTint();
            double weight = gas.getAmount() / (double) total;
            red += (tint >> 16 & 0xFF) * weight;
            green += (tint >> 8 & 0xFF) * weight;
            blue += (tint & 0xFF) * weight;
        }

        int redChannel = Mth.clamp((int) Math.round(red), 0, 255);
        int greenChannel = Mth.clamp((int) Math.round(green), 0, 255);
        int blueChannel = Mth.clamp((int) Math.round(blue), 0, 255);
        return redChannel << 16 | greenChannel << 8 | blueChannel;
    }

    public static void tickInWater(PackageEntity entity) {
        if (!isBalloonPackage(entity) || entity.isPassenger()) {
            return;
        }

        double waterHeight = entity.getFluidTypeHeight(Fluids.WATER.getFluidType());
        if (waterHeight <= 0) {
            return;
        }

        BalloonGasContents contents = getGasContents(entity.getBox());
        long capacity = getCapacity();
        double fillRatio = capacity <= 0 ? 0 : Mth.clamp(contents.totalAmount() / (double) capacity, 0, 1);
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x * 0.85, motion.y + 0.003 + 0.007 * Math.sqrt(fillRatio), motion.z * 0.85);
        entity.setOnGround(false);
        entity.hasImpulse = true;
    }

    public static void renderGasEffects(PackageEntity entity) {
        if (!entity.level().isClientSide) {
            return;
        }

        if (!containsGasContents(entity.getBox())) {
            return;
        }

        if (entity.getDeltaMovement().lengthSqr() < 1.0E-4 || (entity.tickCount & 1) != 0) {
            return;
        }

        List<GasEntry> gases = getGasContents(entity.getBox()).gases();
        if (gases.isEmpty()) {
            return;
        }

        int index = Math.floorMod(entity.tickCount / 2 + entity.getId(), gases.size());
        AirtightCannonVisualHandlerUtils.of(gases.get(index).getGasType()).renderTrailParticles(entity.level(), entity.position().add(0, 0.25, 0));
    }

    public static void windBurst(PackageEntity entity) {
        BalloonGasContents contents = getGasContents(entity.getBox());
        if (contents.isEmpty()) {
            return;
        }

        long total = contents.totalAmount();
        long capacity = getCapacity();
        if (total <= 0 || capacity <= 0) {
            return;
        }

        float burstMultiplier = Mth.clamp((float) total / capacity, 0, 1) * 2;
        Level level = entity.level();
        Vec3 position = entity.position();
        for (GasEntry gas : contents.gases()) {
            float gasMultiplier = burstMultiplier * ((float) gas.getAmount() / total);
            if (gasMultiplier <= 0) {
                continue;
            }

            AirtightCannonHandler handler = AirtightCannonHandlerUtils.of(gas.getGasType());
            handler.explode(level, position, AirtightCannonShotContext.external(entity, gas.getGasHolder(), gasMultiplier));
        }
    }
}
