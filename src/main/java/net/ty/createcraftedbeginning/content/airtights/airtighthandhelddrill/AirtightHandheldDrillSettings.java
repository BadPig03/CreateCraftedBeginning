package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill;

import com.simibubi.create.content.logistics.filter.FilterItem;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.templates.AirtightHandheldDrillMiningTemplates;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightHandheldDrillSettings {
    private AirtightHandheldDrillSettings() {
    }

    public static int @NotNull [] getMiningSizeParams(ItemStack drill) {
        BlockPos miningSize = drill.getOrDefault(CCBDataComponents.DRILL_MINING_SIZE, new BlockPos(1, 1, 1));
        return new int[]{miningSize.getX(), miningSize.getY(), miningSize.getZ()};
    }

    public static int @NotNull [] getRelativePositionParams(ItemStack drill) {
        BlockPos relativePosition = drill.getOrDefault(CCBDataComponents.DRILL_MINING_RELATIVE_POSITION, new BlockPos(0, 0, 0));
        return new int[]{relativePosition.getX(), relativePosition.getY(), relativePosition.getZ()};
    }

    public static Direction getMiningDirection(ItemStack drill) {
        return drill.getOrDefault(CCBDataComponents.DRILL_MINING_DIRECTION, Direction.NORTH);
    }

    static AirtightHandheldDrillMiningTemplates getMiningTemplate(ItemStack drill) {
        return drill.getOrDefault(CCBDataComponents.DRILL_MINING_TEMPLATE, AirtightHandheldDrillMiningTemplates.CUBOID);
    }

    static boolean isRelativePositionValid(AirtightHandheldDrillMiningTemplates template, int[] miningSize, Direction miningDirection, int[] relativePosition) {
        return !template.getTemplate().usesSpatialParameters() || template.getTemplate().getOffset(miningSize, miningDirection, relativePosition).contains(BlockPos.ZERO);
    }

    static boolean isValidFilter(ItemStack filterStack) {
        Item filterItem = filterStack.getItem();
        return filterItem instanceof FilterItem || filterItem instanceof BlockItem;
    }
}
