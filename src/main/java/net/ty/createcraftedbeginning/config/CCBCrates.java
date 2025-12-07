package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CCBCrates extends ConfigBase {
    public final ConfigInt maxAndesiteCapacity = i(2048, 1, "max_andesite_crate_capacity", Comments.maxAndesiteCapacity);
    public final ConfigInt maxBrassCapacity = i(4096, 1, "max_brass_crate_capacity", Comments.maxBrassCapacity);
    public final ConfigInt maxSturdyCapacity = i(16384, 1, "max_sturdy_crate_capacity", Comments.maxSturdyCapacity);
    public final ConfigInt maxCardboardCapacity = i(64, 1, "max_cardboard_crate_capacity", Comments.maxCardboardCapacity);

    @Override
    public @NotNull String getName() {
        return "crates";
    }

    private static class Comments {
        static String maxAndesiteCapacity = "The maximum item capacity of an Andesite Crate.";
        static String maxBrassCapacity = "The maximum item capacity of a Brass Crate.";
        static String maxSturdyCapacity = "The maximum item capacity of a Sturdy Crate.";
        static String maxCardboardCapacity = "The maximum item capacity of a Cardboard Crate.";
    }
}
