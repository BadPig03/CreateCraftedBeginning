package net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LoadingNetworkColors {
    private static final HashFunction HASH = Hashing.murmur3_128(0);
    private static final int RGB_MASK = 0xFFFFFF;

    private LoadingNetworkColors() {
    }

    static int from(UUID id) {
        ByteBuffer input = ByteBuffer.allocate(Long.BYTES * 2).order(ByteOrder.LITTLE_ENDIAN);
        int hash = HASH.hashBytes(input.putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array()).asInt();
        return 0xFF000000 | hash & RGB_MASK;
    }
}
