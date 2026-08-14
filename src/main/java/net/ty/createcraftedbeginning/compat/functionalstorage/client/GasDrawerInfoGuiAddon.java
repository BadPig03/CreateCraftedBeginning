package net.ty.createcraftedbeginning.compat.functionalstorage.client;

import com.buuz135.functionalstorage.util.NumberUtils;
import com.hrznstudio.titanium.client.screen.addon.BasicScreenAddon;
import com.hrznstudio.titanium.client.screen.asset.IAssetProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.inventory.InventoryMenu;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.client.CCBGasClientTextures;
import net.ty.createcraftedbeginning.compat.functionalstorage.GasDrawerBlockEntity;
import net.ty.createcraftedbeginning.compat.functionalstorage.GasDrawerBlockEntity.RenderGas;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasDrawerInfoGuiAddon extends BasicScreenAddon {
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("####0.#");

    private final GasDrawerBlockEntity drawer;

    public GasDrawerInfoGuiAddon(int posX, int posY, GasDrawerBlockEntity drawer) {
        super(posX, posY);
        this.drawer = drawer;
    }

    private static void drawSlotFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF5A5A5A);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF171717);
    }

    private static Rect2i slotRect(int slot, int slots) {
        if (slots == 1) {
            return new Rect2i(6, 6, 36, 36);
        }
        else if (slots == 2) {
            return new Rect2i(4, slot == 0 ? 25 : 5, 40, 18);
        }
        return switch (slot) {
            case 0 -> new Rect2i(25, 25, 18, 18);
            case 1 -> new Rect2i(5, 25, 18, 18);
            case 2 -> new Rect2i(25, 5, 18, 18);
            default -> new Rect2i(5, 5, 18, 18);
        };
    }

    private static String format(long number) {
        if (number <= Integer.MAX_VALUE) {
            return NumberUtils.getFormatedFluidBigNumber((int) number);
        }
        return AMOUNT_FORMAT.format(number / 1000000000) + "M B";
    }

    @Override
    public int getXSize() {
        return 0;
    }

    @Override
    public int getYSize() {
        return 0;
    }

    @Override
    public void drawBackgroundLayer(GuiGraphics graphics, Screen screen, IAssetProvider provider, int guiX, int guiY, int mouseX, int mouseY, float partialTicks) {
        int panelX = guiX + getPosX();
        int panelY = guiY + getPosY();
        drawSlotFrame(graphics, panelX, panelY, 48, 48);
        int slots = drawer.getDrawerType().getSlots();
        for (int slot = 0; slot < slots; slot++) {
            drawSlot(graphics, guiX, guiY, slot, slotRect(slot, slots));
        }
    }

    @Override
    public void drawForegroundLayer(GuiGraphics graphics, Screen screen, IAssetProvider provider, int guiX, int guiY, int mouseX, int mouseY, float partialTicks) {
        int slots = drawer.getDrawerType().getSlots();
        for (int slot = 0; slot < slots; slot++) {
            Rect2i rect = slotRect(slot, slots);
            if (!isHovered(guiX, guiY, mouseX, mouseY, rect)) {
                continue;
            }

            Font font = Minecraft.getInstance().font;
            graphics.renderTooltip(font, createTooltip(slot), Optional.empty(), mouseX - guiX, mouseY - guiY);
        }
    }

    private void drawSlot(GuiGraphics graphics, int guiX, int guiY, int slot, Rect2i rect) {
        int x = guiX + getPosX() + rect.getX();
        int y = guiY + getPosY() + rect.getY();
        drawSlotFrame(graphics, x, y, rect.getWidth(), rect.getHeight());
        RenderGas renderGas = drawer.getRenderGas(slot);
        if (renderGas.isEmpty()) {
            return;
        }

        renderGas(graphics, guiX, guiY, renderGas.stack(), renderGas.filterOnly(), rect);
        if (renderGas.filterOnly()) {
            graphics.fill(x + 1, y + 1, x + rect.getWidth() - 1, y + rect.getHeight() - 1, 0x55000000);
            return;
        }

        drawAmount(graphics, guiX, guiY, rect, renderGas.stack());
    }

    private boolean isHovered(int guiX, int guiY, int mouseX, int mouseY, Rect2i rect) {
        int x = guiX + getPosX() + rect.getX();
        int y = guiY + getPosY() + rect.getY();
        return mouseX >= x && mouseX < x + rect.getWidth() && mouseY >= y && mouseY < y + rect.getHeight();
    }

    private List<Component> createTooltip(int slot) {
        RenderGas renderGas = drawer.getRenderGas(slot);
        List<Component> tooltip = new ArrayList<>();
        if (renderGas.isEmpty()) {
            tooltip.add(CCBLang.translateDirect("gui.gas_canister.content").withStyle(ChatFormatting.GOLD).append(CCBLang.translateDirect("gui.gas_container.empty").withStyle(ChatFormatting.WHITE)));
        }
        else {
            appendGasTooltip(tooltip, renderGas);
        }
        tooltip.add(Component.translatable("gui.functionalstorage.slot").withStyle(ChatFormatting.GOLD).append(Component.literal(Integer.toString(slot + 1)).withStyle(ChatFormatting.WHITE)));
        return tooltip;
    }

    private void appendGasTooltip(List<Component> tooltip, RenderGas renderGas) {
        GasStack stack = renderGas.stack();
        tooltip.add(CCBLang.translateDirect("gui.gas_canister.content").withStyle(ChatFormatting.GOLD).append(stack.getHoverName().copy().withStyle(ChatFormatting.WHITE)));
        if (!stack.isComponentsPatchEmpty()) {
            tooltip.add(CCBLang.translateDirect("compat.functional_storage.gas_drawer.has_components").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (renderGas.filterOnly()) {
            tooltip.add(CCBLang.translateDirect("compat.functional_storage.gas_drawer.locked_filter").withStyle(ChatFormatting.GRAY));
        }

        long amount = renderGas.filterOnly() ? 0 : stack.getAmount();
        String value = drawer.isCreative() && !renderGas.filterOnly() ? "∞" : GasAmountUtils.formatCompact(amount) + '/' + GasAmountUtils.formatCompact(drawer.getPhysicalTankCapacity());
        tooltip.add(Component.translatable("gui.functionalstorage.amount").withStyle(ChatFormatting.GOLD).append(Component.literal(value).withStyle(ChatFormatting.WHITE)));
    }

    private void drawAmount(GuiGraphics graphics, int guiX, int guiY, Rect2i rect, GasStack stack) {
        int x = guiX + getPosX() + rect.getX();
        int y = guiY + getPosY() + rect.getY();
        Font font = Minecraft.getInstance().font;
        String amount = drawer.isCreative() ? "∞" : format(stack.getAmount()) + '/' + format(drawer.getPhysicalTankCapacity());

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 200);
        poseStack.scale(0.5f, 0.5f, 0.5f);
        graphics.drawString(font, amount, Math.round(x * 2 + rect.getWidth() - font.width(amount) / 2.0f), (y + rect.getHeight()) * 2 - 10, 0xFFFFFF, true);

        poseStack.popPose();
    }

    private void renderGas(GuiGraphics graphics, int guiX, int guiY, GasStack stack, boolean filterOnly, Rect2i rect) {
        TextureAtlasSprite sprite = CCBGasClientTextures.getGasTexture(stack.getGasHolder());
        int tint = stack.getHint();
        float red = ARGB32.red(tint) / 255.0f;
        float green = ARGB32.green(tint) / 255.0f;
        float blue = ARGB32.blue(tint) / 255.0f;
        float tintAlpha = ARGB32.alpha(tint) / 255.0f;
        float alpha = filterOnly ? 0.35f : tintAlpha <= 0 ? 1 : tintAlpha;
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.setShaderColor(red, green, blue, alpha);
        RenderSystem.enableBlend();

        int slotX = guiX + getPosX() + rect.getX();
        int slotY = guiY + getPosY() + rect.getY();
        int iconX = slotX + (rect.getWidth() - 16) / 2;
        int iconY = slotY + (rect.getHeight() - 16) / 2;
        graphics.blit(iconX, iconY, 0, 16, 16, sprite);

        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }
}
