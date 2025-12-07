package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;

public class AirtightTankMovementBehavior implements MovementBehaviour {
    @Override
    public boolean mustTickWhileDisabled() {
        return true;
    }
}
