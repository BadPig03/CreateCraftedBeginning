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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public enum GasCanisterOverlay implements Layer {
    INSTANCE;

    private static final ItemStack CANISTER = new ItemStack(CCBItems.GAS_CANISTER.asItem());
    private static final ItemStack PACK = new ItemStack(CCBItems.GAS_CANISTER_PACK.asItem());

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
        if (!data.contains(GasCanisterOverlayPacket.COMPOUND_KEY_OVERLAY)) {
            return;
        }

        CompoundTag compoundTag = data.getCompound(GasCanisterOverlayPacket.COMPOUND_KEY_OVERLAY);
        long capacity = compoundTag.getLong(GasCanisterOverlayPacket.COMPOUND_KEY_CAPACITY);
        if (capacity < 0) {
            return;
        }

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        int xOffset = CCBConfig.client().gasInfoXOffset.get();
        int yOffset = CCBConfig.client().gasInfoYOffset.get();
        poseStack.translate(guiGraphics.guiWidth() / 2.0f + 92, guiGraphics.guiHeight() - 19, 0);

        int packType = compoundTag.getInt(GasCanisterOverlayPacket.COMPOUND_KEY_PACK_TYPE);
        if (packType == -1) {
            GuiGameElement.of(CANISTER).at(xOffset, yOffset).render(guiGraphics);
        }
        else {
            ItemStack copied = PACK.copy();
            copied.set(CCBDataComponents.GAS_CANISTER_PACK_FLAGS, packType);
            GuiGameElement.of(copied).at(xOffset, yOffset).render(guiGraphics);
        }

        GasStack content = GasStack.parseOptional(player.level().registryAccess(), compoundTag.getCompound(GasCanisterOverlayPacket.COMPOUND_KEY_CONTENT));
        long amount = content.getAmount();

        Font font = mc.font;
        guiGraphics.drawString(font, CCBLang.gasName(content).style(ChatFormatting.GOLD).component(), 17 + xOffset, yOffset + (content.isEmpty() ? font.lineHeight / 2 : 0), 0);
        if (capacity > 0) {
            LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
            float ratio = Mth.clamp(2.0f * amount / capacity, 0, 1);
            guiGraphics.drawString(font, CCBLang.number(amount).add(mb).color(Color.mixColors(GasCanisterUtils.COLOR_RED, GasCanisterUtils.COLOR_WHITE, ratio)).add(CCBLang.text(" / ").style(ChatFormatting.WHITE)).add(CCBLang.number(capacity).add(mb).style(ChatFormatting.GRAY)).component(), 17 + xOffset, font.lineHeight + yOffset, 0);
        }

        poseStack.popPose();
    }
}
