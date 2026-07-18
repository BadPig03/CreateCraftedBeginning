package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBCrates extends ConfigBase {
    private static final int MAX_CRATE_CAPACITY = 1048576;

    public final ConfigInt maxAndesiteCapacity = i(2048, 1, MAX_CRATE_CAPACITY, "max_andesite_crate_capacity", Comments.maxAndesiteCapacity);
    public final ConfigInt maxBrassCapacity = i(4096, 1, MAX_CRATE_CAPACITY, "max_brass_crate_capacity", Comments.maxBrassCapacity);
    public final ConfigInt maxSturdyCapacity = i(16384, 1, MAX_CRATE_CAPACITY, "max_sturdy_crate_capacity", Comments.maxSturdyCapacity);
    public final ConfigInt maxCardboardCapacity = i(64, 1, 64, "max_cardboard_crate_capacity", Comments.maxCardboardCapacity);

    @Override
    public String getName() {
        return "crates";
    }

    private static class Comments {
        static String maxAndesiteCapacity = "The maximum item capacity of an Andesite Crate.";
        static String maxBrassCapacity = "The maximum item capacity of a Brass Crate.";
        static String maxSturdyCapacity = "The maximum item capacity of a Sturdy Crate.";
        static String maxCardboardCapacity = "The maximum item capacity of a Cardboard Crate.";
    }
}
