package net.ty.createcraftedbeginning.content.airtights.airtightmeters;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AxisGasTransportBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightMeterTransportBehaviour extends AxisGasTransportBehaviour {
    AirtightMeterTransportBehaviour(SmartBlockEntity blockEntity) {
        super(blockEntity);
    }
}
