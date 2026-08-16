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

    void explode(Level level, Vec3 pos, AirtightCannonShotContext context);

    float getGasConsumptionMultiplier();

    void appendHoverText(ItemStack cannon, TooltipContext context, List<Component> tooltip, TooltipFlag flag);
}
