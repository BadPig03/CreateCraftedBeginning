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
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BalloonUtils {
    private BalloonUtils() {
    }

    public static boolean containsGasContents(ItemStack stack) {
        return isBalloon(stack) && !stack.getOrDefault(CCBDataComponents.BALLOON_GAS_CONTENTS, BalloonGasContents.EMPTY).isEmpty();
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
        return contents.isEmpty() ? BalloonGasContents.EMPTY : contents.copy();
    }

    public static ItemStack containing(BalloonGasContents contents) {
        BalloonGasContents normalized = contents.normalized();
        if (normalized.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack balloon = BalloonStyleUtils.getRandomBalloon();
        setGasContents(balloon, normalized);
        return balloon;
    }

    public static ItemStack containingLike(ItemStack template, BalloonGasContents contents) {
        BalloonGasContents normalized = contents.normalized();
        if (normalized.isEmpty() || !isBalloon(template)) {
            return ItemStack.EMPTY;
        }

        ItemStack balloon = new ItemStack(template.getItem());
        setGasContents(balloon, normalized);
        return balloon;
    }

    public static long getCapacity() {
        return CCBConfig.server().airtights.maxGasPerBalloon.get();
    }

    public static void setGasContents(ItemStack stack, BalloonGasContents contents) {
        if (!isBalloon(stack)) {
            return;
        }

        BalloonGasContents normalized = contents.normalized();
        if (normalized.isEmpty()) {
            stack.remove(CCBDataComponents.BALLOON_GAS_CONTENTS);
            return;
        }

        stack.set(CCBDataComponents.BALLOON_GAS_CONTENTS, normalized.copy());
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
        for (GasStack gas : contents.gases()) {
            int tint = gas.getGasType().getTint();
            double weight = gas.getAmount() / (double) total;
            red += (tint >> 16 & 0xFF) * weight;
            green += (tint >> 8 & 0xFF) * weight;
            blue += (tint & 0xFF) * weight;
        }
        return Mth.clamp((int) Math.round(red), 0, 255) << 16 | Mth.clamp((int) Math.round(green), 0, 255) << 8 | Mth.clamp((int) Math.round(blue), 0, 255);
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
        if (!entity.level().isClientSide || !containsGasContents(entity.getBox()) || entity.getDeltaMovement().lengthSqr() < 1.0E-4 || (entity.tickCount & 1) != 0) {
            return;
        }

        List<GasStack> gases = getGasContents(entity.getBox()).gases();
        if (gases.isEmpty()) {
            return;
        }

        int index = Math.floorMod(entity.tickCount / 2 + entity.getId(), gases.size());
        AirtightCannonHandlerUtils.of(gases.get(index).getGasType()).renderTrailParticles(entity.level(), entity.position().add(0, 0.25, 0));
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

        float totalMultiplier = Mth.clamp(Mth.sqrt((float) total / capacity), 0.5f, 2);
        for (GasStack gas : contents.gases()) {
            float share = (float) (gas.getAmount() / (double) total);
            float multiplier = totalMultiplier * share;
            if (multiplier <= 0) {
                continue;
            }

            AirtightCannonHandler cannonHandler = AirtightCannonHandlerUtils.of(gas.getGasType());
            cannonHandler.explode(entity.level(), entity.position(), entity, multiplier);
        }
    }
}
