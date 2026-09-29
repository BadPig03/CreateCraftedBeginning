package net.ty.createcraftedbeginning.content.airtights.gascanister;

import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClients;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClients.DisplayedGasState;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.gas.visual.GasUnitFormat;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public enum GasCanisterOverlay implements Layer {
    INSTANCE;

    public static final ResourceLocation RESOURCE = CCBAPI.asResource("gas_canister_overlay");
    private static final ItemStack CANISTER = new ItemStack(CCBItems.GAS_CANISTER.asItem());
    private static final ItemStack CREATIVE_CANISTER = new ItemStack(CCBItems.CREATIVE_GAS_CANISTER.asItem());
    private static final ItemStack PACK = new ItemStack(CCBItems.GAS_CANISTER_PACK.asItem());

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.options.hideGui || !CCBConfig.client().overlays.showCurrentGasInfo.get()) {
            return;
        }

        LocalPlayer player = client.player;
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }

        DisplayedGasState displayedState = CanisterContainerClients.getSyncedDisplayedGasState();
        long maxAmount = displayedState.maxAmount();
        if (!displayedState.synced() || maxAmount < 0) {
            return;
        }

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        poseStack.translate(guiGraphics.guiWidth() / 2.0F + 92, guiGraphics.guiHeight() - 19, 0);

        int xOffset = CCBConfig.client().overlays.gasInfoXOffset.get();
        int yOffset = CCBConfig.client().overlays.gasInfoYOffset.get();
        renderCanister(guiGraphics, displayedState.packType(), xOffset, yOffset);

        GasStack displayedGas = displayedState.content();
        long amount = displayedGas.getAmount();

        Font font = client.font;
        guiGraphics.drawString(font, CCBLang.gasName(displayedGas).style(ChatFormatting.GOLD).component(), 17 + xOffset, yOffset + (displayedGas.isEmpty() ? font.lineHeight / 2 : 0), 0);

        MutableComponent amountText = getAmountText(displayedState.creative(), amount, maxAmount);
        guiGraphics.drawString(font, amountText, 17 + xOffset, font.lineHeight + yOffset, 0);

        poseStack.popPose();
    }

    private static void renderCanister(GuiGraphics guiGraphics, int packType, int xOffset, int yOffset) {
        if (packType == -1) {
            GuiGameElement.of(CANISTER).at(xOffset, yOffset).render(guiGraphics);
            return;
        }

        if (packType == -2) {
            GuiGameElement.of(CREATIVE_CANISTER).at(xOffset, yOffset).render(guiGraphics);
            return;
        }

        ItemStack packStack = PACK.copy();
        packStack.set(CCBDataComponents.GAS_CANISTER_PACK_FLAGS, packType);
        GuiGameElement.of(packStack).at(xOffset, yOffset).render(guiGraphics);
    }

    private static MutableComponent getAmountText(boolean isCreative, long amount, long maxAmount) {
        if (isCreative) {
            return CCBLang.translateDirect("gui.gas_container.infinity").withStyle(ChatFormatting.GOLD);
        }

        return GasUnitFormat.amount(amount).color(Color.mixColors(CanisterDisplayColors.COLOR_RED, CanisterDisplayColors.COLOR_WHITE, Mth.clamp(2.0F * amount / maxAmount, 0.0F, 1.0F))).add(CCBLang.text(" / ").style(ChatFormatting.WHITE)).add(GasUnitFormat.amount(maxAmount).style(ChatFormatting.GRAY)).component();
    }
}
