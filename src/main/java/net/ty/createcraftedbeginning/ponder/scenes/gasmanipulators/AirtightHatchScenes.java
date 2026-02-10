package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlock.CanisterType;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

public class AirtightHatchScenes {
    public static void scene(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_hatch", "Gas Exchange via Airtight Hatches");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos tankPos = util.grid().at(3, 1, 3);
        BlockPos hatchPos = tankPos.north();

        Selection tankSelection = util.select().fromTo(tankPos, tankPos.above());
        Selection hatchSelection = util.select().position(hatchPos);

        Vec3 hatchTopVec = util.vector().topOf(hatchPos);

        Object tankObject = new Object();

        AABB tankArea = new AABB(util.vector().centerOf(tankPos), util.vector().centerOf(tankPos));

        ItemStack gasCanisterItem = new ItemStack(CCBItems.GAS_CANISTER.asItem());
        ItemStack wrenchItem = new ItemStack(AllItems.WRENCH.asItem());

        scene.idle(20);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(hatchSelection, Direction.SOUTH);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, tankObject, tankArea, 3);

        scene.idle(3);
        tankArea = tankArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, tankObject, tankArea, 3);

        scene.idle(3);
        tankArea = tankArea.expandTowards(0, 1, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, tankObject, tankArea, 60);
        scene.overlay().showText(60).text("Airtight Hatches must be placed on gas containers").pointAt(Vec3.atCenterOf(tankPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(hatchTopVec, Pointing.DOWN, 67).rightClick().withItem(gasCanisterItem);

        scene.idle(7);
        scene.world().modifyBlock(hatchPos, s -> s.setValue(AirtightHatchBlock.CANISTER_TYPE, CanisterType.NORMAL), false);
        scene.overlay().showText(60).text("Right-click with a Gas Canister to load it into the hatch").pointAt(Vec3.atCenterOf(hatchPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showScrollInput(hatchTopVec.add(new Vec3(0, 0, 0.2)), Direction.UP, 60);
        scene.overlay().showText(60).text("Transfer modes are configurable").pointAt(hatchTopVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(hatchTopVec, Pointing.DOWN, 60).showing(CCBIcons.I_NO_TRANSFER);
        scene.overlay().showText(60).text("\"No Transfer\": Halts all gas exchange").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(hatchPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(hatchTopVec, Pointing.DOWN, 60).showing(CCBIcons.I_INPUT_ONLY);
        scene.overlay().showText(60).text("\"Input Only\": Continuously fills canisters from connected gas containers").colored(PonderPalette.INPUT).pointAt(Vec3.atCenterOf(hatchPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(hatchTopVec, Pointing.DOWN, 60).showing(CCBIcons.I_OUTPUT_ONLY);
        scene.overlay().showText(60).text("\"Output Only\": Continuously empties canisters into connected gas containers").colored(PonderPalette.OUTPUT).pointAt(Vec3.atCenterOf(hatchPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(hatchTopVec, Pointing.DOWN, 140).showing(CCBIcons.I_STAY_HALF);
        scene.overlay().showText(60).text("\"Stay Half\": Maintains canisters at half capacity").colored(PonderPalette.MEDIUM).pointAt(Vec3.atCenterOf(hatchPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("...draws gas when below half, expels excess when above half").colored(PonderPalette.MEDIUM).pointAt(Vec3.atCenterOf(hatchPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(hatchTopVec, Pointing.DOWN, 67).rightClick().withItem(wrenchItem);

        scene.idle(7);
        scene.world().modifyBlock(hatchPos, s -> s.setValue(AirtightHatchBlock.CANISTER_TYPE, CanisterType.EMPTY), false);
        scene.overlay().showText(60).text("Right-click with a Wrench to retrieve loaded canisters").pointAt(Vec3.atCenterOf(hatchPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
