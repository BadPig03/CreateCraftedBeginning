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
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.client.CCBGUITextures;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.templates.AirtightHandheldDrillMiningTemplates;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.AirtightHandheldDrillUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradableMenu;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradableScreen;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeStatus;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class AirtightHandheldDrillScreen extends AirtightUpgradableScreen<AirtightHandheldDrillMenu> {
    private static final Component TEMPLATE_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.template");
    private static final Component SIZE_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.size");
    private static final Component DIRECTION_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.direction");
    private static final Component RELATIVE_POSITION_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.relative_position");
    private static final Component FILTER_SLOT_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.filter_slot");
    private static final int COLOR_DISABLED = 0x7F7F7F;
    private static final int COLOR_INVALID = 0xFF5555;
    private static final int COLOR_VALID = 0xFFFFFF;
    private static final int PARAMETER_COUNT = 3;

    private final List<Label> miningSizeLabels = new ArrayList<>(PARAMETER_COUNT);
    private final List<ScrollInput> miningSizeInputs = new ArrayList<>(PARAMETER_COUNT);
    private final int[] miningSize;
    private final List<Label> relativePositionLabels = new ArrayList<>(PARAMETER_COUNT);
    private final List<ScrollInput> relativePositionInputs = new ArrayList<>(PARAMETER_COUNT);
    private final int[] relativePosition;

    private Label miningTemplateLabel;
    private ScrollInput miningTemplateInput;
    private AirtightHandheldDrillMiningTemplates miningTemplate;
    private Label miningDirectionLabel;
    private ScrollInput miningDirectionInput;
    private Direction miningDirection;

    public AirtightHandheldDrillScreen(AirtightHandheldDrillMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, CCBGUITextures.HANDHELD_DRILL);
        miningTemplate = AirtightHandheldDrillUtils.getMiningTemplate(menu.contentHolder);
        miningSize = AirtightHandheldDrillUtils.getMiningSizeParams(menu.contentHolder);
        miningDirection = AirtightHandheldDrillUtils.getMiningDirection(menu.contentHolder);
        relativePosition = AirtightHandheldDrillUtils.getRelativePositionParams(menu.contentHolder);
    }

    private static State getIndicatorState(AirtightUpgradeStatus status, boolean isAvailable) {
        if (!status.isInstalled()) {
            return isAvailable ? State.YELLOW : State.OFF;
        }
        return status.isEnabled() ? State.GREEN : State.RED;
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
            Component tooltip = disableUpgradeButton.visible ? UPGRADE_FULL.plainCopy().withStyle(ChatFormatting.GRAY) : UPGRADE_SLOT_TITLE.plainCopy().withStyle(ChatFormatting.GRAY);
            guiGraphics.renderTooltip(font, tooltip, mouseX, mouseY);
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

        AirtightHandheldDrillUpgradeRegistry.forEach(this::addUpgradeButton);
    }

    @Override
    protected void updateStates() {
        ItemStack stack = menu.getMenuInventory().getStackInSlot(AirtightUpgradableMenu.UPGRADE_SLOT_INDEX);
        AirtightHandheldDrillUpgradeRegistry.forEach(upgrade -> {
            IconButton button = upgradeButtons.get(upgrade);
            AirtightUpgradeStatus status = menu.getStatus(upgrade);
            if (upgrade.isRightIndicator()) {
                button.green = status.isEnabled();
                return;
            }

            button.active = status.isInstalled() || upgrade.testUpgradeItem(stack);
            button.green = status.isInstalled() && status.isEnabled();
            Indicator indicator = (Indicator) upgradeIndicators.get(upgrade);
            indicator.state = getIndicatorState(status, button.active);
        });
        disableUpgradeButton.visible = menu.getCurrentStatusList().stream().allMatch(AirtightUpgradeStatus::isInstalled);
    }

    private void addUpgradeButton(AirtightUpgrade upgrade) {
        Couple<Integer> offset = upgrade.getOffset();
        IconButton button = new IconButton(leftPos + offset.getFirst(), topPos + offset.getSecond(), CCBIcons.get(upgrade.getIcon())).withCallback(() -> onUpgradeButtonPressed(upgrade));
        upgradeButtons.put(upgrade, button);

        if (upgrade.isRightIndicator()) {
            buttonConfigsMap.put(upgrade, new ScreenButtonConfig(button, upgrade.getTitle(), upgrade.getDescription(), () -> button.green, () -> false, List::of, null));
            addRenderableWidgets(button);
            return;
        }

        Indicator indicator = new Indicator(leftPos + offset.getFirst(), topPos + offset.getSecond() - 6, CommonComponents.EMPTY);
        upgradeIndicators.put(upgrade, indicator);
        buttonConfigsMap.put(upgrade, new ScreenButtonConfig(button, upgrade.getTitle(), upgrade.getDescription(), () -> button.green, () -> !menu.getStatus(upgrade).isInstalled() && button.active, () -> upgrade.getComponents(menu.player, menu.contentHolder.copy()), upgrade.getUpgradeItem()));
        addRenderableWidget(indicator);
        addRenderableWidgets(button);
    }

    @Override
    public void removed() {
        if (!AirtightHandheldDrillUtils.isRelativePositionValid(miningTemplate, miningSize, miningDirection, relativePosition)) {
            int[] defaultPositions = miningTemplate.getTemplate().getDefaultRelativePosition();
            relativePosition[0] = defaultPositions[0];
            relativePosition[1] = defaultPositions[1];
            relativePosition[2] = defaultPositions[2];
        }
        CatnipServices.NETWORK.sendToServer(new AirtightHandheldDrillParametersPacket(miningTemplate, new BlockPos(miningSize[0], miningSize[1], miningSize[2]), miningDirection, new BlockPos(relativePosition[0], relativePosition[1], relativePosition[2])));
        super.removed();
    }

    private void initMiningTemplate() {
        removeWidget(miningTemplateLabel);
        removeWidget(miningTemplateInput);

        miningTemplateLabel = new Label(leftPos + 45, topPos + 30, CommonComponents.EMPTY).withShadow();
        addRenderableWidget(miningTemplateLabel);

        miningTemplateInput = new SelectionScrollInput(leftPos + 40, topPos + 25, 58, 18).forOptions(AirtightHandheldDrillMiningTemplates.TEMPLATE_OPTIONS).withShiftStep(1).titled(TEMPLATE_TITLE.plainCopy()).writingTo(miningTemplateLabel).calling(state -> {
            miningTemplate = AirtightHandheldDrillMiningTemplates.values()[state];
            initMiningSize();
            initMiningDirection();
        });
        miningTemplateInput.setState(miningTemplate.ordinal());
        miningTemplateInput.onChanged();
        addRenderableWidget(miningTemplateInput);
    }

    private void initMiningSize() {
        removeWidgets(miningSizeLabels);
        removeWidgets(miningSizeInputs);
        miningSizeLabels.clear();
        miningSizeInputs.clear();

        for (int index = 0; index < PARAMETER_COUNT; index++) {
            Label label = new Label(leftPos + 49 + 20 * index, topPos + 50, CommonComponents.EMPTY).withShadow();
            miningSizeLabels.add(label);

            int parameterIndex = index;
            ScrollInput input = new ScrollInput(leftPos + 40 + 20 * index, topPos + 45, 18, 18).withRange(miningTemplate.getTemplate().getMinValue(index), miningTemplate.getTemplate().getMaxValue(index) + 1).withShiftStep(3).writingTo(label).titled(miningTemplate.getSizeLabel(index, miningDirection).plainCopy()).calling(state -> {
                miningSize[parameterIndex] = state;
                label.setX(leftPos + 49 + 20 * parameterIndex - font.width(label.text) / 2);
                initMiningRelativePosition();
            });
            input.setState(miningSize[index]);
            input.onChanged();
            input.active = miningTemplate.getTemplate().usesSpatialParameters();
            if (!input.active) {
                label.colored(COLOR_DISABLED);
            }
            miningSizeInputs.add(input);
        }
        addRenderableWidgets(miningSizeLabels);
        addRenderableWidgets(miningSizeInputs);
    }

    private void initMiningRelativePosition() {
        removeWidgets(relativePositionLabels);
        removeWidgets(relativePositionInputs);
        relativePositionLabels.clear();
        relativePositionInputs.clear();

        for (int index = 0; index < PARAMETER_COUNT; index++) {
            Label label = new Label(leftPos + 49 + 20 * index, topPos + 70, CommonComponents.EMPTY).withShadow();
            relativePositionLabels.add(label);

            int parameterIndex = index;
            ScrollInput input = new ScrollInput(leftPos + 40 + 20 * index, topPos + 65, 18, 18).withRange(0, miningSize[index]).withShiftStep(3).writingTo(label).titled(miningTemplate.getRelativeLabel(index, miningDirection).plainCopy()).calling(state -> {
                relativePosition[parameterIndex] = state;
                label.setX(leftPos + 49 + 20 * parameterIndex - font.width(label.text) / 2);
                boolean isValidPosition = AirtightHandheldDrillUtils.isRelativePositionValid(miningTemplate, miningSize, miningDirection, relativePosition);
                relativePositionLabels.forEach(positionLabel -> positionLabel.colored(isValidPosition ? COLOR_VALID : COLOR_INVALID));
            });
            input.setState(relativePosition[index]);
            input.onChanged();
            input.active = miningTemplate.getTemplate().usesSpatialParameters();
            relativePositionInputs.add(input);
        }
        if (!miningTemplate.getTemplate().usesSpatialParameters()) {
            relativePositionLabels.forEach(label -> label.colored(COLOR_DISABLED));
        }
        addRenderableWidgets(relativePositionLabels);
        addRenderableWidgets(relativePositionInputs);
    }

    private void initMiningDirection() {
        removeWidget(miningDirectionLabel);
        removeWidget(miningDirectionInput);

        miningDirectionLabel = new Label(leftPos + 45, topPos + 90, CommonComponents.EMPTY).withShadow();
        addRenderableWidget(miningDirectionLabel);

        List<Component> directionOptions = CCBLang.translatedOptions("gui.airtight_handheld_drill.direction", Arrays.stream(Direction.values()).map(Direction::getSerializedName).toArray(String[]::new));
        miningDirectionInput = new SelectionScrollInput(leftPos + 40, topPos + 85, 58, 18).forOptions(directionOptions).withShiftStep(1).titled(DIRECTION_TITLE.plainCopy()).writingTo(miningDirectionLabel).calling(index -> {
            miningDirection = Direction.values()[index];
            initMiningRelativePosition();
            initMiningSize();
        });
        miningDirectionInput.setState(miningDirection.ordinal());
        miningDirectionInput.active = miningTemplate.getTemplate().usesSpatialParameters();
        if (!miningDirectionInput.active) {
            miningDirectionLabel.colored(COLOR_DISABLED);
        }
        addRenderableWidget(miningDirectionInput);
    }
}
