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
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClients;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClients.DisplayedGasState;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public enum GasCanisterOverlay implements Layer {
    INSTANCE;

    public static final ResourceLocation RESOURCE = CreateCraftedBeginning.asResource("gas_canister_overlay");
    private static final ItemStack CANISTER = new ItemStack(CCBItems.GAS_CANISTER.asItem());
    private static final ItemStack CREATIVE_CANISTER = new ItemStack(CCBItems.CREATIVE_GAS_CANISTER.asItem());
    private static final ItemStack PACK = new ItemStack(CCBItems.GAS_CANISTER_PACK.asItem());

    private static void renderCanister(GuiGraphics guiGraphics, int packType, int xOffset, int yOffset) {
        if (packType == -1) {
            GuiGameElement.of(CANISTER).at(xOffset, yOffset).render(guiGraphics);
            return;
        }

        if (packType == -2) {
            GuiGameElement.of(CREATIVE_CANISTER).at(xOffset, yOffset).render(guiGraphics);
            return;
        }

        ItemStack pack = PACK.copy();
        pack.set(CCBDataComponents.GAS_CANISTER_PACK_FLAGS, packType);
        GuiGameElement.of(pack).at(xOffset, yOffset).render(guiGraphics);
    }

    private static MutableComponent getAmountText(boolean isCreative, long amount, long capacity) {
        if (isCreative) {
            return CCBLang.translateDirect("gui.gas_container.infinity").withStyle(ChatFormatting.GOLD);
        }
        return GasAmountUtils.precise(amount).color(Color.mixColors(GasCanisterUtils.COLOR_RED, GasCanisterUtils.COLOR_WHITE, Mth.clamp(2.0f * amount / capacity, 0, 1))).add(CCBLang.text(" / ").style(ChatFormatting.WHITE)).add(GasAmountUtils.precise(capacity).style(ChatFormatting.GRAY)).component();
    }

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !CCBConfig.client().enableCurrentGasInfo.get()) {
            return;
        }

        LocalPlayer player = mc.player;
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }

        DisplayedGasState state = CanisterContainerClients.getSyncedDisplayedGasState();
        long capacity = state.capacity();
        if (!state.synced() || capacity < 0) {
            return;
        }

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        poseStack.translate(guiGraphics.guiWidth() / 2.0f + 92, guiGraphics.guiHeight() - 19, 0);

        int xOffset = CCBConfig.client().gasInfoXOffset.get();
        int yOffset = CCBConfig.client().gasInfoYOffset.get();
        renderCanister(guiGraphics, state.packType(), xOffset, yOffset);

        GasStack content = state.content();
        long amount = content.getAmount();

        Font font = mc.font;
        guiGraphics.drawString(font, CCBLang.gasName(content).style(ChatFormatting.GOLD).component(), 17 + xOffset, yOffset + (content.isEmpty() ? font.lineHeight / 2 : 0), 0);

        MutableComponent amountText = getAmountText(state.creative(), amount, capacity);
        guiGraphics.drawString(font, amountText, 17 + xOffset, font.lineHeight + yOffset, 0);

        poseStack.popPose();
    }
}
