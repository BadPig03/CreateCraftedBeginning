package net.ty.createcraftedbeginning.content.airtights.gas.behaviours;

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
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterUtils;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.CCBClientBridge;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasFilteringBehaviour extends BlockEntityBehaviour implements ValueSettingsBehaviour {
    public static final BehaviourType<GasFilteringBehaviour> TYPE = new BehaviourType<>();

    private static final String COMPOUND_KEY_FILTER = "Filter";
    private static final String COMPOUND_KEY_FILTERING = "Filtering";

    private final Predicate<ItemStack> predicate = GasFilterUtils::isFilter;
    private final ValueBoxTransform slotPositioning;

    private FilterItemStack filter;
    private Predicate<GasStack> compiledFilter;
    private Consumer<ItemStack> callback;

    public GasFilteringBehaviour(SmartBlockEntity be, ValueBoxTransform slot) {
        super(be);
        filter = FilterItemStack.empty();
        compiledFilter = GasFilterUtils.compile(ItemStack.EMPTY);
        slotPositioning = slot;
        callback = ignoredStack -> {};
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Override
    public void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        FilterItemStack savedFilter = FilterItemStack.of(provider, compoundTag.getCompound(COMPOUND_KEY_FILTER));
        filter = FilterItemStack.of(GasFilterUtils.normalizeStack(savedFilter.item()));
        rebuildCompiledFilter();
    }

    @Override
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
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
    public boolean testHit(Vec3 hit) {
        Vec3 localHit = hit.subtract(Vec3.atLowerCornerOf(blockEntity.getBlockPos()));
        return slotPositioning.testHit(getWorld(), getPos(), blockEntity.getBlockState(), localHit);
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
    public @Nullable ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        return null;
    }

    @Override
    public void setValueSettings(Player player, ValueSettings settings, boolean ctrlDown) {
    }

    @Override
    public @Nullable ValueSettings getValueSettings() {
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
    public boolean writeToClipboard(Provider provider, CompoundTag compoundTag, Direction side) {
        ValueSettingsBehaviour.super.writeToClipboard(provider, compoundTag, side);
        compoundTag.put(COMPOUND_KEY_FILTER, getFilter(side).saveOptional(provider));
        return true;
    }

    @Override
    public boolean readFromClipboard(Provider registries, CompoundTag compoundTag, Player player, Direction side, boolean simulate) {
        if (!mayInteract(player)) {
            return false;
        }

        boolean upstreamResult = ValueSettingsBehaviour.super.readFromClipboard(registries, compoundTag, player, side, simulate);
        if (!compoundTag.contains(COMPOUND_KEY_FILTER)) {
            return upstreamResult;
        }

        if (simulate || getWorld().isClientSide) {
            return true;
        }

        ItemStack filterItem = ItemStack.parseOptional(registries, compoundTag.getCompound(COMPOUND_KEY_FILTER));
        return setFilter(side, filterItem);
    }

    @Override
    public void onShortInteract(Player player, InteractionHand hand, Direction side, BlockHitResult hitResult) {
        Level level = getWorld();
        if (level.isClientSide) {
            return;
        }

        ItemStack heldItem = player.getItemInHand(hand).copy();
        if (AllBlocks.MECHANICAL_ARM.isIn(heldItem) || heldItem.is(Items.TOOLS_WRENCH)) {
            return;
        }

        if (!setFilter(side, heldItem)) {
            GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.invalid_item", heldItem.getHoverName());
            return;
        }

        level.playSound(null, getPos(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.25f, 0.1f);
    }

    @Override
    public int netId() {
        return 2;
    }

    public boolean test(GasStack gasStack) {
        return compiledFilter.test(gasStack);
    }

    @SuppressWarnings("unused")
    private GasFilteringBehaviour withCallback(Consumer<ItemStack> filterCallback) {
        callback = filterCallback;
        return this;
    }

    private void rebuildCompiledFilter() {
        compiledFilter = GasFilterUtils.compile(filter.item());
    }

    private ItemStack getFilter(Direction ignoredSide) {
        return getFilter();
    }

    private boolean setFilter(Direction ignoredSide, ItemStack filterStack) {
        return setFilter(filterStack);
    }

    private boolean setFilter(ItemStack filterStack) {
        ItemStack filterItem = GasFilterUtils.normalizeStack(filterStack);
        if (!filterItem.isEmpty() && !predicate.test(filterItem)) {
            return false;
        }

        if (ItemStack.isSameItemSameComponents(filter.item(), filterItem)) {
            return true;
        }

        filter = FilterItemStack.of(filterItem);
        rebuildCompiledFilter();
        callback.accept(filterItem);
        blockEntity.setChanged();
        blockEntity.sendData();
        return true;
    }

    public MutableComponent getLabel() {
        return CCBLang.translateDirect("gui.gas_filter");
    }

    public MutableComponent getTip() {
        String translationKey = filter.isEmpty() ? "gui.filter.click_to_set" : "gui.filter.click_to_replace";
        return CCBLang.translateDirect(translationKey);
    }

    public float getRenderDistance() {
        return CCBClientBridge.getFilterItemRenderDistance();
    }
}
