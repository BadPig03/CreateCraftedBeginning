package net.ty.createcraftedbeginning.api.gas;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.data.CCBLang;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class GasFilteringBehaviour extends BlockEntityBehaviour implements ValueSettingsBehaviour {
    public static final BehaviourType<GasFilteringBehaviour> TYPE = new BehaviourType<>();
    private final Predicate<ItemStack> predicate = stack -> stack.getItem() instanceof GasCanisterItem;
    protected FilterItemStack filter;
    ValueBoxTransform slotPositioning;
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

    public boolean setFilter(Direction ignored, ItemStack stack) {
        return setFilter(stack);
    }

    public boolean setFilter(@NotNull ItemStack stack) {
        ItemStack filter = stack.copy();
        if (!filter.isEmpty() && !predicate.test(filter)) {
            return false;
        }
        this.filter = FilterItemStack.of(filter);
        callback.accept(filter);
        blockEntity.setChanged();
        blockEntity.sendData();
        return true;
    }

    public ItemStack getFilter(Direction ignored) {
        return getFilter();
    }

    public ItemStack getFilter() {
        return filter.item();
    }

    public boolean test(GasStack stack) {
        return !isActive() || canGasPass(filter, stack);
    }

    private boolean canGasPass(@NotNull FilterItemStack filterItem, GasStack stack) {
        if (filterItem.isEmpty()) {
            return true;
        }
        if (!(filterItem.item().getItem() instanceof GasCanisterItem)) {
            return false;
        }

        GasStack filterGasStack = GasCanisterItem.getContent(filterItem.item());
        if (filterGasStack.isEmpty()) {
            return false;
        }
        return GasStack.isSameGas(filterGasStack, stack);
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Override
    public void read(@NotNull CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        filter = FilterItemStack.of(registries, nbt.getCompound("Filter"));
        super.read(nbt, registries, clientPacket);
    }

    @Override
    public void write(@NotNull CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        nbt.put("Filter", getFilter().saveOptional(registries));
        super.write(nbt, registries, clientPacket);
    }

    @Override
    public boolean isSafeNBT() {
        return true;
    }

    @Override
    public ItemRequirement getRequiredItems() {
        if (filter.isFilterItem()) {
            return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, filter.item());
        }

        return ItemRequirement.NONE;
    }

    @Override
    public boolean testHit(@NotNull Vec3 hit) {
        BlockState state = blockEntity.getBlockState();
        Vec3 localHit = hit.subtract(Vec3.atLowerCornerOf(blockEntity.getBlockPos()));
        return slotPositioning.testHit(getWorld(), getPos(), state, localHit);
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
        return "Filtering";
    }

    @Override
    public boolean writeToClipboard(HolderLookup.@NotNull Provider registries, CompoundTag tag, Direction side) {
        ValueSettingsBehaviour.super.writeToClipboard(registries, tag, side);
        ItemStack filter = getFilter(side);
        tag.put("Filter", filter.saveOptional(registries));
        return true;
    }

    @Override
    public boolean readFromClipboard(HolderLookup.@NotNull Provider registries, CompoundTag tag, Player player, Direction side, boolean simulate) {
        if (!mayInteract(player)) {
            return false;
        }

        boolean upstreamResult = ValueSettingsBehaviour.super.readFromClipboard(registries, tag, player, side, simulate);
        if (!tag.contains("Filter")) {
            return upstreamResult;
        }

        if (simulate || getWorld().isClientSide) {
            return true;
        }

        ItemStack copied = ItemStack.parseOptional(registries, tag.getCompound("Filter"));
        return setFilter(side, copied);
    }

    @Override
    public void onShortInteract(@NotNull Player player, InteractionHand hand, Direction side, BlockHitResult hitResult) {
        Level level = getWorld();
        BlockPos pos = getPos();
        ItemStack itemInHand = player.getItemInHand(hand);
        ItemStack toApply = itemInHand.copy();

        if (AllBlocks.MECHANICAL_ARM.isIn(toApply) || AllItems.WRENCH.isIn(toApply) || level.isClientSide()) {
            return;
        }

        if (!setFilter(side, toApply)) {
            player.displayClientMessage(CCBLang.translateDirect("logistics.filter.invalid_item").withStyle(ChatFormatting.RED), true);
            AllSoundEvents.DENY.playOnServer(player.level(), player.blockPosition(), 1, 1);
            return;
        }

        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, .25f, .1f);
    }

    @Override
    public int netId() {
        return 2;
    }

    public MutableComponent getLabel() {
        return CCBLang.translateDirect("logistics.gas_filter");
    }

    public MutableComponent getTip() {
        return CCBLang.translateDirect(filter.isEmpty() ? "logistics.filter.click_to_set" : "logistics.filter.click_to_replace");
    }

    public float getRenderDistance() {
        return AllConfigs.client().filterItemRenderDistance.getF();
    }
}
