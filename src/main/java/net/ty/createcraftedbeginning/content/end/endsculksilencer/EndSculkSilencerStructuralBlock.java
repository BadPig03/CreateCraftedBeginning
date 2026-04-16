package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.ty.createcraftedbeginning.content.end.endcasing.EndMechanicalStructuralBlock;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

public class EndSculkSilencerStructuralBlock extends EndMechanicalStructuralBlock implements IBE<EndSculkSilencerStructuralBlockEntity> {
    public EndSculkSilencerStructuralBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<EndSculkSilencerStructuralBlockEntity> getBlockEntityClass() {
        return EndSculkSilencerStructuralBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends EndSculkSilencerStructuralBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.END_SCULK_SILENCER_STRUCTURAL.get();
    }
}
