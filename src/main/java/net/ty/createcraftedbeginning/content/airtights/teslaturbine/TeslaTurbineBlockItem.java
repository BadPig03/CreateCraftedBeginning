package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.data.CCBLang;
import org.jetbrains.annotations.NotNull;

public class TeslaTurbineBlockItem extends BlockItem {
    private static final int COLOR_RED = 0xFFFF5D6C;
    private static final String TESLA_TURBINE = "tesla_turbine";

    public TeslaTurbineBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public @NotNull InteractionResult place(@NotNull BlockPlaceContext context) {
        InteractionResult result = super.place(context);
        if (result != InteractionResult.FAIL) {
            return result;
        }

        Direction clickedFace = context.getClickedFace();
        if (!(getBlock() instanceof TeslaTurbineBlock turbine)) {
            return result;
        }

        Axis axis = turbine.getAxisForPlacement(context);
        if (clickedFace.getAxis() != axis) {
            result = super.place(BlockPlaceContext.at(context, context.getClickedPos().relative(clickedFace), clickedFace));
        }
        if (result == InteractionResult.FAIL && context.getLevel().isClientSide()) {
            CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> showBounds(context));
        }
        return result;
    }

    @OnlyIn(Dist.CLIENT)
    public void showBounds(@NotNull BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        if (!(getBlock() instanceof TeslaTurbineBlock turbine)) {
            return;
        }

        Axis axis = turbine.getAxisForPlacement(context);
        if (axis == null) {
            return;
        }

        Vec3 contract = Vec3.atLowerCornerOf(Direction.get(AxisDirection.POSITIVE, axis).getNormal());
        if (!(context.getPlayer() instanceof LocalPlayer localPlayer)) {
            return;
        }

        Outliner.getInstance().showAABB(Pair.of(TESLA_TURBINE, pos), new AABB(pos).inflate(1).deflate(contract.x, contract.y, contract.z)).colored(COLOR_RED);
        CCBLang.translate("gui.warnings.clear_blocks_for_placement").color(COLOR_RED).sendStatus(localPlayer);
    }
}
