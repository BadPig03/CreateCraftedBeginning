package net.ty.createcraftedbeginning.content.crates;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.data.CCBLang;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class CratesBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, ThresholdSwitchObservable {
    private static final String COMPOUND_KEY_INVENTORY = "Inventory";

    private final CrateItemStackHandler handler;

    protected CratesBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, IntSupplier maxCountSupplier) {
        this(type, pos, state, maxCountSupplier, null);
    }

    protected CratesBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, IntSupplier maxCountSupplier, @Nullable Predicate<ItemStack> trackedDiscardPredicate) {
        super(type, pos, state);
        if (trackedDiscardPredicate == null) {
            handler = new CrateItemStackHandler(maxCountSupplier, this::canStoreItem, this::onInventoryChanged);
            return;
        }

        handler = new DiscardingCrateItemStackHandler(maxCountSupplier, this::canStoreItem, this::onInventoryChanged, trackedDiscardPredicate, this::onTrackedItemDiscarded);
    }

    public static <T extends CratesBlockEntity> void registerCapabilities(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(ItemHandler.BLOCK, type, (be, context) -> be.getHandler());
    }

    public final CrateItemStackHandler getHandler() {
        return handler;
    }

    public final void setStoredItems(ItemStack content, int count) {
        handler.setStoredItems(0, content, count);
    }

    protected boolean canStoreItem(ItemStack stack) {
        return true;
    }

    protected void onInventoryChanged() {
        notifyUpdate();
    }

    protected void onTrackedItemDiscarded() {
    }

    @Override
    public int getMaxValue() {
        return handler.getSlotLimit(0);
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        return handler.getCountInSlot(0);
    }

    @Override
    public MutableComponent format(int value) {
        return CCBLang.text(value + " ").add(CCBLang.translate("gui.threshold.items")).component();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CCBLang.translate("gui.crates.header").forGoggles(tooltip);
        ItemStack content = handler.getStoredItem(0);
        int count = handler.getCountInSlot(0);
        int maxCount = handler.getSlotLimit(0);
        if (content.isEmpty() || count == 0) {
            CCBLang.translate("gui.crates.capacity").style(ChatFormatting.GRAY).add(CCBLang.number(maxCount).style(ChatFormatting.GOLD)).forGoggles(tooltip, 1);
            return true;
        }

        CCBLang.itemName(content).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        CCBLang.number(count).style(ChatFormatting.GOLD).add(CCBLang.text(" / ").style(ChatFormatting.GRAY)).add(CCBLang.number(maxCount).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        return true;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.put(COMPOUND_KEY_INVENTORY, handler.serializeNBT(provider));
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (!compoundTag.contains(COMPOUND_KEY_INVENTORY)) {
            return;
        }

        handler.deserializeNBT(provider, compoundTag.getCompound(COMPOUND_KEY_INVENTORY));
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }
}
