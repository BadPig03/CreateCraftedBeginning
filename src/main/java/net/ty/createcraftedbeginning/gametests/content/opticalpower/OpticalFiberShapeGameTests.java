package net.ty.createcraftedbeginning.gametests.content.opticalpower;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.opticalfiber.OpticalFiberBlock;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OpticalFiberShapeGameTests {
    private OpticalFiberShapeGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void allConnectionsMatchModelGeometry(GameTestHelper helper) throws IOException {
        OpticalFiberBlock fiber = CCBBlocks.OPTICAL_FIBER_BLOCK.get();
        JsonArray multipart = readResource("blockstates/optical_fiber").getAsJsonArray("multipart");
        Map<String, VoxelShape> models = new HashMap<>();
        for (JsonElement part : multipart) {
            String name = part.getAsJsonObject().getAsJsonObject("apply").get("model").getAsString();
            JsonObject model = readResource("models/" + name.substring(name.indexOf(':') + 1));
            VoxelShape shape = Shapes.empty();
            for (JsonElement element : model.getAsJsonArray("elements")) {
                JsonArray from = element.getAsJsonObject().getAsJsonArray("from");
                JsonArray to = element.getAsJsonObject().getAsJsonArray("to");
                if (from.get(0).equals(to.get(0)) || from.get(1).equals(to.get(1)) || from.get(2).equals(to.get(2))) {
                    continue;
                }

                shape = Shapes.or(shape, Block.box(from.get(0).getAsDouble(), from.get(1).getAsDouble(), from.get(2).getAsDouble(), to.get(0).getAsDouble(), to.get(1).getAsDouble(), to.get(2).getAsDouble()));
            }
            models.put(name, shape);
        }
        for (BlockState state : fiber.getStateDefinition().getPossibleStates()) {
            VoxelShape expected = Shapes.empty();
            List<String> selected = new ArrayList<>();
            for (JsonElement element : multipart) {
                JsonObject part = element.getAsJsonObject();
                if (!matches(part.getAsJsonObject("when"), state)) {
                    continue;
                }

                String name = part.getAsJsonObject("apply").get("model").getAsString();
                VoxelShape shape = models.get(name);
                if (shape == null) {
                    throw new NullPointerException("Missing optical fiber model shape: " + name);
                }

                selected.add(name.substring("createcraftedbeginning:block/optical_fiber/".length()));
                expected = Shapes.or(expected, shape);
            }
            List<Direction> connections = new ArrayList<>();
            for (Direction direction : Iterate.directions) {
                if (state.getValue(OpticalFiberBlock.PROPERTY_BY_DIRECTION.get(direction))) {
                    connections.add(direction);
                }
            }
            if (connections.size() == 1) {
                helper.assertTrue(selected.equals(List.of("end/" + connections.getFirst().getSerializedName())), "Fiber endpoint did not select its metal end: " + state);
            }
            else if (connections.size() == 2 && connections.getFirst().getOpposite() == connections.getLast()) {
                helper.assertTrue(selected.equals(List.of("straight/" + connections.getFirst().getAxis().getSerializedName())), "Straight fiber retained an intermediate metal port: " + state);
            }
            else {
                helper.assertTrue(selected.contains("junction") && selected.size() == connections.size() + 1, "Fiber junction did not select a hub and all its arms: " + state);
            }
            helper.assertTrue(!Shapes.joinIsNotEmpty(state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO), expected, BooleanOp.NOT_SAME), "Optical fiber outline differs from model geometry: " + state);
            helper.assertTrue(!Shapes.joinIsNotEmpty(state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO), expected, BooleanOp.NOT_SAME), "Optical fiber collision differs from model geometry: " + state);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void devicePortsFollowNeighborsAndOverlapOnePixel(GameTestHelper helper) throws IOException {
        BlockPos center = new BlockPos(1, 1, 1);
        BlockPos absoluteCenter = helper.absolutePos(center);
        BlockState fiber = CCBBlocks.OPTICAL_FIBER_BLOCK.getDefaultState();
        for (Direction direction : Iterate.directions) {
            fiber = fiber.setValue(OpticalFiberBlock.PROPERTY_BY_DIRECTION.get(direction), true);
            helper.setBlock(center.relative(direction), Blocks.AIR);
        }
        VoxelShape base = fiber.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        for (Direction direction : Iterate.directions) {
            BlockPos neighbor = center.relative(direction);
            int portMask = 1 << direction.get3DDataValue();
            helper.setBlock(neighbor, CCBBlocks.AMETHYST_COLLECTOR_PANEL_BLOCK.getDefaultState());
            helper.assertTrue(OpticalFiberBlock.getDeviceConnections(helper.getLevel(), absoluteCenter, fiber) == portMask, "Collector did not add a port on " + direction);

            JsonObject portModel = readResource("models/block/optical_fiber/rim/" + direction.getSerializedName());
            JsonObject element = portModel.getAsJsonArray("elements").get(0).getAsJsonObject();
            JsonArray from = element.getAsJsonArray("from");
            JsonArray to = element.getAsJsonArray("to");
            int[] steps = {direction.getStepX(), direction.getStepY(), direction.getStepZ()};
            for (int axis = 0; axis < steps.length; axis++) {
                double expectedFrom = steps[axis] > 0 ? 14 : steps[axis] < 0 ? -1 : 5;
                double expectedTo = steps[axis] > 0 ? 17 : steps[axis] < 0 ? 2 : 11;
                helper.assertTrue(from.get(axis).getAsDouble() == expectedFrom && to.get(axis).getAsDouble() == expectedTo, "Device port does not leave two pixels outside and one inside on " + direction);
            }
            VoxelShape modelShape = Block.box(from.get(0).getAsDouble(), from.get(1).getAsDouble(), from.get(2).getAsDouble(), to.get(0).getAsDouble(), to.get(1).getAsDouble(), to.get(2).getAsDouble());
            helper.assertTrue(!Shapes.joinIsNotEmpty(modelShape, CCBShapes.OPTICAL_FIBER_DEVICE_PORT.get(direction), BooleanOp.NOT_SAME), "Device port shape differs from its model on " + direction);
            VoxelShape expected = Shapes.or(base, modelShape);
            helper.assertTrue(!Shapes.joinIsNotEmpty(fiber.getShape(helper.getLevel(), absoluteCenter), expected, BooleanOp.NOT_SAME), "Device port missing from selection shape on " + direction);
            helper.assertTrue(!Shapes.joinIsNotEmpty(fiber.getCollisionShape(helper.getLevel(), absoluteCenter), expected, BooleanOp.NOT_SAME), "Device port missing from collision shape on " + direction);

            helper.setBlock(neighbor, CCBBlocks.OPTICAL_FIBER_BLOCK.getDefaultState());
            helper.assertTrue(OpticalFiberBlock.getDeviceConnections(helper.getLevel(), absoluteCenter, fiber) == 0, "Fiber-to-fiber connection retained a device port on " + direction);
            helper.assertTrue(!Shapes.joinIsNotEmpty(fiber.getShape(helper.getLevel(), absoluteCenter), base, BooleanOp.NOT_SAME), "Replacing a device with fiber retained its cached port on " + direction);

            BlockState emitter = CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, direction);
            helper.setBlock(neighbor, emitter);
            helper.assertTrue(OpticalFiberBlock.getDeviceConnections(helper.getLevel(), absoluteCenter, fiber) == portMask, "Emitter input did not add a device port on " + direction);
            helper.setBlock(neighbor, emitter.setValue(DirectionalBlock.FACING, direction.getOpposite()));
            helper.assertTrue(OpticalFiberBlock.getDeviceConnections(helper.getLevel(), absoluteCenter, fiber) == 0, "Emitter output incorrectly added a device port on " + direction);
            helper.setBlock(neighbor, Blocks.GLOWSTONE);
            helper.assertTrue(OpticalFiberBlock.getDeviceConnections(helper.getLevel(), absoluteCenter, fiber) == portMask, "Full-bright light source did not add a device port on " + direction);
            helper.setBlock(neighbor, Blocks.AIR);
            helper.assertTrue(OpticalFiberBlock.getDeviceConnections(helper.getLevel(), absoluteCenter, fiber) == 0, "Removed device retained a port on " + direction);
        }
        VoxelShape allPorts = base;
        for (Direction direction : Iterate.directions) {
            helper.setBlock(center.relative(direction), CCBBlocks.AMETHYST_COLLECTOR_PANEL_BLOCK.getDefaultState());
            allPorts = Shapes.or(allPorts, CCBShapes.OPTICAL_FIBER_DEVICE_PORT.get(direction));
        }
        helper.assertTrue(OpticalFiberBlock.getDeviceConnections(helper.getLevel(), absoluteCenter, fiber) == (1 << Iterate.directions.length) - 1, "Multiple connected devices lost a port");
        helper.assertTrue(!Shapes.joinIsNotEmpty(fiber.getCollisionShape(helper.getLevel(), absoluteCenter), allPorts, BooleanOp.NOT_SAME), "Multiple device ports did not combine in the collision shape");
        helper.succeed();
    }

    private static JsonObject readResource(String path) throws IOException {
        String resource = "/assets/createcraftedbeginning/" + path + ".json";
        InputStream stream = OpticalFiberShapeGameTests.class.getResourceAsStream(resource);
        if (stream == null) {
            throw new NullPointerException("Optical fiber model resource is missing: " + resource);
        }

        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static boolean matches(JsonObject condition, BlockState state) {
        if (condition.has("OR")) {
            for (JsonElement child : condition.getAsJsonArray("OR")) {
                if (matches(child.getAsJsonObject(), state)) {
                    return true;
                }
            }
            return false;
        }
        if (condition.has("AND")) {
            for (JsonElement child : condition.getAsJsonArray("AND")) {
                if (!matches(child.getAsJsonObject(), state)) {
                    return false;
                }
            }
            return true;
        }
        for (Direction direction : Iterate.directions) {
            String name = direction.getSerializedName();
            if (condition.has(name) && condition.get(name).getAsBoolean() != state.getValue(OpticalFiberBlock.PROPERTY_BY_DIRECTION.get(direction))) {
                return false;
            }
        }
        return true;
    }
}
