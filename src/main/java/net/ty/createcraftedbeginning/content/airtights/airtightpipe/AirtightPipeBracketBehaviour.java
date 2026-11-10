package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightPipeBracketBehaviour extends BracketedBlockEntityBehaviour {
    private static final String COMPOUND_KEY_BRACKET = "Bracket";

    AirtightPipeBracketBehaviour(AirtightPipeBlockEntity entity) {
        super(entity, state -> !state.getValue(AirtightPipeBlock.CASED) && AirtightPipeConnections.straightAxis(state) != null);
    }

    @Override
    public void write(CompoundTag tag, Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (!clientPacket || isBracketPresent()) {
            return;
        }

        tag.put(COMPOUND_KEY_BRACKET, NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));
    }
}
