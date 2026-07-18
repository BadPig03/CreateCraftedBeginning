package net.ty.createcraftedbeginning.content.crates.brasscrate;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.crates.CrateItemStackHandler;
import net.ty.createcraftedbeginning.content.crates.FilteredCrateBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BrassCrateBlockEntity extends FilteredCrateBlockEntity {
    private CCBAdvancementBehaviour advancementBehaviour;

    public BrassCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, () -> CCBConfig.server().crates.maxBrassCapacity.get());
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.A_HOUSE_OF_GOLD_IN_THE_CRATE);
        behaviours.add(advancementBehaviour);
    }

    @Override
    protected void onInventoryChanged() {
        CrateItemStackHandler handler = getHandler();
        if (advancementBehaviour != null && handler.getStoredItem(0).is(Items.GOLD_INGOT) && handler.getCountInSlot(0) >= handler.getSlotLimit(0)) {
            advancementBehaviour.awardPlayer(CCBAdvancements.A_HOUSE_OF_GOLD_IN_THE_CRATE);
        }
        super.onInventoryChanged();
    }
}
