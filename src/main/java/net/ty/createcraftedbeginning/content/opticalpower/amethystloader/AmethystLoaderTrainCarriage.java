package net.ty.createcraftedbeginning.content.opticalpower.amethystloader;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.ty.createcraftedbeginning.mixin.common.accessor.CarriageAccessor;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.apache.commons.lang3.tuple.MutablePair;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record AmethystLoaderTrainCarriage(@Nullable CompoundTag source, Direction direction, List<BlockPos> loaders, Map<BlockPos, UUID> identities) {
    private static final String COMPOUND_KEY_CONTRAPTION = "Contraption";

    static @Nullable AmethystLoaderTrainCarriage capture(ServerLevel level, Carriage carriage, @Nullable AmethystLoaderTrainCarriage cached) {
        CarriageContraptionEntity entity = carriage.anyAvailableEntity();
        if (entity == null || !entity.isAlive() || !(entity.getContraption() instanceof CarriageContraption contraption)) {
            CompoundTag source = ((CarriageAccessor) carriage).ccb$getSerialisedEntity();
            if (cached != null && cached.source() == source) {
                return cached;
            }

            return read(level, source);
        }

        if (contraption.disassembled) {
            return null;
        }

        return create(null, contraption);
    }

    static AmethystLoaderTrainCarriage read(ServerLevel level, CompoundTag source) {
        if (!(Contraption.fromNBT(level, source.getCompound(COMPOUND_KEY_CONTRAPTION), false) instanceof CarriageContraption contraption)) {
            throw new IllegalStateException("Expected native carriage geometry in the train loading snapshot.");
        }

        return create(source, contraption);
    }

    private static AmethystLoaderTrainCarriage create(@Nullable CompoundTag source, CarriageContraption contraption) {
        List<BlockPos> loaders = new ArrayList<>();
        Map<BlockPos, UUID> identities = new HashMap<>();
        for (MutablePair<StructureBlockInfo, MovementContext> actor : contraption.getActors()) {
            BlockPos localPos = actor.getLeft().pos();
            StructureBlockInfo info = contraption.getBlocks().get(localPos);
            if (info == null || !info.state().is(CCBBlocks.AMETHYST_LOADER_BLOCK.get())) {
                continue;
            }

            loaders.add(localPos);
            UUID identity = AmethystLoaderSerialization.readNetworkNodeId(info.nbt());
            if (identity == null) {
                continue;
            }

            identities.put(localPos, identity);
        }
        return new AmethystLoaderTrainCarriage(source, contraption.getAssemblyDirection(), List.copyOf(loaders), Map.copyOf(identities));
    }
}
