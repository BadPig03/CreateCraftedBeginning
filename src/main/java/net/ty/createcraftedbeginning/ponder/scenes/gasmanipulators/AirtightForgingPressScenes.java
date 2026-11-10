package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.instruction.RotateSceneInstruction;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeConnections;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressScenes {
    private static final String COMPOUND_KEY_HELD_ITEM = "HeldItem";

    public static void placement(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_forging_press_placement", "Setting Up an Airtight Forging Press");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos centerPos = util.grid().at(3, 1, 3);
        BlockPos leftUpPos = centerPos.above(2).east().south();
        BlockPos industrialIronPos = centerPos.east().south();
        BlockPos corePos = centerPos.above();
        BlockPos rightDownPos = centerPos.west().north();
        BlockPos bottomFarPos = centerPos.east().south();
        BlockPos westShaftPos = centerPos.west(2).above(2);
        BlockPos southShaftPos = centerPos.above(2).south(2);
        BlockPos eastShaftPos = centerPos.east(2).above(2);
        BlockPos northShaftPos = centerPos.above(2).north(2);
        BlockPos motorPos = centerPos.west(3).above(2);
        BlockPos deployerPos = centerPos.west(2).above();
        BlockPos topPortPos = centerPos.above(3);
        BlockPos deployerMotorPos = centerPos.west(2).above().south();

        Selection pressSelection = util.select().fromTo(leftUpPos, rightDownPos);
        Selection industrialIronSelection = util.select().position(industrialIronPos);
        Selection westShaftSelection = util.select().position(westShaftPos);
        Selection southShaftSelection = util.select().position(southShaftPos);
        Selection eastShaftSelection = util.select().position(eastShaftPos);
        Selection northShaftSelection = util.select().position(northShaftPos);
        Selection motorSelection = util.select().position(motorPos);
        Selection pressShaftsSelection = util.select().fromTo(leftUpPos, centerPos.west().north().above(2));
        Selection deployerSelection = util.select().position(deployerPos);
        Selection topPortSelection = util.select().position(topPortPos);
        Selection deployerMotorSelection = util.select().position(deployerMotorPos);

        Vec3 realCenterVec = util.vector().centerOf(corePos);
        Vec3 westShaftVec = util.vector().centerOf(westShaftPos);
        Vec3 deployerVec = util.vector().centerOf(deployerPos);
        Vec3 bottomCenterVec = util.vector().centerOf(centerPos);
        Vec3 topPortVec = util.vector().centerOf(topPortPos);
        Vec3 coreEastVec = util.vector().blockSurface(corePos, Direction.EAST);

        AABB pressStartArea = new AABB(realCenterVec, realCenterVec).inflate(0.5);
        AABB pressArea = new AABB(realCenterVec, realCenterVec);
        AABB shaftArea = new AABB(westShaftVec, westShaftVec);
        AABB pressHeadArea = new AABB(realCenterVec, realCenterVec);
        AABB bottomArea = new AABB(util.vector().centerOf(rightDownPos), util.vector().centerOf(bottomFarPos));

        Object pressObject = new Object();
        Object shaftObject = new Object();
        Object pressHeadObject = new Object();
        Object bottomObject = new Object();

        ItemStack heavyCoreItem = new ItemStack(Items.HEAVY_CORE);

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();
        float fastSpeed = SpeedLevel.FAST.getSpeedValue();

        scene.idle(20);
        scene.world().setBlock(industrialIronPos, AllBlocks.INDUSTRIAL_IRON_BLOCK.getDefaultState(), false);
        scene.world().showSection(industrialIronSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, pressObject, pressArea, 3);

        scene.idle(3);
        pressArea = pressArea.inflate(1.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, pressObject, pressArea, 60);
        scene.overlay().showText(60).text("An Airtight Forging Press automatically forms its 3x3x3 structure when placed").pointAt(realCenterVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, pressObject, pressStartArea, 3);
        scene.overlay().showText(60).text("The entire 3x3x3 area must be clear before placement").colored(PonderPalette.RED).pointAt(realCenterVec).placeNearTarget().attachKeyFrame();

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, pressObject, pressArea, 57);

        scene.idle(42);
        scene.world().hideSection(industrialIronSelection, Direction.UP);

        scene.idle(20);
        scene.world().showSection(pressSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(northShaftSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().showSection(eastShaftSelection, Direction.WEST);

        scene.idle(3);
        scene.world().showSection(southShaftSelection, Direction.NORTH);

        scene.idle(3);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST), false);
        scene.world().showSection(motorSelection, Direction.EAST);
        scene.world().showSection(westShaftSelection, Direction.EAST);

        scene.idle(15);
        scene.world().setKineticSpeed(motorSelection, mediumSpeed);
        scene.world().setKineticSpeed(westShaftSelection, mediumSpeed);
        scene.world().setKineticSpeed(eastShaftSelection, mediumSpeed);
        scene.world().setKineticSpeed(northShaftSelection, mediumSpeed);
        scene.world().setKineticSpeed(southShaftSelection, mediumSpeed);
        scene.world().setKineticSpeed(pressShaftsSelection, mediumSpeed);
        scene.effects().rotationSpeedIndicator(motorPos);

        scene.idle(25);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, shaftObject, shaftArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, shaftObject, shaftArea.inflate(0.5), 60);
        scene.overlay().showText(60).text("Supply rotational power through the shaft interfaces on top").pointAt(westShaftVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, shaftObject, shaftArea.inflate(0.5), 60);
        scene.overlay().showText(60).text("The Forging Press requires at least Fast speed to operate").colored(PonderPalette.RED).pointAt(westShaftVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setKineticSpeed(motorSelection, fastSpeed);
        scene.world().setKineticSpeed(westShaftSelection, fastSpeed);
        scene.world().setKineticSpeed(eastShaftSelection, fastSpeed);
        scene.world().setKineticSpeed(northShaftSelection, fastSpeed);
        scene.world().setKineticSpeed(southShaftSelection, fastSpeed);
        scene.world().setKineticSpeed(pressShaftsSelection, fastSpeed);
        scene.effects().rotationSpeedIndicator(motorPos);
        scene.overlay().showText(60).text("Higher rotational speed shortens each forging cycle").colored(PonderPalette.GREEN).pointAt(westShaftVec).placeNearTarget().attachKeyFrame();

        scene.idle(75);
        scene.addInstruction(new RotateSceneInstruction(30, 0, true));

        scene.idle(40);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, pressHeadObject, pressHeadArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, pressHeadObject, pressHeadArea.inflate(0.6), 60);
        scene.overlay().showText(60).text("Right-click the press head to install or remove its Press Head Tool").colored(PonderPalette.BLUE).pointAt(realCenterVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(coreEastVec, Pointing.RIGHT, 27).rightClick().withItem(heavyCoreItem.copy());

        scene.idle(7);
        scene.world().modifyBlockEntity(corePos, AirtightForgingPressBlockEntity.class, press -> press.getPressHeadInventory().insertItem(0, heavyCoreItem.copy(), false));

        scene.idle(20);
        scene.overlay().showControls(coreEastVec, Pointing.RIGHT, 27).rightClick();

        scene.idle(7);
        scene.world().modifyBlockEntity(corePos, AirtightForgingPressBlockEntity.class, press -> press.getPressHeadInventory().setStackInSlot(0, ItemStack.EMPTY));

        scene.idle(20);
        scene.world().modifyBlockEntityNBT(deployerSelection, DeployerBlockEntity.class, compoundTag -> compoundTag.put(COMPOUND_KEY_HELD_ITEM, heavyCoreItem.copy().saveOptional(scene.world().getHolderLookupProvider())));
        scene.world().setBlock(deployerMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.NORTH), false);
        scene.world().showSection(deployerSelection, Direction.NORTH);

        scene.idle(20);
        scene.world().showSection(deployerMotorSelection, Direction.NORTH);

        scene.idle(15);
        scene.world().setKineticSpeed(deployerSelection, fastSpeed);

        scene.idle(5);
        scene.overlay().showText(60).text("A Deployer can automate Press Head Tool changes when needed").colored(PonderPalette.BLUE).pointAt(deployerVec).placeNearTarget().attachKeyFrame();

        scene.idle(10);
        scene.world().moveDeployer(deployerPos, 1, 10);

        scene.idle(11);
        scene.world().modifyBlockEntityNBT(deployerSelection, DeployerBlockEntity.class, compoundTag -> compoundTag.put(COMPOUND_KEY_HELD_ITEM, new CompoundTag()));
        scene.world().modifyBlockEntity(corePos, AirtightForgingPressBlockEntity.class, press -> press.getPressHeadInventory().insertItem(0, heavyCoreItem.copy(), false));

        scene.idle(10);
        scene.world().moveDeployer(deployerPos, -1, 10);

        scene.idle(29);
        scene.world().hideSection(deployerSelection, Direction.SOUTH);
        scene.world().hideSection(deployerMotorSelection, Direction.SOUTH);
        scene.addInstruction(new RotateSceneInstruction(-30, 0, true));

        scene.idle(40);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, bottomObject, bottomArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, bottomObject, bottomArea.inflate(0.5), 60);
        scene.overlay().showText(60).text("The bottom 3x3 layer accepts Base Items and provides finished outputs").pointAt(bottomCenterVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(topPortSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("The top-center interface accepts the Processing Material used by recipes").colored(PonderPalette.INPUT).pointAt(topPortVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setBlock(topPortPos, AllBlocks.GLASS_FLUID_PIPE.getDefaultState().setValue(GlassFluidPipeBlock.AXIS, Axis.Y), true);
        scene.overlay().showText(60).text("Fluid and Gas ingredients also enter here through their respective networks").colored(PonderPalette.INPUT).pointAt(topPortVec).placeNearTarget().attachKeyFrame();

        scene.idle(30);
        scene.world().setBlock(topPortPos, AirtightPipeConnections.straight(CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState(), Axis.Y), true);

        scene.idle(30);
        scene.markAsFinished();
    }

    public static void processing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_forging_press_processing", "Processing with the Airtight Forging Press");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos centerPos = util.grid().at(3, 1, 3);
        BlockPos corePos = centerPos.above(2);
        BlockPos leftUpPos = centerPos.above(3).east().south();
        BlockPos rightDownPos = centerPos.above().west().north();
        BlockPos filterPos = centerPos.above().north();
        BlockPos belowCornerPos = centerPos.west().north();
        BlockPos shaftPos = centerPos.above(3).west(2);
        BlockPos motorPos = centerPos.west(3).above(3);
        BlockPos inputFunnelPos = centerPos.west(2).above().north();
        BlockPos inputBeltLeftPos = centerPos.west(2).north();
        BlockPos inputBeltRightPos = centerPos.west(3).north();
        BlockPos inputMotorPos = centerPos.west(3).north(2);
        BlockPos outputFunnelPos = centerPos.above().east().north(2);
        BlockPos outputBeltLeftPos = centerPos.east(3).north(2);
        BlockPos outputMotorPos = centerPos.east(3).north();
        BlockPos gasPipePos = centerPos.above(4);
        BlockPos gasPipeTopPos = centerPos.above(5);

        Selection pressSelection = util.select().fromTo(leftUpPos, belowCornerPos);
        Selection pressShaftSelection = util.select().fromTo(leftUpPos, centerPos.west().above(3).north());
        Selection sourceSelection = util.select().fromTo(shaftPos, motorPos);
        Selection inputFunnelSelection = util.select().fromTo(inputFunnelPos, inputMotorPos);
        Selection outputFunnelSelection = util.select().fromTo(outputBeltLeftPos, centerPos.north(2).above());
        Selection outputMotorSelection = util.select().position(outputMotorPos);
        Selection gasPipeSelection = util.select().fromTo(gasPipePos, gasPipeTopPos);

        Vec3 rightDownVec = util.vector().centerOf(rightDownPos);
        Vec3 filterVec = util.vector().blockSurface(filterPos, Direction.NORTH).add(0, -0.0625, -0.0625);
        Vec3 gasPipeVec = util.vector().centerOf(gasPipePos);
        Vec3 coreEastVec = util.vector().blockSurface(corePos, Direction.EAST);
        Vec3 coreVec = util.vector().centerOf(corePos);

        ItemStack charcoalItem = new ItemStack(Items.CHARCOAL);
        ItemStack heavyCoreItem = new ItemStack(Items.HEAVY_CORE);
        ItemStack diamondItem = new ItemStack(Items.DIAMOND);

        float fastSpeed = SpeedLevel.FAST.getSpeedValue();
        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        scene.idle(20);
        scene.world().showSection(pressSelection, Direction.DOWN);

        scene.idle(20);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST), false);
        scene.world().showSection(sourceSelection, Direction.EAST);

        scene.idle(15);
        scene.world().setKineticSpeed(sourceSelection, fastSpeed);
        scene.world().setKineticSpeed(pressShaftSelection, fastSpeed);
        scene.effects().rotationSpeedIndicator(motorPos);

        scene.idle(5);

        scene.overlay().showControls(coreEastVec, Pointing.RIGHT, 27).rightClick().withItem(heavyCoreItem.copy());

        scene.idle(7);
        scene.world().modifyBlockEntity(corePos, AirtightForgingPressBlockEntity.class, press -> press.getPressHeadInventory().insertItem(0, heavyCoreItem.copy(), false));

        scene.idle(12);
        scene.world().setBlock(inputMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH), false);
        scene.world().showSection(inputFunnelSelection, Direction.EAST);

        scene.idle(15);
        scene.world().setKineticSpeed(inputFunnelSelection, -mediumSpeed);

        scene.idle(5);

        scene.idle(10);
        scene.overlay().showText(60).text("This recipe takes Charcoal as its Base Item through the bottom interfaces").colored(PonderPalette.INPUT).pointAt(rightDownVec).placeNearTarget().attachKeyFrame();

        scene.idle(40);
        scene.world().createItemOnBeltLike(inputBeltRightPos, Direction.UP, charcoalItem.copyWithCount(32));

        scene.idle(13);
        scene.world().removeItemsFromBelt(inputBeltLeftPos);
        scene.world().flapFunnel(inputFunnelPos, false);
        scene.world().modifyBlockEntity(corePos, AirtightForgingPressBlockEntity.class, press -> press.getInputInventory().insertItem(0, charcoalItem.copyWithCount(32), false));

        scene.idle(15);
        scene.world().showSection(gasPipeSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Gas-fed recipes also require sufficient input pressure").colored(PonderPalette.RED).pointAt(gasPipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Once all recipe requirements are met, the Forging Press begins its cycle").pointAt(gasPipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(8);
        scene.world().modifyBlockEntity(corePos, AirtightForgingPressBlockEntity.class, AirtightForgingPressBlockEntity::startProcessInPonderLevel);

        scene.idle(20);
        scene.world().modifyBlockEntity(corePos, AirtightForgingPressBlockEntity.class, press -> {
            press.getInputInventory().extractItem(0, 32, false);
            press.getOutputInventory().setStackInSlot(0, diamondItem.copyWithCount(4));
        });

        scene.idle(52);
        scene.overlay().showText(60).text("The installed Press Head Tool remains in place after processing").colored(PonderPalette.BLUE).pointAt(coreVec).placeNearTarget().attachKeyFrame();

        scene.idle(65);
        scene.world().setBlock(outputMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.NORTH), false);
        scene.world().showSection(outputFunnelSelection, Direction.SOUTH);

        scene.idle(20);
        scene.world().showSection(outputMotorSelection, Direction.NORTH);

        scene.idle(15);
        scene.world().setKineticSpeed(outputFunnelSelection, -mediumSpeed);
        scene.world().setKineticSpeed(outputMotorSelection, -mediumSpeed);

        scene.idle(5);
        scene.world().flapFunnel(outputFunnelPos, true);
        scene.world().modifyBlockEntity(corePos, AirtightForgingPressBlockEntity.class, press -> press.getOutputInventory().extractItem(0, 4, false));
        scene.world().createItemOnBeltLike(centerPos.east().north(2), Direction.UP, diamondItem.copyWithCount(4));
        scene.overlay().showText(60).text("Finished items are extracted through the bottom interfaces").colored(PonderPalette.OUTPUT).pointAt(rightDownVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("The Forging Press waits until all recipe outputs can fit").colored(PonderPalette.RED).pointAt(rightDownVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showFilterSlotInput(filterVec, Direction.NORTH, 60);
        scene.overlay().showText(60).text("When multiple recipes match, the Recipe Filter selects by primary output").colored(PonderPalette.BLUE).pointAt(filterVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
