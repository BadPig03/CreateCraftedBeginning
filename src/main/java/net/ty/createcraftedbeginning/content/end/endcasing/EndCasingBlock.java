package net.ty.createcraftedbeginning.content.end.endcasing;

import com.simibubi.create.content.decoration.encasing.CasingBlock;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.ty.createcraftedbeginning.content.end.endincinerationblower.EndIncinerationBlowerPlacementHelper;
import net.ty.createcraftedbeginning.content.end.endsculksilencer.EndSculkSilencerPlacementHelper;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EndCasingBlock extends CasingBlock {
    private static final Map<Item, Integer> PLACEMENT_HELPERS = new HashMap<>();

    public EndCasingBlock(Properties properties) {
        super(properties);
    }

    public static void registerPlacementHelpers() {
        PLACEMENT_HELPERS.put(CCBBlocks.END_INCINERATION_BLOWER_BLOCK.asItem(), PlacementHelpers.register(new EndIncinerationBlowerPlacementHelper()));
        PLACEMENT_HELPERS.put(CCBBlocks.END_SCULK_SILENCER_BLOCK.asItem(), PlacementHelpers.register(new EndSculkSilencerPlacementHelper()));
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return 15;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        for (Entry<Item, Integer> entry : PLACEMENT_HELPERS.entrySet()) {
            if (!stack.is(entry.getKey())) {
                continue;
            }

            IPlacementHelper placementHelper = PlacementHelpers.get(entry.getValue());
            if (!placementHelper.matchesItem(stack)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            return placementHelper.getOffset(player, level, state, pos, hitResult).placeInWorld(level, (BlockItem) stack.getItem(), player, hand, hitResult);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
