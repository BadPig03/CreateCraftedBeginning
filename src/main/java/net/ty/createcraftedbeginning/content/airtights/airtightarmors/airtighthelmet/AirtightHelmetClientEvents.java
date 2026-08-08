package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent.RenderFog;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.VisionUpgrade;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
public final class AirtightHelmetClientEvents {
    private AirtightHelmetClientEvents() {
    }

    @SubscribeEvent
    public static void onRenderFog(RenderFog event) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        Camera camera = event.getCamera();
        BlockPos pos = camera.getBlockPosition();
        FluidState fluid = level.getFluidState(pos);
        if (camera.getPosition().y >= pos.getY() + fluid.getHeight(level, pos)) {
            return;
        }

        FluidType fluidType = fluid.getType().getFluidType();
        if (fluidType == Fluids.EMPTY.getFluidType()) {
            return;
        }

        Entity entity = camera.getEntity();
        if (!(entity instanceof Player player) || player.isSpectator() || !VisionUpgrade.INSTANCE.canApply(player)) {
            return;
        }

        event.setNearPlaneDistance(-8);
        event.setFarPlaneDistance(128);
        event.setCanceled(true);
    }
}
