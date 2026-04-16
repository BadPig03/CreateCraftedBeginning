package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Indicator;
import com.simibubi.create.foundation.gui.widget.Indicator.State;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.templates.AirtightHandheldDrillMiningTemplates;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.AirtightHandheldDrillUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradableMenu;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradableScreen;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeStatus;
import net.ty.createcraftedbeginning.data.CCBGUITextures;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class AirtightHandheldDrillScreen extends AirtightUpgradableScreen<AirtightHandheldDrillMenu> {
    private static final Component TEMPLATE_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.template");
    private static final Component SIZE_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.size");
    private static final Component DIRECTION_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.direction");
    private static final Component RELATIVE_POSITION_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.relative_position");
    private static final Component FILTER_SLOT_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.filter_slot");
    private static final int COLOR_INVALID = 0xFF5555;
    private static final int COLOR_VALID = 0xFFFFFF;

    private final List<Label> miningSizeLabels = new ArrayList<>(3);
    private final List<ScrollInput> miningSizeScrollInputs = new ArrayList<>(3);
    private final int[] currentMiningSizeParams;
    private final List<Label> miningRelativePositionLabels = new ArrayList<>(3);
    private final List<ScrollInput> miningRelativePositionScrollInputs = new ArrayList<>(3);
    private final int[] currentRelativePositionParams;

    private Label miningTemplateLabel;
    private ScrollInput miningTemplateScrollInput;
    private AirtightHandheldDrillMiningTemplates currentMiningTemplate;
    private Label miningDirectionLabel;
    private ScrollInput miningDirectionScrollInput;
    private Direction currentDirection;

    public AirtightHandheldDrillScreen(AirtightHandheldDrillMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, CCBGUITextures.HANDHELD_DRILL);
        currentMiningTemplate = AirtightHandheldDrillUtils.getMiningTemplate(menu.contentHolder);
        currentMiningSizeParams = AirtightHandheldDrillUtils.getMiningSizeParams(menu.contentHolder);
        currentDirection = AirtightHandheldDrillUtils.getMiningDirection(menu.contentHolder);
        currentRelativePositionParams = AirtightHandheldDrillUtils.getRelativePositionParams(menu.contentHolder);
    }

    @Override
    protected void init() {
        setWindowSize(Math.max(background.getWidth(), PLAYER_INVENTORY.getWidth()), background.getHeight() + 4 + PLAYER_INVENTORY.getHeight());
        setWindowOffset(-13, -4);
        super.init();
    }

    @Override
    protected void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isMouseOverSlot(mouseX, mouseY, leftPos + 16, topPos + 25)) {
            guiGraphics.renderTooltip(font, TEMPLATE_TITLE, mouseX, mouseY);
        }
        if (isMouseOverSlot(mouseX, mouseY, leftPos + 16, topPos + 45)) {
            guiGraphics.renderTooltip(font, SIZE_TITLE, mouseX, mouseY);
        }
        if (isMouseOverSlot(mouseX, mouseY, leftPos + 16, topPos + 65)) {
            guiGraphics.renderTooltip(font, RELATIVE_POSITION_TITLE, mouseX, mouseY);
        }
        if (isMouseOverSlot(mouseX, mouseY, leftPos + 16, topPos + 85)) {
            guiGraphics.renderTooltip(font, DIRECTION_TITLE, mouseX, mouseY);
        }
        if (hoveredSlot == null || hoveredSlot.hasItem() || hoveredSlot.getMaxStackSize() != 1) {
            return;
        }

        int slot = hoveredSlot.getSlotIndex();
        if (slot == AirtightHandheldDrillMenu.FILTER_SLOT_INDEX) {
            guiGraphics.renderTooltip(font, FILTER_SLOT_TITLE.plainCopy().withStyle(ChatFormatting.GRAY), mouseX, mouseY);
        }
        else if (slot == AirtightHandheldDrillMenu.UPGRADE_SLOT_INDEX) {
            guiGraphics.renderTooltip(font, disableUpgradeButton.visible ? UPGRADE_FULL.plainCopy().withStyle(ChatFormatting.GRAY) : UPGRADE_SLOT_TITLE.plainCopy().withStyle(ChatFormatting.GRAY), mouseX, mouseY);
        }
    }

    @Override
    protected void initWidgets() {
        initMiningTemplate();
        initMiningSize();
        initMiningRelativePosition();
        initMiningDirection();
    }

    @Override
    protected void initButtons() {
        upgradeButtons.clear();
        upgradeIndicators.clear();
        buttonConfigsMap.clear();

        IconButton confirmButton = new IconButton(leftPos + background.getWidth() - 31, topPos + background.getHeight() - 24, AllIcons.I_CONFIRM).withCallback(() -> menu.player.closeContainer());
        addRenderableWidget(confirmButton);

        disableUpgradeButton = new IconButton(leftPos + 151, topPos + 35, CCBIcons.I_FINISHED).setActive(false);
        disableUpgradeButton.visible = false;
        addRenderableWidget(disableUpgradeButton);

        AirtightHandheldDrillUpgradeRegistry.forEach(upgrade -> {
            Couple<Integer> offset = upgrade.getOffset();
            IconButton button = new IconButton(leftPos + offset.getFirst(), topPos + offset.getSecond(), upgrade.getIcon()).withCallback(() -> onUpgradeButtonPressed(upgrade));
            upgradeButtons.put(upgrade, button);

            if (upgrade.isRightIndicator()) {
                buttonConfigsMap.put(upgrade, new ScreenButtonConfig(button, upgrade.getTitle(), upgrade.getDescription(), () -> button.green, () -> false, () -> null, null));
            }
            else {
                Indicator indicator = new Indicator(leftPos + offset.getFirst(), topPos + offset.getSecond() - 6, CommonComponents.EMPTY);
                upgradeIndicators.put(upgrade, indicator);
                buttonConfigsMap.put(upgrade, new ScreenButtonConfig(button, upgrade.getTitle(), upgrade.getDescription(), () -> button.green, () -> !menu.getStatus(upgrade).isInstalled() && button.active, () -> upgrade.getGasCostComponent(menu.player), upgrade.getUpgradeItem()));
                addRenderableWidget(indicator);
            }
            addRenderableWidgets(button);
        });
    }

    @Override
    protected void updateStates() {
        ItemStack stack = menu.getMenuInventory().getStackInSlot(AirtightUpgradableMenu.UPGRADE_SLOT_INDEX);
        AirtightHandheldDrillUpgradeRegistry.forEach(upgrade -> {
            IconButton button = upgradeButtons.get(upgrade);
            AirtightUpgradeStatus upgradeStatus = menu.getStatus(upgrade);

            if (upgrade.isRightIndicator()) {
                button.green = upgradeStatus.isEnabled();
            }
            else {
                button.active = upgradeStatus.isInstalled() || stack.is(upgrade.getUpgradeItem());
                button.green = upgradeStatus.isInstalled() && upgradeStatus.isEnabled();
                Indicator indicator = (Indicator) upgradeIndicators.get(upgrade);
                indicator.state = upgradeStatus.isInstalled() ? upgradeStatus.isEnabled() ? State.GREEN : State.RED : button.active ? State.YELLOW : State.OFF;
            }
        });
        disableUpgradeButton.visible = menu.getCurrentStatusList().stream().allMatch(AirtightUpgradeStatus::isInstalled);
    }

    @Override
    public void removed() {
        if (!AirtightHandheldDrillUtils.isRelativePositionValid(currentMiningTemplate, currentMiningSizeParams, currentDirection, currentRelativePositionParams)) {
            int[] defaultPositions = currentMiningTemplate.getTemplate().getDefaultRelativePosition();
            currentRelativePositionParams[0] = defaultPositions[0];
            currentRelativePositionParams[1] = defaultPositions[1];
            currentRelativePositionParams[2] = defaultPositions[2];
        }
        CatnipServices.NETWORK.sendToServer(new AirtightHandheldDrillParametersPacket(currentMiningTemplate, new BlockPos(currentMiningSizeParams[0], currentMiningSizeParams[1], currentMiningSizeParams[2]), currentDirection, new BlockPos(currentRelativePositionParams[0], currentRelativePositionParams[1], currentRelativePositionParams[2])));
        super.removed();
    }

    private void initMiningTemplate() {
        removeWidget(miningTemplateLabel);
        removeWidget(miningTemplateScrollInput);

        miningTemplateLabel = new Label(leftPos + 45, topPos + 30, CommonComponents.EMPTY).withShadow();
        addRenderableWidget(miningTemplateLabel);

        miningTemplateScrollInput = new SelectionScrollInput(leftPos + 40, topPos + 25, 58, 18).forOptions(AirtightHandheldDrillMiningTemplates.TEMPLATE_OPTIONS).withShiftStep(1).titled(TEMPLATE_TITLE.plainCopy()).writingTo(miningTemplateLabel).calling(state -> {
            currentMiningTemplate = AirtightHandheldDrillMiningTemplates.values()[state];
            initMiningSize();
        });
        miningTemplateScrollInput.setState(currentMiningTemplate.ordinal());
        miningTemplateScrollInput.onChanged();
        addRenderableWidget(miningTemplateScrollInput);
    }

    private void initMiningSize() {
        removeWidgets(miningSizeLabels);
        removeWidgets(miningSizeScrollInputs);
        miningSizeLabels.clear();
        miningSizeScrollInputs.clear();

        for (int index = 0; index < 3; index++) {
            Label label = new Label(leftPos + 49 + 20 * index, topPos + 50, CommonComponents.EMPTY).withShadow();
            miningSizeLabels.add(label);

            int finalIndex = index;
            ScrollInput input = new ScrollInput(leftPos + 40 + 20 * index, topPos + 45, 18, 18).withRange(currentMiningTemplate.getTemplate().getMinValue(index), currentMiningTemplate.getTemplate().getMaxValue(index) + 1).withShiftStep(3).writingTo(label).titled(currentMiningTemplate.getSizeLabel(index, currentDirection).plainCopy()).calling(state -> {
                currentMiningSizeParams[finalIndex] = state;
                label.setX(leftPos + 49 + 20 * finalIndex - font.width(label.text) / 2);
                initMiningRelativePosition();
            });
            input.setState(currentMiningSizeParams[index]);
            input.onChanged();
            miningSizeScrollInputs.add(input);
        }
        addRenderableWidgets(miningSizeLabels);
        addRenderableWidgets(miningSizeScrollInputs);
    }

    private void initMiningRelativePosition() {
        removeWidgets(miningRelativePositionLabels);
        removeWidgets(miningRelativePositionScrollInputs);
        miningRelativePositionLabels.clear();
        miningRelativePositionScrollInputs.clear();

        for (int index = 0; index < 3; index++) {
            Label label = new Label(leftPos + 49 + 20 * index, topPos + 70, CommonComponents.EMPTY).withShadow();
            miningRelativePositionLabels.add(label);

            int finalIndex = index;
            ScrollInput input = new ScrollInput(leftPos + 40 + 20 * index, topPos + 65, 18, 18).withRange(0, currentMiningSizeParams[index]).withShiftStep(3).writingTo(label).titled(currentMiningTemplate.getRelativeLabel(index, currentDirection).plainCopy()).calling(state -> {
                currentRelativePositionParams[finalIndex] = state;
                label.setX(leftPos + 49 + 20 * finalIndex - font.width(label.text) / 2);
                boolean isValid = AirtightHandheldDrillUtils.isRelativePositionValid(currentMiningTemplate, currentMiningSizeParams, currentDirection, currentRelativePositionParams);
                miningRelativePositionLabels.forEach(l -> l.colored(isValid ? COLOR_VALID : COLOR_INVALID));
            });
            input.setState(currentRelativePositionParams[index]);
            input.onChanged();
            miningRelativePositionScrollInputs.add(input);
        }
        addRenderableWidgets(miningRelativePositionLabels);
        addRenderableWidgets(miningRelativePositionScrollInputs);
    }

    private void initMiningDirection() {
        removeWidget(miningDirectionLabel);
        removeWidget(miningDirectionScrollInput);

        miningDirectionLabel = new Label(leftPos + 45, topPos + 90, CommonComponents.EMPTY).withShadow();
        addRenderableWidget(miningDirectionLabel);

        miningDirectionScrollInput = new SelectionScrollInput(leftPos + 40, topPos + 85, 58, 18).forOptions(CCBLang.translatedOptions("gui.airtight_handheld_drill.direction", Arrays.stream(Direction.values()).map(Direction::getSerializedName).toArray(String[]::new))).withShiftStep(1).titled(DIRECTION_TITLE.plainCopy()).writingTo(miningDirectionLabel).calling(index -> {
            currentDirection = Direction.values()[index];
            initMiningRelativePosition();
        });
        miningDirectionScrollInput.setState(currentDirection.ordinal());
        addRenderableWidget(miningDirectionScrollInput);
    }
}
