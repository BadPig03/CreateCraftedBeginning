package net.ty.createcraftedbeginning.content.airtights.airtightcannon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.CreateCraftedBeginningClient;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.api.gas.cansiters.GasCanisterSupplierUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.weatherflares.WeatherFlareSupplierUtils;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(value = Dist.CLIENT, modid = CreateCraftedBeginning.MOD_ID)
public class AirtightCannonItemRenderer extends CustomRenderedItemModelRenderer {
    @SubscribeEvent
    public static void register(@NotNull RegisterClientExtensionsEvent event) {
        event.registerItem(SimpleCustomRenderer.create(CCBItems.AIRTIGHT_CANNON.asItem(), new AirtightCannonItemRenderer()), CCBItems.AIRTIGHT_CANNON.asItem());
    }

    public static final IItemDecorator DECORATOR = (guiGraphics, font, stack, xOffset, yOffset) -> {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }

        ItemStack flareItem = WeatherFlareSupplierUtils.getFirstFlare(player);
        if (!flareItem.isEmpty()) {
            renderItem(guiGraphics, xOffset, yOffset, flareItem);
            return false;
        }

        if (GasCanisterSupplierUtils.noUsableGasAvailable(player)) {
            return false;
        }

        GasStack gasStack = GasCanisterSupplierUtils.getFirstNonEmptyGasContent(player);
        if (gasStack.isEmpty()) {
            return false;
        }

        AirtightCannonHandler cannonHandler = AirtightCannonHandler.REGISTRY.get(gasStack.getGas());
        if (cannonHandler == null) {
            return false;
        }

        renderItem(guiGraphics, xOffset, yOffset, cannonHandler.getRenderIcon(level));
        return false;
    };

    private static void renderItem(@NotNull GuiGraphics guiGraphics, int xOffset, int yOffset, ItemStack icon) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(xOffset, yOffset + 8, 100);
        poseStack.scale(0.5f, 0.5f, 0.5f);
        guiGraphics.renderItem(icon, 0, 0);
        poseStack.popPose();
    }

    @Override
    protected void render(ItemStack cannon, @NotNull CustomRenderedItemModel model, @NotNull PartialItemModelRenderer renderer, ItemDisplayContext transformType, @NotNull PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        renderer.render(model.getOriginalModel(), light);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        boolean inMainHand = player.getMainHandItem() == cannon;
        boolean inOffHand = player.getOffhandItem() == cannon;
        if (!inMainHand && !inOffHand) {
            renderer.render(CCBPartialModels.AIRTIGHT_CANNON_BARREL.get(), light);
            renderer.render(CCBPartialModels.AIRTIGHT_CANNON_PISTON_LEFT.get(), light);
            renderer.render(CCBPartialModels.AIRTIGHT_CANNON_PISTON_RIGHT.get(), light);
            return;
        }

        float partialTicks = AnimationTickHolder.getPartialTicks();
        boolean isUsing = player.getUseItem() == cannon;
        int useTime = isUsing ? cannon.getUseDuration(player) - player.getUseItemRemainingTicks() : 0;
        float ratio = useTime + (isUsing ? partialTicks: 0);
        int efficientUseTime = AirtightCannonUtils.getEfficientUseTime(cannon);
        float barrelOffset = Mth.clamp(ratio / efficientUseTime, 0, 2) / 10.0f;

        ms.pushPose();
        ms.translate(0, 0, barrelOffset);
        renderer.render(CCBPartialModels.AIRTIGHT_CANNON_BARREL.get(), light);
        ms.popPose();

        boolean leftHanded = player.getMainArm() == HumanoidArm.LEFT;
        float animation = CreateCraftedBeginningClient.AIRTIGHT_CANNON_RENDER_HANDLER.getAnimation(inMainHand ^ leftHanded, partialTicks);
        float pistonOffset = Mth.clamp(animation * 2, 0, 1) / 8.0f;

        ms.pushPose();
        ms.translate(pistonOffset, 0, 0);
        renderer.render(CCBPartialModels.AIRTIGHT_CANNON_PISTON_LEFT.get(), light);
        ms.popPose();

        ms.pushPose();
        ms.translate(-pistonOffset, 0, 0);
        renderer.render(CCBPartialModels.AIRTIGHT_CANNON_PISTON_RIGHT.get(), light);
        ms.popPose();
    }
}
