package net.ty.createcraftedbeginning.content.airtights.airtightupgrades;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record AirtightUpgradeStatus(ResourceLocation id, boolean isEnabled, boolean isInstalled) {
    public static final Codec<AirtightUpgradeStatus> CODEC = RecordCodecBuilder.create(i -> i.group(ResourceLocation.CODEC.fieldOf("id").forGetter(AirtightUpgradeStatus::id), Codec.BOOL.fieldOf("isEnabled").forGetter(AirtightUpgradeStatus::isEnabled), Codec.BOOL.fieldOf("isInstalled").forGetter(AirtightUpgradeStatus::isInstalled)).apply(i, AirtightUpgradeStatus::new));
    public static final StreamCodec<ByteBuf, AirtightUpgradeStatus> STREAM_CODEC = StreamCodec.composite(ResourceLocation.STREAM_CODEC, AirtightUpgradeStatus::id,ByteBufCodecs.BOOL, AirtightUpgradeStatus::isEnabled, ByteBufCodecs.BOOL, AirtightUpgradeStatus::isInstalled, AirtightUpgradeStatus::new);
}