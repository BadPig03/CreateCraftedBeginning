package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.management;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.ChunkStabilizerLocation;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.management.ChunkStabilizerSnapshot.Member;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkRuntime.Status;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class ChunkStabilizerDisplay {
    private ChunkStabilizerDisplay() {
    }

    static String name(Member member) {
        String name = member.name();
        if (name.isBlank()) {
            return CCBLang.translateDirect("loading_network.default_loader_name").getString();
        }

        return name;
    }

    static Component position(Member member, @Nullable ChunkStabilizerLocation location) {
        if (location == null && (member.train() || member.moving() || member.physical() || member.status() == Status.ASSEMBLED)) {
            return Component.empty();
        }

        BlockPos pos = member.position().pos();
        if (location != null) {
            pos = location.blockPosition().pos();
        }
        return Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
    }
}
