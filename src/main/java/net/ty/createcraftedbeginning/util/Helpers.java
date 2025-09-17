package net.ty.createcraftedbeginning.util;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class Helpers {
    private static final double DISTRIBUTION_DEVIATION = 0.11485000171139836;

    public static Vec3 generateItemDropVelocity(RandomSource random) {
        return new Vec3(random.triangle(0.0, DISTRIBUTION_DEVIATION), random.triangle(0.2, DISTRIBUTION_DEVIATION), random.triangle(0.0, DISTRIBUTION_DEVIATION));
    }

    public static <T extends IBE<? extends BlockEntity>> int calculateRedstoneSignal(T ibe, Level level, BlockPos pos) {
        return ibe.getBlockEntityOptional(level, pos).map(be -> level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null)).map(Helpers::calculateRedstoneFromInventory).orElse(0);
    }

    private static int calculateRedstoneFromInventory(@Nullable IItemHandler inv) {
        if (inv == null) {
            return 0;
        }

        int i = 0;
        float f = 0f;
        int totalSlots = inv.getSlots();

        for (int j = 0; j < inv.getSlots(); ++j) {
            int slotLimit = inv.getSlotLimit(j);
            if (slotLimit == 0) {
                totalSlots--;
                continue;
            }
            ItemStack itemStack = inv.getStackInSlot(j);
            if (!itemStack.isEmpty()) {
                f += (float) itemStack.getCount() / (float) Math.min(slotLimit, itemStack.getOrDefault(DataComponents.MAX_STACK_SIZE, 64));
                ++i;
            }
        }

        if (totalSlots == 0) {
            return 0;
        }

        f /= totalSlots;
        return Mth.floor(f * 14f) + (i > 0 ? 1 : 0);
    }

    private static FluidStack getSourceFluidStack(FluidStack fluidStack) {
        Fluid fluid = fluidStack.getFluid();
        int amount = fluidStack.getAmount();
        if (fluid instanceof FlowingFluid flowingFluid) {
            return new FluidStack(flowingFluid.getSource(), amount);
        }
        return fluidStack;
    }

    public static boolean isFluidTheSame(FluidStack fluidStack1, FluidStack fluidStack2) {
        return FluidStack.isSameFluidSameComponents(getSourceFluidStack(fluidStack1), getSourceFluidStack(fluidStack2));
    }

    public static float getActualTickRate(@NotNull Level level) {
        if (level.isClientSide || level.getServer() == null) {
            return 20f;
        }
        return level.getServer().tickRateManager().tickrate();
    }
}