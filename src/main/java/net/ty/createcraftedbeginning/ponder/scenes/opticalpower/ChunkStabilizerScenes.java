package net.ty.createcraftedbeginning.ponder.scenes.opticalpower;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.actors.psi.PortableItemInterfaceBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.MinecartElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.ChunkStabilizerBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ChunkStabilizerScenes {
    private static final int CART_ROUND_TRIP_TICKS = 220;
    private static final String COMPOUND_KEY_DISTANCE = "Distance";
    private static final String COMPOUND_KEY_TIMER = "Timer";

    private ChunkStabilizerScenes() {
    }

    public static void factory(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("chunk_stabilizer_factory", "Keeping a Factory Loaded");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos loaderPos = util.grid().at(2, 1, 2);
        BlockPos inputPos = loaderPos.west().south(2);
        BlockPos outputPos = loaderPos.east(2).south(2);
        BlockPos chestPos = loaderPos.east(3).above().south(2);
        BlockPos chunkCorner = loaderPos.west(2).north(2);

        Selection drive = util.select().fromTo(inputPos, outputPos).add(util.select().position(outputPos.south()));
        Selection feedLine = conveyorEnd(util, inputPos.west(), inputPos.above()).add(drive);
        Selection outfeed = conveyorEnd(util, chestPos.below(), outputPos.above());
        Selection loader = util.select().position(loaderPos);

        Vec3 loaderVec = util.vector().centerOf(loaderPos);
        Vec3 chestVec = util.vector().topOf(chestPos);
        Vec3 chunkMin = util.vector().centerOf(chunkCorner).add(-0.5, -0.46, -0.5);

        AABB rangeStart = new AABB(loaderVec, loaderVec).move(0, -0.44, 0).inflate(0.08);
        AABB range = new AABB(chunkMin, chunkMin.add(16, 0.05, 16));

        Object rangeOutline = new Object();

        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack goggles = new ItemStack(AllItems.GOGGLES.asItem());
        ItemStack wrench = new ItemStack(AllItems.WRENCH.asItem());

        scene.idle(17);
        showInSequence(scene, feedLine, outfeed);

        scene.idle(12);
        scene.world().setKineticSpeed(drive, -32);

        scene.idle(8);
        scene.world().createItemOnBelt(inputPos, Direction.UP, iron.copy());

        scene.idle(25);
        scene.world().showSection(loader, Direction.DOWN);

        scene.idle(15);
        startLoader(scene, loaderPos);

        scene.idle(5);
        scene.overlay().showText(60).text("Place a Chunk Stabilizer to keep its chunk loaded without requiring power").colored(PonderPalette.GREEN).pointAt(loaderVec).placeNearTarget().attachKeyFrame();
        receiveItem(scene, outputPos);

        scene.idle(55);
        scene.world().createItemOnBelt(inputPos, Direction.UP, iron.copy());

        scene.idle(25);
        scene.overlay().showText(60).text("Machines in the loaded chunk will keep running after the player leaves").pointAt(chestVec).placeNearTarget().attachKeyFrame();

        scene.idle(20);
        receiveItem(scene, outputPos);

        scene.idle(40);
        scene.world().createItemOnBelt(inputPos, Direction.UP, iron.copy());

        scene.idle(20);
        scene.overlay().showText(60).text("Equip Engineer's Goggles to see the boundaries of the loaded chunk").colored(PonderPalette.BLUE).pointAt(loaderVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(loaderVec, Pointing.RIGHT, 35).withItem(goggles.copy());
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, rangeOutline, rangeStart, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, rangeOutline, range, 84);

        scene.idle(22);
        receiveItem(scene, outputPos);

        scene.idle(55);
        scene.overlay().showText(60).text("Right-click the stabilizer with a Wrench to toggle the chunk outline").colored(PonderPalette.BLUE).pointAt(loaderVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(loaderVec, Pointing.RIGHT, 35).rightClick().withItem(wrench.copy());

        scene.idle(7);
        scene.world().createItemOnBelt(inputPos, Direction.UP, iron.copy());

        scene.idle(45);
        receiveItem(scene, outputPos);

        scene.idle(8);
        scene.markAsFinished();
    }

    public static void moving(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("chunk_stabilizer_moving", "Mobile Chunk Loading");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos loaderPos = util.grid().at(1, 3, 4);
        BlockPos cartBase = loaderPos.east().below(3).north();
        BlockPos projectedLoader = loaderPos.north();

        Selection[] rails = trackSections(util, projectedLoader.east(5).below(2));
        Selection chassis = util.select().fromTo(loaderPos.below(), loaderPos.east(2).below()).add(util.select().position(loaderPos.east().below(2)));
        Selection loader = util.select().position(loaderPos);

        Vec3 cartVec = util.vector().topOf(cartBase);
        Vec3 startVec = util.vector().centerOf(projectedLoader);
        Vec3 projection = startVec.subtract(util.vector().centerOf(loaderPos));
        Vec3 travel = util.vector().centerOf(projectedLoader.east(3)).subtract(startVec);
        Vec3 floorMin = util.vector().topOf(loaderPos.west().north(4).below(3)).add(-0.5, 0.02, -0.5);
        Vec3 seam = floorMin.add(4, 0, 0);
        Vec3 crossingVec = seam.add(0, 0.05, 2);
        Vec3 returnVec = crossingVec.add(-1, 0, 0);

        AABB leftStart = new AABB(startVec, startVec).move(0, -2.44, 0).inflate(0.08);
        AABB left = new AABB(floorMin, seam.add(0, 0.05, 7));
        AABB rightStart = new AABB(seam.add(0.02, 0, 3.4), seam.add(0.18, 0.05, 3.6));
        AABB returnStart = rightStart.move(-0.2, 0, 0);
        AABB right = new AABB(seam, floorMin.add(7, 0.05, 7));

        Object leftOutline = new Object();
        Object rightOutline = new Object();

        scene.idle(10);
        showInSequence(scene, rails);

        scene.idle(3);
        ElementLink<MinecartElement> cart = scene.special().createCart(cartVec, 0, Minecart::new);
        ElementLink<WorldSectionElement> structure = scene.world().showIndependentSection(chassis, Direction.DOWN);
        scene.world().moveSection(structure, projection, 0);

        scene.idle(3);
        scene.world().showSectionAndMerge(loader, Direction.DOWN, structure);

        scene.idle(15);
        startLoader(scene, loaderPos);

        scene.idle(5);
        scene.overlay().showText(60).text("Mount a Chunk Stabilizer on a minecart contraption").colored(PonderPalette.GREEN).pointAt(startVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, leftOutline, leftStart, 3);
        scene.addInstruction(new PonderInstruction() {
            private int phase;

            @Override
            public void reset(PonderScene ponderScene) {
                phase = 0;
            }

            @Override
            public boolean isComplete() {
                return false;
            }

            @Override
            public void tick(PonderScene ponderScene) {
                phase = (phase + 1) % CART_ROUND_TRIP_TICKS;
                MinecartElement cartElement = ponderScene.resolve(cart);
                WorldSectionElement body = ponderScene.resolve(structure);
                if (cartElement == null || body == null) {
                    return;
                }

                double fraction = (1 - Math.cos(2 * Math.PI * phase / CART_ROUND_TRIP_TICKS)) / 2;
                Vec3 offset = travel.scale(fraction);
                cartElement.setPositionOffset(offset, false);
                body.setAnimatedOffset(projection.add(offset), false);
            }
        });

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, leftOutline, left, 77);

        scene.idle(77);
        scene.overlay().showText(60).text("The loaded chunk only changes when the stabilizer itself crosses a chunk boundary").pointAt(crossingVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, rightOutline, rightStart, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, rightOutline, right, 56);

        scene.idle(56);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, leftOutline, returnStart, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, leftOutline, left, 78);

        scene.idle(18);
        scene.overlay().showText(60).text("When the stabilizer crosses back over the boundary, it loads the original chunk again").colored(PonderPalette.GREEN).pointAt(returnVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void train(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("chunk_stabilizer_train", "Unattended Unloading");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos loaderPos = util.grid().at(2, 3, 3);
        BlockPos fixedLoaderPos = loaderPos.east().north(3).below(2);
        BlockPos stationPos = loaderPos.west(2).south(3).below(2);
        BlockPos bogeyPos = loaderPos.east().south().below();
        BlockPos controlsPos = loaderPos.east().south();
        BlockPos inputPos = loaderPos.north(2).below(2);
        BlockPos outputPos = loaderPos.east(3).north(2).below(2);
        BlockPos carInterface = loaderPos.west().below();
        BlockPos fixedInterface = loaderPos.west().north(2).below();
        Selection[] rails = trackSections(util, bogeyPos.east(3).below());
        Selection drive = util.select().fromTo(inputPos, outputPos).add(util.select().position(outputPos.south()));
        Selection platform = util.select().position(stationPos).add(conveyorEnd(util, fixedInterface.below(), inputPos.above())).add(drive).add(conveyorEnd(util, outputPos.east(), outputPos.above()));
        Selection loader = util.select().position(loaderPos);
        Selection carriage = util.select().fromTo(carInterface, controlsPos.east().south().above()).substract(loader);
        Selection fixedLoader = util.select().position(fixedLoaderPos);
        Selection interfaces = util.select().position(carInterface).add(util.select().position(fixedInterface));
        Vec3 loaderVec = util.vector().centerOf(loaderPos);
        Vec3 fixedVec = util.vector().centerOf(fixedLoaderPos);
        Vec3 connectionVec = util.vector().centerOf(fixedInterface).add(0, 0, 1);
        Vec3 east = util.vector().centerOf(loaderPos.east()).subtract(loaderVec);
        Vec3 pullAway = east.scale(2);
        Vec3 approach = east.scale(-2);
        Vec3 departure = east.scale(5);
        ItemStack copper = new ItemStack(Items.COPPER_INGOT);

        scene.idle(10);
        showInSequence(scene, rails);

        scene.idle(3);
        scene.world().showSection(platform, Direction.DOWN);

        scene.idle(3);
        ElementLink<WorldSectionElement> train = scene.world().showIndependentSection(carriage, Direction.DOWN);

        scene.idle(12);
        scene.world().setKineticSpeed(drive, -25);

        scene.idle(8);
        scene.overlay().showText(60).text("Place a Chunk Stabilizer beside the Portable Storage Interface on the train").colored(PonderPalette.GREEN).pointAt(loaderVec).placeNearTarget().attachKeyFrame();
        scene.world().showSectionAndMerge(loader, Direction.DOWN, train);

        scene.idle(15);
        startLoader(scene, loaderPos);

        scene.idle(5);
        scene.effects().superGlue(loaderPos, Direction.DOWN, true);
        scene.world().toggleControls(controlsPos);
        scene.world().animateTrainStation(stationPos, true);

        scene.idle(17);
        scene.world().animateTrainStation(stationPos, false);
        scene.world().moveSection(train, pullAway, 24);
        scene.world().animateBogey(bogeyPos, -2, 24);

        scene.idle(45);
        scene.world().moveSection(train, approach, 24);
        scene.world().animateBogey(bogeyPos, 2, 24);

        scene.idle(24);
        scene.world().animateTrainStation(stationPos, true);
        scene.overlay().showText(60).text("A platform in the same chunk can unload cargo while the train is stopped").pointAt(connectionVec).placeNearTarget().attachKeyFrame();

        scene.idle(5);
        setConnection(scene, interfaces, 14);

        scene.idle(10);
        scene.world().createItemOnBelt(inputPos, Direction.UP, copper.copy());

        scene.idle(58);
        receiveItem(scene, outputPos);

        scene.idle(2);
        scene.world().createItemOnBelt(inputPos, Direction.UP, copper.copy());

        scene.idle(5);
        scene.overlay().showText(60).text("Place another stabilizer on the platform to keep cargo moving after the train leaves").colored(PonderPalette.GREEN).pointAt(fixedVec).placeNearTarget().attachKeyFrame();
        scene.world().showSection(fixedLoader, Direction.DOWN);

        scene.idle(15);
        startLoader(scene, fixedLoaderPos);

        scene.idle(5);
        setConnection(scene, interfaces, 3);

        scene.idle(5);
        scene.world().animateTrainStation(stationPos, false);
        scene.world().moveSection(train, departure, 25);
        scene.world().animateBogey(bogeyPos, -5, 25);

        scene.idle(6);
        scene.world().hideIndependentSection(train, Direction.EAST);

        scene.idle(22);
        receiveItem(scene, outputPos);

        scene.idle(7);
        scene.markAsFinished();
    }

    private static Selection conveyorEnd(SceneBuildingUtil util, BlockPos supportPos, BlockPos funnelPos) {
        return util.select().fromTo(supportPos, supportPos.above()).add(util.select().position(funnelPos));
    }

    private static Selection[] trackSections(SceneBuildingUtil util, BlockPos eastEnd) {
        return new Selection[]{
                util.select().fromTo(eastEnd, eastEnd.west()),
                util.select().fromTo(eastEnd.west(2), eastEnd.west(4)),
                util.select().fromTo(eastEnd.west(5), eastEnd.west(6))
        };
    }

    private static void showInSequence(CreateSceneBuilder scene, Selection... sections) {
        scene.world().showSection(sections[0], Direction.DOWN);
        for (int i = 1; i < sections.length; i++) {
            scene.idle(3);
            scene.world().showSection(sections[i], Direction.DOWN);
        }
    }

    private static void startLoader(CreateSceneBuilder scene, BlockPos pos) {
        scene.world().modifyBlockEntity(pos, ChunkStabilizerBlockEntity.class, ChunkStabilizerBlockEntity::startPonderLoading);
    }

    private static void receiveItem(CreateSceneBuilder scene, BlockPos outputPos) {
        scene.world().removeItemsFromBelt(outputPos);
        scene.world().flapFunnel(outputPos.above(), false);
    }

    private static void setConnection(CreateSceneBuilder scene, Selection interfaces, int timer) {
        scene.world().modifyBlockEntityNBT(interfaces, PortableItemInterfaceBlockEntity.class, tag -> {
            tag.putFloat(COMPOUND_KEY_DISTANCE, 1);
            tag.putInt(COMPOUND_KEY_TIMER, timer);
        });
    }
}
