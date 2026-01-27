package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Indicator;
import com.simibubi.create.foundation.gui.widget.Indicator.State;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.templates.AirtightHandheldDrillMiningTemplates;
import net.ty.createcraftedbeginning.data.CCBGUITextures;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

@OnlyIn(Dist.CLIENT)
public class AirtightHandheldDrillScreen extends AbstractSimiContainerScreen<AirtightHandheldDrillMenu> {
    private static final Component FILTER_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.filter");
    private static final Component FILTER_DESCRIPTION = CCBLang.translateDirect("gui.airtight_handheld_drill.filter.description");
    private static final Component CONTAINER_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.container");
    private static final Component CONTAINER_DESCRIPTION = CCBLang.translateDirect("gui.airtight_handheld_drill.container.description");
    private static final Component OUTLINE_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.outline");
    private static final Component OUTLINE_DESCRIPTION = CCBLang.translateDirect("gui.airtight_handheld_drill.outline.description");
    private static final Component DRILL_ATTACK_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.drill_attack");
    private static final Component DRILL_ATTACK_DESCRIPTION = CCBLang.translateDirect("gui.airtight_handheld_drill.drill_attack.description");

    private static final Component TEMPLATE_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.template");
    private static final Component SIZE_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.size");
    private static final Component DIRECTION_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.direction");
    private static final Component RELATIVE_POSITION_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.relative_position");

    private static final Component SILK_TOUCH_UPGRADE_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.silk_touch_upgrade");
    private static final Component SILK_TOUCH_UPGRADE_DESCRIPTION = CCBLang.translateDirect("gui.airtight_handheld_drill.silk_touch_upgrade.description");
    private static final Component MAGNET_UPGRADE_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.magnet_upgrade");
    private static final Component MAGNET_UPGRADE_DESCRIPTION = CCBLang.translateDirect("gui.airtight_handheld_drill.magnet_upgrade.description");
    private static final Component CONVERSION_UPGRADE_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.conversion_upgrade");
    private static final Component CONVERSION_UPGRADE_DESCRIPTION = CCBLang.translateDirect("gui.airtight_handheld_drill.conversion_upgrade.description");
    private static final Component LIQUID_REPLACEMENT_UPGRADE_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.liquid_replacement_upgrade");
    private static final Component LIQUID_REPLACEMENT_UPGRADE_DESCRIPTION = CCBLang.translateDirect("gui.airtight_handheld_drill.liquid_replacement_upgrade.description");

    private static final Component FILTER_SLOT_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.filter_slot");
    private static final Component UPGRADE_SLOT_TITLE = CCBLang.translateDirect("gui.airtight_handheld_drill.upgrade_slot");
    private static final Component OPTION_ENABLED = CCBLang.translateDirect("gui.airtight_handheld_drill.option_enabled");
    private static final Component OPTION_DISABLED = CCBLang.translateDirect("gui.airtight_handheld_drill.option_disabled");
    private static final Component UPGRADE_NOT_INSTALLED = CCBLang.translateDirect("gui.airtight_handheld_drill.upgrade_not_installed");
    private static final Component UPGRADE_CAN_BE_INSTALLED = CCBLang.translateDirect("gui.airtight_handheld_drill.upgrade_can_be_installed");
    private static final Component UPGRADE_FULL = CCBLang.translateDirect("gui.airtight_handheld_drill.upgrade_full");

    private static final int COLOR_INVALID = 0xFF5555;
    private static final int COLOR_VALID = 0xFFFFFF;

    private static final CCBGUITextures BACKGROUND = CCBGUITextures.HANDHELD_DRILL;
    private static final AllGuiTextures PLAYER_INVENTORY = AllGuiTextures.PLAYER_INVENTORY;

    private final ItemStack drill;

    private final List<Label> miningSizeLabels = new ArrayList<>(3);
    private final List<ScrollInput> miningSizeScrollInputs = new ArrayList<>(3);
    private final int[] currentMiningSizeParams;

