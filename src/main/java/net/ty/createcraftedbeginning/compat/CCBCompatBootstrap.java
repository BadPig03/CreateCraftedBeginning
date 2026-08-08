package net.ty.createcraftedbeginning.compat;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.compat.sable.SableSubLevelCompat;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBCompatBootstrap {
    private CCBCompatBootstrap() {
    }

    public static void initialize() {
        CCBCompatMods.SABLE.executeIfInstalled(() -> SableSubLevelCompat::install);
    }
}
