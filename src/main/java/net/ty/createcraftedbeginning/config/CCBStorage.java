package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBStorage extends ConfigBase {
    private static final int MAX_CRATE_CAPACITY = 1048576;

    public final AndesiteCrate andesiteCrate = nested(0, AndesiteCrate::new, "Andesite Crate");
    public final BrassCrate brassCrate = nested(0, BrassCrate::new, "Brass Crate");
    public final SturdyCrate sturdyCrate = nested(0, SturdyCrate::new, "Sturdy Crate");
    public final CardboardCrate cardboardCrate = nested(0, CardboardCrate::new, "Cardboard Crate");

    @Override
    public String getName() {
        return "storage";
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class AndesiteCrate extends ConfigBase {
        public final ConfigInt itemCapacity = i(2048, 1, MAX_CRATE_CAPACITY, "item_capacity", "[Unit: items]", "Maximum number of individual items stored in one Andesite Crate. This is an item count, not a count of item stacks.");

        @Override
        public String getName() {
            return "andesite_crate";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class BrassCrate extends ConfigBase {
        public final ConfigInt itemCapacity = i(4096, 1, MAX_CRATE_CAPACITY, "item_capacity", "[Unit: items]", "Maximum number of individual items stored in one Brass Crate. This is an item count, not a count of item stacks.");

        @Override
        public String getName() {
            return "brass_crate";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class SturdyCrate extends ConfigBase {
        public final ConfigInt itemCapacity = i(16384, 1, MAX_CRATE_CAPACITY, "item_capacity", "[Unit: items]", "Maximum number of individual items stored in one Sturdy Crate. This is an item count, not a count of item stacks.");

        @Override
        public String getName() {
            return "sturdy_crate";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class CardboardCrate extends ConfigBase {
        public final ConfigInt itemCapacity = i(64, 1, 64, "item_capacity", "[Unit: items]", "Maximum number of individual items stored in one Cardboard Crate. This is an item count, not a count of item stacks.");

        @Override
        public String getName() {
            return "cardboard_crate";
        }
    }
}
