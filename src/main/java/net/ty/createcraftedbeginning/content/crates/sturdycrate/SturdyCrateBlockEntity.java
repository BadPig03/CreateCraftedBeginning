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
import net.ty.createcraftedbeginning.content.crates.FilteredCrateBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SturdyCrateBlockEntity extends FilteredCrateBlockEntity {
    private CCBAdvancementBehaviour advancementBehaviour;

    public SturdyCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, () -> CCBConfig.server().crates.maxSturdyCapacity.get());
    }

    public boolean hasStoredData() {
        return SturdyCratePortableStorage.hasStoredData(this);
    }

    public void saveToItem(ItemStack crate) {
        SturdyCratePortableStorage.saveToItem(this, crate);
    }

    public void loadFromItem(ItemStack crate) {
        SturdyCratePortableStorage.loadFromItem(this, crate);
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
        if (advancementBehaviour != null && getStoredItem().is(Items.LAVA_BUCKET) && getStoredCount() >= 10000) {
            advancementBehaviour.awardPlayer(CCBAdvancements.PORTABLE_LAVA_SEA);
        }
        super.onInventoryChanged();
    }
}
