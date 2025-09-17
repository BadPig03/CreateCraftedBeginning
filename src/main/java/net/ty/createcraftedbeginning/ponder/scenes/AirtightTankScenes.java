package net.ty.createcraftedbeginning.ponder.scenes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllFluids;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.foundation.gui.AllIcons;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

public class AirtightTankScenes {
    public static void scene(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_tank", "Storing Compressed Air using Airtight Tanks");
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
        Selection sourceSelection = util.select().fromTo(cogPos, motorPos);

        ItemStack honeyBucket = new ItemStack(AllFluids.HONEY.getBucket().orElse(Items.AIR));
        ItemStack wrench = new ItemStack(AllItems.WRENCH.asItem());

        FluidStack honey = new FluidStack(AllFluids.HONEY, 8000);

        scene.world().setBlock(leftPipePos, AllBlocks.GLASS_FLUID_PIPE.getDefaultState().setValue(GlassFluidPipeBlock.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(pumpPos, AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, Direction.WEST), false);
        scene.world().setBlock(rightPipePos, AllBlocks.GLASS_FLUID_PIPE.getDefaultState().setValue(GlassFluidPipeBlock.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(cogPos, AllBlocks.COGWHEEL.getDefaultState().setValue(CogWheelBlock.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST), false);
        scene.world().modifyBlockEntity(lowerCreativeTankPos, CreativeFluidTankBlockEntity.class, be -> ((CreativeFluidTankBlockEntity.CreativeSmartFluidTank) be.getTankInventory()).setContainedFluid(honey));
        scene.world().showSection(creativeTankSelection, Direction.DOWN);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(5);
        scene.world().showSection(pumpsSelection, Direction.DOWN);

        scene.idle(5);
        scene.world().showSection(sourceSelection, Direction.DOWN);
        scene.world().setKineticSpeed(sourceSelection, 64);
        scene.world().setKineticSpeed(pumpsSelection, -64);
        scene.effects().rotationSpeedIndicator(motorPos);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.world().propagatePipeChange(pumpPos);

        scene.idle(10);
        scene.overlay().showOutline(PonderPalette.RED, new Object(), tankSelection, 60);
        scene.overlay().showText(60).text("Airtight Tanks cannot store any fluids; they exclusively store Compressed Air").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(upperTankPos)).placeNearTarget().attachKeyFrame();

        scene.idle(20);
        scene.overlay().showControls(util.vector().blockSurface(upperTankPos, Direction.NORTH), Pointing.RIGHT, 40).showing(AllIcons.I_MTD_CLOSE).withItem(honeyBucket);

        scene.idle(60);
        scene.overlay().showControls(util.vector().blockSurface(pumpPos, Direction.UP), Pointing.DOWN, 10).rightClick().withItem(wrench);

        scene.idle(7);
        scene.world().modifyBlock(pumpPos, s -> s.setValue(PumpBlock.FACING, Direction.EAST), false);
        scene.world().propagatePipeChange(pumpPos);

        scene.idle(13);
        scene.markAsFinished();
    }

    public static void max(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_tank_max", "Size of Airtight Tanks");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos tankPos = util.grid().at(2, 4, 2);

        Selection tankSelection = util.select().fromTo(1, 1, 1, 3, 4, 3);

        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(10);
        scene.overlay().showOutline(PonderPalette.GREEN, new Object(), tankSelection, 60);
        scene.overlay().showText(60).text("Airtight Tanks can be combined up to 3x3x4 to increase total capacity").colored(PonderPalette.GREEN).pointAt(Vec3.atCenterOf(tankPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
