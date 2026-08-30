package net.ty.createcraftedbeginning.content.airtights.airtightcannon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.cannonhandlers.visual.AirtightCannonVisualHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.weatherflares.WeatherFlareSupplierUtils;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClients;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(value = Dist.CLIENT, modid = CCBAPI.MOD_ID)
public class AirtightCannonItemRenderer extends CustomRenderedItemModelRenderer {
    private static long cachedGameTime = Long.MIN_VALUE;
    private static WeakReference<LocalPlayer> cachedPlayer = new WeakReference<>(null);
    private static WeakReference<ClientLevel> cachedLevel = new WeakReference<>(null);
    private static ItemStack cachedDecoratorIcon = ItemStack.EMPTY;

    public static final IItemDecorator DECORATOR = (guiGraphics, font, stack, xOffset, yOffset) -> {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return false;
        }

        ClientLevel level = minecraft.level;
        if (level == null) {
            return false;
        }

        ItemStack decoratorIcon = getDecoratorIcon(player, level);
        if (!decoratorIcon.isEmpty()) {
            renderItem(guiGraphics, xOffset, yOffset, decoratorIcon);
        }
        return false;
    };

    private AirtightCannonItemRenderer() {
    }

    @SubscribeEvent
    private static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(SimpleCustomRenderer.create(CCBItems.AIRTIGHT_CANNON.asItem(), new AirtightCannonItemRenderer()), CCBItems.AIRTIGHT_CANNON.asItem());
    }

    private static ItemStack getDecoratorIcon(LocalPlayer player, ClientLevel level) {
        long gameTime = level.getGameTime();
        if (cachedGameTime == gameTime && cachedPlayer.get() == player && cachedLevel.get() == level) {
            return cachedDecoratorIcon;
        }

        cachedGameTime = gameTime;
        if (cachedPlayer.get() != player) {
            cachedPlayer = new WeakReference<>(player);
        }
        if (cachedLevel.get() != level) {
            cachedLevel = new WeakReference<>(level);
        }

        ItemStack flareStack = WeatherFlareSupplierUtils.getFirstFlare(player);
        if (!flareStack.isEmpty()) {
            cachedDecoratorIcon = flareStack;
            return cachedDecoratorIcon;
        }

        GasStack gasContent = CanisterContainerClients.getDisplayedGasContent();
        if (gasContent.isEmpty()) {
            cachedDecoratorIcon = ItemStack.EMPTY;
            return cachedDecoratorIcon;
        }

        cachedDecoratorIcon = AirtightCannonVisualHandlerUtils.of(gasContent.getGasType()).getRenderIcon(level);
        return cachedDecoratorIcon;
    }

    private static void renderItem(GuiGraphics guiGraphics, int xOffset, int yOffset, ItemStack decoratorIcon) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(xOffset, yOffset + 8, 100);
        poseStack.scale(0.5f, 0.5f, 0.5f);
        guiGraphics.renderItem(decoratorIcon, 0, 0);
        poseStack.popPose();
    }

    @Override
    protected void render(ItemStack cannon, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ItemDisplayContext transformType, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        renderer.render(model.getOriginalModel(), light);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        boolean isInMainHand = player.getMainHandItem() == cannon;
        boolean isInOffHand = player.getOffhandItem() == cannon;
        if (!isInMainHand && !isInOffHand) {
            renderer.render(CCBPartialModels.AIRTIGHT_CANNON_BARREL.get(), light);
            renderer.render(CCBPartialModels.AIRTIGHT_CANNON_PISTON_LEFT.get(), light);
            renderer.render(CCBPartialModels.AIRTIGHT_CANNON_PISTON_RIGHT.get(), light);
            return;
        }

        float partialTick = AnimationTickHolder.getPartialTicks();
        boolean isUsing = player.getUseItem() == cannon;
        int useTime = isUsing ? cannon.getUseDuration(player) - player.getUseItemRemainingTicks() : 0;
        float chargeTime = useTime + (isUsing ? partialTick : 0);
        float barrelOffset = CCBMathUtils.clampNonNegative(chargeTime / AirtightCannonUtils.getEfficientUseTime(cannon), 2) / 10;

        poseStack.pushPose();
        poseStack.translate(0, 0, barrelOffset);
        renderer.render(CCBPartialModels.AIRTIGHT_CANNON_BARREL.get(), light);
        poseStack.popPose();

        boolean isLeftHanded = player.getMainArm() == HumanoidArm.LEFT;
        float pistonAnimation = AirtightCannonRenderHandler.INSTANCE.getAnimation(isInMainHand ^ isLeftHanded, partialTick);
        float pistonOffset = CCBMathUtils.clampUnit(pistonAnimation * 2) / 8;

        poseStack.pushPose();
        poseStack.translate(pistonOffset, 0, 0);
        renderer.render(CCBPartialModels.AIRTIGHT_CANNON_PISTON_LEFT.get(), light);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(-pistonOffset, 0, 0);
        renderer.render(CCBPartialModels.AIRTIGHT_CANNON_PISTON_RIGHT.get(), light);
        poseStack.popPose();
    }
}
