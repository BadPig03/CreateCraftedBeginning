package net.ty.createcraftedbeginning.content.airtights.gasfactorygauge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.AddressEditBox;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConfigurationPacket;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnectionHandler;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.client.GasFactoryGaugeClientUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonStyleUtils;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestUtils;
import net.ty.createcraftedbeginning.client.CCBGUITextures;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GasFactoryGaugeScreen extends AbstractSimiScreen {
    private final boolean restocker;
    private final GasFactoryGaugeBehaviour behaviour;

    private AddressEditBox addressBox;
    private ScrollInput promiseExpiration;
    private boolean sendReset;
    private boolean sendRedstoneReset;

    private BigItemStack outputConfig;
    private List<BigItemStack> inputConfig;
    private List<FactoryPanelConnection> connections;

    public GasFactoryGaugeScreen(GasFactoryGaugeBehaviour behaviour) {
        this.behaviour = behaviour;
        minecraft = Minecraft.getInstance();
        restocker = behaviour.panelBE().restocker;
        updateConfigs();
    }

    private static void addGasScrollTooltips(List<Component> tooltips) {
        addScrollTooltip(tooltips, "gui.gas_virtual_item.scroll", GasAmountUtils.formatPrecise(GasRequestUtils.getScrollStep()));
        addScrollTooltip(tooltips, "gui.gas_virtual_item.shift_to_scroll", GasAmountUtils.formatPrecise(GasRequestUtils.getShiftStep()));
        addScrollTooltip(tooltips, "gui.gas_virtual_item.alt_to_scroll", GasAmountUtils.formatPrecise(GasRequestUtils.getAltStep()));
        addScrollTooltip(tooltips, "gui.gas_virtual_item.ctrl_to_scroll", GasAmountUtils.formatPrecise(GasRequestUtils.getCtrlStep()));
    }

    private static void addItemScrollTooltips(List<Component> tooltips) {
        addScrollTooltip(tooltips, "gui.gas_virtual_item.scroll", GasRequestUtils.getScrollStep());
        addScrollTooltip(tooltips, "gui.gas_virtual_item.shift_to_scroll", GasRequestUtils.getShiftStep());
        addScrollTooltip(tooltips, "gui.gas_virtual_item.alt_to_scroll", GasRequestUtils.getAltStep());
        addScrollTooltip(tooltips, "gui.gas_virtual_item.ctrl_to_scroll", GasRequestUtils.getCtrlStep());
    }

    private static void addScrollTooltip(List<Component> tooltips, String key, Object amount) {
        tooltips.add(CCBLang.translate(key, amount).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
    }

    private static void addActionTooltip(List<Component> tooltips, String key) {
        tooltips.add(CCBLang.translate(key).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
    }

    private void updateConfigs() {
        if (minecraft == null) {
            return;
        }

        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        connections = new ArrayList<>(behaviour.targetedBy.values());
        outputConfig = new BigItemStack(behaviour.getFilter(), behaviour.recipeOutput);
        inputConfig = connections.stream().map(connection -> {
            FactoryPanelBehaviour inputBehaviour = FactoryPanelBehaviour.at(level, connection.from);
            return inputBehaviour == null ? new BigItemStack(ItemStack.EMPTY, 0) : new BigItemStack(inputBehaviour.getFilter(), connection.amount);
        }).toList();
    }

    @Override
    protected void init() {
        if (minecraft == null) {
            return;
        }

        CCBGUITextures background = restocker ? CCBGUITextures.GAS_FACTORY_GAUGE_RESTOCK : CCBGUITextures.GAS_FACTORY_GAUGE_RECIPE;
        int sizeX = background.getWidth();
        int sizeY = background.getHeight() - 16;
        setWindowSize(sizeX, sizeY);
        super.init();
        clearWidgets();

        int x = guiLeft;
        int y = guiTop;
        if (addressBox == null) {
            addressBox = new AddressEditBox(this, new NoShadowFontWrapper(font), x + 36, y + windowHeight - 51 + 16, 108, 10, false, behaviour.getFrogAddress());
            addressBox.setValue(behaviour.recipeAddress);
            addressBox.setTextColor(0x555555);
        }
        addressBox.setX(x + 36);
        addressBox.setY(y + windowHeight - 51 + 16);
        addRenderableWidget(addressBox);

        IconButton confirmButton = new IconButton(x + sizeX - 33, y + sizeY - 25 + 16, CCBIcons.I_CONFIRM);
        confirmButton.withCallback(() -> minecraft.setScreen(null));
        confirmButton.setToolTip(CCBLang.translateDirect("gui.gas_factory_gauge.save_and_close"));
        addRenderableWidget(confirmButton);

        IconButton deleteButton = new IconButton(x + sizeX - 55, y + sizeY - 25 + 16, CCBIcons.I_TRASH);
        deleteButton.withCallback(() -> {
            sendReset = true;
            minecraft.setScreen(null);
        });
        deleteButton.setToolTip(CCBLang.translateDirect("gui.gas_factory_gauge.reset"));
        addRenderableWidget(deleteButton);

        promiseExpiration = new ScrollInput(x + 97, y + windowHeight - 24 + 16, 28, 16).withRange(-1, 31).titled(CCBLang.translateDirect("gui.gas_factory_gauge.promises_expire_title")).setState(behaviour.promiseClearingInterval);
        addRenderableWidget(promiseExpiration);
        if (restocker) {
            return;
        }

        IconButton newInputButton = new IconButton(x + 31, y + 35, CCBIcons.I_ADD);
        newInputButton.withCallback(() -> {
            FactoryPanelConnectionHandler.startConnection(behaviour);
            minecraft.setScreen(null);
        });
        newInputButton.setToolTip(CCBLang.translateDirect("gui.gas_factory_gauge.connect_input"));
        addRenderableWidget(newInputButton);

        IconButton relocateButton = new IconButton(x + 31, y + 59, CCBIcons.I_MOVE_GAUGE);
        relocateButton.withCallback(() -> {
            FactoryPanelConnectionHandler.startRelocating(behaviour);
            minecraft.setScreen(null);
        });
        relocateButton.setToolTip(CCBLang.translateDirect("gui.gas_factory_gauge.relocate"));
        addRenderableWidget(relocateButton);
    }

    @Override
    public void tick() {
        super.tick();
        if (inputConfig.size() != behaviour.targetedBy.size()) {
            updateConfigs();
            init();
        }
        addressBox.tick();
        if (promiseExpiration.getState() == -1) {
            promiseExpiration.titled(CCBLang.translateDirect("gui.gas_factory_gauge.promises_do_not_expire"));
            return;
        }

        promiseExpiration.titled(CCBLang.translateDirect("gui.gas_factory_gauge.promises_expire_title"));
    }

    @Override
    protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x = guiLeft;
        int y = guiTop;

        CCBGUITextures background = restocker ? CCBGUITextures.GAS_FACTORY_GAUGE_RESTOCK : CCBGUITextures.GAS_FACTORY_GAUGE_RECIPE;
        background.render(graphics, x, y);

        renderOutputs(graphics, mouseX, mouseY, renderInputs(graphics, mouseX, mouseY), x, y);

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 10);

        if (addressBox.isHovered() && !addressBox.isFocused()) {
            renderAddressBoxTooltip(graphics, mouseX, mouseY);
        }
        Component title = CCBLang.translateDirect(restocker ? "gui.gas_factory_gauge.title_as_restocker" : "gui.gas_factory_gauge.title_as_recipe");
        graphics.drawString(font, title, x + 97 - font.width(title) / 2, y + 4, 0x3D3C48, false);

        poseStack.pushPose();
        poseStack.translate(0, restocker ? -16 : 60, 0);
        GuiGameElement.of(new ItemStack(CCBBlocks.GAS_FACTORY_GAUGE_BLOCK)).scale(4).at(0, 0, -200).render(graphics, x + 195, y + 55);
        if (!behaviour.getFilter().isEmpty()) {
            GuiGameElement.of(behaviour.getFilter()).scale(1.625).at(0, 0, 100).render(graphics, x + 214, y + 68);
        }
        poseStack.popPose();

        if (!behaviour.targetedByLinks.isEmpty()) {
            renderLinks(graphics, mouseX, mouseY, x, y);
        }
        renderPromises(graphics, mouseX, mouseY, x, y);
        poseStack.popPose();
    }

    private void renderPromises(GuiGraphics graphics, int mouseX, int mouseY, int x, int y) {
        int state = promiseExpiration.getState();
        MutableComponent text = CCBLang.text(state == -1 ? " /" : state == 0 ? "30s" : state + "m").component();
        graphics.drawString(font, text, promiseExpiration.getX() + 3, promiseExpiration.getY() + 4, 0xFFEEEEEE, true);

        ItemStack balloon = BalloonStyleUtils.getDefaultBalloon();
        int itemX = x + 68;
        int itemY = y + windowHeight - 8;
        int promised = behaviour.getPromised();
        graphics.renderItem(balloon, itemX, itemY);
        graphics.renderItemDecorations(font, balloon, itemX, itemY, GasRequestUtils.format(promised, false));
        if (mouseX < itemX || mouseX >= itemX + 16 || mouseY < itemY || mouseY >= itemY + 16) {
            return;
        }

        List<Component> tooltips = new ArrayList<>();
        if (promised == 0) {
            tooltips.add(CCBLang.translate("gui.gas_factory_gauge.no_open_promises").color(ScrollInput.HEADER_RGB).component());
            tooltips.add(CCBLang.translate(restocker ? "gui.gas_factory_gauge.restocker_promises_tip" : "gui.gas_factory_gauge.recipe_promises_tip").style(ChatFormatting.GRAY).component());
            tooltips.add(CCBLang.translate(restocker ? "gui.gas_factory_gauge.restocker_promises_tip_1" : "gui.gas_factory_gauge.recipe_promises_tip_1").style(ChatFormatting.GRAY).component());
            tooltips.add(CCBLang.translate("gui.gas_factory_gauge.promise_prevents_oversending").style(ChatFormatting.GRAY).component());
        }
        else {
            tooltips.add(CCBLang.translate("gui.gas_factory_gauge.promised_items").color(ScrollInput.HEADER_RGB).component());
            ItemStack filter = behaviour.getFilter();
            String filterName = filter.getHoverName().getString();
            BigItemStack promisedGas = new BigItemStack(filter, promised);
            tooltips.add(CCBLang.text(filterName + ' ' + GasFactoryGaugeClientUtils.formatPrecise(promisedGas)).component());
            addActionTooltip(tooltips, "gui.gas_factory_gauge.left_click_reset");
        }
        graphics.renderComponentTooltip(font, tooltips, mouseX, mouseY);
    }

    private void renderLinks(GuiGraphics graphics, int mouseX, int mouseY, int x, int y) {
        int itemX = x + 9;
        int itemY = y + windowHeight - 24;
        AllGuiTextures.FROGPORT_SLOT.render(graphics, itemX - 1, itemY - 1);
        graphics.renderItem(new ItemStack(AllBlocks.REDSTONE_LINK), itemX, itemY);
        if (mouseX < itemX || mouseX >= itemX + 16 || mouseY < itemY || mouseY >= itemY + 16) {
            return;
        }

        List<Component> tooltips = new ArrayList<>();
        tooltips.add(CCBLang.translate("gui.gas_factory_gauge.has_link_connections").color(ScrollInput.HEADER_RGB).component());
        addActionTooltip(tooltips, "gui.gas_factory_gauge.left_click_disconnect");
        graphics.renderComponentTooltip(font, tooltips, mouseX, mouseY);
    }

    private void renderOutputs(GuiGraphics graphics, int mouseX, int mouseY, int slot, int x, int y) {
        if (restocker) {
            renderInputItem(graphics, slot, new BigItemStack(behaviour.getFilter(), 1), mouseX, mouseY);
            return;
        }

        int outputX = x + 160;
        int outputY = y + 48;
        graphics.renderItem(outputConfig.stack, outputX, outputY);
        graphics.renderItemDecorations(font, behaviour.getFilter(), outputX, outputY, GasFactoryGaugeClientUtils.format(outputConfig, false));
        if (mouseX <= outputX || mouseX > outputX + 18 || mouseY <= outputY || mouseY > outputY + 18) {
            return;
        }

        List<Component> tooltips = new ArrayList<>();
        tooltips.add(CCBLang.translate("gui.gas_factory_gauge.expected_output", CCBLang.itemName(outputConfig.stack).add(CCBLang.text(' ' + GasFactoryGaugeClientUtils.formatPrecise(outputConfig)))).color(ScrollInput.HEADER_RGB).component());
        tooltips.add(CCBLang.translate("gui.gas_factory_gauge.expected_output_tip").style(ChatFormatting.GRAY).component());
        tooltips.add(CCBLang.translate("gui.gas_factory_gauge.expected_output_tip_1").style(ChatFormatting.GRAY).component());
        addGasScrollTooltips(tooltips);
        graphics.renderComponentTooltip(font, tooltips, mouseX, mouseY);
    }

    private int renderInputs(GuiGraphics graphics, int mouseX, int mouseY) {
        int slot = 0;
        for (BigItemStack itemStack : inputConfig) {
            renderInputItem(graphics, slot++, itemStack, mouseX, mouseY);
        }
        if (!inputConfig.isEmpty()) {
            return slot;
        }

        int inputX = guiLeft + (restocker ? 88 : 68 + slot % 3 * 20);
        int inputY = guiTop + (restocker ? 12 : 28) + slot / 3 * 20;
        if (restocker || mouseY <= inputY || mouseY >= inputY + 60 || mouseX <= inputX || mouseX >= inputX + 60) {
            return slot;
        }

        List<Component> tooltips = new ArrayList<>();
        tooltips.add(CCBLang.translate("gui.gas_factory_gauge.unconfigured_input").color(ScrollInput.HEADER_RGB).component());
        tooltips.add(CCBLang.translate("gui.gas_factory_gauge.unconfigured_input_tip").style(ChatFormatting.GRAY).component());
        tooltips.add(CCBLang.translate("gui.gas_factory_gauge.unconfigured_input_tip_1").style(ChatFormatting.GRAY).component());
        graphics.renderComponentTooltip(font, tooltips, mouseX, mouseY);
        return slot;
    }

    private void renderInputItem(GuiGraphics graphics, int slot, BigItemStack entry, int mouseX, int mouseY) {
        int inputX = guiLeft + (restocker ? 88 : 68 + slot % 3 * 20);
        int inputY = guiTop + (restocker ? 33 : 28) + slot / 3 * 20;
        graphics.renderItem(entry.stack, inputX, inputY);
        if (!restocker && !entry.stack.isEmpty()) {
            graphics.renderItemDecorations(font, entry.stack, inputX, inputY, GasFactoryGaugeClientUtils.format(entry, false));
        }
        if (mouseX < inputX - 2 || mouseX >= inputX - 2 + 20 || mouseY < inputY - 2 || mouseY >= inputY - 2 + 20) {
            return;
        }

        List<Component> tooltips = new ArrayList<>();
        if (entry.stack.isEmpty()) {
            tooltips.add(CCBLang.translate("gui.gas_factory_gauge.empty_panel").color(ScrollInput.HEADER_RGB).component());
            addActionTooltip(tooltips, "gui.gas_factory_gauge.left_click_disconnect");
            graphics.renderComponentTooltip(font, tooltips, mouseX, mouseY);
            return;
        }

        if (restocker) {
            tooltips.add(CCBLang.translate("gui.gas_factory_gauge.sending_item", CCBLang.itemName(entry.stack)).color(ScrollInput.HEADER_RGB).component());
            tooltips.add(CCBLang.translate("gui.gas_factory_gauge.sending_item_tip").style(ChatFormatting.GRAY).component());
            tooltips.add(CCBLang.translate("gui.gas_factory_gauge.sending_item_tip_1").style(ChatFormatting.GRAY).component());
            graphics.renderComponentTooltip(font, tooltips, mouseX, mouseY);
            return;
        }

        tooltips.add(CCBLang.translate("gui.gas_factory_gauge.sending_item", CCBLang.itemName(entry.stack).add(CCBLang.text(' ' + GasFactoryGaugeClientUtils.format(entry, true)))).color(ScrollInput.HEADER_RGB).component());
        addItemScrollTooltips(tooltips);
        addActionTooltip(tooltips, "gui.gas_factory_gauge.left_click_disconnect");
        graphics.renderComponentTooltip(font, tooltips, mouseX, mouseY);
    }

    private void renderAddressBoxTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Component> tooltips = new ArrayList<>();
        if (!addressBox.getValue().isBlank()) {
            tooltips.add(CCBLang.translate(restocker ? "gui.gas_factory_gauge.restocker_address_given" : "gui.gas_factory_gauge.recipe_address_given").color(ScrollInput.HEADER_RGB).component());
            tooltips.add(CCBLang.text('\'' + addressBox.getValue() + '\'').style(ChatFormatting.GRAY).component());
            graphics.renderComponentTooltip(font, tooltips, mouseX, mouseY);
            return;
        }

        if (restocker) {
            tooltips.add(CCBLang.translate("gui.gas_factory_gauge.restocker_address").color(ScrollInput.HEADER_RGB).component());
            tooltips.add(CCBLang.translate("gui.gas_factory_gauge.restocker_address_tip").style(ChatFormatting.GRAY).component());
            tooltips.add(CCBLang.translate("gui.gas_factory_gauge.restocker_address_tip_1").style(ChatFormatting.GRAY).component());
            addActionTooltip(tooltips, "gui.gas_factory_gauge.left_mouse_to_edit");
            graphics.renderComponentTooltip(font, tooltips, mouseX, mouseY);
            return;
        }

        tooltips.add(CCBLang.translate("gui.gas_factory_gauge.recipe_address").color(ScrollInput.HEADER_RGB).component());
        tooltips.add(CCBLang.translate("gui.gas_factory_gauge.recipe_address_tip").style(ChatFormatting.GRAY).component());
        tooltips.add(CCBLang.translate("gui.gas_factory_gauge.recipe_address_tip_1").style(ChatFormatting.GRAY).component());
        addActionTooltip(tooltips, "gui.gas_factory_gauge.left_mouse_to_edit");
        graphics.renderComponentTooltip(font, tooltips, mouseX, mouseY);
    }

    @SuppressWarnings("ConstantValue")
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        GuiEventListener focused = getFocused();
        if (focused != null && !focused.isMouseOver(mouseX, mouseY)) {
            setFocused(null);
        }

        int x = guiLeft;
        int itemX = x + 68;
        int itemY = guiTop + windowHeight - 24;
        if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
            sendIt(true);
            playButtonSound();
            return true;
        }

        itemX = x + 9;
        if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
            sendRedstoneReset = true;
            sendIt(false);
            playButtonSound();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int x = guiLeft;
        int y = guiTop;
        if (addressBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }

        for (int i = 0; i < inputConfig.size(); i++) {
            int inputX = x + 68 + i % 3 * 20;
            int inputY = y + 26 + i / 3 * 20;
            if (mouseX < inputX || mouseX >= inputX + 16 || mouseY < inputY || mouseY >= inputY + 16) {
                continue;
            }

            BigItemStack itemStack = inputConfig.get(i);
            if (itemStack.stack.isEmpty()) {
                return true;
            }

            GasFactoryGaugeClientUtils.adjustAmount(itemStack, scrollY);
            return true;
        }

        if (restocker) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int outputX = x + 160;
        int outputY = y + 48;
        if (mouseX < outputX || mouseX >= outputX + 16 || mouseY < outputY || mouseY >= outputY + 16) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        GasFactoryGaugeClientUtils.adjustAmount(outputConfig, scrollY);
        return true;
    }

    @Override
    public void removed() {
        sendIt(false);
        super.removed();
    }

    private void playButtonSound() {
        if (minecraft == null) {
            return;
        }

        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1, 0.25f));
    }

    private void sendIt(boolean clearPromises) {
        FactoryPanelConfigurationPacket packet = new FactoryPanelConfigurationPacket(behaviour.getPanelPosition(), addressBox.getValue(), collectInputAmounts(), List.of(), outputConfig.count, promiseExpiration.getState(), null, clearPromises, sendReset, sendRedstoneReset);
        CatnipServices.NETWORK.sendToServer(packet);
    }

    private Map<FactoryPanelPosition, Integer> collectInputAmounts() {
        Map<FactoryPanelPosition, Integer> inputs = new HashMap<>();
        if (inputConfig.size() != connections.size()) {
            return inputs;
        }

        for (int i = 0; i < inputConfig.size(); i++) {
            inputs.put(connections.get(i).from, inputConfig.get(i).count);
        }
        return inputs;
    }
}
