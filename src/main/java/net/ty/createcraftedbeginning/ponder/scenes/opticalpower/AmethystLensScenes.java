package net.ty.createcraftedbeginning.ponder.scenes.opticalpower;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.amethystlens.AmethystLensBlock;
import net.ty.createcraftedbeginning.content.opticalpower.amethystlens.AmethystLensBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystLensScenes {
    private AmethystLensScenes() {
    }

    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("amethyst_lens", "Using Amethyst Lenses");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos lensPos = util.grid().at(2, 1, 3);
        BlockPos previewPos = lensPos.north();
        BlockPos emitterPos = lensPos.west();
        BlockPos fiberPos = lensPos.west(2);
        BlockPos arrayCornerPos = lensPos.west().south(2);
        BlockPos nearFocusPos = lensPos.east(2);
        BlockPos farFocusPos = lensPos.east(4);

        Selection lensSelection = util.select().position(lensPos);
        Selection setupSelection = util.select().fromTo(fiberPos, arrayCornerPos).add(lensSelection);
        Selection previewSelection = util.select().position(previewPos);

        Vec3 lensVec = util.vector().centerOf(lensPos);
        Vec3 previewOffset = lensVec.subtract(util.vector().centerOf(previewPos));
        Vec3 controlVec = lensVec.add(-0.03125, 0, 0);
        Vec3 nearFocusVec = util.vector().centerOf(nearFocusPos);
        Vec3 farFocusVec = util.vector().centerOf(farFocusPos);

        AABB controlArea = new AABB(controlVec, controlVec).inflate(0.02, 0.14, 0.14);

        Object controlOutline = new Object();

        scene.world().setBlock(lensPos, Blocks.AIR.defaultBlockState(), false);

        scene.idle(20);
        scene.world().showSection(setupSelection, Direction.DOWN);

        scene.idle(20);
        scene.world().modifyBlockEntity(emitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(4));

        scene.idle(30);
        scene.world().setBlock(previewPos, CCBBlocks.AMETHYST_CONVEX_LENS_BLOCK.getDefaultState().setValue(AmethystLensBlock.FACING, Direction.WEST), false);
        ElementLink<WorldSectionElement> fallingLens = scene.world().showIndependentSection(previewSelection, Direction.DOWN);
        scene.world().moveSection(fallingLens, previewOffset, 0);

        scene.idle(15);
        scene.world().setBlock(previewPos, Blocks.AIR.defaultBlockState(), false);
        scene.world().restoreBlocks(lensSelection);

        scene.idle(5);
        scene.overlay().showText(60).text("An Amethyst Convex Lens focuses the beam before it spreads out again").colored(PonderPalette.GREEN).pointAt(nearFocusVec).attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Hold right-click at the center of either face to adjust the focal length").colored(PonderPalette.BLUE).pointAt(controlVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, controlOutline, controlArea, 60);
        scene.overlay().showControls(controlVec, Pointing.DOWN, 60).rightClick();

        scene.idle(80);
        scene.overlay().showText(60).text("Increasing the focal length moves the focus farther from the lens").pointAt(farFocusVec).placeNearTarget().attachKeyFrame();

        scene.idle(10);
        setFocalLength(scene, lensPos, 3);

        scene.idle(10);
        setFocalLength(scene, lensPos, 4);

        scene.idle(40);
        scene.world().setBlock(lensPos, Blocks.AIR.defaultBlockState(), true);

        scene.idle(20);
        scene.world().setBlock(lensPos, CCBBlocks.AMETHYST_CONCAVE_LENS_BLOCK.getDefaultState().setValue(AmethystLensBlock.FACING, Direction.WEST), false);
        setFocalLength(scene, lensPos, 4);
        scene.overlay().showText(60).text("Swap in an Amethyst Concave Lens to spread the beam instead").colored(PonderPalette.GREEN).pointAt(lensVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Shorten the focal length to make the beam wider at the same distance").pointAt(farFocusVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(controlVec, Pointing.DOWN, 20).rightClick();

        scene.idle(10);
        setFocalLength(scene, lensPos, 3);

        scene.idle(10);
        setFocalLength(scene, lensPos, 2);

        scene.idle(40);
        scene.markAsFinished();
    }

    private static void setFocalLength(CreateSceneBuilder scene, BlockPos lensPos, int focalLength) {
        scene.world().modifyBlockEntity(lensPos, AmethystLensBlockEntity.class, lens -> lens.getBehaviour(ScrollValueBehaviour.TYPE).setValue(focalLength));
    }
}
