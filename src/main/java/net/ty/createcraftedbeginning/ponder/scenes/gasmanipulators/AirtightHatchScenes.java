package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity.Phase;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.client.gui.CCBIcons;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlock.CanisterType;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightHatchScenes {
    public static void exchange(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_hatch_exchange", "Exchanging Gas with Airtight Hatches");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos tankPos = util.grid().at(3, 1, 3);
        BlockPos tankTopPos = tankPos.above();
        BlockPos hatchPos = tankPos.north();

        Selection tankSelection = util.select().fromTo(tankPos, tankTopPos);
        Selection hatchSelection = util.select().position(hatchPos);

        Vec3 tankVec = util.vector().centerOf(tankPos);
        Vec3 hatchVec = util.vector().centerOf(hatchPos);
        Vec3 hatchTopVec = util.vector().topOf(hatchPos).add(0, 0, 0.1875);
        Vec3 tankTopFaceVec = util.vector().topOf(tankTopPos);
        Vec3 tankNorthFaceVec = util.vector().blockSurface(tankPos, Direction.NORTH);
        Vec3 targetPressurePanelVec = hatchVec.add(0.21875, 0, 0.1875);

        AABB invalidTopArea = new AABB(tankTopFaceVec, tankTopFaceVec);
        AABB validSideArea = new AABB(tankNorthFaceVec, tankNorthFaceVec);
        AABB noTransferArea = new AABB(tankVec, tankVec);
        AABB fillFlowArea = new AABB(tankVec, tankVec);
        AABB drainFlowArea = new AABB(hatchVec, hatchVec);

        Object invalidTopObject = new Object();
        Object validSideObject = new Object();
        Object noTransferObject = new Object();
        Object fillFlowObject = new Object();
        Object drainFlowObject = new Object();

        ItemStack gasCanisterItem = new ItemStack(CCBItems.GAS_CANISTER.asItem());

        scene.idle(20);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(hatchSelection, Direction.SOUTH);

        scene.idle(15);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, invalidTopObject, invalidTopArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, validSideObject, validSideArea, 3);

        scene.idle(3);
        invalidTopArea = invalidTopArea.inflate(0.5, 0, 0.5);
        validSideArea = validSideArea.inflate(0.5, 0.5, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, invalidTopObject, invalidTopArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, validSideObject, validSideArea, 60);
        scene.overlay().showText(60).text("Airtight Hatches cannot be attached to the top or bottom of a compatible gas container").colored(PonderPalette.RED).pointAt(tankTopFaceVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Airtight Hatches can attach to compatible side faces instead").colored(PonderPalette.GREEN).pointAt(tankNorthFaceVec).placeNearTarget().attachKeyFrame();

        scene.idle(73);
        scene.overlay().showText(60).text("Right-click the hatch with a compatible Gas Canister to insert it").colored(PonderPalette.BLUE).pointAt(hatchVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(hatchTopVec, Pointing.DOWN, 60).rightClick().withItem(gasCanisterItem.copy());

        scene.idle(7);
        scene.world().modifyBlock(hatchPos, state -> state.setValue(AirtightHatchBlock.CANISTER_TYPE, CanisterType.NORMAL), false);
        scene.effects().indicateSuccess(hatchPos);

        scene.idle(80);
        scene.overlay().showScrollInput(hatchTopVec, Direction.UP, 60);
        scene.overlay().showControls(hatchTopVec, Pointing.DOWN, 60).scroll();
        scene.overlay().showText(60).text("Scroll the top value panel to select a transfer mode").colored(PonderPalette.BLUE).pointAt(hatchTopVec).placeNearTarget().attachKeyFrame();

        scene.idle(74);
        scene.overlay().showControls(hatchTopVec, Pointing.DOWN, 60).showing(CCBIcons.I_NO_TRANSFER);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, noTransferObject, noTransferArea, 3);

        scene.idle(3);
        noTransferArea = noTransferArea.inflate(0.3125, 0.3125, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, noTransferObject, noTransferArea, 3);

        scene.idle(3);
        noTransferArea = noTransferArea.expandTowards(0, 0, -0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, noTransferObject, noTransferArea, 60);
        scene.overlay().showText(60).text("\"No Transfer\" prevents all gas exchange").colored(PonderPalette.RED).pointAt(tankNorthFaceVec).placeNearTarget().attachKeyFrame();

        scene.idle(74);
        scene.overlay().showControls(hatchTopVec, Pointing.DOWN, 60).showing(CCBIcons.I_INPUT_ONLY);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, fillFlowObject, fillFlowArea, 3);

        scene.idle(3);
        fillFlowArea = fillFlowArea.inflate(0.3125, 0.3125, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, fillFlowObject, fillFlowArea, 3);

        scene.idle(3);
        fillFlowArea = fillFlowArea.expandTowards(0, 0, -1);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, fillFlowObject, fillFlowArea, 60);
        scene.overlay().showText(60).text("\"Fill Canister\" lets higher-pressure gas flow from the attached container into the canister").colored(PonderPalette.INPUT).pointAt(hatchVec).placeNearTarget().attachKeyFrame();

        scene.idle(74);
        scene.overlay().showControls(hatchTopVec, Pointing.DOWN, 60).showing(CCBIcons.I_OUTPUT_ONLY);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, drainFlowObject, drainFlowArea, 3);

        scene.idle(3);
        drainFlowArea = drainFlowArea.inflate(0.3125, 0.3125, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, drainFlowObject, drainFlowArea, 3);

        scene.idle(3);
        drainFlowArea = drainFlowArea.expandTowards(0, 0, 1);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, drainFlowObject, drainFlowArea, 60);
        scene.overlay().showText(60).text("\"Drain Canister\" lets higher-pressure gas flow from the canister into the attached container").colored(PonderPalette.OUTPUT).pointAt(tankNorthFaceVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(hatchTopVec, Pointing.DOWN, 60).showing(CCBIcons.I_TARGET_PRESSURE);
        scene.overlay().showText(60).text("\"Target Pressure\" automatically fills or drains a normal canister toward a selected pressure").colored(PonderPalette.GREEN).pointAt(hatchVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showScrollInput(util.vector().blockSurface(hatchPos, Direction.NORTH).add(0, 0, 0.325), Direction.NORTH, 60);
        scene.overlay().showControls(targetPressurePanelVec, Pointing.RIGHT, 60).scroll();
        scene.overlay().showText(60).text("With \"Target Pressure\" selected, scroll an exposed side panel to set the pressure").colored(PonderPalette.BLUE).pointAt(targetPressurePanelVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, noTransferObject, noTransferArea, 60);
        scene.overlay().showText(60).text("Airtight Hatches transfer gas passively, only from higher to lower pressure").colored(PonderPalette.RED).pointAt(tankNorthFaceVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void handling(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_hatch_handling", "Handling Canisters with Airtight Hatches");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos tankPos = util.grid().at(3, 1, 3);
        BlockPos hatchPos = tankPos.north();
        BlockPos armPos = hatchPos.west(2).south();
        BlockPos armCogPos = hatchPos.west(3);
        BlockPos armMotorPos = armCogPos.above();
        BlockPos newArmPos = armPos.north(2);
        BlockPos deployerPos = hatchPos.east(2);
        BlockPos deployerMotorPos = deployerPos.north();

        Selection tankSelection = util.select().fromTo(tankPos, tankPos.above());
        Selection hatchSelection = util.select().position(hatchPos);
        Selection armSelection = util.select().position(armPos);
        Selection armCogSelection = util.select().position(armCogPos);
        Selection armMotorSelection = util.select().position(armMotorPos);
        Selection armPowerSelection = util.select().fromTo(armCogPos, armMotorPos);
        Selection deployerSelection = util.select().position(deployerPos);
        Selection deployerMotorSelection = util.select().position(deployerMotorPos);
        Selection newArmSelection = util.select().position(newArmPos);

        Vec3 hatchVec = util.vector().centerOf(hatchPos);
        Vec3 hatchTopVec = util.vector().topOf(hatchPos);
        Vec3 armVec = util.vector().centerOf(armPos);
        Vec3 newArmVec = util.vector().centerOf(newArmPos);

        ItemStack wrenchItem = new ItemStack(AllItems.WRENCH.asItem());
        ItemStack gasCanisterItem = new ItemStack(CCBItems.GAS_CANISTER.asItem());
        ItemStack creativeCanisterItem = new ItemStack(CCBItems.CREATIVE_GAS_CANISTER.asItem());

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        scene.idle(20);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(hatchSelection, Direction.SOUTH);

        scene.idle(20);
        scene.overlay().showText(60).text("Right-click a loaded hatch with a Wrench to remove its canister without losing gas").colored(PonderPalette.BLUE).pointAt(hatchVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(hatchTopVec, Pointing.DOWN, 60).rightClick().withItem(wrenchItem.copy());

        scene.idle(7);
        scene.world().modifyBlock(hatchPos, state -> state.setValue(AirtightHatchBlock.CANISTER_TYPE, CanisterType.EMPTY), false);
        scene.effects().indicateSuccess(hatchPos);

        scene.idle(73);
        scene.world().setBlock(armMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.DOWN), false);
        scene.world().showSection(armPowerSelection, Direction.DOWN);
        scene.world().showSection(armSelection, Direction.DOWN);
        scene.world().showSection(newArmSelection, Direction.DOWN);

        scene.idle(15);
        scene.world().setKineticSpeed(armCogSelection, mediumSpeed / 2);
        scene.world().setKineticSpeed(armMotorSelection, mediumSpeed / 2);
        scene.world().setKineticSpeed(armSelection, -mediumSpeed);
        scene.world().setKineticSpeed(newArmSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(armPos);
        scene.overlay().showText(60).text("Mechanical Arms can automatically insert compatible Gas Canisters").colored(PonderPalette.BLUE).pointAt(armVec).placeNearTarget().attachKeyFrame();
        scene.world().instructArm(armPos, Phase.MOVE_TO_OUTPUT, gasCanisterItem.copy(), 0);

        scene.idle(40);
        scene.world().modifyBlock(hatchPos, state -> state.setValue(AirtightHatchBlock.CANISTER_TYPE, CanisterType.NORMAL), false);
        scene.effects().indicateSuccess(hatchPos);
        scene.world().instructArm(armPos, Phase.SEARCH_INPUTS, ItemStack.EMPTY, -1);

        scene.idle(40);
        scene.overlay().showText(60).text("Mechanical Arms can also remove loaded Gas Canisters automatically").colored(PonderPalette.BLUE).pointAt(newArmVec).placeNearTarget().attachKeyFrame();
        scene.world().instructArm(newArmPos, Phase.MOVE_TO_INPUT, ItemStack.EMPTY, 0);

        scene.idle(40);
        scene.world().modifyBlock(hatchPos, state -> state.setValue(AirtightHatchBlock.CANISTER_TYPE, CanisterType.EMPTY), false);
        scene.effects().indicateSuccess(hatchPos);
        scene.world().instructArm(newArmPos, Phase.SEARCH_OUTPUTS, gasCanisterItem.copy(), -1);

        scene.idle(20);
        scene.world().hideSection(armSelection, Direction.WEST);
        scene.world().hideSection(armPowerSelection, Direction.WEST);
        scene.world().hideSection(newArmSelection, Direction.WEST);

        scene.idle(20);
        scene.world().setBlock(deployerMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH), false);
        scene.world().showSection(deployerMotorSelection, Direction.DOWN);
        scene.world().showSection(deployerSelection, Direction.DOWN);

        scene.idle(15);
        scene.world().setKineticSpeed(deployerMotorSelection, mediumSpeed);
        scene.world().setKineticSpeed(deployerSelection, mediumSpeed);
        scene.world().modifyBlockEntityNBT(deployerSelection, DeployerBlockEntity.class, compoundTag -> compoundTag.put("HeldItem", gasCanisterItem.copy().saveOptional(scene.world().getHolderLookupProvider())));
        scene.overlay().showText(60).text("Deployers can insert canisters, or remove them while holding a Wrench").colored(PonderPalette.BLUE).pointAt(util.vector().centerOf(deployerPos)).placeNearTarget().attachKeyFrame();

        scene.idle(20);
        scene.world().moveDeployer(deployerPos, 1, 20);

        scene.idle(21);
        scene.world().modifyBlockEntityNBT(deployerSelection, DeployerBlockEntity.class, compoundTag -> compoundTag.put("HeldItem", ItemStack.EMPTY.saveOptional(scene.world().getHolderLookupProvider())));
        scene.world().modifyBlock(hatchPos, state -> state.setValue(AirtightHatchBlock.CANISTER_TYPE, CanisterType.NORMAL), false);
        scene.effects().indicateSuccess(hatchPos);
        scene.world().moveDeployer(deployerPos, -1, 20);

        scene.idle(21);
        scene.world().modifyBlockEntityNBT(deployerSelection, DeployerBlockEntity.class, compoundTag -> compoundTag.put("HeldItem", wrenchItem.copy().saveOptional(scene.world().getHolderLookupProvider())));
        scene.world().moveDeployer(deployerPos, 1, 20);

        scene.idle(21);
        scene.world().modifyBlock(hatchPos, state -> state.setValue(AirtightHatchBlock.CANISTER_TYPE, CanisterType.EMPTY), false);
        scene.effects().indicateSuccess(hatchPos);
        scene.world().moveDeployer(deployerPos, -1, 20);

        scene.idle(21);
        scene.world().hideSection(deployerSelection, Direction.EAST);
        scene.world().hideSection(deployerMotorSelection, Direction.EAST);

        scene.idle(20);
        scene.overlay().showControls(hatchTopVec, Pointing.DOWN, 60).rightClick().withItem(creativeCanisterItem.copy());

        scene.idle(7);
        scene.world().modifyBlock(hatchPos, state -> state.setValue(AirtightHatchBlock.CANISTER_TYPE, CanisterType.CREATIVE), false);
        scene.effects().indicateSuccess(hatchPos);
        scene.overlay().showText(60).text("\"Target Pressure\" is unavailable with Creative Gas Canisters").colored(PonderPalette.RED).pointAt(hatchTopVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(hatchTopVec, Pointing.DOWN, 60).showing(CCBIcons.I_OUTPUT_ONLY);
        scene.overlay().showText(60).text("With \"Drain Canister\", a Creative Gas Canister supplies gas indefinitely at its own pressure").colored(PonderPalette.OUTPUT).pointAt(hatchVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(hatchTopVec, Pointing.DOWN, 60).showing(CCBIcons.I_INPUT_ONLY);
        scene.overlay().showText(60).text("With \"Fill Canister\", incoming gas is discarded").colored(PonderPalette.INPUT).pointAt(hatchVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
