package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.InternalFluidHandler;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

public class AirtightEngineScenes {
    public static void settingUp(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_engine_setting_up", "Setting up Airtight Engines");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9f);
        scene.showBasePlate();

        BlockPos smallTankBottomPos = util.grid().at(5, 1, 1);
        BlockPos smallTankTopPos = smallTankBottomPos.above(2);
        BlockPos smallTankChamberPos = smallTankTopPos.above();
        BlockPos smallTankEnginePos = smallTankBottomPos.above().west();
        BlockPos smallTankOutletPos = smallTankTopPos.north();
        BlockPos largeTankBottomPos = util.grid().at(3, 1, 3);
        BlockPos largeTankTopPos = largeTankBottomPos.above(3).west(2).south(2);
        BlockPos largeTankChamberLeftPos = largeTankBottomPos.above(4);
        BlockPos largeTankChamberRightPos = largeTankChamberLeftPos.west(2).south(2);
        BlockPos largeTankEngineLeftPos = largeTankBottomPos.above(2).west().north();
        BlockPos largeTankEngineRightPos = largeTankBottomPos.above(3).east();
        BlockPos largeTankOutletPos = largeTankEngineLeftPos.west(2).south(2);

        Selection smallTankSelection = util.select().fromTo(smallTankBottomPos, smallTankTopPos);
        Selection smallTankChamberSelection = util.select().fromTo(smallTankChamberPos, smallTankChamberPos);
        Selection smallTankEngineSelection = util.select().fromTo(smallTankEnginePos, smallTankEnginePos);
        Selection smallTankOutletSelection = util.select().fromTo(smallTankOutletPos, smallTankOutletPos);
        Selection largeTankSelection = util.select().fromTo(largeTankBottomPos, largeTankTopPos);
        Selection largeTankChamberSelection = util.select().fromTo(largeTankChamberLeftPos, largeTankChamberRightPos);
        Selection largeTankEngineLeftSelection = util.select().fromTo(largeTankEngineLeftPos, largeTankEngineLeftPos);
        Selection largeTankEngineRightSelection = util.select().fromTo(largeTankEngineRightPos, largeTankEngineRightPos);
        Selection largeTankOutletSelection = util.select().fromTo(largeTankOutletPos, largeTankOutletPos);

        AABB smallTankArea = new AABB(util.vector().centerOf(smallTankBottomPos), util.vector().centerOf(smallTankTopPos));
        AABB smallTankChamberArea = new AABB(util.vector().centerOf(smallTankChamberPos), util.vector().centerOf(smallTankChamberPos));
        AABB smallTankOutletArea = new AABB(util.vector().centerOf(smallTankOutletPos), util.vector().centerOf(smallTankOutletPos));

        Object smallTankObject = new Object();
        Object smallTankChamberObject = new Object();
        Object smallTankOutletObject = new Object();

        scene.idle(20);
        scene.world().showSection(smallTankSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(smallTankEngineSelection, Direction.EAST);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, smallTankObject, smallTankArea, 3);

        scene.idle(3);
        smallTankArea = smallTankArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, smallTankObject, smallTankArea, 60);
        scene.overlay().showText(60).text("Airtight Engines must be placed on Airtight Tanks, forming an Airtight Assembly Driver").pointAt(Vec3.atCenterOf(smallTankEnginePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(smallTankChamberSelection, Direction.DOWN);

        scene.idle(10);
        scene.world().showSection(smallTankOutletSelection, Direction.SOUTH);

        scene.idle(10);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, smallTankChamberObject, smallTankChamberArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, smallTankOutletObject, smallTankOutletArea, 3);

        scene.idle(3);
        smallTankChamberArea = smallTankChamberArea.inflate(0.5, 0.5, 0.5);
        smallTankOutletArea = smallTankOutletArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, smallTankChamberObject, smallTankChamberArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, smallTankOutletObject, smallTankOutletArea, 60);
        scene.overlay().showText(60).text("Additionally, at least one Breeze Chamber and Residue Outlet must be placed...").pointAt(Vec3.atCenterOf(smallTankTopPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(largeTankSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(largeTankOutletSelection, Direction.EAST);

        scene.idle(3);
        scene.world().showSection(largeTankEngineLeftSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().showSection(largeTankEngineRightSelection, Direction.WEST);

        scene.idle(3);
        scene.world().showSection(largeTankChamberSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("...Alternatively, place more Breeze Chambers, Residue Outlets, Airtight Engines, or even Airtight Tanks").pointAt(Vec3.atCenterOf(largeTankOutletPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void generating(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_engine_generating_rotational_force", "Generating Rotational Force via an Airtight Assembly Driver");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9f);
        scene.showBasePlate();

        BlockPos tankBottomPos = util.grid().at(2, 1, 2);
        BlockPos tankTopPos = util.grid().at(4, 4, 4);
        BlockPos airtightPipePos = tankBottomPos.west();
        BlockPos airtightPumpPos = airtightPipePos.west();
        BlockPos airtightPumpCogPos = airtightPumpPos.south();
        BlockPos airtightMotorPos = airtightPumpCogPos.east();
        BlockPos engineBottomPos = tankBottomPos.north().above().east(2);
        BlockPos engineTopPos = engineBottomPos.above(2).west();
        BlockPos outletPos = tankBottomPos.above(2).west();
        BlockPos funnelPos = outletPos.north();
        BlockPos fluidPipePos = outletPos.above();
        BlockPos fluidPumpPos = fluidPipePos.west();
        BlockPos fluidPumpCogPos = fluidPumpPos.south();
        BlockPos fluidMotorPos = fluidPumpCogPos.east();
        BlockPos chamberLeftPos = tankBottomPos.above(4);
        BlockPos chamberRightPos = chamberLeftPos.south(2).east(2);

        Selection tankSelection = util.select().fromTo(tankBottomPos, tankTopPos);
        Selection outletSelection = util.select().fromTo(outletPos, outletPos);
        Selection engineBottomSelection = util.select().fromTo(engineBottomPos, engineBottomPos);
        Selection engineTopSelection = util.select().fromTo(engineTopPos, engineTopPos);
        Selection chamberSelection = util.select().fromTo(chamberLeftPos, chamberRightPos);
        Selection gasSupplySelection = util.select().fromTo(airtightPipePos, airtightPumpCogPos);
        Selection airtightPumpSelection = util.select().fromTo(airtightPipePos, airtightPumpPos);
        Selection airtightPumpMotorSelection = util.select().fromTo(airtightPumpCogPos, airtightMotorPos);
        Selection residueSelection = util.select().fromTo(fluidPipePos, fluidPumpPos);
        Selection residueCogSelection = util.select().fromTo(fluidPumpCogPos, fluidMotorPos);
        Selection fluidPumpSelection = util.select().fromTo(fluidPipePos, fluidPumpPos);
        Selection fluidPumpMotorSelection = util.select().fromTo(fluidPumpCogPos, fluidMotorPos);
        Selection funnelSelection = util.select().position(funnelPos);

        AABB gasSupplyArea = new AABB(util.vector().centerOf(airtightPumpPos), util.vector().centerOf(airtightPumpPos));
        AABB chamberArea = new AABB(util.vector().centerOf(chamberLeftPos), util.vector().centerOf(chamberRightPos));
        AABB outletArea = new AABB(util.vector().centerOf(outletPos), util.vector().centerOf(outletPos));
        AABB outletUpArea = new AABB(util.vector().centerOf(fluidPipePos), util.vector().centerOf(fluidPipePos));
        AABB funnelArea = new AABB(util.vector().centerOf(funnelPos), util.vector().centerOf(funnelPos));

        Object gasSupplyObject = new Object();
        Object chamberObject = new Object();
        Object outletObject = new Object();
        Object outletUpObject = new Object();
        Object funnelObject = new Object();

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        ItemStack wrenchItem = new ItemStack(AllItems.WRENCH.asItem());

        scene.idle(20);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(engineBottomSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().showSection(outletSelection, Direction.EAST);

        scene.idle(3);
        scene.world().showSection(chamberSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Generating Stress requires fulfilling multiple conditions...").pointAt(Vec3.atCenterOf(engineBottomPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setBlock(airtightMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST), false);
        scene.world().showSection(gasSupplySelection, Direction.EAST);
        scene.world().setKineticSpeed(airtightPumpSelection, mediumSpeed);
        scene.world().setKineticSpeed(airtightPumpMotorSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(airtightPumpPos);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, gasSupplyObject, gasSupplyArea, 3);

        scene.idle(3);
        gasSupplyArea = gasSupplyArea.inflate(0.5, 0.375, 0.375);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, gasSupplyObject, gasSupplyArea, 3);

        scene.idle(3);
        gasSupplyArea = gasSupplyArea.expandTowards(1, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, gasSupplyObject, gasSupplyArea, 60);
        scene.overlay().showText(60).text("1. Continuous and sufficient gas input").colored(PonderPalette.INPUT).pointAt(Vec3.atCenterOf(airtightPumpPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, chamberObject, chamberArea, 3);

        scene.idle(3);
        chamberArea = chamberArea.inflate(0.5, 0.5, 0.5);
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= 1; j++) {
                scene.world().setBlock(chamberLeftPos.south(i * 2).east(j * 2), CCBBlocks.BREEZE_CHAMBER_BLOCK.getDefaultState().setValue(BreezeChamberBlock.WIND_LEVEL, WindLevel.GALE), false);
                scene.world().modifyBlockEntity(chamberLeftPos.south(i * 2).east(j * 2), BreezeChamberBlockEntity.class, BreezeChamberBlockEntity::SwitchToGaleState);
            }
        }
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, chamberObject, chamberArea, 60);
        scene.overlay().showText(60).text("2. Breeze Chambers maintained in Gale state").colored(PonderPalette.INPUT).pointAt(Vec3.atCenterOf(chamberLeftPos)).placeNearTarget().attachKeyFrame();
        scene.world().setKineticSpeed(engineBottomSelection, 8);
        scene.effects().rotationSpeedIndicator(engineBottomPos);

        scene.idle(80);
        scene.world().setKineticSpeed(engineBottomSelection, 16);
        scene.effects().rotationSpeedIndicator(engineBottomPos);
        scene.world().showSection(residueSelection, Direction.EAST);
        scene.world().showSection(funnelSelection, Direction.SOUTH);
        scene.world().setBlock(fluidMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST), false);
        scene.world().showSection(residueCogSelection, Direction.EAST);
        scene.world().setKineticSpeed(fluidPumpSelection, mediumSpeed);
        scene.world().setKineticSpeed(fluidPumpMotorSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(fluidPumpPos);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outletObject, outletArea, 3);

        scene.idle(3);
        outletArea = outletArea.inflate(0.375, 0.375, 0.375).expandTowards(0, -0.125, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outletObject, outletArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outletUpObject, outletUpArea, 3);

        scene.idle(3);
        outletArea = outletArea.expandTowards(0, 1, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outletObject, outletArea, 63);
        outletUpArea = outletUpArea.inflate(0.375, 0.375, 0.375).expandTowards(-0.125, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outletUpObject, outletUpArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, funnelObject, funnelArea, 3);

        scene.idle(3);
        scene.world().modifyBlockEntity(outletPos, ResidueOutletBlockEntity.class, be -> {
            InternalFluidHandler handler = (InternalFluidHandler) be.getFluidTankBehaviour().getCapability();
            handler.forceFill(new FluidStack(Fluids.WATER, 4000), FluidAction.EXECUTE);
        });
        scene.world().propagatePipeChange(fluidPumpPos);
        outletUpArea = outletUpArea.expandTowards(-1, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outletUpObject, outletUpArea, 60);
        funnelArea = funnelArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, funnelObject, funnelArea, 60);
        scene.overlay().showText(60).text("3. Timely handling of generated Residue").colored(PonderPalette.OUTPUT).pointAt(Vec3.atCenterOf(outletPos)).placeNearTarget().attachKeyFrame();

        scene.idle(51);
        scene.world().setKineticSpeed(engineBottomSelection, 24);
        scene.effects().rotationSpeedIndicator(engineBottomPos);

        scene.idle(29);
        scene.overlay().showText(60).text("This enables Airtight Engines to steadily generate Stress").pointAt(Vec3.atCenterOf(engineBottomPos)).placeNearTarget();

        scene.idle(51);
        scene.world().setKineticSpeed(engineBottomSelection, 32);
        scene.effects().rotationSpeedIndicator(engineBottomPos);

        scene.idle(29);
        scene.world().showSection(engineTopSelection, Direction.SOUTH);

        scene.idle(20);
        scene.world().setKineticSpeed(engineBottomSelection, 16);
        scene.world().setKineticSpeed(engineTopSelection, 16);
        scene.effects().rotationSpeedIndicator(engineBottomPos);
        scene.effects().rotationSpeedIndicator(engineTopPos);
        scene.overlay().showText(60).text("Total generated Stress and rotational speed is evenly distributed per Airtight Engine").colored(PonderPalette.SLOW).pointAt(Vec3.atCenterOf(engineTopPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(util.vector().blockSurface(engineTopPos, Direction.UP), Pointing.DOWN, 67).rightClick().withItem(wrenchItem);

        scene.idle(7);
        scene.world().setKineticSpeed(engineTopSelection, -16);
        scene.effects().rotationSpeedIndicator(engineTopPos);
        scene.overlay().showText(60).text("A Wrench can be used to reverse the direction").pointAt(Vec3.atCenterOf(engineTopPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
