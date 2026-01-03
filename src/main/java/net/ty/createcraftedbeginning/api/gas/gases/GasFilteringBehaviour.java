package net.ty.createcraftedbeginning.api.gas.gases;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags.Items;
import net.ty.createcraftedbeginning.api.gas.cansiters.GasCanisterExecuteUtils;
import net.ty.createcraftedbeginning.api.gas.cansiters.GasCanisterQueryUtils;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterItem;
import net.ty.createcraftedbeginning.data.CCBLang;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class GasFilteringBehaviour extends BlockEntityBehaviour implements ValueSettingsBehaviour {
    public static final BehaviourType<GasFilteringBehaviour> TYPE = new BehaviourType<>();
    private static final String COMPOUND_KEY_FILTER = "Filter";
    private static final String COMPOUND_KEY_FILTERING = "Filtering";

    private final Predicate<ItemStack> predicate = stack -> stack.getItem() instanceof GasCanisterItem;
    private final ValueBoxTransform slotPositioning;

    protected FilterItemStack filter;
    private Consumer<ItemStack> callback;

    public GasFilteringBehaviour(SmartBlockEntity be, ValueBoxTransform slot) {
        super(be);
        filter = FilterItemStack.empty();
        slotPositioning = slot;
        callback = stack -> {
        };
    }

    public GasFilteringBehaviour withCallback(Consumer<ItemStack> filterCallback) {
        callback = filterCallback;
        return this;
    }

    public boolean test(GasStack stack) {
        return !isActive() || canGasPass(filter, stack);
    }

    private static boolean canGasPass(@NotNull FilterItemStack filterItem, GasStack stack) {
        if (filterItem.isEmpty()) {
            return true;
        }
        if (!(filterItem.item().getItem() instanceof GasCanisterItem)) {
            return false;
        }

        GasStack filterGasStack = GasCanisterQueryUtils.getCanisterContent(filterItem.item());
        return !filterGasStack.isEmpty() && GasStack.isSameGas(filterGasStack, stack);
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Override
    public void read(@NotNull CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        filter = FilterItemStack.of(provider, compoundTag.getCompound(COMPOUND_KEY_FILTER));
    }

    @Override
    public void write(@NotNull CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.put(COMPOUND_KEY_FILTER, getFilter().saveOptional(provider));
    }

    @Override
    public boolean isSafeNBT() {
        return true;
    }

    @Override
    public ItemRequirement getRequiredItems() {
        return filter.isFilterItem() ? new ItemRequirement(ItemUseType.CONSUME, filter.item()) : ItemRequirement.NONE;
    }

    public ItemStack getFilter() {
        return filter.item();
    }

    @Override
    public boolean testHit(@NotNull Vec3 hit) {
        return slotPositioning.testHit(getWorld(), getPos(), blockEntity.getBlockState(), hit.subtract(Vec3.atLowerCornerOf(blockEntity.getBlockPos())));
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public ValueBoxTransform getSlotPositioning() {
        return slotPositioning;
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        return null;
    }

    @Override
    public void setValueSettings(Player player, ValueSettings settings, boolean ctrlDown) {
    }

    @Override
    public ValueSettings getValueSettings() {
        return null;
    }

    @Override
    public boolean acceptsValueSettings() {
        return false;
    }

    @Override
    public String getClipboardKey() {
        return COMPOUND_KEY_FILTERING;
    }

    @Override
    public boolean writeToClipboard(@NotNull Provider provider, CompoundTag compoundTag, Direction side) {
        ValueSettingsBehaviour.super.writeToClipboard(provider, compoundTag, side);
        compoundTag.put(COMPOUND_KEY_FILTER, getFilter(side).saveOptional(provider));
        return true;
    }

    @Override
    public boolean readFromClipboard(@NotNull Provider registries, CompoundTag compoundTag, Player player, Direction side, boolean simulate) {
        if (!mayInteract(player)) {
            return false;
        }

        boolean upstreamResult = ValueSettingsBehaviour.super.readFromClipboard(registries, compoundTag, player, side, simulate);
        if (!compoundTag.contains(COMPOUND_KEY_FILTER)) {
            return upstreamResult;
        }

        return simulate || getWorld().isClientSide || setFilter(side, ItemStack.parseOptional(registries, compoundTag.getCompound(COMPOUND_KEY_FILTER)));
    }

    @Override
    public void onShortInteract(@NotNull Player player, InteractionHand hand, Direction side, BlockHitResult hitResult) {
        Level level = getWorld();
        ItemStack toApply = player.getItemInHand(hand).copy();
        if (AllBlocks.MECHANICAL_ARM.isIn(toApply) || toApply.is(Items.TOOLS_WRENCH) || level.isClientSide) {
            return;
        }

        if (!setFilter(side, toApply)) {
            GasCanisterExecuteUtils.displayCustomWarningHint(player, "gui.warnings.invalid_item", toApply.getHoverName());
            return;
        }

        level.playSound(null, getPos(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.25f, 0.1f);
    }

    @Override
    public int netId() {
        return 2;
    }

    public ItemStack getFilter(Direction ignored) {
        return getFilter();
    }

    public boolean setFilter(Direction ignored, ItemStack stack) {
        return setFilter(stack);
    }

    public boolean setFilter(@NotNull ItemStack stack) {
        ItemStack filterItem = stack.copy();
        if (!filterItem.isEmpty() && !predicate.test(filterItem)) {
            return false;
        }

        filter = FilterItemStack.of(filterItem);
        callback.accept(filterItem);
        blockEntity.setChanged();
        blockEntity.sendData();
        return true;
    }

    public MutableComponent getLabel() {
        return CCBLang.translateDirect("gui.gas_filter");
    }

    public MutableComponent getTip() {
        return CreateLang.translateDirect(filter.isEmpty() ? "logistics.filter.click_to_set" : "logistics.filter.click_to_replace");
    }

    public float getRenderDistance() {
        return AllConfigs.client().filterItemRenderDistance.getF();
    }
}
