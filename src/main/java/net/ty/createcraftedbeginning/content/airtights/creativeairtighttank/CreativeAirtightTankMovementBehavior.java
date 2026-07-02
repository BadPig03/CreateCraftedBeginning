package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeAirtightTankMovementBehavior implements MovementBehaviour {
    @Override
    public boolean mustTickWhileDisabled() {
        return true;
    }
}
