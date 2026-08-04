package net.ty.createcraftedbeginning.content.crates.sturdycrate;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.crates.CrateItemStackHandler;
import net.ty.createcraftedbeginning.content.crates.FilteredCrateBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SturdyCrateBlockEntity extends FilteredCrateBlockEntity {
    private CCBAdvancementBehaviour advancementBehaviour;

    public SturdyCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, () -> CCBConfig.server().crates.maxSturdyCapacity.get());
    }

    public boolean isInventoryEmpty() {
        return getHandler().getCountInSlot(0) == 0;
    }

    public boolean hasStoredData() {
        return !isInventoryEmpty() || !getFilterItem().isEmpty();
    }

    public void saveToItem(ItemStack crate) {
        CrateItemStackHandler handler = getHandler();
        SturdyCrateContents contents = new SturdyCrateContents(handler.getStoredItem(0), handler.getCountInSlot(0), getFilterItem());
        if (contents.hasData()) {
            crate.set(CCBDataComponents.STURDY_CRATE_CONTENTS, contents);
            return;
        }

        crate.remove(CCBDataComponents.STURDY_CRATE_CONTENTS);
    }

    public void loadFromItem(ItemStack crate) {
        SturdyCrateContents contents = crate.get(CCBDataComponents.STURDY_CRATE_CONTENTS);
        if (contents == null) {
            contents = SturdyCrateContents.empty();
        }

        setStoredItems(contents.content(), contents.count());
        setFilterItem(contents.filterItem());
    }

    @Override
    protected boolean canStoreItem(ItemStack stack) {
        return stack.canFitInsideContainerItems() && super.canStoreItem(stack);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.PORTABLE_LAVA_SEA);
        behaviours.add(advancementBehaviour);
    }

    @Override
    protected void onInventoryChanged() {
        CrateItemStackHandler handler = getHandler();
        if (advancementBehaviour != null && handler.isStoredItem(0, Items.LAVA_BUCKET) && handler.getCountInSlot(0) >= 10000) {
            advancementBehaviour.awardPlayer(CCBAdvancements.PORTABLE_LAVA_SEA);
        }
        super.onInventoryChanged();
    }
}
