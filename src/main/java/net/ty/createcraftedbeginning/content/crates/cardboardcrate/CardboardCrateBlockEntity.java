package net.ty.createcraftedbeginning.content.crates.cardboardcrate;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.crates.CratesBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CardboardCrateBlockEntity extends CratesBlockEntity {
    private CCBAdvancementBehaviour advancementBehaviour;

    public CardboardCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, () -> CCBConfig.server().crates.maxCardboardCapacity.get(), CardboardCrateBlockEntity::isPackage);
    }

    static boolean isPackage(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof PackageItem;
    }

    public void awardPackageDisposal() {
        if (advancementBehaviour == null) {
            return;
        }

        advancementBehaviour.awardPlayer(CCBAdvancements.CUT_FROM_THE_SAME_CARDBOARD);
    }

    public void awardStoredPackageDisposal() {
        if (getStoredCount() <= 0 || !isPackage(getStoredItem())) {
            return;
        }

        awardPackageDisposal();
    }

    @Override
    protected void onTrackedItemDiscarded() {
        awardPackageDisposal();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.CUT_FROM_THE_SAME_CARDBOARD);
        behaviours.add(advancementBehaviour);
    }
}
