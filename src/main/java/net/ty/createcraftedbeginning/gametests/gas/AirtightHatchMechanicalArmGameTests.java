package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchInteractionPoint.AirtightHatchType;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AirtightHatchMechanicalArmGameTests {
    private static final BlockPos HATCH_POS = new BlockPos(1, 1, 1);

    private AirtightHatchMechanicalArmGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void mechanicalArmCanInsertAndExtractCanister(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AirtightHatchBlockEntity hatch = placeHatch(helper);
        BlockPos absolutePos = helper.absolutePos(HATCH_POS);
        BlockState state = level.getBlockState(absolutePos);
        ArmInteractionPoint point = new AirtightHatchType().createPoint(level, absolutePos, state);

        ItemStack input = new ItemStack(CCBItems.GAS_CANISTER.asItem(), 2);
        ItemStack simulatedRemainder = point.insert(null, input, true);
        helper.assertValueEqual(simulatedRemainder.getCount(), 1, "simulated arm insertion remainder");
        helper.assertTrue(hatch.isEmpty(), "simulated arm insertion mutated the hatch");

        ItemStack remainder = point.insert(null, input, false);
        helper.assertValueEqual(remainder.getCount(), 1, "arm insertion remainder");
        helper.assertTrue(!hatch.isEmpty(), "mechanical arm did not install the canister");

        ItemStack simulatedExtract = point.extract(null, 0, 1, true);
        helper.assertTrue(!simulatedExtract.isEmpty(), "simulated arm extraction found no canister");
        helper.assertTrue(!hatch.isEmpty(), "simulated arm extraction mutated the hatch");

        ItemStack extracted = point.extract(null, 0, 1, false);
        helper.assertTrue(!extracted.isEmpty(), "mechanical arm did not extract the canister");
        helper.assertTrue(hatch.isEmpty(), "hatch remained occupied after arm extraction");
        helper.succeed();
    }

    private static AirtightHatchBlockEntity placeHatch(GameTestHelper helper) {
        BlockPos attachmentPos = HATCH_POS.relative(Direction.SOUTH);
        helper.setBlock(attachmentPos, CCBBlocks.AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        helper.setBlock(HATCH_POS, CCBBlocks.AIRTIGHT_HATCH_BLOCK.get().defaultBlockState().setValue(AirtightHatchBlock.FACING, Direction.SOUTH));

        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(HATCH_POS));
        helper.assertTrue(blockEntity instanceof AirtightHatchBlockEntity, "Airtight hatch block entity was not initialized");
        if (!(blockEntity instanceof AirtightHatchBlockEntity hatch)) {
            throw new IllegalStateException("Airtight hatch block entity missing at " + HATCH_POS + '.');
        }

        return hatch;
    }
}
