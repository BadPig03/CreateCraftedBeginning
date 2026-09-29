package net.ty.createcraftedbeginning.gametests.gas.network.solver.endpoint;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointDiscovery.DiscoveredEndpoints;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointDiscovery.PressureBoundaryAccess;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointPlanner;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor.PlannedDrain;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor.PlannedFill;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor.PooledTransferExecutionResult;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasEndpointDiscoveryGameTests {
    private GasEndpointDiscoveryGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void highPressureOnlyBoundarySurvivesDiscoveryAndUsesActualTransferPressure(GameTestHelper helper) {
        HighPressureBoundary boundary = new HighPressureBoundary();
        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 1);
        GasEndpointPlanner planner = planner(helper, boundary);
        helper.assertValueEqual(boundary.fill(gas, GasAction.SIMULATE), 0L, "Legacy fill must reject the high-pressure-only gas");
        List<GasNetworkPressureEndpoint> endpoints = planner.planPressureEndpoints(helper.getLevel(), gas);
        helper.assertValueEqual(endpoints.size(), 1, "High-pressure-only fill endpoint count");
        GasNetworkPressureEndpoint endpoint = endpoints.getFirst();
        long highPressure = GameplayPressureProfiles.HIGH_PRESSURE.minimumPressurePa();
        helper.assertValueEqual(endpoint.maxSafeFillAmount(gas, highPressure, 10), 10L, "High-pressure continuous fill");
        helper.assertValueEqual(endpoint.maxSafeFillAmount(gas, highPressure - 1, 10), 0L, "Low-pressure continuous fill");
        helper.assertValueEqual(endpoint.maxQuantizedFillAmount(gas, 1, highPressure), 1L, "High-pressure quantized fill");
        helper.assertValueEqual(endpoint.maxQuantizedFillAmount(gas, 1, highPressure - 1), 0L, "Low-pressure quantized fill");
        helper.assertValueEqual(boundary.received, 0L, "Discovery and planning must not consume gas");

        GasTank source = new GasTank(100, GasPressure.pascals(20));
        source.tryReplaceContents(gas.copyWithAmount(20)).requireAccepted();
        GasHandler handler = endpoint.access().fillHandler();
        if (handler == null) {
            return;
        }

        PooledTransferExecutionResult result = GasTransferExecutor.executePooledTransfer(gas, List.of(new PlannedDrain(source, 10)), List.of(new PlannedFill(handler, 10, highPressure)));
        helper.assertValueEqual(result.filledAmounts()[0], 10L, "High-pressure execution");
        helper.assertValueEqual(boundary.received, 10L, "High-pressure boundary received amount");
        helper.assertValueEqual(source.getStoredAmount() + boundary.received, 20L, "Conserved gas after pressure-aware transfer");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void pressureAwareDiscoveryStillRejectsUnsupportedGas(GameTestHelper helper) {
        HighPressureBoundary boundary = new HighPressureBoundary();
        GasEndpointPlanner planner = planner(helper, boundary);
        helper.assertTrue(planner.planPressureEndpoints(helper.getLevel(), new GasStack(CCBGases.STEAM.get(), 1)).isEmpty(), "Pressure probe admitted an unsupported gas");
        helper.assertValueEqual(boundary.received, 0L, "Rejected discovery consumed gas");
        helper.succeed();
    }

    private static GasEndpointPlanner planner(GameTestHelper helper, GasPressureBoundary boundary) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));
        BlockFace face = new BlockFace(helper.absolutePos(pos), Direction.EAST);
        return GasEndpointPlanner.prepare(new DiscoveredEndpoints(List.of(), List.of(new PressureBoundaryAccess(face, face.getOpposite(), boundary, null)), List.of()));
    }

    private static final class HighPressureBoundary implements GasPressureBoundary {
        private long received;

        @Override
        public boolean isGasValid(int tank, GasStack stack) {
            return tank == 0 && stack.is(CCBGases.NATURAL_AIR);
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            return GasStack.EMPTY;
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            return GasStack.EMPTY;
        }

        @Override
        public GasStack getGasInTank(int tank) {
            return GasStack.EMPTY;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            return fillFromPressure(resource, GasPressure.REFERENCE_PRESSURE_PA, action);
        }

        @Override
        public long fillFromPressure(GasStack resource, long sourcePressurePa, GasAction action) {
            if (!isGasValid(0, resource) || sourcePressurePa < GameplayPressureProfiles.HIGH_PRESSURE.minimumPressurePa()) {
                return 0;
            }

            long accepted = Math.min(100 - received, resource.getAmount());
            if (action.execute()) {
                received += accepted;
            }
            return accepted;
        }
    }
}
