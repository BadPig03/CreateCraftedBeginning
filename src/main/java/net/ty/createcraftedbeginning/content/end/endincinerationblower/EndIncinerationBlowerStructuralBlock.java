package net.ty.createcraftedbeginning.content.end.endincinerationblower;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.ty.createcraftedbeginning.content.end.endcasing.EndMechanicalStructuralBlock;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

public class EndIncinerationBlowerStructuralBlock extends EndMechanicalStructuralBlock implements IBE<EndIncinerationBlowerStructuralBlockEntity> {
    public EndIncinerationBlowerStructuralBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<EndIncinerationBlowerStructuralBlockEntity> getBlockEntityClass() {
        return EndIncinerationBlowerStructuralBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends EndIncinerationBlowerStructuralBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.END_INCINERATION_BLOWER_STRUCTURAL.get();
    }
}
