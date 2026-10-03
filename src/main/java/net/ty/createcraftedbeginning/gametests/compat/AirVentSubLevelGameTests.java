package net.ty.createcraftedbeginning.gametests.compat;

import com.mojang.authlib.GameProfile;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData.DataValue;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.compat.CCBCompatMods;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlock.VentState;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentTraversal;
import net.ty.createcraftedbeginning.gametests.compat.SubLevelGameTestFixtures.Fixture;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.joml.Quaterniond;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static net.ty.createcraftedbeginning.gametests.compat.SubLevelGameTestFixtures.assemble;
import static net.ty.createcraftedbeginning.gametests.compat.SubLevelGameTestFixtures.clear;
import static net.ty.createcraftedbeginning.gametests.compat.SubLevelGameTestFixtures.move;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AirVentSubLevelGameTests {
    private static final double CRAWLING_HEIGHT_EPSILON = 1.0E-6;

    @GameTest(template = "gametest/empty_20x12x20")
    public static void localEntry(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(8, 4, 8));
        level.setBlockAndUpdate(pos, CCBBlocks.AIR_VENT_BLOCK.getDefaultState());
        AirVentBlockEntity vent = requireVent(level, pos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (Direction face : Iterate.directions) {
            Vec3 position = Vec3.atCenterOf(pos.relative(face));
            Vec3 look = Vec3.atLowerCornerOf(face.getOpposite().getNormal());
            vent.setLouverState(face, VentState.CLOSED);
            assertEntry(helper, player, position, look, true, false, "closed " + face);
            vent.setLouverState(face, VentState.OPENED);
            assertEntry(helper, player, position, look, true, true, "open " + face);
            assertEntry(helper, player, position, look, false, false, "not sneaking " + face);
            assertEntry(helper, player, position, look.reverse(), true, false, "looking away " + face);
            vent.setLouverState(face, VentState.EMPTY);
            assertEntry(helper, player, position, look, true, false, "missing louver " + face);
        }
        vent.setLouverState(Direction.DOWN, VentState.OPENED);
        assertEntry(helper, player, Vec3.atCenterOf(pos.below(2)), new Vec3(0, 1, 0), true, true, "overhead entry");
        assertEntry(helper, player, Vec3.atCenterOf(pos), new Vec3(0, 0, 1), false, true, "already inside");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_20x12x20")
    public static void localCrawling(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(8, 4, 8));
        level.setBlockAndUpdate(pos, CCBBlocks.AIR_VENT_BLOCK.getDefaultState());
        level.setBlockAndUpdate(pos.east(), CCBBlocks.AIR_VENT_BLOCK.getDefaultState());
        CrawlPlayer player = new CrawlPlayer(level, pos);
        assertPoseIndependentOccupancy(helper, player, Vec3.atLowerCornerOf(pos).add(0.5, 0.9999, 0.5), false, "local roof contact");
        assertPoseIndependentOccupancy(helper, player, Vec3.atLowerCornerOf(pos).add(0.5, -0.02, 0.5), true, "local floor contact");
        assertCeilingCrawling(helper, player, Vec3.atLowerCornerOf(pos)::add);
        assertSyncedPoseProtection(helper, player, Vec3.atLowerCornerOf(pos)::add);
        player.setPose(Pose.SWIMMING);
        for (double x : new double[]{0.5, 0.95, 1, 1.05, 1.5, 2.1}) {
            player.setPos(Vec3.atLowerCornerOf(pos).add(x, 0.02, 0.5));
            assertCrawling(helper, player, true, "local seam or exit at " + x);
        }
        player.setPos(Vec3.atLowerCornerOf(pos).add(2.4, 0.02, 0.5));
        assertCrawling(helper, player, false, "fully outside");
        player.setPose(Pose.SWIMMING);
        player.setPos(Vec3.atLowerCornerOf(pos).add(0.5, 1.02, 0.5));
        assertCrawling(helper, player, false, "on the roof");
        player.setPos(Vec3.atLowerCornerOf(pos).add(0.5, 0.02, 0.5));
        player.setForcedPose(Pose.STANDING);
        assertCrawling(helper, player, false, "another forced pose");
        player.setForcedPose(null);
        player.spectator = true;
        assertCrawling(helper, player, false, "spectator");
        player.spectator = false;
        assertCrawling(helper, player, true, "back inside");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x5x5")
    public static void directionalStateRotation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos ventPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockState vent = CCBBlocks.AIR_VENT_BLOCK.getDefaultState();
        for (Direction face : Iterate.directions) {
            BooleanProperty property = (BooleanProperty) vent.getBlock().getStateDefinition().getProperty(face.getName());
            if (property == null) {
                throw new NullPointerException("Expected the vent connection property for " + face + '.');
            }

            for (Rotation rotation : Rotation.values()) {
                BlockState rotated = vent.setValue(property, true).rotate(level, ventPos, rotation);
                for (Direction target : Iterate.directions) {
                    BooleanProperty targetProperty = (BooleanProperty) vent.getBlock().getStateDefinition().getProperty(target.getName());
                    if (targetProperty == null) {
                        throw new NullPointerException("Expected the rotated vent connection property for " + target + '.');
                    }

                    helper.assertValueEqual(rotated.getValue(targetProperty), target == rotation.rotate(face), "vent connection " + face + " after " + rotation);
                }
                CompoundTag tag = new CompoundTag();
                tag.putInt("LouverMask", 1 << face.get3DDataValue());
                tag.putInt("OpenedMask", 1 << face.get3DDataValue());
                AirVentBlockEntity.transformLouverNbt(tag, pos -> pos.rotate(rotation).offset(31, 7, -19));
                int expected = 1 << rotation.rotate(face).get3DDataValue();
                helper.assertValueEqual(tag.getInt("LouverMask"), expected, "rotated louver presence");
                helper.assertValueEqual(tag.getInt("OpenedMask"), expected, "rotated louver opening");
            }
        }
        helper.succeed();
    }

    @GameTestGenerator
    public static Collection<TestFunction> physicalVents() {
        if (!CCBCompatMods.SIMULATED.isLoaded() || !CCBCompatMods.SABLE.isLoaded()) {
            return List.of();
        }

        String template = CCBAPI.MOD_ID + ":gametest/empty_20x12x20";
        return List.of(new TestFunction("vent_entry", "vent_entry.rotated", template, 100, 0, true, AirVentSubLevelGameTests::physicalEntry), new TestFunction("vent_crawling", "vent_crawling.continuous", template, 100, 0, true, AirVentSubLevelGameTests::physicalCrawling), new TestFunction("vent_landing", "vent_landing.louvers", template, 100, 0, true, AirVentSubLevelGameTests::physicalLanding));
    }

    private static void physicalEntry(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(8, 4, 8));
        level.setBlockAndUpdate(pos, CCBBlocks.AIR_VENT_BLOCK.getDefaultState());
        Fixture fixture = assemble(level, pos, Set.of(pos));
        try {
            AirVentBlockEntity vent = requireVent(level, fixture.center());
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            List<Quaterniond> orientations = List.of(new Quaterniond(), new Quaterniond().rotationY(Math.PI / 2), new Quaterniond().rotationY(Math.PI / 4), new Quaterniond().rotationX(Math.PI / 2));
            for (Quaterniond orientation : orientations) {
                move(fixture, Vec3.atCenterOf(pos).add(0.3, 0.2, 0.4), orientation);
                for (Direction face : Iterate.directions) {
                    Vec3 position = fixture.subLevel().logicalPose().transformPosition(Vec3.atCenterOf(fixture.center().relative(face)));
                    Vec3 look = fixture.subLevel().logicalPose().transformNormal(Vec3.atLowerCornerOf(face.getOpposite().getNormal()));
                    vent.setLouverState(face, VentState.CLOSED);
                    assertEntry(helper, player, position, look, true, false, "physical closed " + face);
                    vent.setLouverState(face, VentState.OPENED);
                    assertEntry(helper, player, position, look, true, true, "physical open " + face);
                    assertEntry(helper, player, position, look, false, false, "physical not sneaking " + face);
                    assertEntry(helper, player, position, look.reverse(), true, false, "physical looking away " + face);
                    vent.setLouverState(face, VentState.EMPTY);
                }
                vent.setLouverState(Direction.DOWN, VentState.OPENED);
                Vec3 below = fixture.subLevel().logicalPose().transformPosition(Vec3.atCenterOf(fixture.center().below(2)));
                Vec3 up = fixture.subLevel().logicalPose().transformNormal(new Vec3(0, 1, 0));
                assertEntry(helper, player, below, up, true, true, "physical overhead");
                Vec3 inside = fixture.subLevel().logicalPose().transformPosition(Vec3.atCenterOf(fixture.center()));
                assertEntry(helper, player, inside, up, false, true, "physical already inside");
                vent.setLouverState(Direction.DOWN, VentState.EMPTY);
            }
            vent.setLouverState(Direction.WEST, VentState.OPENED);
            move(fixture, Vec3.atCenterOf(pos), new Quaterniond());
            Vec3 outside = Vec3.atCenterOf(pos.west());
            assertEntry(helper, player, outside, new Vec3(1, 0, 0), true, true, "before moving away");
            move(fixture, Vec3.atCenterOf(pos).add(6, 0, 0), new Quaterniond());
            assertEntry(helper, player, outside, new Vec3(1, 0, 0), true, false, "after moving away");
            helper.succeed();
        }
        finally {
            clear(level, fixture);
        }
    }

    private static void physicalCrawling(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(8, 4, 8));
        level.setBlockAndUpdate(pos, CCBBlocks.AIR_VENT_BLOCK.getDefaultState());
        level.setBlockAndUpdate(pos.east(), CCBBlocks.AIR_VENT_BLOCK.getDefaultState());
        Fixture fixture = assemble(level, pos, Set.of(pos, pos.east()));
        try {
            CrawlPlayer player = new CrawlPlayer(level, pos);
            move(fixture, Vec3.atCenterOf(pos).add(0.3, 0.2, 0.4), new Quaterniond());
            for (double height : new double[]{0.9999, 1, 1.0001}) {
                Vec3 roof = Vec3.atLowerCornerOf(fixture.center()).add(0.5, height, 0.5);
                assertPoseIndependentOccupancy(helper, player, fixture.subLevel().logicalPose().transformPosition(roof), false, "physical roof contact " + height);
            }
            for (double height : new double[]{-0.02, 0, 0.02}) {
                Vec3 floor = Vec3.atLowerCornerOf(fixture.center()).add(0.5, height, 0.5);
                assertPoseIndependentOccupancy(helper, player, fixture.subLevel().logicalPose().transformPosition(floor), true, "physical floor contact " + height);
            }
            List<Quaterniond> orientations = List.of(new Quaterniond(), new Quaterniond().rotationY(Math.PI / 2), new Quaterniond().rotationY(Math.PI / 4), new Quaterniond().rotationX(Math.PI / 2));
            for (Quaterniond orientation : orientations) {
                move(fixture, Vec3.atCenterOf(pos).add(0.3, 0.2, 0.4), orientation);
                UnaryOperator<Vec3> toWorld = offset -> fixture.subLevel().logicalPose().transformPosition(Vec3.atLowerCornerOf(fixture.center()).add(offset));
                assertCeilingCrawling(helper, player, toWorld);
                assertSyncedPoseProtection(helper, player, toWorld);
                Vec3 exit = Vec3.atLowerCornerOf(fixture.center()).add(2.1, 0.2, 0.5);
                assertPoseIndependentOccupancy(helper, player, fixture.subLevel().logicalPose().transformPosition(exit), true, "physical partially outside");
                player.setPose(Pose.SWIMMING);
                for (double x : new double[]{0.5, 0.95, 1, 1.05, 1.5, 2.1}) {
                    Vec3 local = Vec3.atLowerCornerOf(fixture.center()).add(x, 0.2, 0.5);
                    player.setPos(fixture.subLevel().logicalPose().transformPosition(local));
                    assertCrawling(helper, player, true, "physical seam or exit at " + x);
                }
                Vec3 corner = Vec3.atLowerCornerOf(fixture.center()).add(2.3, 0.2, 1.3);
                player.setPos(fixture.subLevel().logicalPose().transformPosition(corner));
                helper.assertTrue(!AirVentTraversal.shouldCrawl(player), "Rotated bounding boxes must not count an exterior corner as interior");
                Vec3 outside = Vec3.atLowerCornerOf(fixture.center()).add(3, 0.2, 0.5);
                player.setPos(fixture.subLevel().logicalPose().transformPosition(outside));
                assertCrawling(helper, player, false, "fully outside rotated vents");
            }
            move(fixture, Vec3.atCenterOf(pos), new Quaterniond());
            player.setPos(Vec3.atLowerCornerOf(pos).add(0.5, 0.02, 0.5));
            player.setPose(Pose.SWIMMING);
            for (int tick = 0; tick < 20; tick++) {
                player.expanded = false;
                player.setDeltaMovement(Vec3.ZERO);
                player.tick();
                helper.assertTrue(player.getPose() == Pose.SWIMMING && !player.expanded, "A full player tick must never expand inside a physical vent");
            }
            move(fixture, Vec3.atCenterOf(pos).add(6, 0, 0), new Quaterniond());
            assertCrawling(helper, player, false, "structure moved away");
            helper.succeed();
        }
        finally {
            clear(level, fixture);
        }
    }

    private static void assertSyncedPoseProtection(GameTestHelper helper, CrawlPlayer player, UnaryOperator<Vec3> toWorld) {
        player.localPlayer = true;
        player.setPos(toWorld.apply(new Vec3(0.5, 0.2, 0.5)));
        assertCrawling(helper, player, true, "before receiving server pose");
        for (Pose receivedPose : new Pose[]{Pose.STANDING, Pose.CROUCHING}) {
            player.receivePose(receivedPose);
            helper.assertValueEqual(player.getPose(), Pose.SWIMMING, "An incoming pose must not expand the local player inside a vent");
            helper.assertTrue(Math.abs(player.getBoundingBox().getYsize() - player.getDimensions(Pose.SWIMMING).height()) < CRAWLING_HEIGHT_EPSILON, "Incoming pose must preserve crawling dimensions before movement");
        }
        player.setPos(toWorld.apply(new Vec3(-0.7, 0.2, 0.5)));
        player.receivePose(Pose.STANDING);
        helper.assertValueEqual(player.getPose(), Pose.STANDING, "An incoming standing pose must be allowed after leaving the vent");
        player.setPos(toWorld.apply(new Vec3(0.5, 0.2, 0.5)));
        assertCrawling(helper, player, true, "reenter before external forced pose");
        player.setForcedPose(Pose.STANDING);
        player.receivePose(Pose.STANDING);
        helper.assertValueEqual(player.getPose(), Pose.STANDING, "An external forced pose must retain priority over vent protection");
        player.setForcedPose(null);
        assertCrawling(helper, player, true, "reenter before remote pose update");
        player.localPlayer = false;
        player.receivePose(Pose.STANDING);
        helper.assertValueEqual(player.getPose(), Pose.STANDING, "Remote player poses must not be overridden by local vent protection");
    }

    private static void assertCeilingCrawling(GameTestHelper helper, CrawlPlayer player, UnaryOperator<Vec3> toWorld) {
        player.setShiftKeyDown(false);
        player.setPos(toWorld.apply(new Vec3(0.5, 0.2, 0.5)));
        helper.assertTrue(AirVentTraversal.shouldCrawl(player), "Ceiling traversal must start inside the vent at " + player.position());
        assertCrawling(helper, player, true, "enter before climbing to ceiling");
        for (double height : new double[]{0.8, 0.84, 0.9, 0.97}) {
            player.setPos(toWorld.apply(new Vec3(0.5, height, 0.5)));
            assertCrawling(helper, player, true, "keep crawling at ceiling height " + height);
        }
        player.setPos(toWorld.apply(new Vec3(0.5, 1.7, 0.5)));
        assertCrawling(helper, player, false, "release after leaving above vent");
        player.setPos(toWorld.apply(new Vec3(0.5, 0.2, 0.5)));
        assertCrawling(helper, player, true, "reenter after leaving above vent");
        player.setPos(toWorld.apply(new Vec3(0.5, 0.9, 0.5)));
        assertCrawling(helper, player, true, "ceiling before horizontal exit");
        player.setPos(toWorld.apply(new Vec3(-0.7, 0.9, 0.5)));
        assertCrawling(helper, player, false, "release after horizontal exit near ceiling");
    }

    private static void assertPoseIndependentOccupancy(GameTestHelper helper, CrawlPlayer player, Vec3 position, boolean expected, String stage) {
        player.setShiftKeyDown(false);
        player.setPos(position);
        for (Pose pose : new Pose[]{Pose.STANDING, Pose.CROUCHING, Pose.SWIMMING}) {
            player.setPose(pose);
            helper.assertValueEqual(AirVentTraversal.shouldCrawl(player), expected, "pose-independent occupancy from " + pose + ": " + stage);
            if (!expected) {
                continue;
            }

            assertCrawling(helper, player, true, stage + " from " + pose);
        }
        player.setPose(Pose.STANDING);
        assertCrawling(helper, player, expected, stage);
        helper.assertValueEqual(player.position(), position, "Pose updates must not move the player: " + stage);
    }

    private static void assertCrawling(GameTestHelper helper, CrawlPlayer player, boolean expected, String stage) {
        float eyeHeight = 0;
        for (int update = 0; update < 10; update++) {
            player.expanded = false;
            player.updatePlayerPose();
            helper.assertValueEqual(player.getPose() == Pose.SWIMMING, expected, "continuous crawling: " + stage);
            helper.assertTrue(!expected || !player.expanded, "No intermediate standing or crouching pose: " + stage);
            if (update == 0) {
                eyeHeight = player.getEyeHeight();
                continue;
            }

            helper.assertValueEqual(player.getEyeHeight(), eyeHeight, "Eye height must remain stable at a fixed position: " + stage);
        }
    }

    private static void physicalLanding(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos start = helper.absolutePos(new BlockPos(4, 3, 4));
        BlockPos target = helper.absolutePos(new BlockPos(12, 3, 12));
        for (Rotation rotation : Rotation.values()) {
            level.setBlockAndUpdate(start, CCBBlocks.AIR_VENT_BLOCK.getDefaultState());
            level.setBlockAndUpdate(start.east(), CCBBlocks.AIR_VENT_BLOCK.getDefaultState());
            AirVentBlockEntity original = requireVent(level, start);
            for (Direction face : Iterate.directions) {
                VentState state = face.get3DDataValue() % 2 == 0 ? VentState.OPENED : VentState.CLOSED;
                original.setLouverState(face, state);
            }
            CompoundTag before = original.saveWithoutMetadata(level.registryAccess());
            MultiblockAssemblyGameTests.move(level, start, target, rotation, List.of(start, start.east()));
            AirVentBlockEntity landed = requireVent(level, target);
            CompoundTag after = landed.saveWithoutMetadata(level.registryAccess());
            int expectedOpened = 0;
            for (Direction face : Iterate.directions) {
                if ((before.getInt("OpenedMask") & 1 << face.get3DDataValue()) == 0) {
                    continue;
                }

                expectedOpened |= 1 << rotation.rotate(face).get3DDataValue();
            }
            helper.assertValueEqual(after.getInt("LouverMask"), before.getInt("LouverMask"), "all six louvers preserved after " + rotation);
            helper.assertValueEqual(after.getInt("OpenedMask"), expectedOpened, "open and closed louvers rotated after " + rotation);
            helper.assertTrue(level.getBlockState(start).isAir() && level.getBlockState(start.east()).isAir(), "Old vent positions must be empty");
            Direction connection = rotation.rotate(Direction.EAST);
            BlockPos neighbor = target.relative(connection);
            requireVent(level, neighbor);
            landed.onLoad();
            BlockState landedState = landed.getBlockState();
            for (Direction face : Iterate.directions) {
                BooleanProperty property = (BooleanProperty) landedState.getBlock().getStateDefinition().getProperty(face.getName());
                if (property == null) {
                    throw new NullPointerException("Expected the landed vent connection property for " + face + '.');
                }

                helper.assertValueEqual(landedState.getValue(property), face == connection, "landed vent connection after " + rotation);
            }
            level.setBlockAndUpdate(neighbor, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    private static AirVentBlockEntity requireVent(ServerLevel level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null) {
            throw new NullPointerException("Expected a vent at " + pos + '.');
        }

        return (AirVentBlockEntity) entity;
    }

    private static void assertEntry(GameTestHelper helper, Player player, Vec3 position, Vec3 look, boolean sneaking, boolean expected, String stage) {
        player.setPos(position);
        player.setYRot((float) Math.toDegrees(Math.atan2(-look.x, look.z)));
        player.setXRot((float) -Math.toDegrees(Math.asin(look.normalize().y)));
        player.setShiftKeyDown(sneaking);
        player.setPose(Pose.STANDING);
        helper.assertValueEqual(AirVentTraversal.shouldCrawl(player), expected, "vent entry: " + stage);
    }

    private static final class CrawlPlayer extends Player {
        private boolean expanded;
        private boolean spectator;
        private boolean localPlayer;

        private CrawlPlayer(Level level, BlockPos pos) {
            super(level, pos, 0, new GameProfile(UUID.randomUUID(), "VentCrawlTest"));
        }

        @Override
        public boolean isLocalPlayer() {
            return localPlayer;
        }

        @Override
        public boolean isSpectator() {
            return spectator;
        }

        @Override
        public boolean isCreative() {
            return false;
        }

        @Override
        public void setPose(Pose pose) {
            if (pose == Pose.STANDING || pose == Pose.CROUCHING) {
                expanded = true;
            }
            super.setPose(pose);
        }

        @Override
        public void updatePlayerPose() {
            super.updatePlayerPose();
        }

        private void receivePose(Pose pose) {
            getEntityData().assignValues(List.of(DataValue.create(DATA_POSE, pose)));
        }
    }
}
