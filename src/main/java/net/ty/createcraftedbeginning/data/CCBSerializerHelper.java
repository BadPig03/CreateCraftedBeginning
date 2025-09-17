package net.ty.createcraftedbeginning.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.Util;

import java.util.function.Function;

public class CCBSerializerHelper {
    public static final Codec<Long> NON_NEGATIVE_LONG_CODEC = Util.make(() -> {
        final Function<Long, DataResult<Long>> checker = Codec.checkRange(0L, Long.MAX_VALUE);
        return Codec.LONG.flatXmap(checker, checker);
    });

    private CCBSerializerHelper() {
    }
}
