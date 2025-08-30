package net.ty.createcraftedbeginning.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;

public class CondensateDrainScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("condensate_drain", "Drain condensates via Condensate Drains");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        scene.idle(60);
        scene.markAsFinished();
    }
}
