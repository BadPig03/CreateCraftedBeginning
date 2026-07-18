package net.ty.createcraftedbeginning.api.gas.gases.interfaces;

import com.simibubi.create.api.packager.InventoryIdentifier;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IGasInventoryIdentifierProvider {
    /**
     * Returns the gas inventory identifier.
     *
     * @param direction the direction associated with the operation
     * @return the gas inventory identifier
     */
    @Nullable InventoryIdentifier getGasInventoryIdentifier(Direction direction);
}
