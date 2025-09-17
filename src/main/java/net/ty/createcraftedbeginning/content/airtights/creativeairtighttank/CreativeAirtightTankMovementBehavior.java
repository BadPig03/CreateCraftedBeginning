package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;

public class CreativeAirtightTankMovementBehavior implements MovementBehaviour {
    @Override
	public boolean mustTickWhileDisabled() {
		return true;
	}
}
