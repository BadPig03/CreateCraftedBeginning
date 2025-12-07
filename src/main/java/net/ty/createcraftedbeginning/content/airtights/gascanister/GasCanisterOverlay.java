package net.ty.createcraftedbeginning.content.airtights.gascanister;

import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public enum GasCanisterOverlay implements Layer {
    INSTANCE;

    public static final ResourceLocation RESOURCE = CreateCraftedBeginning.asResource("gas_canister_overlay");

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, @NotNull DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !CCBConfig.client().enableCurrentGasInfo.get()) {
            return;
        }

        LocalPlayer player = mc.player;
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        if (!data.contains(GasCanisterOverlayPacket.COMPOUND_KEY_CONTENT) || !data.contains(GasCanisterOverlayPacket.COMPOUND_KEY_CAPACITY)) {
            return;
        }

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        int xOffset = CCBConfig.client().gasInfoXOffset.get();
        int yOffset = CCBConfig.client().gasInfoYOffset.get();
        poseStack.translate(guiGraphics.guiWidth() / 2.0f + 91, guiGraphics.guiHeight() - 19, 0);
        GuiGameElement.of(CCBItems.GAS_CANISTER.asStack()).at(xOffset, yOffset).render(guiGraphics);

        GasStack content = GasStack.parseOptional(player.level().registryAccess(), data.getCompound(GasCanisterOverlayPacket.COMPOUND_KEY_CONTENT));
        long amount = content.getAmount();
        long capacity = data.getLong(GasCanisterOverlayPacket.COMPOUND_KEY_CAPACITY);

        Font font = mc.font;
        Component gasNameText = CCBLang.gasName(content).style(ChatFormatting.GOLD).component();
        guiGraphics.drawString(font, gasNameText, 16 + xOffset, yOffset + (content.isEmpty() ? font.lineHeight / 2 : 0), 0);
        if (capacity > 0) {
            LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
            int color = Color.mixColors(GasCanisterUtils.COLOR_RED, GasCanisterUtils.COLOR_WHITE, Mth.clamp((float) amount / capacity, 0, 1));
            Component gasAmountText = CCBLang.number(amount).add(mb).color(color).add(CCBLang.text(" / ").style(ChatFormatting.WHITE)).add(CCBLang.number(capacity).add(mb).style(ChatFormatting.GRAY)).component();
            guiGraphics.drawString(font, gasAmountText, 16 + xOffset, font.lineHeight + yOffset, 0);
        }

        poseStack.popPose();
    }
}
