package net.ty.createcraftedbeginning.registry;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

public class CCBPartialModels {
    public static final PartialModel
        PNEUMATIC_ENGINE_COGS = block("pneumatic_engine/block_cogs"),
        PHOTO_STRESS_BEARING_SHAFT = block("photo-stress_bearing/block_shaft"),
        CINDER_NOZZLE_SHAFT = block("cinder_nozzle/block_shaft");;

    private static PartialModel block(String path) {
        return PartialModel.of(CreateCraftedBeginning.asResource("block/" + path));
    }

    public static void register() {
    }
}
