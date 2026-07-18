package net.ty.createcraftedbeginning.content.airtights.gasfactorygauge;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelEffectPacket;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour.RequestType;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.simibubi.create.content.logistics.packagerLink.RequestPromise;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonUtils;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasLogisticsUtils;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestUtils;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasFactoryGaugeBehaviour extends FactoryPanelBehaviour {
    private static final int BOARD_MAX_VALUE = 100;
    private static final int[] ROW_MULTIPLIERS = {1, 10, 100, 1000, 10000};

    public static final int MAX_TARGET_AMOUNT = BOARD_MAX_VALUE * ROW_MULTIPLIERS[ROW_MULTIPLIERS.length - 1];

    public GasFactoryGaugeBehaviour(GasFactoryGaugeBlockEntity blockEntity, PanelSlot slot) {
        super(blockEntity, slot);
    }

    private static int toGasAmount(ValueSettings settings) {
        int row = Mth.clamp(settings.row(), 0, ROW_MULTIPLIERS.length - 1);
        int value = Mth.clamp(settings.value(), 0, BOARD_MAX_VALUE);
        return Math.min(value * ROW_MULTIPLIERS[row], MAX_TARGET_AMOUNT);
    }

    @Override
    public void onShortInteract(Player player, InteractionHand hand, Direction side, BlockHitResult hitResult) {
        if (!Create.LOGISTICS.mayInteract(network, player)) {
            player.displayClientMessage(CCBLang.translate("gui.gas_factory_gauge.protected").style(ChatFormatting.RED).component(), true);
            return;
        }

        Level level = getWorld();
        ItemStack held = player.getItemInHand(hand);
        if (!getFilter().isEmpty() || held.isEmpty()) {
            super.onShortInteract(player, hand, side, hitResult);
            return;
        }

        List<ItemStack> virtualItems = GasVirtualUtils.getVirtualItems(held);
        if (virtualItems.size() != 1) {
            if (virtualItems.isEmpty()) {
                player.displayClientMessage(CCBLang.translateDirect("gui.warnings.empty_gas_source", held.getHoverName()).withStyle(ChatFormatting.RED), true);
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

        setFilter(virtualItems.getFirst());
        resetTimerSlightly();
    }

    @Override
    public void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        activeCraftingArrangement = List.of();
        upTo = true;
        count = Mth.clamp(count, 0, MAX_TARGET_AMOUNT);
        ItemStack filter = getFilter();
        if (filter.isEmpty() || GasVirtualUtils.isVirtualItem(filter)) {
            return;
        }

        super.setFilter(ItemStack.EMPTY);
    }

    @Override
    public MutableComponent formatValue(ValueSettings value) {
        if (value.value() == 0) {
            return CCBLang.translateDirect("gui.gas_factory_gauge.inactive");
        }
        return CCBLang.text(GasAmountUtils.formatLosslessCompact(toGasAmount(value))).component();
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
        int newAmount = toGasAmount(settings);
        if (count == newAmount && upTo) {
            return;
        }

        count = newAmount;
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
        List<Component> rows = List.of(CCBLang.text("×1mB").component(), CCBLang.text("×10mB").component(), CCBLang.text("×100mB").component(), CCBLang.text("×1B").component(), CCBLang.text("×10B").component());
        return new ValueSettingsBoard(CCBLang.translateDirect("gui.gas_factory_gauge.target_amount"), BOARD_MAX_VALUE, 10, rows, new ValueSettingsFormatter(this::formatValue));
    }

    @Override
    public ValueSettings getValueSettings() {
        int amount = Mth.clamp(count, 0, MAX_TARGET_AMOUNT);
        if (amount == 0) {
            return new ValueSettings(0, 0);
        }

        for (int row = 0; row < ROW_MULTIPLIERS.length; row++) {
            int multiplier = ROW_MULTIPLIERS[row];
            if (amount > multiplier * BOARD_MAX_VALUE) {
                continue;
            }

            int displayed = Math.max(1, (amount + multiplier / 2) / multiplier);
            return new ValueSettings(row, Mth.clamp(displayed, 1, BOARD_MAX_VALUE));
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

        int inStorage = getLevelInStorage();
        int promised = getPromised();
        String storedText = GasRequestUtils.format(inStorage, false);
        if (count == 0) {
            return CCBLang.text(storedText).color(0xF1EFE8).component();
        }
        int color = satisfied ? 0xD7FFA8 : promisedSatisfied ? 0xFFCD75 : 0xFFBFA8;
        return CCBLang.text(storedText).color(color).add(CCBLang.text(promised == 0 ? "" : "⏶")).add(CCBLang.text("/").style(ChatFormatting.WHITE)).add(CCBLang.text(GasRequestUtils.format(count, false)).color(0xF1EFE8)).component();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void displayScreen(Player player) {
        if (!(player instanceof LocalPlayer)) {
            return;
        }

        ScreenOpener.open(new GasFactoryGaugeScreen(this));
    }

    @Override
    public ItemRequirement getRequiredItems() {
        return isActive() ? new ItemRequirement(ItemUseType.CONSUME, new ItemStack(CCBBlocks.GAS_FACTORY_GAUGE_BLOCK)) : ItemRequirement.NONE;
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

        IdentifiedInventory excludedInventory = packager.getIdentifiedGasInventory();
        if (excludedInventory == null) {
            return;
        }

        int available = GasLogisticsUtils.getUniqueStockOf(network, gasToken, excludedInventory);
        if (available <= 0) {
            sendGasEffect(false);
            return;
        }

        int missing = Math.max(0, count - getPromised() - getLevelInStorage());
        int cycleLimit = GasRequestUtils.toLogisticsAmount(Math.max(1, BalloonUtils.getCapacity()) * 9);
        int orderAmount = Math.min(Math.min(missing, available), cycleLimit);
        if (orderAmount <= 0) {
            return;
        }

        BigItemStack orderedGas = new BigItemStack(gasToken, orderAmount);
        PackageOrderWithCrafts order = PackageOrderWithCrafts.simple(List.of(orderedGas));
        sendGasEffect(true);
        boolean accepted = LogisticsManager.broadcastPackageRequest(network, RequestType.RESTOCK, order, excludedInventory, recipeAddress);
        if (!accepted) {
            return;
        }

        restockerPromises.add(new RequestPromise(orderedGas));
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

    private void sendGasEffect(boolean success) {
        if (!(getWorld() instanceof ServerLevel serverLevel)) {
            return;
        }

        FactoryPanelEffectPacket packet = new FactoryPanelEffectPacket(getPanelPosition(), getPanelPosition(), success);
        CatnipServices.NETWORK.sendToClientsAround(serverLevel, getPos(), 64, packet);
    }
}