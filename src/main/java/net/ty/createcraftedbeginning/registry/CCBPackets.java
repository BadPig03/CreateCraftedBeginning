package net.ty.createcraftedbeginning.registry;

import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.createmod.catnip.net.base.CatnipPacketRegistry.PacketType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseHandlers;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.AirtightChestplateElytraBoostPacket;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonPacket;
import net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower.AirtightFractionationTowerFailurePacket;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.AirtightHandheldDrillGhostItemSubmitPacket;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.AirtightHandheldDrillParametersPacket;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.AirtightHandheldDrillSingleBlockPacket;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeMaterialsSyncPacket;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeMenuSyncPacket;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradePacket;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeSyncPacket;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterOverlayPacket;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClientPacket;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackMenuSyncPacket;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterGhostItemSubmitPacket;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterScreenPacket;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.AmethystLoaderOutlinePacket;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.AmethystLoaderStatePacket;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management.AmethystLoaderBeamPacket;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management.AmethystLoaderColorPacket;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management.AmethystLoaderControlPacket;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management.AmethystLoaderMapPositionPacket;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management.AmethystLoaderMapRequestPacket;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management.AmethystLoaderMapSyncPacket;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management.AmethystLoaderRenamePacket;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management.AmethystLoaderSyncPacket;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorAimPacket;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorAimReplyPacket;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorRotatePacket;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorRotateReplyPacket;
import net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor.AmethystCrystalGrowthPacket;
import net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor.AmethystSoundSuppressorResetPacket;
import net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor.AmethystSoundSuppressorUpdatePacket;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserDisplayPacket;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserDisplayRequestPacket;
import net.ty.createcraftedbeginning.gas.mounted.MountedGasStorageSyncPacket;
import net.ty.createcraftedbeginning.gas.visual.GasAreaOutlinePacket;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum CCBPackets implements PacketTypeProvider {
    AIRTIGHT_CANNON(AirtightCannonPacket.class, AirtightCannonPacket.STREAM_CODEC),
    AIRTIGHT_FRACTIONATION_TOWER_FAILURE(AirtightFractionationTowerFailurePacket.class, AirtightFractionationTowerFailurePacket.STREAM_CODEC),

    AIRTIGHT_HANDHELD_DRILL_PARAMETERS(AirtightHandheldDrillParametersPacket.class, AirtightHandheldDrillParametersPacket.STREAM_CODEC),
    AIRTIGHT_HANDHELD_DRILL_SINGLE_BLOCK(AirtightHandheldDrillSingleBlockPacket.class, AirtightHandheldDrillSingleBlockPacket.STREAM_CODEC),
    AIRTIGHT_HANDHELD_DRILL_GHOST_ITEM_SUBMIT(AirtightHandheldDrillGhostItemSubmitPacket.class, AirtightHandheldDrillGhostItemSubmitPacket.STREAM_CODEC),

    AIRTIGHT_BOOST_ELYTRA(AirtightChestplateElytraBoostPacket.class, AirtightChestplateElytraBoostPacket.STREAM_CODEC),

    AIRTIGHT_UPGRADE(AirtightUpgradePacket.class, AirtightUpgradePacket.STREAM_CODEC),
    AIRTIGHT_UPGRADE_MATERIALS_SYNC(AirtightUpgradeMaterialsSyncPacket.class, AirtightUpgradeMaterialsSyncPacket.STREAM_CODEC),
    AIRTIGHT_UPGRADE_MENU_SYNC(AirtightUpgradeMenuSyncPacket.class, AirtightUpgradeMenuSyncPacket.STREAM_CODEC),
    AIRTIGHT_UPGRADE_SYNC(AirtightUpgradeSyncPacket.class, AirtightUpgradeSyncPacket.STREAM_CODEC),

    GAS_FILTER_SCREEN(GasFilterScreenPacket.class, GasFilterScreenPacket.STREAM_CODEC),
    GAS_FILTER_GHOST_ITEM_SUBMIT(GasFilterGhostItemSubmitPacket.class, GasFilterGhostItemSubmitPacket.STREAM_CODEC),

    GAS_CANISTER_OVERLAY(GasCanisterOverlayPacket.class, GasCanisterOverlayPacket.STREAM_CODEC),
    GAS_CANISTER_PACK_MENU_SYNC(GasCanisterPackMenuSyncPacket.class, GasCanisterPackMenuSyncPacket.STREAM_CODEC),
    GAS_AREA_OUTLINE(GasAreaOutlinePacket.class, GasAreaOutlinePacket.STREAM_CODEC),
    CANISTER_CONTAINER_CLIENT(CanisterContainerClientPacket.class, CanisterContainerClientPacket.STREAM_CODEC),

    AMETHYST_LOADER_CONTROL(AmethystLoaderControlPacket.class, AmethystLoaderControlPacket.STREAM_CODEC),
    AMETHYST_LOADER_RENAME(AmethystLoaderRenamePacket.class, AmethystLoaderRenamePacket.STREAM_CODEC),
    AMETHYST_LOADER_COLOR(AmethystLoaderColorPacket.class, AmethystLoaderColorPacket.STREAM_CODEC),
    AMETHYST_LOADER_BEAM(AmethystLoaderBeamPacket.class, AmethystLoaderBeamPacket.STREAM_CODEC),
    AMETHYST_LOADER_SYNC(AmethystLoaderSyncPacket.class, AmethystLoaderSyncPacket.STREAM_CODEC),
    AMETHYST_LOADER_MAP_REQUEST(AmethystLoaderMapRequestPacket.class, AmethystLoaderMapRequestPacket.STREAM_CODEC),
    AMETHYST_LOADER_MAP_POSITION(AmethystLoaderMapPositionPacket.class, AmethystLoaderMapPositionPacket.STREAM_CODEC),
    AMETHYST_LOADER_MAP_SYNC(AmethystLoaderMapSyncPacket.class, AmethystLoaderMapSyncPacket.STREAM_CODEC),

    AMETHYST_LOADER_STATE(AmethystLoaderStatePacket.class, AmethystLoaderStatePacket.STREAM_CODEC),

    AMETHYST_LOADER_OUTLINE(AmethystLoaderOutlinePacket.class, AmethystLoaderOutlinePacket.STREAM_CODEC),

    AMETHYST_CRYSTAL_GROWTH(AmethystCrystalGrowthPacket.class, AmethystCrystalGrowthPacket.STREAM_CODEC),
    AMETHYST_SOUND_SUPPRESSOR_RESET(AmethystSoundSuppressorResetPacket.class, AmethystSoundSuppressorResetPacket.STREAM_CODEC),
    AMETHYST_SOUND_SUPPRESSOR_UPDATE(AmethystSoundSuppressorUpdatePacket.class, AmethystSoundSuppressorUpdatePacket.STREAM_CODEC),

    AMETHYST_MIRROR_ROTATE(AmethystMirrorRotatePacket.class, AmethystMirrorRotatePacket.STREAM_CODEC),
    AMETHYST_MIRROR_ROTATE_REPLY(AmethystMirrorRotateReplyPacket.class, AmethystMirrorRotateReplyPacket.STREAM_CODEC),
    AMETHYST_MIRROR_AIM(AmethystMirrorAimPacket.class, AmethystMirrorAimPacket.STREAM_CODEC),
    AMETHYST_MIRROR_AIM_REPLY(AmethystMirrorAimReplyPacket.class, AmethystMirrorAimReplyPacket.STREAM_CODEC),

    MOUNTED_GAS_STORAGE_SYNC(MountedGasStorageSyncPacket.class, MountedGasStorageSyncPacket.STREAM_CODEC),
    LASER_DISPLAY(LaserDisplayPacket.class, LaserDisplayPacket.STREAM_CODEC),
    LASER_DISPLAY_REQUEST(LaserDisplayRequestPacket.class, LaserDisplayRequestPacket.STREAM_CODEC);

    private final PacketType<?> type;

    <T extends BasePacketPayload> CCBPackets(Class<T> clazz, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        String name = Lang.asId(name());
        type = new PacketType<>(new Type<>(CCBAPI.asResource(name)), clazz, codec);
    }

    public static void register() {
        CatnipPacketRegistry packetRegistry = new CatnipPacketRegistry(CCBAPI.MOD_ID, 45);
        Arrays.stream(values()).map(packet -> packet.type).forEach(packetRegistry::registerPacket);
        packetRegistry.registerAllPackets();
        GasReleaseHandlers.registerOutlineSender(GasAreaOutlinePacket::send);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> Type<T> getType() {
        return (Type<T>) type.type();
    }
}
