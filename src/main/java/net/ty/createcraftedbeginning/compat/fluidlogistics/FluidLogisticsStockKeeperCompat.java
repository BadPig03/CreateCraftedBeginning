package net.ty.createcraftedbeginning.compat.fluidlogistics;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.compat.CCBCompatMods;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class FluidLogisticsStockKeeperCompat {
    private static final String PACKAGE_RESOURCE_TYPES = "com.yision.fluidlogistics.api.packager.PackageResourceTypes";
    private static final @Nullable Class<?> RESOURCE_TYPES_CLASS = findResourceTypesClass();
    private static final @Nullable Method CREATE_FLUID_KEY = findMethod("createFluidKey", FluidStack.class);
    private static final @Nullable Method GET_FLUID_PER_PACKAGE = findMethod("getFluidPerPackage");

    private FluidLogisticsStockKeeperCompat() {
    }

    public static boolean isAvailable() {
        return CREATE_FLUID_KEY != null;
    }

    public static ItemStack createFluidKey(FluidStack fluid) {
        if (fluid.isEmpty() || CREATE_FLUID_KEY == null) {
            return ItemStack.EMPTY;
        }

        try {
            Object value = CREATE_FLUID_KEY.invoke(null, fluid);
            if (!(value instanceof ItemStack stack) || stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            return stack.copyWithCount(1);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            CCBAPI.LOGGER.warn("Failed to create a CreateFluidLogistic stock-keeper fluid key", exception);
            return ItemStack.EMPTY;
        }
    }

    public static int getFluidPerPackage(int fallback) {
        if (GET_FLUID_PER_PACKAGE == null) {
            return Math.max(1, fallback);
        }

        try {
            Object value = GET_FLUID_PER_PACKAGE.invoke(null);
            if (value instanceof Number number) {
                return Math.max(1, number.intValue());
            }
        } catch (IllegalAccessException | InvocationTargetException exception) {
            CCBAPI.LOGGER.warn("Failed to query CreateFluidLogistic fluid package capacity", exception);
        }
        return Math.max(1, fallback);
    }

    private static @Nullable Class<?> findResourceTypesClass() {
        if (!CCBCompatMods.CREATE_FLUID_LOGISTICS.isLoaded()) {
            return null;
        }

        try {
            return Class.forName(PACKAGE_RESOURCE_TYPES, false, FluidLogisticsStockKeeperCompat.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError exception) {
            CCBAPI.LOGGER.warn("CreateFluidLogistic is installed, but its package-resource API is unavailable; mixed gas/fluid stock-keeper transfer will not include fluids", exception);
            return null;
        }
    }

    private static @Nullable Method findMethod(String name, Class<?>... parameterTypes) {
        if (RESOURCE_TYPES_CLASS == null) {
            return null;
        }

        try {
            return RESOURCE_TYPES_CLASS.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException | SecurityException exception) {
            CCBAPI.LOGGER.warn("CreateFluidLogistic package-resource API method '{}' is unavailable", name, exception);
            return null;
        }
    }
}
