package net.ty.createcraftedbeginning.api.gas.cannonhandlers;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface AirtightCannonHandler {
    SimpleRegistry<Gas, AirtightCannonHandler> REGISTRY = SimpleRegistry.create();

    ItemStack getRenderIcon(Level level);

    void renderTrailParticles(Level level, Vec3 pos);

    void explode(Level level, Vec3 pos, Entity source, float multiplier);

    ResourceLocation getTextureLocation();

    LayerDefinition getLayerDefinition();

    float[] getSetupAnim(float ageInTicks);

    float getGasConsumptionMultiplier();

    void appendHoverText(@NotNull ItemStack cannon, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag);

    default String getRenderStr(float n) {
        return String.format("%.2f", n).replaceAll("\\.?0+$", "");
    }
}