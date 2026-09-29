package net.ty.createcraftedbeginning.content.airtights.airtightmeters;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightManometerBlock extends AbstractAirtightMeterBlock<AirtightManometerBlockEntity> {
    public AirtightManometerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<AirtightManometerBlockEntity> getBlockEntityClass() {
        return AirtightManometerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightManometerBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_MANOMETER.get();
    }
}
