package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CCBEquipment extends ConfigBase {
    public final ConfigInt maxAirtightCannonShots = i(200, 0, "maxAirtightCannonShots", Comments.maxAirtightCannonShots);

    @Override
    public @NotNull String getName() {
        return "equipment";
    }

    private static class Comments {
        static String maxAirtightCannonShots = "Amount of free Airtight Cannon shots provided by Compressed Air Canisters. Set to 0 makes Airtight Cannons unbreakable.";
    }
}
