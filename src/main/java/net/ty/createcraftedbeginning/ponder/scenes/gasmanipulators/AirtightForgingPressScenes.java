package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.instruction.RotateSceneInstruction;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressScenes {
    public static void placement(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_forging_press_placement", "Placing an Airtight Forging Press");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos centerPos = util.grid().at(3, 1, 3);
        BlockPos leftUpPos = centerPos.above(2).east().south();
        BlockPos industrialIronPos = leftUpPos.below(2);
        BlockPos realCenterPos = centerPos.above();
        BlockPos rightDownPos = centerPos.west().north();
        BlockPos shaftCenterPos = centerPos.above(2);
        BlockPos westShaftPos = shaftCenterPos.west(2);
        BlockPos southShaftPos = shaftCenterPos.south(2);
        BlockPos eastShaftPos = shaftCenterPos.east(2);
        BlockPos northShaftPos = shaftCenterPos.north(2);
        BlockPos motorPos = westShaftPos.west();
        BlockPos deployerPos = westShaftPos.below();
        BlockPos chutePos = shaftCenterPos.above();
        BlockPos pipePos = chutePos.above();

        Selection pressSelection = util.select().fromTo(leftUpPos, rightDownPos);
        Selection industrialIronSelection = util.select().position(industrialIronPos);
        Selection westShaftSelection = util.select().position(westShaftPos);
        Selection southShaftSelection = util.select().position(southShaftPos);
        Selection eastShaftSelection = util.select().position(eastShaftPos);
        Selection northShaftSelection = util.select().position(northShaftPos);
        Selection motorSelection = util.select().position(motorPos);
        Selection pressShaftsSelection = util.select().fromTo(leftUpPos, rightDownPos.above(2));
        Selection deployerSelection = util.select().position(deployerPos);
        Selection chuteSelection = util.select().position(chutePos);
        Selection pipeSelection = util.select().fromTo(pipePos, chutePos);

        Vec3 realCenterVec = util.vector().centerOf(realCenterPos);
        Vec3 deployerVec = util.vector().centerOf(deployerPos);
        Vec3 centerVec = util.vector().centerOf(centerPos);
        Vec3 westShaftVec = util.vector().centerOf(westShaftPos);
        Vec3 chuteVec = util.vector().centerOf(chutePos);
        Vec3 pipeVec = util.vector().centerOf(pipePos);
        Vec3 motorVec = util.vector().centerOf(motorPos);

        Object pressObject = new Object();
        Object shaftObject = new Object();
        Object pressHeadObject = new Object();
        Object bottomObject = new Object();

        AABB pressArea = new AABB(realCenterVec, realCenterVec);
        AABB shaftArea = new AABB(motorVec, motorVec);
        AABB pressHeadArea = new AABB(realCenterVec, realCenterVec);
        AABB bottomArea = new AABB(centerVec, centerVec);

        float fastSpeed = SpeedLevel.FAST.getSpeedValue();

        scene.idle(20);
        scene.world().setBlock(industrialIronPos, AllBlocks.INDUSTRIAL_IRON_BLOCK.getDefaultState(), false);
        scene.world().showSection(industrialIronSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, pressObject, pressArea, 3);

        scene.idle(3);
        pressArea = pressArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, pressObject, pressArea, 3);

        scene.idle(3);
        pressArea = pressArea.inflate(1);
        scene.overlay().showText(60).text("The Airtight Forging Press occupies a 3x3x3 area...").pointAt(realCenterVec).placeNearTarget();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, pressObject, pressArea, 60);

        scene.idle(80);
        scene.world().hideSection(industrialIronSelection, Direction.UP);

        scene.idle(20);
        scene.world().showSection(pressSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("...with different sections serving distinct purposes").pointAt(realCenterVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
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
        scene.world().setKineticSpeed(motorSelection, fastSpeed);
        scene.world().setKineticSpeed(westShaftSelection, fastSpeed);
        scene.world().setKineticSpeed(eastShaftSelection, fastSpeed);
        scene.world().setKineticSpeed(northShaftSelection, fastSpeed);
        scene.world().setKineticSpeed(southShaftSelection, fastSpeed);
        scene.world().setKineticSpeed(pressShaftsSelection, fastSpeed);
        scene.effects().rotationSpeedIndicator(motorPos);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, shaftObject, shaftArea, 3);

        scene.idle(3);
        shaftArea = shaftArea.inflate(0.5);

        scene.idle(3);
        shaftArea = shaftArea.expandTowards(1, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, shaftObject, shaftArea, 60);
        scene.overlay().showText(60).text("Stress must be supplied from the top").colored(PonderPalette.INPUT).pointAt(westShaftVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.addInstruction(new RotateSceneInstruction(30, 0, true));

        scene.idle(40);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, pressHeadObject, pressHeadArea, 3);
        scene.overlay().showControls(util.vector().blockSurface(realCenterPos, Direction.EAST), Pointing.RIGHT, 53).rightClick();

        scene.idle(3);
        pressHeadArea = pressHeadArea.inflate(0.6);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, pressHeadObject, pressHeadArea, 60);
        scene.overlay().showText(60).text("Right-click the press head to place or remove the press head tool").colored(PonderPalette.BLUE).pointAt(realCenterVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(deployerSelection, Direction.NORTH);

        scene.idle(20);
        scene.overlay().showText(60).text("Or automate this with a Deployer").colored(PonderPalette.GREEN).pointAt(deployerVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().hideSection(deployerSelection, Direction.SOUTH);
        scene.addInstruction(new RotateSceneInstruction(-30, 0, true));

        scene.idle(40);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, bottomObject, bottomArea, 3);

        scene.idle(3);
        bottomArea = bottomArea.inflate(1.5, 0.40625, 1.5);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, bottomObject, bottomArea, 60);
        scene.overlay().showText(60).text("Items to be processed can be placed at the bottom").colored(PonderPalette.INPUT).pointAt(centerVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(chuteSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Items used for processing can be placed on top...").colored(PonderPalette.INPUT).pointAt(chuteVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().hideSection(chuteSelection, Direction.UP);

        scene.idle(20);
        scene.world().setBlock(chutePos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(AirtightPipeBlock.AXIS, Axis.Y), false);
        scene.world().showSection(pipeSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("...or fluids and gases can be input via pipes").colored(PonderPalette.INPUT).pointAt(pipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void processing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_forging_press_processing", "Processing Items with the Airtight Forging Press");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos centerPos = util.grid().at(3, 1, 3);
        BlockPos corePos = centerPos.above(2);
        BlockPos leftUpPos = centerPos.above(3).east().south();
        BlockPos rightDownPos = centerPos.above().west().north();
        BlockPos filterPos = rightDownPos.east();
        BlockPos belowCornerPos = rightDownPos.below();
        BlockPos shaftPos = centerPos.above(3).west(2);
        BlockPos motorPos = shaftPos.west();
        BlockPos andesiteFunnelPos = rightDownPos.west();
        BlockPos andesiteBeltLeftPos = andesiteFunnelPos.below();
        BlockPos andesiteBeltRightPos = andesiteBeltLeftPos.west();
        BlockPos andesiteMotorPos = andesiteBeltRightPos.north();
        BlockPos brassFunnelPos = centerPos.above().east().north(2);
        BlockPos brassBeltRightPos = brassFunnelPos.below().west();
        BlockPos brassBeltLeftPos = brassBeltRightPos.east(3);
        BlockPos brassMotorPos = brassBeltLeftPos.south();
        BlockPos deployerPos = corePos.east(2);
        BlockPos gearboxPos = deployerPos.above();
        BlockPos chutePos = corePos.above(2);

        Selection pressSelection = util.select().fromTo(leftUpPos, belowCornerPos);
        Selection pressShaftSelection = util.select().fromTo(leftUpPos, rightDownPos.above(2));
        Selection sourceSelection = util.select().fromTo(shaftPos, motorPos);
        Selection andesiteFunnelSelection = util.select().fromTo(andesiteFunnelPos, andesiteMotorPos);
        Selection brassFunnelSelection = util.select().fromTo(brassBeltLeftPos, brassBeltRightPos.above());
        Selection brassMotorSelection = util.select().position(brassMotorPos);
        Selection deployerSelection = util.select().position(deployerPos);
        Selection gearboxSelection = util.select().position(gearboxPos);
        Selection chuteSelection = util.select().position(chutePos);

        Vec3 rightDownVec = util.vector().centerOf(rightDownPos);
        Vec3 filterVec = util.vector().blockSurface(filterPos, Direction.NORTH).add(0, -0.0625f, -0.0625);
        Vec3 coreVec = util.vector().centerOf(corePos);

        ItemStack diamondAxeItem = new ItemStack(Items.DIAMOND_AXE);
        ItemStack templateItem = new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ItemStack netheriteIngotItem = new ItemStack(Items.NETHERITE_INGOT);
        ItemStack netheriteAxeItem = new ItemStack(Items.NETHERITE_AXE);

        float fastSpeed = SpeedLevel.FAST.getSpeedValue();
        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        scene.idle(20);
        scene.world().showSection(pressSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST), false);
        scene.world().showSection(sourceSelection, Direction.EAST);

        scene.idle(15);
        scene.world().setKineticSpeed(sourceSelection, fastSpeed);
        scene.world().setKineticSpeed(pressShaftSelection, fastSpeed);
        scene.effects().rotationSpeedIndicator(motorPos);

        scene.idle(20);
        scene.overlay().showText(60).text("Airtight Forging Press can automate certain crafting recipes").colored(PonderPalette.GREEN).pointAt(rightDownVec).placeNearTarget().attachKeyFrame();

        scene.idle(20);
        scene.addInstruction(new RotateSceneInstruction(15, 15, true));

        scene.idle(60);
        scene.world().setBlock(andesiteMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH), false);
        scene.world().showSection(andesiteFunnelSelection, Direction.EAST);

        scene.idle(15);
        scene.world().setKineticSpeed(andesiteFunnelSelection, -mediumSpeed);

        scene.idle(20);
        scene.world().createItemOnBeltLike(andesiteBeltRightPos, Direction.UP, diamondAxeItem.copy());

        scene.idle(13);
        scene.world().removeItemsFromBelt(andesiteBeltLeftPos);
        scene.world().flapFunnel(andesiteFunnelPos, false);
        scene.world().modifyBlockEntity(corePos, AirtightForgingPressBlockEntity.class, be -> be.getInputOutputCapability().insertItem(0, diamondAxeItem.copy(), false));
        scene.world().modifyBlockEntity(corePos, AirtightForgingPressBlockEntity.class, AirtightForgingPressBlockEntity::startProcessInPonderLevel);

        scene.idle(20);
        scene.world().modifyBlockEntityNBT(deployerSelection, DeployerBlockEntity.class, compoundTag -> compoundTag.put("HeldItem", templateItem.copy().saveOptional(scene.world().getHolderLookupProvider())));
        scene.world().showSection(deployerSelection, Direction.WEST);

        scene.idle(20);
        scene.world().showSection(gearboxSelection, Direction.DOWN);

        scene.idle(15);
        scene.world().setKineticSpeed(deployerSelection, fastSpeed);
        scene.world().setKineticSpeed(gearboxSelection, -fastSpeed);
        scene.world().moveDeployer(deployerPos, 1, 20);

        scene.idle(20);
        scene.world().modifyBlockEntity(corePos, AirtightForgingPressBlockEntity.class, be -> be.getProcessingInventories().getFirst().insertItem(0, templateItem.copy(), false));
        scene.world().modifyBlockEntityNBT(deployerSelection, DeployerBlockEntity.class, compoundTag -> compoundTag.put("HeldItem", ItemStack.EMPTY.saveOptional(scene.world().getHolderLookupProvider())));
        scene.world().moveDeployer(deployerPos, -1, 20);

        scene.idle(20);
        scene.world().hideSection(gearboxSelection, Direction.NORTH);
        scene.world().setKineticSpeed(deployerSelection, 0);
        scene.world().setKineticSpeed(gearboxSelection, 0);
        scene.world().showSection(chuteSelection, Direction.DOWN);

        scene.idle(20);
        ElementLink<EntityElement> remove = scene.world().createItemEntity(util.vector().centerOf(chutePos.above()), util.vector().of(0, -0.1, 0), netheriteIngotItem.copy());

        scene.idle(2);
        scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, netheriteIngotItem.copy());
        scene.world().modifyEntity(remove, Entity::discard);
        scene.world().modifyBlockEntity(corePos, AirtightForgingPressBlockEntity.class, be -> be.getProcessingInventories().getSecond().insertItem(0, netheriteIngotItem.copy(), false));
        scene.overlay().showText(60).text("The press head tool will not be consumed").colored(PonderPalette.GREEN).pointAt(coreVec).placeNearTarget().attachKeyFrame();

        scene.idle(20);
        scene.world().modifyBlockEntity(corePos, AirtightForgingPressBlockEntity.class, be -> be.getInputOutputCapability().setStackInSlot(0, ItemStack.EMPTY));
        scene.world().modifyBlockEntity(corePos, AirtightForgingPressBlockEntity.class, be -> be.getProcessingInventories().getSecond().setStackInSlot(0, ItemStack.EMPTY));
        scene.world().modifyBlockEntity(corePos, AirtightForgingPressBlockEntity.class, be -> be.getInputOutputCapability().setStackInSlot(1, netheriteAxeItem.copy()));

        scene.idle(20);
        scene.addInstruction(new RotateSceneInstruction(-15, -15, true));

        scene.idle(40);
        scene.overlay().showFilterSlotInput(filterVec, Direction.NORTH, 60);
        scene.overlay().showText(60).text("The filter slot can be used in case two or more recipes are conflicting").colored(PonderPalette.GREEN).pointAt(filterVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(brassFunnelSelection, Direction.SOUTH);

        scene.idle(20);
        scene.world().setBlock(brassMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.NORTH), false);
        scene.world().showSection(brassMotorSelection, Direction.NORTH);

        scene.idle(15);
        scene.world().setKineticSpeed(brassFunnelSelection, -mediumSpeed);
        scene.world().setKineticSpeed(brassMotorSelection, -mediumSpeed);
        scene.world().flapFunnel(brassFunnelPos, true);
        scene.world().modifyBlockEntity(corePos, AirtightForgingPressBlockEntity.class, be -> be.getInputOutputCapability().setStackInSlot(1, ItemStack.EMPTY));
        scene.world().createItemOnBeltLike(brassFunnelPos.below(), Direction.UP, netheriteAxeItem.copy());

        scene.idle(40);
        scene.markAsFinished();
    }
}
