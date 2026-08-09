package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightPipeTransportBehaviour extends AxisGasTransportBehaviour {
    public AirtightPipeTransportBehaviour(SmartBlockEntity blockEntity) {
        super(blockEntity);
    }
}
