package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightReactorKettleScenes {
    public static void placement(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_reactor_kettle_placement", "Placing an Airtight Reactor Kettle");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos centerPos = util.grid().at(3, 1, 3);
        BlockPos leftUpPos = centerPos.above(2).east().south();
        BlockPos industrialIronPos = leftUpPos.below(2);
        BlockPos realCenterPos = centerPos.above();
        BlockPos rightDownPos = centerPos.west().north();
        BlockPos cogCenterPos = centerPos.above(2);
        BlockPos westCogPos = cogCenterPos.west(2);
        BlockPos southCogPos = cogCenterPos.south(2);
        BlockPos eastCogPos = cogCenterPos.east(2);
        BlockPos northCogPos = cogCenterPos.north(2);
        BlockPos motorPos = westCogPos.above();
        BlockPos funnelPos = rightDownPos.west();
        BlockPos hatchPos = funnelPos.south(2);
        BlockPos fluidPipePos = rightDownPos.north();
        BlockPos fluidPumpPos = fluidPipePos.north();
        BlockPos airtightPipePos = fluidPipePos.east(2);
        BlockPos airtightPumpPos = fluidPumpPos.east(2);

        Selection kettleSelection = util.select().fromTo(leftUpPos, rightDownPos);
        Selection industrialIronSelection = util.select().position(industrialIronPos);
        Selection westCogSelection = util.select().position(westCogPos);
        Selection southCogSelection = util.select().position(southCogPos);
        Selection eastCogSelection = util.select().position(eastCogPos);
        Selection northCogSelection = util.select().position(northCogPos);
        Selection motorSelection = util.select().position(motorPos);
        Selection kettleCogsSelection = util.select().fromTo(leftUpPos, rightDownPos.above(2));
        Selection fluidPipeSelection = util.select().fromTo(fluidPipePos, fluidPumpPos);
        Selection airtightPipeSelection = util.select().fromTo(airtightPipePos, airtightPumpPos);
        Selection funnelSelection = util.select().position(funnelPos);
        Selection hatchSelection = util.select().position(hatchPos);

        Vec3 realCenterVec = util.vector().centerOf(realCenterPos);
        Vec3 westCogVec = util.vector().centerOf(westCogPos);
        Vec3 rightDownVec = util.vector().centerOf(rightDownPos);
        Vec3 funnelVec = util.vector().centerOf(funnelPos);
        Vec3 fluidPumpVec = util.vector().centerOf(fluidPumpPos);
        Vec3 airtightPumpVec = util.vector().centerOf(airtightPumpPos);

        AABB kettleArea = new AABB(realCenterVec, realCenterVec);
        AABB cogArea = new AABB(westCogVec, westCogVec);
        AABB fluidPipeArea = new AABB(fluidPumpVec, fluidPumpVec);
        AABB airtightPipeArea = new AABB(airtightPumpVec, airtightPumpVec);

        Object kettleObject = new Object();
        Object cogObject = new Object();
        Object fluidPipeObject = new Object();
        Object airtightPipeObject = new Object();

        float fastSpeed = SpeedLevel.FAST.getSpeedValue();

        scene.idle(20);
        scene.world().setBlock(industrialIronPos, AllBlocks.INDUSTRIAL_IRON_BLOCK.getDefaultState(), false);
        scene.world().showSection(industrialIronSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, kettleObject, kettleArea, 3);

        scene.idle(3);
        kettleArea = kettleArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, kettleObject, kettleArea, 3);

        scene.idle(3);
        kettleArea = kettleArea.inflate(1);
        scene.overlay().showText(60).text("The Airtight Reactor Kettle occupies a 3x3x3 area...").pointAt(realCenterVec).placeNearTarget();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, kettleObject, kettleArea, 60);

        scene.idle(80);
        scene.world().hideSection(industrialIronSelection, Direction.UP);

        scene.idle(20);
        scene.world().showSection(kettleSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("...with different sections serving distinct purposes").pointAt(realCenterVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(northCogSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().showSection(eastCogSelection, Direction.WEST);

        scene.idle(3);
        scene.world().showSection(southCogSelection, Direction.NORTH);

        scene.idle(3);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.DOWN), false);
        scene.world().showSection(motorSelection, Direction.EAST);
        scene.world().showSection(westCogSelection, Direction.EAST);

        scene.idle(15);
        scene.world().setKineticSpeed(motorSelection, fastSpeed);
        scene.world().setKineticSpeed(westCogSelection, fastSpeed);
        scene.world().setKineticSpeed(eastCogSelection, fastSpeed);
        scene.world().setKineticSpeed(northCogSelection, fastSpeed);
        scene.world().setKineticSpeed(southCogSelection, fastSpeed);
        scene.world().setKineticSpeed(kettleCogsSelection, -fastSpeed);
        scene.effects().rotationSpeedIndicator(motorPos);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, cogObject, cogArea, 3);

        scene.idle(3);
        cogArea = cogArea.inflate(0.5);
        scene.overlay().showText(60).text("Stress must be supplied from the top").colored(PonderPalette.INPUT).pointAt(westCogVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, cogObject, cogArea, 60);

        scene.idle(80);
        scene.world().showSection(fluidPipeSelection, Direction.SOUTH);
        scene.world().showSection(airtightPipeSelection, Direction.SOUTH);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, fluidPipeObject, fluidPipeArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, airtightPipeObject, airtightPipeArea, 3);

        scene.idle(3);
        fluidPipeArea = fluidPipeArea.inflate(0.375, 0.375, 0.5);
        airtightPipeArea = airtightPipeArea.inflate(0.375, 0.375, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, fluidPipeObject, fluidPipeArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, airtightPipeObject, airtightPipeArea, 3);

        scene.idle(3);
        fluidPipeArea = fluidPipeArea.expandTowards(0, 0, 1);
        airtightPipeArea = airtightPipeArea.expandTowards(0, 0, 1);
        scene.overlay().showText(60).text("while Items, Fluids, and Gases are input from below").colored(PonderPalette.INPUT).pointAt(rightDownVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, fluidPipeObject, fluidPipeArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, airtightPipeObject, airtightPipeArea, 60);

        scene.idle(80);
        scene.world().showSection(funnelSelection, Direction.EAST);
        scene.world().showSection(hatchSelection, Direction.EAST);

        scene.idle(20);
        scene.overlay().showText(60).text("Items, in particular, can be fed via Funnels or dropped in directly").colored(PonderPalette.INPUT).pointAt(funnelVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void processing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_reactor_kettle_processing", "Processing Items with the Airtight Reactor Kettle");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos centerPos = util.grid().at(3, 1, 3);
        BlockPos corePos = centerPos.above(2);
        BlockPos leftUpPos = centerPos.above(3).east().south();
        BlockPos rightDownPos = centerPos.above().west().north();
        BlockPos filterPos = rightDownPos.east();
        BlockPos belowCornerPos = rightDownPos.below();
        BlockPos cogPos = centerPos.above(3).west(2);
        BlockPos motorPos = cogPos.above();
        BlockPos andesiteFunnelPos = rightDownPos.west();
        BlockPos andesiteBeltLeftPos = andesiteFunnelPos.below();
        BlockPos andesiteBeltRightPos = andesiteBeltLeftPos.west();
        BlockPos andesiteMotorPos = andesiteBeltRightPos.north();
        BlockPos brassFunnelPos = centerPos.above().east().north(2);
        BlockPos brassBeltRightPos = brassFunnelPos.below().west();
        BlockPos brassBeltLeftPos = brassBeltRightPos.east(3);
        BlockPos brassMotorPos = brassBeltLeftPos.south();
        BlockPos blazePos = centerPos.south();
        BlockPos breezePos = centerPos.east();

        Selection kettleSelection = util.select().fromTo(leftUpPos, belowCornerPos);
        Selection kettleCogsSelection = util.select().fromTo(leftUpPos, rightDownPos.above(2));
        Selection sourceSelection = util.select().fromTo(cogPos, motorPos);
        Selection andesiteFunnelSelection = util.select().fromTo(andesiteFunnelPos, andesiteMotorPos);
        Selection brassFunnelSelection = util.select().fromTo(brassBeltLeftPos, brassBeltRightPos.above());
        Selection brassMotorSelection = util.select().position(brassMotorPos);

        ItemStack cobblestoneItem = new ItemStack(Blocks.COBBLESTONE);
        ItemStack quartzItem = new ItemStack(Items.QUARTZ);
        ItemStack ironNuggetItem = new ItemStack(Items.IRON_NUGGET);
        ItemStack andesiteAlloyItem = new ItemStack(AllItems.ANDESITE_ALLOY.asItem());

        float fastSpeed = SpeedLevel.FAST.getSpeedValue();
        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        Vec3 filterVec = util.vector().blockSurface(filterPos, Direction.NORTH).add(0, 0, -0.0625);
        Vec3 rightDownVec = util.vector().centerOf(rightDownPos);
        Vec3 blazeVec = util.vector().centerOf(blazePos);
        Vec3 breezeVec = util.vector().centerOf(breezePos);

        AABB blazeArea = new AABB(blazeVec, blazeVec);
        AABB breezeArea = new AABB(breezeVec, breezeVec);

        Object blazeObject = new Object();
        Object breezeObject = new Object();

        scene.idle(20);
        scene.world().showSection(kettleSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.DOWN), false);
        scene.world().showSection(sourceSelection, Direction.EAST);

        scene.idle(15);
        scene.world().setKineticSpeed(sourceSelection, fastSpeed);
        scene.world().setKineticSpeed(kettleCogsSelection, -fastSpeed);
        scene.effects().rotationSpeedIndicator(motorPos);

        scene.idle(20);
        scene.overlay().showText(60).text("Airtight Reactor Kettle can automate certain crafting recipes").colored(PonderPalette.GREEN).pointAt(rightDownVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setBlock(andesiteMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH), false);
        scene.world().showSection(andesiteFunnelSelection, Direction.EAST);

        scene.idle(15);
        scene.world().setKineticSpeed(andesiteFunnelSelection, -mediumSpeed);

        scene.idle(20);
        scene.world().createItemOnBeltLike(andesiteBeltRightPos, Direction.UP, cobblestoneItem.copyWithCount(32));

        scene.idle(13);
        scene.world().removeItemsFromBelt(andesiteBeltLeftPos);
        scene.world().flapFunnel(andesiteFunnelPos, false);
        scene.world().modifyBlockEntity(corePos, AirtightReactorKettleBlockEntity.class, be -> be.getItemCapability().insertItem(0, cobblestoneItem.copyWithCount(32), false));

        scene.idle(10);
        scene.world().createItemOnBeltLike(andesiteBeltRightPos, Direction.UP, quartzItem.copyWithCount(32));

        scene.idle(13);
        scene.world().removeItemsFromBelt(andesiteBeltLeftPos);
        scene.world().flapFunnel(andesiteFunnelPos, false);
        scene.world().modifyBlockEntity(corePos, AirtightReactorKettleBlockEntity.class, be -> be.getItemCapability().insertItem(1, quartzItem.copyWithCount(32), false));

        scene.idle(10);
        scene.world().createItemOnBeltLike(andesiteBeltRightPos, Direction.UP, ironNuggetItem.copyWithCount(32));

        scene.idle(13);
        scene.world().removeItemsFromBelt(andesiteBeltLeftPos);
        scene.world().flapFunnel(andesiteFunnelPos, false);
        scene.world().modifyBlockEntity(corePos, AirtightReactorKettleBlockEntity.class, be -> be.getItemCapability().insertItem(2, ironNuggetItem.copyWithCount(32), false));
        scene.world().modifyBlockEntity(corePos, AirtightReactorKettleBlockEntity.class, AirtightReactorKettleBlockEntity::startProcessInPonderLevel);

        scene.idle(20);
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
        scene.world().modifyBlockEntity(corePos, AirtightReactorKettleBlockEntity.class, be -> be.getItemCapability().setStackInSlot(27, ItemStack.EMPTY));
        scene.world().createItemOnBeltLike(brassFunnelPos.below(), Direction.UP, andesiteAlloyItem.copyWithCount(32));

        scene.idle(60);
        scene.rotateCameraY(180);

        scene.idle(40);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, blazeObject, blazeArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, breezeObject, breezeArea, 3);

        scene.idle(3);
        blazeArea = blazeArea.inflate(0.5);
        breezeArea = breezeArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, blazeObject, blazeArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, breezeObject, breezeArea, 60);
        scene.overlay().showText(60).text("Some recipes may require a Blaze Burner or Breeze Cooler to adjust temperature conditions").colored(PonderPalette.MEDIUM).attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
