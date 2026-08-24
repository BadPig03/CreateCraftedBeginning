package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class AirtightPipeTransportBehaviour extends AxisGasTransportBehaviour {
    AirtightPipeTransportBehaviour(SmartBlockEntity blockEntity) {
        super(blockEntity);
    }
}
