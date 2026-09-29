package net.ty.createcraftedbeginning.compat.sable;

import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructural;
import net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower.AirtightFractionationTowerBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower.AirtightFractionationTowerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructural;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class MultiblockAssemblyCompat {
    private MultiblockAssemblyCompat() {
    }

    public static void register() {
        BlockMovementChecks.registerAttachedCheck(MultiblockAssemblyCompat::isAttached);
    }

    private static CheckResult isAttached(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (state.getBlock() instanceof AirtightFractionationTowerBlock && level.getBlockEntity(pos) instanceof AirtightFractionationTowerBlockEntity tower) {
            if (!(level.getBlockEntity(pos.relative(direction)) instanceof AirtightFractionationTowerBlockEntity neighbor) || !tower.isPartOfSameTower(neighbor)) {
                return CheckResult.PASS;
            }

            return CheckResult.SUCCESS;
        }

        BlockPos master = findMaster(level, pos, state);
        if (master == null) {
            return CheckResult.PASS;
        }

        BlockPos neighbor = pos.relative(direction);
        if (!master.equals(findMaster(level, neighbor, level.getBlockState(neighbor)))) {
            return CheckResult.PASS;
        }

        return CheckResult.SUCCESS;
    }

    private static @Nullable BlockPos findMaster(Level level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof AirtightReactorKettleBlock || block instanceof AirtightForgingPressBlock || block instanceof TeslaTurbineBlock) {
            return pos;
        }

        if (block instanceof AirtightReactorKettleStructural structural && structural.stillValid(level, pos, state)) {
            return AirtightReactorKettleStructural.getMaster(pos, state);
        }

        if (block instanceof AirtightForgingPressStructural structural && structural.stillValid(level, pos, state)) {
            return AirtightForgingPressStructural.getMaster(pos, state);
        }

        if (block instanceof TeslaTurbineStructuralBlock structural && structural.stillValid(level, pos, state, false)) {
            return TeslaTurbineStructuralBlock.getMaster(pos, state);
        }

        return null;
    }
}