    private final List<Label> miningRelativePositionLabels = new ArrayList<>(3);
    private final List<ScrollInput> miningRelativePositionScrollInputs = new ArrayList<>(3);
    private final int[] currentRelativePositionParams;
    private final int[] previousRelativePositionParams;

    private final Map<String, ScreenButtonConfig> buttonConfigs = new HashMap<>();

    private Label miningTemplateLabel;
    private ScrollInput miningTemplateScrollInput;
    private AirtightHandheldDrillMiningTemplates currentMiningTemplate;

    private Label miningDirectionLabel;
    private ScrollInput miningDirectionScrollInput;
    private Direction currentDirection;

    private IconButton filterButton;
    private IconButton containerButton;
    private IconButton outlineButton;
    private IconButton drillAttackButton;
    private IconButton disableUpgradeButton;

    private Indicator silkTouchIndicator;
    private IconButton silkTouchButton;
    private Indicator magnetIndicator;
    private IconButton magnetButton;
    private Indicator conversionIndicator;
    private IconButton conversionButton;
    private Indicator liquidReplacementIndicator;
    private IconButton liquidReplacementButton;

    private boolean isRelativePositionDirty;

    public AirtightHandheldDrillScreen(AirtightHandheldDrillMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        drill = menu.player.getMainHandItem();
        currentMiningTemplate = AirtightHandheldDrillUtils.getMiningTemplate(drill);
        currentMiningSizeParams = AirtightHandheldDrillUtils.getMiningSizeParams(drill);
        currentDirection = AirtightHandheldDrillUtils.getMiningDirection(drill);
        currentRelativePositionParams = AirtightHandheldDrillUtils.getRelativePositionParams(drill);
        previousRelativePositionParams = currentRelativePositionParams.clone();
    }

    @Override
    protected void init() {
        setWindowSize(Math.max(BACKGROUND.getWidth(), PLAYER_INVENTORY.getWidth()), BACKGROUND.getHeight() + 4 + PLAYER_INVENTORY.getHeight());
        setWindowOffset(-13, -4);
        super.init();
        clearWidgets();
        initButtons();
        initMiningTemplate();
        initMiningSize();
        initMiningRelativePosition();
        initMiningDirection();
        updateStates();
    }

    @Override
    protected void containerTick() {
        Player player = menu.player;
        if (!ItemStack.isSameItem(player.getMainHandItem(), menu.contentHolder)) {
            player.closeContainer();
        }
        super.containerTick();
        updateStates();
        renderTooltips();
    }

