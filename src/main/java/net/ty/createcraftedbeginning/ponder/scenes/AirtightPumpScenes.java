package net.ty.createcraftedbeginning.ponder.scenes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

public class AirtightPumpScenes {
    public static void scene(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("airtight_pump", "Moving Compressed Air using Airtight Pumps");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos lowerCreativeTankPos = util.grid().at(4, 1, 2);
        BlockPos upperCreativeTankPos = util.grid().at(4, 2, 2);
        BlockPos lowerTankPos = util.grid().at(0, 1, 2);
        BlockPos upperTankPos = util.grid().at(0, 2, 2);
        BlockPos leftPipePos = util.grid().at(3, 2, 2);
        BlockPos pumpPos = util.grid().at(2, 2, 2);
        BlockPos rightPipePos = util.grid().at(1, 2, 2);
        BlockPos cogPos = util.grid().at(2, 2, 3);
        BlockPos motorPos = util.grid().at(3, 2, 3);

        Selection creativeTankSelection = util.select().fromTo(lowerCreativeTankPos, upperCreativeTankPos);
        Selection tankSelection = util.select().fromTo(lowerTankPos, upperTankPos);
        Selection pumpsSelection = util.select().fromTo(leftPipePos, rightPipePos);
        Selection leftPipeSelection = util.select().fromTo(leftPipePos, leftPipePos);
        Selection rightPipeSelection = util.select().fromTo(rightPipePos, rightPipePos);
        Selection sourceSelection = util.select().fromTo(cogPos, motorPos);

        FluidStack lava = new FluidStack(Fluids.LAVA, 8000);

        ItemStack lavaBucket = new ItemStack(Items.LAVA_BUCKET);

        scene.world().setBlock(leftPipePos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(AirtightPipeBlock.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(pumpPos, CCBBlocks.AIRTIGHT_PUMP_BLOCK.getDefaultState().setValue(PumpBlock.FACING, Direction.WEST), false);
        scene.world().setBlock(rightPipePos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(AirtightPipeBlock.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(cogPos, AllBlocks.COGWHEEL.getDefaultState().setValue(CogWheelBlock.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST), false);
        scene.world().showSection(creativeTankSelection, Direction.DOWN);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(5);
        scene.world().showSection(pumpsSelection, Direction.DOWN);

        scene.idle(5);
        scene.world().showSection(sourceSelection, Direction.DOWN);

        scene.idle(20);
        scene.world().setKineticSpeed(sourceSelection, 128);
        scene.world().setKineticSpeed(pumpsSelection, -128);
        scene.effects().rotationSpeedIndicator(motorPos);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.overlay().showOutline(PonderPalette.FAST, new Object(), pumpsSelection, 60);
        scene.overlay().showText(60).text("Airtight Pumps paired with Airtight Pipes can transport Compressed Air at high rotation speed").colored(PonderPalette.FAST).pointAt(Vec3.atCenterOf(rightPipePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().replaceBlocks(leftPipeSelection, AllBlocks.GLASS_FLUID_PIPE.getDefaultState().setValue(GlassFluidPipeBlock.AXIS, Direction.Axis.X), true);
        scene.world().replaceBlocks(rightPipeSelection, AllBlocks.GLASS_FLUID_PIPE.getDefaultState().setValue(GlassFluidPipeBlock.AXIS, Direction.Axis.X), true);
        scene.overlay().showControls(util.vector().blockSurface(upperCreativeTankPos, Direction.UP).subtract(0, 1 / 8f, 0), Pointing.DOWN, 60).rightClick().withItem(lavaBucket);

        scene.idle(5);
        scene.world().modifyBlockEntity(lowerCreativeTankPos, CreativeFluidTankBlockEntity.class, be -> ((CreativeFluidTankBlockEntity.CreativeSmartFluidTank) be.getTankInventory()).setContainedFluid(lava));
        scene.world().modifyBlockEntity(pumpPos, AirtightPumpBlockEntity.class, be -> be.onSpeedChanged(0));
        scene.overlay().showText(60).text("Similarly to Airtight Pipes, Airtight Pumps cannot transport fluids").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(pumpPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
