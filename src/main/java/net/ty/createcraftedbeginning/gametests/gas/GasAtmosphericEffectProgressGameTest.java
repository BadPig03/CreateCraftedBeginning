package net.ty.createcraftedbeginning.gametests.gas;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import net.ty.createcraftedbeginning.gas.network.endpoint.AtmosphericGasEndpoint;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBMobEffects;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasAtmosphericEffectProgressGameTest {
    private static final String EFFECT_PROGRESS_KEY = "EffectProgress";
    private static final BlockPos PIPE_POS = new BlockPos(1, 1, 1);
    private static final long HIGH_FLOW_AMOUNT = 15250;
    private static final long REMAINING_TO_NEXT_EFFECT = 750;
    private static final BlockPos DIRECTIONAL_PIPE_POS = new BlockPos(3, 1, 3);
    private static final BlockPos DIRECTIONAL_EFFECT_TARGET_POS = new BlockPos(5, 1, 3);
    private static final BlockPos DIRECTIONAL_CONTROL_POS = new BlockPos(1, 1, 3);

    private GasAtmosphericEffectProgressGameTest() {
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void highFlowAtmosphericOutputKeepsEffectProgressNormalized(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(PIPE_POS, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));

        BlockPos absolutePipePos = helper.absolutePos(PIPE_POS);
        BlockEntity pipe = level.getBlockEntity(absolutePipePos);
        helper.assertTrue(pipe != null, "Airtight pipe block entity was not initialized");
        if (pipe == null) {
            throw new NullPointerException("Airtight pipe block entity was not initialized.");
        }

        AtmosphericGasEndpoint endpoint = new AtmosphericGasEndpoint(new BlockFace(absolutePipePos, Direction.EAST));
        endpoint.bind(level, pipe);
        GasHandler handler = endpoint.getGasHandlerProvider().getCapability();
        helper.assertTrue(handler != null, "Atmospheric gas handler was not initialized");
        if (handler == null) {
            throw new NullPointerException("Atmospheric gas handler was not initialized.");
        }

        long firstAccepted = handler.fill(new GasStack(CCBGases.NATURAL_AIR.get(), HIGH_FLOW_AMOUNT), GasAction.EXECUTE);
        helper.assertValueEqual(firstAccepted, HIGH_FLOW_AMOUNT, "accepted high-flow atmospheric output");

        CompoundTag afterHighFlow = endpoint.write(level.registryAccess());
        helper.assertValueEqual(NbtValues.getLongOrDefault(afterHighFlow, EFFECT_PROGRESS_KEY, -1), 250L, "effect progress after 15,250 GU output");

        long secondAccepted = handler.fill(new GasStack(CCBGases.NATURAL_AIR.get(), REMAINING_TO_NEXT_EFFECT), GasAction.EXECUTE);
        helper.assertValueEqual(secondAccepted, REMAINING_TO_NEXT_EFFECT, "accepted follow-up atmospheric output");

        CompoundTag afterExactInterval = endpoint.write(level.registryAccess());
        helper.assertTrue(!afterExactInterval.contains(EFFECT_PROGRESS_KEY), "Effect progress should return to zero after the remaining 750 GU");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 20)
    public static void atmosphericOutputAppliesDirectionalReleaseAtOutlet(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(DIRECTIONAL_PIPE_POS, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));

        BlockPos absolutePipePos = helper.absolutePos(DIRECTIONAL_PIPE_POS);
        BlockEntity pipe = level.getBlockEntity(absolutePipePos);
        helper.assertTrue(pipe != null, "Directional atmospheric pipe block entity was not initialized");
        if (pipe == null) {
            throw new NullPointerException("Directional atmospheric pipe block entity was not initialized.");
        }

        AtmosphericGasEndpoint endpoint = new AtmosphericGasEndpoint(new BlockFace(absolutePipePos, Direction.EAST));
        endpoint.bind(level, pipe);
        GasHandler handler = endpoint.getGasHandlerProvider().getCapability();
        helper.assertTrue(handler != null, "Directional atmospheric gas handler was not initialized");
        if (handler == null) {
            throw new NullPointerException("Directional atmospheric gas handler was not initialized.");
        }

        Piglin target = helper.spawn(EntityType.PIGLIN, DIRECTIONAL_EFFECT_TARGET_POS);
        Piglin control = helper.spawn(EntityType.PIGLIN, DIRECTIONAL_CONTROL_POS);
        helper.assertTrue(!target.hasEffect(CCBMobEffects.ZOMBIFICATION), "Directional atmospheric target piglin started with the zombification effect");
        helper.assertTrue(!control.hasEffect(CCBMobEffects.ZOMBIFICATION), "Directional atmospheric control piglin started with the zombification effect");

        long accepted = handler.fill(new GasStack(CCBGases.NATURAL_AIR.get(), 1000), GasAction.EXECUTE);
        helper.assertValueEqual(accepted, 1000L, "accepted directional atmospheric output");
        helper.assertTrue(target.hasEffect(CCBMobEffects.ZOMBIFICATION), "Directional atmospheric release did not affect the outlet-side target");
        helper.assertTrue(!control.hasEffect(CCBMobEffects.ZOMBIFICATION), "Directional atmospheric release affected an entity outside the outlet effect area");
        helper.succeed();
    }
}
