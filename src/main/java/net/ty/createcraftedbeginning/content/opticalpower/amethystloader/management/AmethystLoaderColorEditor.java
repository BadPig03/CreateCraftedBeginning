package net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScreenOverlay;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.ty.createcraftedbeginning.client.gui.CCBGUITextures;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Locale;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystLoaderColorEditor extends ScreenOverlay {
    private static final CCBGUITextures BACKGROUND = CCBGUITextures.AMETHYST_LOADER_COLOR;
    private static final ResourceLocation BEAM_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");
    private static final int RGB_MASK = 0xFFFFFF;
    private static final int TEXT_COLOR = 0x363636;
    private static final int INVALID_TEXT_COLOR = 0xFF5555;
    private final Font font;
    private final int defaultColor;
    private final Consumer<@Nullable Integer> submit;
    private final EditBox hex;
    private final EditBox[] channels = new EditBox[3];
    private final IconButton reset;
    private final IconButton confirm;
    private int left;
    private int top;
    private int previewColor;
    private boolean customized;
    private boolean syncing;

    AmethystLoaderColorEditor(Font font, AmethystLoaderSnapshot snapshot, Consumer<@Nullable Integer> submit) {
        super(200);
        this.font = new NoShadowFontWrapper(font) {
            @Override
            public int drawInBatch(FormattedCharSequence text, float x, float y, int color, boolean dropShadow, Matrix4f matrix, MultiBufferSource buffer, DisplayMode displayMode, int backgroundColor, int packedLightCoords) {
                int end = super.drawInBatch(text, x, y, color, dropShadow, matrix, buffer, displayMode, backgroundColor, packedLightCoords);
                if (dropShadow) {
                    return end + 1;
                }

                return end;
            }
        };
        this.submit = submit;
        defaultColor = snapshot.defaultColor();
        hex = add(new CenteredInput(76, Component.literal("HEX")));
        hex.setMaxLength(7);
        hex.setFilter(value -> value.matches("#?[0-9a-fA-F]{0,6}"));
        hex.setResponder(this::updateHex);
        String[] labels = {"R", "G", "B"};
        for (int i = 0; i < channels.length; i++) {
            EditBox channel = add(new CenteredInput(20, Component.literal(labels[i])));
            channel.setMaxLength(3);
            channel.setFilter(value -> value.matches("[0-9]{0,3}"));
            channel.setResponder(value -> updateChannels());
            channels[i] = channel;
        }
        reset = add(new IconButton(0, 0, AllIcons.I_REFRESH).withCallback(() -> setColor(defaultColor, false)));
        reset.setToolTip(CCBLang.translateDirect("loading_network.color.reset"));
        confirm = add(new IconButton(0, 0, AllIcons.I_CONFIRM).withCallback(this::confirm));
        confirm.setToolTip(CCBLang.translateDirect("loading_network.color.apply"));
        setColor(snapshot.color(), snapshot.customized());
    }

    private static void setInputColor(EditBox input, boolean valid) {
        int color = valid ? TEXT_COLOR : INVALID_TEXT_COLOR;
        input.setTextColor(color);
        input.setTextColorUneditable(color);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            confirm();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(0, 0, zOffset);
            BACKGROUND.render(graphics, left, top);
            graphics.flush();
            graphics.enableScissor(left + 11, top + 25, left + 47, top + 59);
            pose.pushPose();
            try {
                pose.translate(left + 29, top + 42, 10);
                pose.scale(16, -16, 16);
                pose.translate(-0.5, 0, -0.5);
                BeaconRenderer.renderBeaconBeam(pose, graphics.bufferSource(), BEAM_TEXTURE, partialTick, 1, AnimationTickHolder.getTicks(true), -2, 4, previewColor, 0.12F, 0.2F);
                graphics.flush();
            }
            finally {
                pose.popPose();
                graphics.disableScissor();
            }
        }
        finally {
            pose.popPose();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    void setPosition(int left, int top) {
        this.left = left;
        this.top = top;
        hex.setPosition(left + 54, top + 24);
        for (int i = 0; i < channels.length; i++) {
            channels[i].setPosition(left + 54 + 28 * i, top + 44);
        }
        reset.setPosition(left + 137, top + 23);
        confirm.setPosition(left + 137, top + 43);
    }

    Rect2i bounds() {
        return new Rect2i(left, top, BACKGROUND.getWidth(), BACKGROUND.getHeight());
    }

    void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(0, 0, zOffset);
            if (reset.isMouseOver(mouseX, mouseY)) {
                graphics.renderComponentTooltip(font, reset.getToolTip(), mouseX, mouseY);
            }
            else if (confirm.isMouseOver(mouseX, mouseY)) {
                Component hint = CCBLang.translateDirect(confirm.active ? "loading_network.color.apply" : "loading_network.color.invalid");
                graphics.renderTooltip(font, hint, mouseX, mouseY);
            }
        }
        finally {
            pose.popPose();
        }
    }

    private void setColor(int color, boolean customized) {
        syncing = true;
        previewColor = 0xFF000000 | color & RGB_MASK;
        this.customized = customized;
        hex.setValue(String.format(Locale.ROOT, "#%06X", color & RGB_MASK));
        setInputColor(hex, true);
        for (int i = 0; i < channels.length; i++) {
            int channelShift = 16 - 8 * i;
            channels[i].setValue(Integer.toString(color >> channelShift & 0xFF));
            setInputColor(channels[i], true);
        }
        confirm.active = true;
        syncing = false;
    }

    private void updateHex(String value) {
        if (syncing) {
            return;
        }

        String digits = value.startsWith("#") ? value.substring(1) : value;
        confirm.active = digits.length() == 6;
        setInputColor(hex, confirm.active);
        if (!confirm.active) {
            return;
        }

        syncing = true;
        previewColor = 0xFF000000 | Integer.parseInt(digits, 16);
        customized = true;
        for (int i = 0; i < channels.length; i++) {
            int channelShift = 16 - 8 * i;
            channels[i].setValue(Integer.toString(previewColor >> channelShift & 0xFF));
            setInputColor(channels[i], true);
        }
        syncing = false;
    }

    private void updateChannels() {
        if (syncing) {
            return;
        }

        boolean valid = true;
        int color = 0;
        for (EditBox channel : channels) {
            String value = channel.getValue();
            int number = value.isEmpty() ? -1 : Integer.parseInt(value);
            boolean channelValid = number >= 0 && number <= 255;
            setInputColor(channel, channelValid);
            valid &= channelValid;
            color = color << 8 | number;
        }
        confirm.active = valid;
        if (!valid) {
            return;
        }

        syncing = true;
        previewColor = 0xFF000000 | color;
        customized = true;
        hex.setValue(String.format(Locale.ROOT, "#%06X", color));
        setInputColor(hex, true);
        syncing = false;
    }

    private void confirm() {
        if (!confirm.active) {
            return;
        }

        Integer color = customized ? previewColor & RGB_MASK : null;
        submit.accept(color);
    }

    private final class CenteredInput extends EditBox {
        private CenteredInput(int width, Component label) {
            super(AmethystLoaderColorEditor.this.font, 0, 0, width, 16, label);
            setBordered(false);
            setInputColor(this, true);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            super.onClick(mouseX - textOffset(), mouseY);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            PoseStack pose = graphics.pose();
            pose.pushPose();
            try {
                int textOffsetY = (getHeight() - 8) / 2;
                pose.translate(textOffset(), textOffsetY, 0);
                super.renderWidget(graphics, mouseX, mouseY, partialTick);
            }
            finally {
                pose.popPose();
            }
        }

        private int textOffset() {
            return (getWidth() - font.width(getValue())) / 2;
        }
    }
}
