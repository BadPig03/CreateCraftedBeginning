package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightmeters.AirtightFlowmeterBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightmeters.AirtightManometerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowDirection;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowState;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasOnDemandPipeTelemetryGameTests {
    private static final BlockPos SOURCE_POS = new BlockPos(0, 1, 1);
    private static final BlockPos PIPE_POS = new BlockPos(1, 1, 1);
    private static final BlockPos METER_POS = new BlockPos(2, 1, 1);
    private static final long SOURCE_PRESSURE_PA = 2 * GasPressure.REFERENCE_PRESSURE_PA;

    private GasOnDemandPipeTelemetryGameTests() {
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 80)
    public static void manometerOwnsPressureTelemetry(GameTestHelper helper) {
        placeSource(helper);
        helper.setBlock(PIPE_POS, CCBBlocks.AIRTIGHT_MANOMETER_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));

        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(PIPE_POS));
        helper.assertTrue(blockEntity instanceof AirtightManometerBlockEntity, "Airtight manometer block entity was not initialized");
        if (!(blockEntity instanceof AirtightManometerBlockEntity manometer)) {
            return;
        }

        helper.succeedWhen(() -> {
            helper.assertTrue(manometer.hasPressureReading(), "Airtight manometer did not receive on-demand pressure telemetry");
            helper.assertTrue(manometer.getMinPressurePa() >= GasPressure.VACUUM_PA, "Airtight manometer reported pressure below vacuum");
            helper.assertTrue(manometer.getMaxPressurePa() <= SOURCE_PRESSURE_PA, "Airtight manometer pressure exceeded its source boundary");
            helper.assertTrue(manometer.getPressureDifferencePa() > 0, "Airtight manometer did not measure the source-to-atmosphere pressure gradient");
        });
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 80)
    public static void flowmeterOwnsFlowTelemetryWhileOrdinaryPipeRetainsServerFlowState(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeSource(helper);
        helper.setBlock(PIPE_POS, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));
        helper.setBlock(METER_POS, CCBBlocks.AIRTIGHT_FLOWMETER_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));

        BlockEntity meter = level.getBlockEntity(helper.absolutePos(METER_POS));
        helper.assertTrue(meter instanceof AirtightFlowmeterBlockEntity, "Airtight flowmeter block entity was not initialized");
        if (!(meter instanceof AirtightFlowmeterBlockEntity flowmeter)) {
            return;
        }

        helper.succeedWhen(() -> {
            GasTransportBehaviour ordinaryPipe = BlockEntityBehaviour.get(level, helper.absolutePos(PIPE_POS), GasTransportBehaviour.TYPE);
            helper.assertTrue(ordinaryPipe != null, "Ordinary airtight pipe transport behaviour was not initialized");
            if (ordinaryPipe == null) {
                throw new NullPointerException("Ordinary airtight pipe transport behaviour was not initialized.");
            }

            FlowState inletFlow = ordinaryPipe.getFlowState(Direction.WEST);
            FlowState outletFlow = ordinaryPipe.getFlowState(Direction.EAST);
            helper.assertTrue(inletFlow != null, "Ordinary pipe lost its server-side inbound FlowState");
            if (inletFlow == null) {
                throw new NullPointerException("Required inlet flow is missing.");
            }

            helper.assertTrue(inletFlow.direction() == FlowDirection.INBOUND && inletFlow.flowRate() > 0, "Ordinary pipe lost its server-side inbound FlowState");
            helper.assertTrue(outletFlow != null, "Ordinary pipe lost its server-side outbound FlowState");
            if (outletFlow == null) {
                throw new NullPointerException("Required outlet flow is missing.");
            }

            helper.assertTrue(outletFlow.direction() == FlowDirection.OUTBOUND && outletFlow.flowRate() > 0, "Ordinary pipe lost its server-side outbound FlowState");
            helper.assertTrue(flowmeter.getFlowRate() > 0, "Airtight flowmeter did not receive its final on-demand flow telemetry");
            helper.assertValueEqual(flowmeter.getFlowRate(), ordinaryPipe.getThroughputFlowRate(), "flowmeter versus upstream server throughput");
        });
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 80)
    public static void ordinaryPipeClientBehaviourPacketOmitsRuntimeTelemetry(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeSource(helper);
        helper.setBlock(PIPE_POS, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));

        helper.succeedWhen(() -> {
            GasTransportBehaviour ordinaryPipe = BlockEntityBehaviour.get(level, helper.absolutePos(PIPE_POS), GasTransportBehaviour.TYPE);
            helper.assertTrue(ordinaryPipe != null, "Ordinary airtight pipe transport behaviour was not initialized");
            if (ordinaryPipe == null) {
                throw new NullPointerException("Ordinary airtight pipe transport behaviour was not initialized.");
            }

            FlowState flow = ordinaryPipe.getFlowState(Direction.WEST);
            helper.assertTrue(flow != null, "Ordinary pipe did not retain server-side runtime flow before client-packet characterization");
            if (flow == null) {
                throw new NullPointerException("Required flow is missing.");
            }

            helper.assertTrue(flow.flowRate() > 0, "Ordinary pipe did not retain server-side runtime flow before client-packet characterization");
            CompoundTag clientPacket = new CompoundTag();
            ordinaryPipe.write(clientPacket, level.registryAccess(), true);
            helper.assertTrue(clientPacket.isEmpty(), "Ordinary pipe transport behaviour serialized runtime telemetry into a client packet");
        });
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void clientConnectionPacketOmitsServerFlowState(GameTestHelper helper) {
        GasPipeConnection connection = new GasPipeConnection(Direction.EAST);
        helper.assertTrue(connection.setFlowState(new GasStack(CCBGases.NATURAL_AIR.get(), 1), false, 1234), "Failed to seed server-side FlowState for client-packet characterization");

        CompoundTag clientPacket = new CompoundTag();
        connection.write(clientPacket, helper.getLevel().registryAccess(), true);
        helper.assertTrue(clientPacket.isEmpty(), "GasPipeConnection serialized server FlowState into a client packet");
        helper.assertTrue(connection.getFlowState() != null, "Writing a client packet mutated the server-side FlowState");
        helper.succeed();
    }

    private static void placeSource(GameTestHelper helper) {
        helper.setBlock(SOURCE_POS, CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(SOURCE_POS));
        helper.assertTrue(blockEntity instanceof CreativeAirtightTankBlockEntity, "Creative airtight tank block entity was not initialized");
        if (!(blockEntity instanceof CreativeAirtightTankBlockEntity source)) {
            throw new IllegalStateException("Creative airtight tank block entity was not initialized at " + SOURCE_POS + '.');
        }

        source.getTankInventory().setFixedPressurePa(SOURCE_PRESSURE_PA);
        source.getTankInventory().setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
    }
}
