package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.ty.createcraftedbeginning.mixin.common.accessor.ContraptionAccessor;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ChunkStabilizerMovingInteraction extends MovingInteractionBehaviour {
    private static void syncOutline(AbstractContraptionEntity entity, BlockPos localPos, boolean showOutline) {
        CompoundTag updateTag = ((ContraptionAccessor) entity.getContraption()).ccb$getUpdateTags().computeIfAbsent(localPos, ignored -> new CompoundTag());
        ChunkStabilizerSerialization.writeShowOutline(updateTag, showOutline);
        CatnipServices.NETWORK.sendToClientsTrackingEntity(entity, new ChunkStabilizerOutlinePacket(entity.getId(), localPos, showOutline));
    }

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand hand, BlockPos localPos, AbstractContraptionEntity entity) {
        if (player.isShiftKeyDown() || !AllItems.WRENCH.isIn(player.getItemInHand(hand)) || !ChunkStabilizerMovementBehaviour.supports(entity)) {
            return false;
        }

        if (entity.level().isClientSide) {
            return true;
        }

        StructureBlockInfo info = entity.getContraption().getBlocks().get(localPos);
        if (info == null) {
            return false;
        }

        CompoundTag tag = info.nbt();
        tag = tag == null ? new CompoundTag() : tag.copy();
        boolean showOutline = !ChunkStabilizerSerialization.readShowOutline(tag);
        ChunkStabilizerSerialization.writeShowOutline(tag, showOutline);
        setContraptionBlockData(entity, localPos, new StructureBlockInfo(localPos, info.state(), tag));
        if (entity instanceof CarriageContraptionEntity carriageEntity) {
            Carriage carriage = carriageEntity.getCarriage();
            if (carriage == null) {
                return false;
            }

            carriage.forEachPresentEntity(present -> syncOutline(present, localPos, showOutline));
            return true;
        }

        syncOutline(entity, localPos, showOutline);
        return true;
    }
}
