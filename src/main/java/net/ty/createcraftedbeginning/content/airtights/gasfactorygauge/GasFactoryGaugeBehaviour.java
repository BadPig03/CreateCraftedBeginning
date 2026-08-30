package net.ty.createcraftedbeginning.content.airtights.gasfactorygauge;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelEffectPacket;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.content.logistics.packagerLink.RequestPromise;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeRestockController.Status;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeRestockController.Result;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestUtils;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.client.ClientScreenBridge;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasFactoryGaugeBehaviour extends FactoryPanelBehaviour {
    private static final int BOARD_MAX_VALUE = 100;
    private static final int[] ROW_MULTIPLIERS = {1, 10, 100, 1000, 10000};

    public static final int MAX_TARGET_AMOUNT = BOARD_MAX_VALUE * ROW_MULTIPLIERS[ROW_MULTIPLIERS.length - 1];

    GasFactoryGaugeBehaviour(GasFactoryGaugeBlockEntity blockEntity, PanelSlot slot) {
        super(blockEntity, slot);
    }

    private static int toGasAmount(ValueSettings settings) {
        int row = CCBMathUtils.clampNonNegative(settings.row(), ROW_MULTIPLIERS.length - 1);
        int settingValue = CCBMathUtils.clampNonNegative(settings.value(), BOARD_MAX_VALUE);
        return Math.min(settingValue * ROW_MULTIPLIERS[row], MAX_TARGET_AMOUNT);
    }

    @Override
    public void onShortInteract(Player player, InteractionHand hand, Direction side, BlockHitResult hitResult) {
        if (!Create.LOGISTICS.mayInteract(network, player)) {
            player.displayClientMessage(CCBLang.translate("gui.gas_factory_gauge.protected").style(ChatFormatting.RED).component(), true);
            return;
        }

        Level level = getWorld();
        ItemStack heldStack = player.getItemInHand(hand);
        if (!getFilter().isEmpty() || heldStack.isEmpty()) {
            super.onShortInteract(player, hand, side, hitResult);
            return;
        }

        List<ItemStack> gasTokens = GasVirtualUtils.getVirtualItems(heldStack);
        if (gasTokens.size() != 1) {
            if (gasTokens.isEmpty()) {
                player.displayClientMessage(CCBLang.translateDirect("gui.warnings.empty_gas_source", heldStack.getHoverName()).withStyle(ChatFormatting.RED), true);
            }
            else {
                player.displayClientMessage(CCBLang.translateDirect("gui.warnings.requires_single_gas").withStyle(ChatFormatting.RED), true);
            }
            AllSoundEvents.DENY.playOnServer(player.level(), player.blockPosition(), 1, 1);
            return;
        }

        if (level.isClientSide()) {
            return;
        }

        setFilter(gasTokens.getFirst());
        resetTimerSlightly();
    }

    @Override
    public void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        activeCraftingArrangement = List.of();
        upTo = true;
        count = CCBMathUtils.clampNonNegative(count, MAX_TARGET_AMOUNT);
        ItemStack gasFilter = getFilter();
        if (gasFilter.isEmpty() || GasVirtualUtils.isVirtualItem(gasFilter)) {
            return;
        }

        super.setFilter(ItemStack.EMPTY);
    }

    @Override
    public MutableComponent formatValue(ValueSettings value) {
        if (value.value() == 0) {
            return CCBLang.translateDirect("gui.gas_factory_gauge.inactive");
        }
        return CCBLang.text(GasAmounts.formatLosslessCompact(toGasAmount(value))).component();
    }

    @Override
    public boolean setFilter(ItemStack stack) {
        if (stack.isEmpty()) {
            return super.setFilter(ItemStack.EMPTY);
        }
        return GasVirtualUtils.isVirtualItem(stack) && super.setFilter(stack.copyWithCount(1));
    }

    @Override
    public void setValueSettings(Player player, ValueSettings settings, boolean ctrlDown) {
        int targetAmount = toGasAmount(settings);
        if (count == targetAmount && upTo) {
            return;
        }

        count = targetAmount;
        upTo = true;
        panelBE().redraw = true;
        blockEntity.setChanged();
        blockEntity.sendData();
        playFeedbackSound(this);
        resetTimerSlightly();
        notifyLinkedRedstoneOutputs();
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        List<Component> rowLabels = List.of(CCBLang.text("×1mB").component(), CCBLang.text("×10mB").component(), CCBLang.text("×100mB").component(), CCBLang.text("×1B").component(), CCBLang.text("×10B").component());
        return new ValueSettingsBoard(CCBLang.translateDirect("gui.gas_factory_gauge.target_amount"), BOARD_MAX_VALUE, 10, rowLabels, new ValueSettingsFormatter(this::formatValue));
    }

    @Override
    public ValueSettings getValueSettings() {
        int targetAmount = CCBMathUtils.clampNonNegative(count, MAX_TARGET_AMOUNT);
        if (targetAmount == 0) {
            return new ValueSettings(0, 0);
        }

        for (int row = 0; row < ROW_MULTIPLIERS.length; row++) {
            int multiplier = ROW_MULTIPLIERS[row];
            if (targetAmount > multiplier * BOARD_MAX_VALUE) {
                continue;
            }

            int displayedValue = Math.max(1, (targetAmount + multiplier / 2) / multiplier);
            return new ValueSettings(row, Mth.clamp(displayedValue, 1, BOARD_MAX_VALUE));
        }
        return new ValueSettings(ROW_MULTIPLIERS.length - 1, BOARD_MAX_VALUE);
    }

    @Override
    public MutableComponent getAmountTip() {
        return CCBLang.translateDirect("gui.gas_factory_gauge.hold_to_set_amount");
    }

    @Override
    public MutableComponent getCountLabelForValueBox() {
        if (getFilter().isEmpty()) {
            return Component.empty();
        }

        if (waitingForNetwork) {
            return CCBLang.text("?").component();
        }

        int storedAmount = getLevelInStorage();
        int promisedAmount = getPromised();
        String storedText = GasRequestUtils.format(storedAmount, false);
        if (count == 0) {
            return CCBLang.text(storedText).color(0xF1EFE8).component();
        }

        int color = satisfied ? 0xD7FFA8 : promisedSatisfied ? 0xFFCD75 : 0xFFBFA8;
        return CCBLang.text(storedText).color(color).add(CCBLang.text(promisedAmount == 0 ? "" : "⏶")).add(CCBLang.text("/").style(ChatFormatting.WHITE)).add(CCBLang.text(GasRequestUtils.format(count, false)).color(0xF1EFE8)).component();
    }

    @Override
    public void displayScreen(Player player) {
        ClientScreenBridge.openGasFactoryGaugeScreen(getPanelPosition(), player);
    }

    @Override
    public ItemRequirement getRequiredItems() {
        if (!isActive()) {
            return ItemRequirement.NONE;
        }
        return new ItemRequirement(ItemUseType.CONSUME, new ItemStack(CCBBlocks.GAS_FACTORY_GAUGE_BLOCK));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return GasFactoryGaugeSetGasMenu.create(containerId, playerInventory, this);
    }

    public void performGasRestock() {
        ItemStack gasToken = getFilter();
        if (!GasVirtualUtils.isVirtualItem(gasToken) || !(panelBE() instanceof GasFactoryGaugeBlockEntity gasGauge)) {
            return;
        }

        GasPackagerBlockEntity packager = gasGauge.getRestockedPackager();
        if (packager == null || packager.getGasInventoryIdentifier() == null) {
            return;
        }

        Result restockResult = GasFactoryGaugeRestockController.request(network, gasToken, packager, count, getPromised(), getLevelInStorage(), recipeAddress);
        if (restockResult.status() != Status.NONE) {
            sendGasStatus(restockResult.status() == Status.SUCCESS);
        }
        if (restockResult.promisedGas() == null) {
            return;
        }

        restockerPromises.add(new RequestPromise(restockResult.promisedGas()));
    }

    private void notifyLinkedRedstoneOutputs() {
        if (getWorld().isClientSide()) {
            return;
        }

        for (FactoryPanelConnection connection : targetedByLinks.values()) {
            if (!getWorld().isLoaded(connection.from.pos())) {
                continue;
            }

            FactoryPanelSupportBehaviour link = linkAt(getWorld(), connection);
            if (link == null || link.isOutput()) {
                continue;
            }

            link.notifyLink();
        }
    }

    private void sendGasStatus(boolean success) {
        if (!(getWorld() instanceof ServerLevel serverLevel)) {
            return;
        }

        FactoryPanelEffectPacket effectPacket = new FactoryPanelEffectPacket(getPanelPosition(), getPanelPosition(), success);
        CatnipServices.NETWORK.sendToClientsAround(serverLevel, getPos(), 64, effectPacket);
    }
}