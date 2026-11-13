package net.ty.createcraftedbeginning.ponder.scenes.opticalpower;

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.amethystantenna.AmethystAntennaBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.ChunkStabilizerBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkBinding;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystLoadingNetworkScenes {
    private static final String COMPOUND_KEY_ACTIVE = "Active";

    private AmethystLoadingNetworkScenes() {
    }

    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("amethyst_loading_network", "Linking and Managing Stabilizers");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.85F);
        scene.showBasePlate();

        BlockPos firstLoaderPos = util.grid().at(5, 1, 4);
        BlockPos secondLoaderPos = firstLoaderPos.west(4);
        BlockPos firstAntennaPos = firstLoaderPos.above();
        BlockPos secondAntennaPos = firstLoaderPos.west(4).above();
        Selection firstLoader = util.select().position(firstLoaderPos);
        Selection secondLoader = util.select().position(secondLoaderPos);
        Selection firstAntenna = util.select().position(firstAntennaPos);
        Selection secondAntenna = util.select().position(secondAntennaPos);
        Vec3 firstLoaderVec = util.vector().centerOf(firstLoaderPos);
        Vec3 firstAntennaVec = util.vector().centerOf(firstAntennaPos);
        Vec3 secondAntennaVec = util.vector().centerOf(secondAntennaPos);
        Vec3 firstLoaderControl = util.vector().topOf(firstLoaderPos).add(0, 0, -0.5);
        Vec3 secondLoaderControl = util.vector().topOf(secondLoaderPos).add(0, 0, -0.5);
        AABB firstLoaderArea = new AABB(firstLoaderPos);
        AABB antennaBounds = CCBShapes.AMETHYST_ANTENNA.get(Direction.UP).bounds();
        AABB firstAntennaArea = antennaBounds.move(firstAntennaPos);
        AABB secondAntennaArea = antennaBounds.move(secondAntennaPos);
        Object firstLoaderOutline = new Object();
        Object firstAntennaOutline = new Object();
        Object secondAntennaOutline = new Object();
        ItemStack antenna = new ItemStack(CCBBlocks.AMETHYST_ANTENNA_BLOCK.get());
        ItemStack tunedAntenna = antenna.copy();
        LoadingNetworkBinding network = new LoadingNetworkBinding(new UUID(0, 1), "");
        tunedAntenna.set(CCBDataComponents.LOADING_NETWORK, network);

        scene.idle(10);
        scene.world().showSection(firstLoader, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(secondLoader, Direction.DOWN);

        scene.idle(12);
        setLoading(scene, firstLoaderPos, true);

        scene.idle(3);
        setLoading(scene, secondLoaderPos, true);

        scene.idle(8);
        scene.overlay().showText(60).text("Attach an Amethyst Antenna to a Chunk Stabilizer").colored(PonderPalette.BLUE).pointAt(firstAntennaVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(firstLoaderControl, Pointing.RIGHT, 30).rightClick().withItem(antenna.copy());

        scene.idle(10);
        scene.world().showSection(firstAntenna, Direction.DOWN);

        scene.idle(20);
        scene.world().modifyBlockEntityNBT(firstAntenna, AmethystAntennaBlockEntity.class, network::write);

        scene.idle(50);
        scene.overlay().showText(60).text("Installing an antenna automatically creates a loading network").colored(PonderPalette.GREEN).pointAt(firstAntennaVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, firstAntennaOutline, firstAntennaArea, 35);

        scene.idle(80);
        scene.overlay().showText(60).text("While holding an antenna, right-click this stabilizer to tune the antenna to its loading network").colored(PonderPalette.BLUE).pointAt(firstLoaderVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(firstLoaderControl, Pointing.RIGHT, 25).rightClick().withItem(antenna.copy());

        scene.idle(40);
        scene.overlay().showControls(firstLoaderControl, Pointing.RIGHT, 20).withItem(tunedAntenna.copy());

        scene.idle(40);
        scene.overlay().showText(60).text("A single loading network can manage multiple Chunk Stabilizers").colored(PonderPalette.GREEN).pointAt(secondAntennaVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(secondLoaderControl, Pointing.RIGHT, 30).rightClick().withItem(tunedAntenna.copy());

        scene.idle(10);
        scene.world().showSection(secondAntenna, Direction.DOWN);

        scene.idle(20);
        scene.world().modifyBlockEntityNBT(secondAntenna, AmethystAntennaBlockEntity.class, network::write);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, firstAntennaOutline, firstAntennaArea, 25);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, secondAntennaOutline, secondAntennaArea, 25);

        scene.idle(50);
        scene.overlay().showText(60).text("Right-click a stabilizer to view the devices connected to its loading network").colored(PonderPalette.BLUE).pointAt(firstLoaderVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(firstLoaderControl, Pointing.RIGHT, 35).rightClick();

        scene.idle(80);
        scene.overlay().showText(60).text("Pause one stabilizer in the list").colored(PonderPalette.RED).pointAt(firstLoaderVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(firstLoaderControl, Pointing.RIGHT, 35).showing(AllIcons.I_PAUSE);

        scene.idle(10);
        setLoading(scene, firstLoaderPos, false);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, firstLoaderOutline, firstLoaderArea, 45);

        scene.idle(70);
        scene.overlay().showText(60).text("Enable it to resume loading").colored(PonderPalette.GREEN).pointAt(firstLoaderVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(firstLoaderControl, Pointing.RIGHT, 35).showing(AllIcons.I_PLAY);

        scene.idle(10);
        setLoading(scene, firstLoaderPos, true);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, firstLoaderOutline, firstLoaderArea, 45);

        scene.idle(50);
        scene.markAsFinished();
    }

    private static void setLoading(CreateSceneBuilder scene, BlockPos pos, boolean active) {
        scene.addInstruction(ponder -> {
            PonderLevel level = ponder.getWorld();
            if (!(level.getBlockEntity(pos) instanceof ChunkStabilizerBlockEntity loader)) {
                return;
            }

            Provider registries = level.registryAccess();
            CompoundTag tag = loader.writeClient(new CompoundTag(), registries);
            tag.putBoolean(COMPOUND_KEY_ACTIVE, active);
            loader.readClient(tag, registries);
        });
    }
}
