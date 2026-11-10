package net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScreenOverlay;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.ScreenElement;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ty.createcraftedbeginning.client.gui.CCBGUITextures;
import net.ty.createcraftedbeginning.client.gui.CCBIcons;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management.AmethystLoaderSnapshot.Member;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapSnapshot;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapSnapshot.State;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapTexture;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkBinding;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkRuntime.Status;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystLoaderScreen extends AbstractSimiContainerScreen<AmethystLoaderMenu> {
    private static final CCBGUITextures BACKGROUND = CCBGUITextures.AMETHYST_LOADER;
    private static final CCBGUITextures STRIP = CCBGUITextures.AMETHYST_LOADER_STRIP;
    private static final CCBGUITextures NODE = CCBGUITextures.AMETHYST_LOADER_NODE;
    private static final CCBGUITextures EMPTY_STRIP = CCBGUITextures.AMETHYST_LOADER_EMPTY_STRIP;
    private static final CCBGUITextures ENTRY = CCBGUITextures.AMETHYST_LOADER_ENTRY;
    private static final CCBGUITextures DETAILS = CCBGUITextures.AMETHYST_LOADER_DETAILS;
    private static final ScreenElement EMPTY_ICON = (graphics, x, y) -> {
    };
    private static final int CONTENT_LEFT = 3;
    private static final int CONTENT_RIGHT = 223;
    private static final int CONTENT_TOP = 21;
    private static final int CONTENT_BOTTOM = 189;
    private static final int LIST_X = 7;
    private static final int STRIP_X = LIST_X + 8;
    private static final int ENTRY_X = LIST_X + 23;
    private static final int ENTRY_Y = 27;
    private static final int ENTRY_HEIGHT = ENTRY.getHeight();
    private static final int ENTRY_GAP = 2;
    private static final int TEXT_X = 17;
    private static final int NAME_Y = 14;
    private static final int NAME_WIDTH = 104;
    private static final int DETAILS_BUTTON_X = 132;
    private static final int BUTTON_X = 150;
    private static final int BUTTON_Y = 9;
    private static final int BUTTON_SIZE = 18;
    private static final int MAP_X = 54;
    private static final int MAP_Y = 25;
    private static final int MAP_FADE_HEIGHT = 8;
    private static final int DETAILS_NAME_WIDTH = DETAILS.getWidth() - 20;
    private static final int DETAILS_Z = 200;
    private static final int TITLE_WIDTH = 206;
    private static final int TITLE_EDIT_SPACE = 18;
    private final List<EntryButton> entryButtons = new ArrayList<>();
    private final ScreenOverlay detailsWidgets = new ScreenOverlay(DETAILS_Z);
    private final LerpedFloat scroll = LerpedFloat.linear().startWithValue(0);
    private final AmethystLoaderMapTexture mapTexture = new AmethystLoaderMapTexture();
    @Nullable
    private UUID selected;
    private int detailsLeft;
    private int detailsTop;
    private EditBox networkName;
    private boolean editingName;
    private IconButton networkToggle;
    private IconButton networkColor;
    @Nullable
    private IconButton beamToggle;
    @Nullable
    private AmethystLoaderColorEditor colorEditor;

    public AmethystLoaderScreen(AmethystLoaderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (colorEditor != null) {
            colorEditor.mouseClicked(mouseX, mouseY, button);
            setFocused(colorEditor);
            setDragging(button == 0);
            return true;
        }

        if (selected != null) {
            detailsWidgets.mouseClicked(mouseX, mouseY, button);
            return true;
        }

        AmethystLoaderSnapshot snapshot = menu.snapshot();
        if (networkColor.isMouseOver(mouseX, mouseY)) {
            finishNetworkName(true);
            return networkColor.mouseClicked(mouseX, mouseY, button);
        }

        if (button == 0 && isOverTitle(mouseX, mouseY) && snapshot.network() != null && snapshot.editable()) {
            if (editingName) {
                networkName.mouseClicked(mouseX, mouseY, button);
            }
            else {
                networkName.setValue(snapshot.name());
                editingName = true;
                networkName.visible = true;
                networkColor.visible = false;
                networkName.moveCursorToEnd(false);
                networkName.setHighlightPos(0);
            }
            setFocused(networkName);
            networkName.setFocused(true);
            return true;
        }

        finishNetworkName(true);
        updateEntryButtons((int) scroll.getValue(AnimationTickHolder.getPartialTicksUI()));
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (selected != null) {
            setFocused(detailsWidgets);
            setDragging(false);
        }
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (colorEditor != null) {
            if (keyCode == InputConstants.KEY_ESCAPE) {
                closeColor();
                return true;
            }

            if (keyCode == InputConstants.KEY_TAB) {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }

            return colorEditor.keyPressed(keyCode, scanCode, modifiers);
        }

        if (!editingName) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            finishNetworkName(true);
            return true;
        }

        if (keyCode == InputConstants.KEY_ESCAPE) {
            finishNetworkName(false);
            return true;
        }

        return networkName.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (colorEditor != null) {
            closeColor();
            return;
        }

        if (selected != null) {
            showList();
            return;
        }

        finishNetworkName(true);
        super.onClose();
    }

    @Override
    public void removed() {
        mapTexture.close();
        super.removed();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (selected != null || colorEditor != null) {
            return true;
        }

        if (!isOverList(mouseX, mouseY) || scrollY == 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int target = (int) Math.clamp(scroll.getChaseTarget() - scrollY * 12, 0, getMaxScroll());
        scroll.chase(target, 0.7F, Chaser.EXP);
        return true;
    }

    @Override
    public List<Rect2i> getExtraAreas() {
        if (colorEditor != null) {
            return List.of(colorEditor.bounds());
        }

        if (selected == null) {
            return List.of();
        }

        return List.of(new Rect2i(detailsLeft, detailsTop, DETAILS.getWidth(), DETAILS.getHeight()));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        AmethystLoaderSnapshot snapshot = menu.snapshot();
        boolean connected = snapshot.network() != null;
        if (!connected || !snapshot.editable()) {
            finishNetworkName(false);
            if (colorEditor != null) {
                closeColor();
            }
        }
        networkName.setEditable(snapshot.editable());
        networkColor.active = snapshot.editable();
        networkColor.visible = selected == null && colorEditor == null && !editingName && connected;
        networkToggle.visible = selected == null && colorEditor == null && connected;
        networkToggle.active = snapshot.editable();
        networkToggle.setIcon(snapshot.enabled() ? AllIcons.I_PAUSE : AllIcons.I_PLAY);
        networkToggle.setToolTip(CCBLang.translateDirect(snapshot.enabled() ? "loading_network.pause_network" : "loading_network.enable_network"));
        updateBeamButton(snapshot);
        int maxScroll = getMaxScroll();
        float chaseTarget = scroll.getChaseTarget();
        float value = scroll.getValue();
        float target = Math.min(chaseTarget, maxScroll);
        if (value > maxScroll || chaseTarget > maxScroll) {
            scroll.startWithValue(Math.min(value, maxScroll)).chase(target, 0.7F, Chaser.EXP);
        }
        scroll.tickChaser();
    }

    @Override
    protected void init() {
        String draft = editingName ? networkName.getValue() : menu.snapshot().name();
        int backgroundWidth = BACKGROUND.getWidth();
        int backgroundHeight = BACKGROUND.getHeight();
        setWindowSize(backgroundWidth, backgroundHeight);
        super.init();
        clearWidgets();
        setFocused(null);
        entryButtons.clear();
        detailsWidgets.clear();
        detailsWidgets.setFocused(null);
        beamToggle = null;
        detailsLeft = getLeftOfCentered(DETAILS.getWidth());
        detailsTop = topPos + 40;
        AmethystLoaderSnapshot snapshot = menu.snapshot();
        networkName = new EditBox(new NoShadowFontWrapper(font), leftPos + 10, topPos + 4, TITLE_WIDTH - 2 * TITLE_EDIT_SPACE, 10, CCBLang.translateDirect("loading_network.name"));
        networkName.setBordered(false);
        networkName.setTextColor(0xFFFFFF);
        networkName.setMaxLength(LoadingNetworkBinding.MAX_NAME_LENGTH);
        networkName.setResponder(this::positionNetworkName);
        networkName.setValue(draft);
        editingName = selected == null && colorEditor == null && snapshot.network() != null && snapshot.editable() && editingName;
        networkName.visible = editingName;
        networkName.setEditable(snapshot.editable());
        addRenderableWidget(networkName);
        if (networkName.visible) {
            setFocused(networkName);
            networkName.setFocused(true);
        }
        networkToggle = new IconButton(leftPos + 10, topPos + backgroundHeight - 24, snapshot.enabled() ? AllIcons.I_PAUSE : AllIcons.I_PLAY).withCallback(() -> PacketDistributor.sendToServer(new AmethystLoaderControlPacket(menu.containerId, null, !menu.snapshot().enabled())));
        networkToggle.visible = selected == null && colorEditor == null && snapshot.network() != null;
        networkToggle.active = snapshot.editable();
        networkToggle.setToolTip(CCBLang.translateDirect(snapshot.enabled() ? "loading_network.pause_network" : "loading_network.enable_network"));
        addRenderableWidget(networkToggle);
        networkColor = new ColorButton(leftPos + (BACKGROUND.getWidth() - 8 - font.width(networkHeading())) / 2 - TITLE_EDIT_SPACE, topPos + 1);
        networkColor.visible = selected == null && colorEditor == null && !editingName && snapshot.network() != null;
        networkColor.active = snapshot.editable();
        int buttonCount = (CONTENT_BOTTOM - CONTENT_TOP) / (ENTRY_HEIGHT + ENTRY_GAP) + 2;
        for (int i = 0; i < buttonCount; i++) {
            EntryButton details = new EntryButton(true);
            EntryButton toggle = new EntryButton(false);
            entryButtons.add(details);
            entryButtons.add(toggle);
            addWidget(details);
            addWidget(toggle);
        }
        addRenderableWidget(networkColor);
        IconButton confirmButton = new IconButton(leftPos + backgroundWidth - 33, topPos + backgroundHeight - 24, AllIcons.I_CONFIRM).withCallback(this::onClose);
        confirmButton.visible = selected == null && colorEditor == null;
        confirmButton.active = confirmButton.visible;
        addRenderableWidget(confirmButton);
        if (colorEditor != null) {
            colorEditor.setPosition(getLeftOfCentered(CCBGUITextures.AMETHYST_LOADER_COLOR.getWidth()), topPos + 65);
            addRenderableWidget(colorEditor);
            setFocused(colorEditor);
            return;
        }

        if (selected == null) {
            return;
        }

        beamToggle = new IconButton(detailsLeft + 10, detailsTop + 104, CCBIcons.I_LOADER_BEAM).withCallback(this::toggleBeam);
        updateBeamButton(snapshot);
        detailsWidgets.add(beamToggle);
        detailsWidgets.add(new IconButton(detailsLeft + 176, detailsTop + 104, AllIcons.I_CONFIRM).withCallback(this::showList));
        addRenderableWidget(detailsWidgets);
        setFocused(detailsWidgets);
        updateEntryButtons((int) scroll.getValue(AnimationTickHolder.getPartialTicksUI()));
    }

    @Override
    protected void setInitialFocus() {
        if (colorEditor != null) {
            setFocused(colorEditor);
            return;
        }

        if (editingName) {
            setInitialFocus(networkName);
            return;
        }

        super.setInitialFocus();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        BACKGROUND.render(graphics, leftPos, topPos);
        AmethystLoaderSnapshot snapshot = menu.snapshot();
        if (!networkName.visible) {
            boolean rename = snapshot.network() != null && snapshot.editable();
            String heading = networkHeading();
            int headingWidth = font.width(heading);
            int headingX = leftPos + (BACKGROUND.getWidth() - 8 - headingWidth) / 2;
            networkColor.setX(headingX - TITLE_EDIT_SPACE);
            graphics.drawString(font, heading, headingX, topPos + 4, 0xFFFFFF, false);
            if (rename) {
                CCBGUITextures.AMETHYST_LOADER_EDIT_NAME.render(graphics, headingX + headingWidth + 5, topPos + 1);
            }
        }
        if (snapshot.network() == null) {
            Component independent = CCBLang.translateDirect("loading_network.independent");
            graphics.drawString(font, independent, leftPos + (BACKGROUND.getWidth() - font.width(independent)) / 2, topPos + 68, 0x3D3D46, false);
            return;
        }

        List<Member> members = snapshot.members();
        int scrollOffset = (int) scroll.getValue(partialTick);
        updateEntryButtons(scrollOffset);
        int rowHeight = ENTRY_HEIGHT + ENTRY_GAP;
        int nodeOffset = (ENTRY_HEIGHT - NODE.getHeight()) / 2;
        int firstRow = Math.floorDiv(CONTENT_TOP - ENTRY_Y - nodeOffset + scrollOffset, rowHeight);
        int lastRow = Math.floorDiv(CONTENT_BOTTOM - 1 - ENTRY_Y + scrollOffset, rowHeight);
        graphics.enableScissor(leftPos + CONTENT_LEFT, topPos + CONTENT_TOP, leftPos + CONTENT_RIGHT, topPos + CONTENT_BOTTOM);
        try {
            for (int i = firstRow; i <= lastRow; i++) {
                int y = topPos + ENTRY_Y + i * rowHeight - scrollOffset;
                boolean hasMember = i >= 0 && i < members.size();
                CCBGUITextures segment = hasMember ? NODE : EMPTY_STRIP;
                segment.render(graphics, leftPos + STRIP_X - 4, y + nodeOffset);
                int segmentHeight = segment.getHeight();
                UIRenderHelper.drawStretched(graphics, leftPos + STRIP_X, y + nodeOffset + segmentHeight, STRIP.getWidth(), rowHeight - segmentHeight, 0, STRIP);
                if (!hasMember) {
                    continue;
                }

                renderEntry(graphics, members.get(i), leftPos + ENTRY_X, y);
            }
            for (EntryButton button : entryButtons) {
                button.render(graphics, mouseX, mouseY, partialTick);
            }
        }
        finally {
            graphics.disableScissor();
        }
        graphics.fillGradient(leftPos + CONTENT_LEFT, topPos + CONTENT_TOP, leftPos + CONTENT_RIGHT, topPos + CONTENT_TOP + 10, 200, 0x77000000, 0x00000000);
        graphics.fillGradient(leftPos + CONTENT_LEFT, topPos + CONTENT_BOTTOM - 10, leftPos + CONTENT_RIGHT, topPos + CONTENT_BOTTOM, 200, 0x00000000, 0x77000000);
        if (selected == null && colorEditor == null) {
            return;
        }

        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(0, 0, DETAILS_Z);
            graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
            if (selected != null) {
                renderDetails(graphics);
            }
        }
        finally {
            pose.popPose();
        }
    }

    @Override
    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (colorEditor != null) {
            colorEditor.renderTooltip(graphics, mouseX, mouseY);
            return;
        }

        if (selected != null) {
            PoseStack pose = graphics.pose();
            pose.pushPose();
            try {
                pose.translate(0, 0, DETAILS_Z);
                renderDetailsTooltip(graphics, mouseX, mouseY);
                if (beamToggle != null && beamToggle.isMouseOver(mouseX, mouseY)) {
                    graphics.renderComponentTooltip(font, beamToggle.getToolTip(), mouseX, mouseY);
                }
            }
            finally {
                pose.popPose();
            }
            return;
        }

        super.renderForeground(graphics, mouseX, mouseY, partialTick);
        if (!isOverList(mouseX, mouseY)) {
            return;
        }

        AmethystLoaderSnapshot snapshot = menu.snapshot();
        List<Member> members = snapshot.members();
        int x = leftPos + ENTRY_X;
        int scrollOffset = (int) scroll.getValue(partialTick);
        int rowHeight = ENTRY_HEIGHT + ENTRY_GAP;
        int index = Math.floorDiv(mouseY - topPos - ENTRY_Y + scrollOffset, rowHeight);
        if (index < 0 || index >= members.size()) {
            return;
        }

        Member member = members.get(index);
        int y = topPos + ENTRY_Y + index * rowHeight - scrollOffset;
        if (mouseX >= x + DETAILS_BUTTON_X && mouseX < x + DETAILS_BUTTON_X + BUTTON_SIZE && mouseY >= y + BUTTON_Y && mouseY < y + BUTTON_Y + BUTTON_SIZE) {
            graphics.renderTooltip(font, CCBLang.translateDirect("loading_network.view_details"), mouseX, mouseY);
            return;
        }

        if (mouseX >= x + BUTTON_X && mouseX < x + BUTTON_X + BUTTON_SIZE && mouseY >= y + BUTTON_Y && mouseY < y + BUTTON_Y + BUTTON_SIZE) {
            String action = member.enabled() ? "loading_network.disable_node" : "loading_network.enable_node";
            graphics.renderTooltip(font, CCBLang.translateDirect(action), mouseX, mouseY);
            return;
        }

        if (mouseX < x + TEXT_X) {
            return;
        }

        String name = AmethystLoaderDisplay.name(member);
        if (mouseX >= x + TEXT_X + NAME_WIDTH || mouseY < y + NAME_Y || mouseY >= y + NAME_Y + font.lineHeight || font.width(name) <= NAME_WIDTH) {
            return;
        }

        graphics.renderTooltip(font, font.split(Component.literal(name), Math.min(300, width - 20)), mouseX, mouseY);
    }

    private String networkHeading() {
        AmethystLoaderSnapshot snapshot = menu.snapshot();
        int availableWidth = snapshot.network() == null ? TITLE_WIDTH : TITLE_WIDTH - 2 * TITLE_EDIT_SPACE;
        String heading = snapshot.network() == null ? title.getString() : snapshot.name();
        if (font.width(heading) <= availableWidth) {
            return heading;
        }

        return font.plainSubstrByWidth(heading, availableWidth - font.width("…")) + '…';
    }

    private void positionNetworkName(String name) {
        int nameWidth = Math.min(font.width(name), networkName.getWidth());
        networkName.setX(leftPos + (BACKGROUND.getWidth() - 8 - nameWidth) / 2);
    }

    private void finishNetworkName(boolean save) {
        if (!editingName) {
            return;
        }

        String name = networkName.getValue().trim();
        editingName = false;
        networkName.setFocused(false);
        networkName.visible = false;
        setFocused(null);
        AmethystLoaderSnapshot snapshot = menu.snapshot();
        networkColor.visible = selected == null && colorEditor == null && snapshot.network() != null;
        if (!save || snapshot.network() == null || !snapshot.editable() || name.isEmpty() || name.equals(snapshot.name())) {
            return;
        }

        PacketDistributor.sendToServer(new AmethystLoaderRenamePacket(menu.containerId, name));
    }

    private boolean isOverTitle(double mouseX, double mouseY) {
        return selected == null && colorEditor == null && mouseX >= leftPos + CONTENT_LEFT && mouseX < leftPos + CONTENT_RIGHT && mouseY >= topPos + 2 && mouseY < topPos + 14;
    }

    private boolean isOverList(double mouseX, double mouseY) {
        return selected == null && colorEditor == null && mouseX >= leftPos + CONTENT_LEFT && mouseX < leftPos + CONTENT_RIGHT && mouseY >= topPos + CONTENT_TOP && mouseY < topPos + CONTENT_BOTTOM;
    }

    private int getMaxScroll() {
        return Math.max(0, menu.snapshot().members().size() * (ENTRY_HEIGHT + ENTRY_GAP) - ENTRY_GAP + 12 - (CONTENT_BOTTOM - CONTENT_TOP));
    }

    private void updateEntryButtons(int scrollOffset) {
        AmethystLoaderSnapshot snapshot = menu.snapshot();
        List<Member> members = snapshot.members();
        int rowHeight = ENTRY_HEIGHT + ENTRY_GAP;
        int firstRow = Math.max(0, Math.floorDiv(CONTENT_TOP - ENTRY_Y + scrollOffset, rowHeight));
        for (int i = 0; i < entryButtons.size(); i++) {
            EntryButton button = entryButtons.get(i);
            int index = firstRow + i / 2;
            int y = topPos + ENTRY_Y + index * rowHeight - scrollOffset + BUTTON_Y;
            button.visible = index < members.size() && y < topPos + CONTENT_BOTTOM && y + BUTTON_SIZE > topPos + CONTENT_TOP;
            button.member = null;
            button.active = false;
            if (!button.visible) {
                continue;
            }

            Member member = members.get(index);
            button.member = member;
            button.setPosition(leftPos + ENTRY_X + (button.details ? DETAILS_BUTTON_X : BUTTON_X), y);
            boolean enabled = member.enabled();
            ScreenElement icon = enabled ? AllIcons.I_PAUSE : AllIcons.I_PLAY;
            if (button.details) {
                icon = CCBIcons.I_LOADER_DETAILS;
            }
            button.setIcon(icon);
            button.green = !button.details && enabled;
            button.active = selected == null && colorEditor == null && (button.details || snapshot.editable() && member.status() != Status.ASSEMBLED);
        }
    }

    private void renderEntry(GuiGraphics graphics, Member member, int x, int y) {
        ENTRY.render(graphics, x, y);
        drawEntryText(graphics, AmethystLoaderDisplay.name(member), x + TEXT_X, y + NAME_Y);
    }

    private void drawEntryText(GuiGraphics graphics, String text, int x, int y) {
        if (font.width(text) > NAME_WIDTH) {
            text = font.plainSubstrByWidth(text, NAME_WIDTH - font.width("…")) + '…';
        }
        graphics.drawString(font, text, x + (NAME_WIDTH - font.width(text)) / 2, y, 0x373737, false);
    }

    private @Nullable Member findSelectedMember(AmethystLoaderSnapshot snapshot) {
        for (Member member : snapshot.members()) {
            if (member.identity().equals(selected)) {
                return member;
            }
        }
        return null;
    }

    private void showList() {
        selected = null;
        AmethystLoaderMapSession maps = menu.maps();
        maps.select(null);
        PacketDistributor.sendToServer(new AmethystLoaderMapRequestPacket(menu.containerId, null, maps.request()));
        mapTexture.close();
        init();
    }

    private void updateBeamButton(AmethystLoaderSnapshot snapshot) {
        IconButton button = beamToggle;
        if (button == null) {
            return;
        }

        Member member = findSelectedMember(snapshot);
        boolean enabled = member != null && member.beamEnabled();
        button.active = colorEditor == null && snapshot.editable() && member != null && member.status() != Status.ASSEMBLED;
        button.green = enabled;
        button.setToolTip(CCBLang.translateDirect(enabled ? "loading_network.disable_beam" : "loading_network.enable_beam"));
    }

    private void toggleBeam() {
        AmethystLoaderSnapshot snapshot = menu.snapshot();
        Member member = findSelectedMember(snapshot);
        if (member == null || member.status() == Status.ASSEMBLED || !snapshot.editable()) {
            return;
        }

        PacketDistributor.sendToServer(new AmethystLoaderBeamPacket(menu.containerId, member.identity(), !member.beamEnabled()));
    }



    private void closeColor() {
        colorEditor = null;
        init();
    }

    private void renderDetails(GuiGraphics graphics) {
        DETAILS.render(graphics, detailsLeft, detailsTop);
        Member member = findSelectedMember(menu.snapshot());
        String name = member == null ? "" : AmethystLoaderDisplay.name(member);
        if (font.width(name) > DETAILS_NAME_WIDTH) {
            name = font.plainSubstrByWidth(name, DETAILS_NAME_WIDTH - font.width("…")) + '…';
        }
        int detailsWidth = DETAILS.getWidth();
        graphics.drawString(font, name, detailsLeft + detailsWidth / 2 - font.width(name) / 2, detailsTop + 4, 0x373737, false);
        AmethystLoaderMapSession maps = menu.maps();
        AmethystLoaderMapSnapshot map = maps.snapshot();
        if (member != null) {
            String position = AmethystLoaderDisplay.position(member, maps.location()).getString();
            graphics.drawString(font, position, detailsLeft + detailsWidth / 2 - font.width(position) / 2, detailsTop + 134, 0x373737, false);
        }
        long nanos = System.nanoTime();
        Vec3 point = maps.point(nanos);
        Vec3 origin = maps.origin(nanos);
        if (member != null && map != null && map.state() == State.READY && point != null && origin != null) {
            mapTexture.render(graphics, map, point, origin, detailsLeft + MAP_X, detailsTop + MAP_Y, member.status() == Status.RUNNING);
        }
        else {
            mapTexture.renderUnknown(graphics, detailsLeft + MAP_X, detailsTop + MAP_Y);
        }
        int mapLeft = detailsLeft + MAP_X;
        int mapTop = detailsTop + MAP_Y;
        int mapRight = mapLeft + AmethystLoaderMapSnapshot.WIDTH;
        int mapBottom = mapTop + AmethystLoaderMapSnapshot.HEIGHT;
        graphics.fillGradient(mapLeft, mapTop, mapRight, mapTop + MAP_FADE_HEIGHT, 200, 0x77000000, 0x00000000);
        graphics.fillGradient(mapLeft, mapBottom - MAP_FADE_HEIGHT, mapRight, mapBottom, 200, 0x00000000, 0x77000000);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(mapLeft, mapTop, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(-90));
            graphics.fillGradient(-AmethystLoaderMapSnapshot.HEIGHT, 0, 0, MAP_FADE_HEIGHT, 200, 0x77000000, 0x00000000);
            graphics.fillGradient(-AmethystLoaderMapSnapshot.HEIGHT, AmethystLoaderMapSnapshot.WIDTH - MAP_FADE_HEIGHT, 0, AmethystLoaderMapSnapshot.WIDTH, 200, 0x00000000, 0x77000000);
        }
        finally {
            pose.popPose();
        }
    }

    private void renderDetailsTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Member member = findSelectedMember(menu.snapshot());
        if (member == null) {
            return;
        }

        String name = AmethystLoaderDisplay.name(member);
        if (mouseY < detailsTop || mouseY >= detailsTop + 14 || mouseX < detailsLeft || mouseX >= detailsLeft + DETAILS.getWidth() || font.width(name) <= DETAILS_NAME_WIDTH) {
            return;
        }

        graphics.renderTooltip(font, font.split(Component.literal(name), Math.min(300, width - 20)), mouseX, mouseY);
    }

    private final class ColorButton extends IconButton {
        private ColorButton(int x, int y) {
            super(x, y, 13, 13, EMPTY_ICON);
            withCallback(this::showColor);
        }

        @Override
        public void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX() + 2;
            int y = getY() + 2;
            graphics.fill(x, y, x + 9, y + 9, 0xFF23262F);
            graphics.fill(x, y, x + 8, y + 1, 0xFF9EA3B2);
            graphics.fill(x, y, x + 1, y + 8, 0xFF9EA3B2);
            graphics.fill(x + 1, y + 1, x + 8, y + 8, menu.snapshot().color());
        }

        private void showColor() {
            AmethystLoaderSnapshot snapshot = menu.snapshot();
            if (!snapshot.editable() || snapshot.network() == null) {
                return;
            }

            finishNetworkName(true);
            colorEditor = new AmethystLoaderColorEditor(font, snapshot, color -> {
                PacketDistributor.sendToServer(new AmethystLoaderColorPacket(menu.containerId, color));
                closeColor();
            });
            init();
        }
    }

    private final class EntryButton extends IconButton {
        private final boolean details;
        @Nullable
        private Member member;

        private EntryButton(boolean details) {
            super(0, 0, details ? CCBIcons.I_LOADER_DETAILS : AllIcons.I_PLAY);
            this.details = details;
            visible = false;
            withCallback(this::toggle);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return isOverList(mouseX, mouseY) && super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return isOverList(mouseX, mouseY) && super.isMouseOver(mouseX, mouseY);
        }

        @Override
        public void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!isOverList(mouseX, mouseY)) {
                super.doRender(graphics, -1, -1, partialTick);
                return;
            }

            super.doRender(graphics, mouseX, mouseY, partialTick);
        }

        private void toggle() {
            Member target = member;
            if (target == null) {
                return;
            }

            AmethystLoaderSnapshot snapshot = menu.snapshot();
            for (Member current : snapshot.members()) {
                UUID identity = current.identity();
                if (!identity.equals(target.identity())) {
                    continue;
                }

                if (details) {
                    showDetails(identity);
                    return;
                }

                if (!snapshot.editable() || current.status() == Status.ASSEMBLED) {
                    return;
                }

                PacketDistributor.sendToServer(new AmethystLoaderControlPacket(menu.containerId, identity, !current.enabled()));
                return;
            }
        }

        private void showDetails(UUID identity) {
            int offset = (int) scroll.getValue(AnimationTickHolder.getPartialTicksUI());
            scroll.startWithValue(offset).chase(offset, 0.7F, Chaser.EXP);
            selected = identity;
            AmethystLoaderMapSession maps = menu.maps();
            maps.select(identity);
            PacketDistributor.sendToServer(new AmethystLoaderMapRequestPacket(menu.containerId, identity, maps.request()));
            init();
        }
    }
}
