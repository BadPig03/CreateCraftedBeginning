package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.ty.createcraftedbeginning.api.gas.AxisGasPipeBlock;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

public class AirtightPipeBlock extends AxisGasPipeBlock implements IBE<AirtightPipeBlockEntity> {
    public AirtightPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<AirtightPipeBlockEntity> getBlockEntityClass() {
        return AirtightPipeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightPipeBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_PIPE.get();
    }
}
