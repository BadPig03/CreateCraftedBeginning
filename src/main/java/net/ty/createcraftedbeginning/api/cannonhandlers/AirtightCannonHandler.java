package net.ty.createcraftedbeginning.api.cannonhandlers;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AirtightCannonHandler {
    SimpleRegistry<Gas, AirtightCannonHandler> REGISTRY = SimpleRegistry.create();

    /**
     * Applies this handler's explosion behavior at the supplied position.
     *
     * @param level   the level in which the operation is performed
     * @param pos     the target block position
     * @param context the context for the operation
     */
    void explode(Level level, Vec3 pos, AirtightCannonShotContext context);

    /**
     * Returns the gas consumption multiplier.
     *
     * @return the gas consumption multiplier
     */
    float getGasConsumptionMultiplier();

    /**
     * Appends this object's contextual information to the supplied tooltip.
     *
     * @param cannon  the airtight cannon item stack
     * @param context the context for the operation
     * @param tooltip the tooltip entries to append to
     * @param flag    the tooltip display flags
     */
    void appendHoverText(ItemStack cannon, TooltipContext context, List<Component> tooltip, TooltipFlag flag);
}
