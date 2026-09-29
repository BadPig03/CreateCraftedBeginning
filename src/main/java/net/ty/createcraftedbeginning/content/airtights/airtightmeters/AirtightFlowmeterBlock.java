package net.ty.createcraftedbeginning.content.airtights.airtightmeters;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightFlowmeterBlock extends AbstractAirtightMeterBlock<AirtightFlowmeterBlockEntity> {
    public AirtightFlowmeterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<AirtightFlowmeterBlockEntity> getBlockEntityClass() {
        return AirtightFlowmeterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightFlowmeterBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_FLOWMETER.get();
    }
}