    @Override
    protected void renderForeground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.renderForeground(guiGraphics, mouseX, mouseY, partialTicks);
        renderForeground(guiGraphics, mouseX, mouseY);
    }

    @Override
    public List<Rect2i> getExtraAreas() {
        return ImmutableList.of(new Rect2i(leftPos + 2 + BACKGROUND.getWidth(), topPos + BACKGROUND.getHeight() - 32, 64, 32));
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPlayerInventory(guiGraphics, getLeftOfCentered(PLAYER_INVENTORY.getWidth()) + 1, topPos + BACKGROUND.getHeight() + 4);
        BACKGROUND.render(guiGraphics, leftPos + 2, topPos);
        Component drillHoverName = drill.getHoverName();
        guiGraphics.drawString(font, drillHoverName, leftPos + (BACKGROUND.getWidth() - 8) / 2 - font.width(drillHoverName) / 2 + 2, topPos + 4, 0xFFFFFF, false);
        GuiGameElement.of(drill).scale(4).at(leftPos + BACKGROUND.getWidth() + 2, topPos + BACKGROUND.getHeight() - 48, -200).render(guiGraphics);
    }

    @Override
    public void removed() {
        Set<BlockPos> set = currentMiningTemplate.getTemplate().getOffset(currentMiningSizeParams, currentDirection, currentRelativePositionParams);
        if (!set.contains(BlockPos.ZERO)) {
            System.arraycopy(Arrays.equals(previousRelativePositionParams, currentRelativePositionParams) ? currentMiningTemplate.getTemplate().getDefaultRelativePosition() : previousRelativePositionParams, 0, currentRelativePositionParams, 0, 3);
        }
        AirtightHandheldDrillScreenPacket packet = new AirtightHandheldDrillScreenPacket(menu.saveFlagsToInt(), currentMiningTemplate, new BlockPos(currentMiningSizeParams[0], currentMiningSizeParams[1], currentMiningSizeParams[2]), currentDirection, new BlockPos(currentRelativePositionParams[0], currentRelativePositionParams[1], currentRelativePositionParams[2]));
        packet.configureDrill(drill);
        CatnipServices.NETWORK.sendToServer(packet);
    }

    private void consumeUpgradeItem() {
        menu.drillInventory.setStackInSlot(AirtightHandheldDrillMenu.UPGRADE_SLOT_INDEX, ItemStack.EMPTY);
        menu.slots.get(AirtightHandheldDrillMenu.UPGRADE_SLOT_INDEX + AirtightHandheldDrillMenu.PLAYER_INVENTORY_SLOTS).setChanged();
        AirtightHandheldDrillInventoryPacket packet = new AirtightHandheldDrillInventoryPacket(menu.saveFlagsToInt());
        CatnipServices.NETWORK.sendToServer(packet);
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
                previousRelativePositionParams[finalIndex] = currentRelativePositionParams[finalIndex];
                currentRelativePositionParams[finalIndex] = state;
                label.setX(leftPos + 49 + 20 * finalIndex - font.width(label.text) / 2);
                isRelativePositionDirty = true;
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

    private void initButtons() {
        filterButton = new IconButton(leftPos + 40, topPos + BACKGROUND.getHeight() - 24, CCBIcons.I_FILTER).withCallback(() -> menu.filterDisabled = !menu.filterDisabled);
        addRenderableWidget(filterButton);
        buttonConfigs.put("filter", new ScreenButtonConfig(filterButton, FILTER_TITLE, FILTER_DESCRIPTION, () -> filterButton.green, () -> false, null));

        containerButton = new IconButton(leftPos + 58, topPos + BACKGROUND.getHeight() - 24, CCBIcons.I_CONTAINER).withCallback(() -> menu.containerDisabled = !menu.containerDisabled);
        addRenderableWidget(containerButton);
        buttonConfigs.put("container", new ScreenButtonConfig(containerButton, CONTAINER_TITLE, CONTAINER_DESCRIPTION, () -> containerButton.green, () -> false, null));

        drillAttackButton = new IconButton(leftPos + BACKGROUND.getWidth() - 78, topPos + BACKGROUND.getHeight() - 24, CCBIcons.I_DRILL_ATTACK).withCallback(() -> menu.drillAttackDisabled = !menu.drillAttackDisabled);
        addRenderableWidget(drillAttackButton);
        buttonConfigs.put("drill_attack", new ScreenButtonConfig(drillAttackButton, DRILL_ATTACK_TITLE, DRILL_ATTACK_DESCRIPTION, () -> drillAttackButton.green, () -> false, null));

        outlineButton = new IconButton(leftPos + BACKGROUND.getWidth() - 60, topPos + BACKGROUND.getHeight() - 24, CCBIcons.I_OUTLINE).withCallback(() -> menu.outlineDisabled = !menu.outlineDisabled);
        addRenderableWidget(outlineButton);
        buttonConfigs.put("outline", new ScreenButtonConfig(outlineButton, OUTLINE_TITLE, OUTLINE_DESCRIPTION, () -> outlineButton.green, () -> false, null));

        silkTouchButton = new IconButton(leftPos + 123, topPos + BACKGROUND.getHeight() - 60, CCBIcons.I_SILK_TOUCH_UPGRADE).withCallback(() -> {
            if (!menu.silkTouchInstalled && menu.drillInventory.getStackInSlot(AirtightHandheldDrillMenu.UPGRADE_SLOT_INDEX).is(AirtightHandheldDrillUtils.SILK_TOUCH_UPGRADE_ITEM)) {
                menu.silkTouchInstalled = true;
                menu.silkTouchEnabled = true;
                consumeUpgradeItem();
                return;
            }
            menu.silkTouchEnabled = !menu.silkTouchEnabled;
        });
        silkTouchIndicator = new Indicator(leftPos + 123, topPos + 72, CommonComponents.EMPTY);
        addRenderableWidgets(silkTouchButton, silkTouchIndicator);
        buttonConfigs.put("silk_touch", new ScreenButtonConfig(silkTouchButton, SILK_TOUCH_UPGRADE_TITLE, SILK_TOUCH_UPGRADE_DESCRIPTION, () -> silkTouchButton.green, () -> !menu.silkTouchInstalled && silkTouchButton.active, AirtightHandheldDrillUtils.SILK_TOUCH_UPGRADE_ITEM));

        magnetButton = new IconButton(leftPos + 141, topPos + BACKGROUND.getHeight() - 60, CCBIcons.I_MAGNET_UPGRADE).withCallback(() -> {
            if (!menu.magnetInstalled && menu.drillInventory.getStackInSlot(AirtightHandheldDrillMenu.UPGRADE_SLOT_INDEX).is(AirtightHandheldDrillUtils.MAGNET_UPGRADE_ITEM)) {
                menu.magnetInstalled = true;
                menu.magnetEnabled = true;
                consumeUpgradeItem();
                return;
            }
            menu.magnetEnabled = !menu.magnetEnabled;
        });
        magnetIndicator = new Indicator(leftPos + 141, topPos + 72, CommonComponents.EMPTY);
        addRenderableWidgets(magnetButton, magnetIndicator);
        buttonConfigs.put("magnet", new ScreenButtonConfig(magnetButton, MAGNET_UPGRADE_TITLE, MAGNET_UPGRADE_DESCRIPTION, () -> magnetButton.green, () -> !menu.magnetInstalled && magnetButton.active, AirtightHandheldDrillUtils.MAGNET_UPGRADE_ITEM));

        conversionButton = new IconButton(leftPos + 159, topPos + BACKGROUND.getHeight() - 60, CCBIcons.I_CONVERSION_UPGRADE).withCallback(() -> {
            if (!menu.conversionInstalled && menu.drillInventory.getStackInSlot(AirtightHandheldDrillMenu.UPGRADE_SLOT_INDEX).is(AirtightHandheldDrillUtils.CONVERSION_UPGRADE_ITEM)) {
                menu.conversionInstalled = true;
                menu.conversionEnabled = true;
                consumeUpgradeItem();
                return;
            }
            menu.conversionEnabled = !menu.conversionEnabled;
        });
        conversionIndicator = new Indicator(leftPos + 159, topPos + 72, CommonComponents.EMPTY);
        addRenderableWidgets(conversionButton, conversionIndicator);
        buttonConfigs.put("conversion", new ScreenButtonConfig(conversionButton, CONVERSION_UPGRADE_TITLE, CONVERSION_UPGRADE_DESCRIPTION, () -> conversionButton.green, () -> !menu.conversionInstalled && conversionButton.active, AirtightHandheldDrillUtils.CONVERSION_UPGRADE_ITEM));

        liquidReplacementButton = new IconButton(leftPos + 177, topPos + BACKGROUND.getHeight() - 60, CCBIcons.I_LIQUID_REPLACEMENT_UPGRADE).withCallback(() -> {
            if (!menu.liquidReplacementInstalled && menu.drillInventory.getStackInSlot(AirtightHandheldDrillMenu.UPGRADE_SLOT_INDEX).is(AirtightHandheldDrillUtils.LIQUID_REPLACEMENT_UPGRADE_ITEM)) {
                menu.liquidReplacementInstalled = true;
                menu.liquidReplacementEnabled = true;
                consumeUpgradeItem();
                return;
            }
            menu.liquidReplacementEnabled = !menu.liquidReplacementEnabled;
        });
        liquidReplacementIndicator = new Indicator(leftPos + 177, topPos + 72, CommonComponents.EMPTY);
        addRenderableWidgets(liquidReplacementButton, liquidReplacementIndicator);
        buttonConfigs.put("liquid_replacement", new ScreenButtonConfig(liquidReplacementButton, LIQUID_REPLACEMENT_UPGRADE_TITLE, LIQUID_REPLACEMENT_UPGRADE_DESCRIPTION, () -> liquidReplacementButton.green, () -> !menu.liquidReplacementInstalled && liquidReplacementButton.active, AirtightHandheldDrillUtils.LIQUID_REPLACEMENT_UPGRADE_ITEM));

        IconButton confirmButton = new IconButton(leftPos +  BACKGROUND.getWidth() - 31, topPos + BACKGROUND.getHeight() - 24, AllIcons.I_CONFIRM).withCallback(() -> menu.player.closeContainer());
        addRenderableWidget(confirmButton);

        disableUpgradeButton = new IconButton(leftPos + 151, topPos + 35, CCBIcons.I_FINISHED).setActive(false);
        disableUpgradeButton.visible = false;
        addRenderableWidget(disableUpgradeButton);
    }

    private void renderTooltips() {
        for (ScreenButtonConfig buttonConfig : buttonConfigs.values()) {
            IconButton button = buttonConfig.getIconButton();
            button.setToolTip(buttonConfig.getTitle());

            List<Component> tooltips = button.getToolTip();
            Item upgradeItem = buttonConfig.getUpgradeItem();
            if (!button.active && upgradeItem != null) {
                tooltips.add(UPGRADE_NOT_INSTALLED.plainCopy().append(upgradeItem.getDescription()).withStyle(ChatFormatting.RED));
            }

            if (buttonConfig.canBeInstalled()) {
                tooltips.add(UPGRADE_CAN_BE_INSTALLED.plainCopy().withStyle(ChatFormatting.GOLD));
            }
            else if (button.active) {
                boolean isEnabled = buttonConfig.isEnabled();
                tooltips.add((isEnabled ? OPTION_ENABLED : OPTION_DISABLED).plainCopy().withStyle(isEnabled ? ChatFormatting.DARK_GREEN : ChatFormatting.RED));
            }

            boolean hasShiftDown = hasShiftDown();
            tooltips.add(CCBLang.translateDirect("gui.hold_for_description", CCBLang.translateDirect("gui.key.shift").withStyle(hasShiftDown ? ChatFormatting.WHITE : ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY));
            if (!hasShiftDown || !button.isHoveredOrFocused()) {
                continue;
            }

            tooltips.addAll(TooltipHelper.cutTextComponent(buttonConfig.getDescription(), Palette.ALL_GRAY));
        }
    }

    private void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (AirtightHandheldDrillUtils.isMouseOverSlot(mouseX, mouseY, leftPos + 16, topPos + 25)) {
            guiGraphics.renderTooltip(font, TEMPLATE_TITLE, mouseX, mouseY);
        }
        if (AirtightHandheldDrillUtils.isMouseOverSlot(mouseX, mouseY, leftPos + 16, topPos + 45)) {
            guiGraphics.renderTooltip(font, SIZE_TITLE, mouseX, mouseY);
        }
        if (AirtightHandheldDrillUtils.isMouseOverSlot(mouseX, mouseY, leftPos + 16, topPos + 65)) {
            guiGraphics.renderTooltip(font, RELATIVE_POSITION_TITLE, mouseX, mouseY);
        }
        if (AirtightHandheldDrillUtils.isMouseOverSlot(mouseX, mouseY, leftPos + 16, topPos + 85)) {
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

    private void updateStates() {
        filterButton.green = !menu.filterDisabled;
        containerButton.green = !menu.containerDisabled;
        drillAttackButton.green = !menu.drillAttackDisabled;
        outlineButton.green = !menu.outlineDisabled;

        ItemStack upgrade = menu.drillInventory.getStackInSlot(AirtightHandheldDrillMenu.UPGRADE_SLOT_INDEX);
        silkTouchButton.green = menu.silkTouchEnabled && menu.silkTouchInstalled;
        silkTouchButton.active = menu.silkTouchInstalled || upgrade.is(AirtightHandheldDrillUtils.SILK_TOUCH_UPGRADE_ITEM);
        silkTouchIndicator.state = menu.silkTouchInstalled ? menu.silkTouchEnabled ? State.GREEN : State.RED : silkTouchButton.active ? State.YELLOW : State.OFF;

        magnetButton.green = menu.magnetEnabled && menu.magnetInstalled;
        magnetButton.active = menu.magnetInstalled || upgrade.is(AirtightHandheldDrillUtils.MAGNET_UPGRADE_ITEM);
        magnetIndicator.state = menu.magnetInstalled ? menu.magnetEnabled ? State.GREEN : State.RED : magnetButton.active ? State.YELLOW : State.OFF;

        conversionButton.green = menu.conversionEnabled && menu.conversionInstalled;
        conversionButton.active = menu.conversionInstalled || upgrade.is(AirtightHandheldDrillUtils.CONVERSION_UPGRADE_ITEM);
        conversionIndicator.state = menu.conversionInstalled ? menu.conversionEnabled ? State.GREEN : State.RED : conversionButton.active ? State.YELLOW : State.OFF;

        liquidReplacementButton.green = menu.liquidReplacementEnabled && menu.liquidReplacementInstalled;
        liquidReplacementButton.active = menu.liquidReplacementInstalled || upgrade.is(AirtightHandheldDrillUtils.LIQUID_REPLACEMENT_UPGRADE_ITEM);
        liquidReplacementIndicator.state = menu.liquidReplacementInstalled ? menu.liquidReplacementEnabled ? State.GREEN : State.RED : liquidReplacementButton.active ? State.YELLOW : State.OFF;
        if (menu.silkTouchInstalled && menu.magnetInstalled && menu.conversionInstalled && menu.liquidReplacementInstalled && !disableUpgradeButton.visible) {
            disableUpgradeButton.visible = true;
        }
        if (!isRelativePositionDirty) {
            return;
        }

        isRelativePositionDirty = false;
        boolean isValid = currentMiningTemplate.getTemplate().getOffset(currentMiningSizeParams, currentDirection, currentRelativePositionParams).contains(BlockPos.ZERO);
        for (int index = 0; index < 3; index++) {
            miningRelativePositionLabels.get(index).colored(isValid ? COLOR_VALID : COLOR_INVALID);
        }
    }

    private class ScreenButtonConfig {
        private final IconButton iconButton;
        private final Component title;
        private final Component description;
        private final BooleanSupplier isEnabled;
        private final BooleanSupplier canBeInstalled;
        @Nullable
        private final Item upgradeItem;

        public ScreenButtonConfig(IconButton iconButton, Component title, Component description, BooleanSupplier isEnabled, BooleanSupplier canBeInstalled, @Nullable Item upgradeItem) {
            this.iconButton = iconButton;
            this.title = title;
            this.description = description;
            this.isEnabled = isEnabled;
            this.canBeInstalled = canBeInstalled;
            this.upgradeItem = upgradeItem;
        }

        public IconButton getIconButton() {
            return iconButton;
        }

        public Component getTitle() {
            return title;
        }

        public Component getDescription() {
            return description;
        }

        public boolean isEnabled() {
            return isEnabled.getAsBoolean();
        }

        public boolean canBeInstalled() {
            return canBeInstalled.getAsBoolean();
        }

        @Nullable
        public Item getUpgradeItem() {
            return upgradeItem;
        }
    }
}
