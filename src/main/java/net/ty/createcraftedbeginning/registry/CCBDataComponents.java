package net.ty.createcraftedbeginning.registry;

import com.mojang.serialization.Codec;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponentType.Builder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.templates.AirtightHandheldDrillMiningTemplates;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeStatus;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonGasContents;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterUtils.GasFilterData;
import net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateContents;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.UnaryOperator;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, CCBAPI.MOD_ID);

    public static final DataComponentType<SturdyCrateContents> STURDY_CRATE_CONTENTS = register("sturdy_crate_contents", builder -> builder.persistent(SturdyCrateContents.CODEC).networkSynchronized(SturdyCrateContents.STREAM_CODEC));

    public static final DataComponentType<GasStack> CANISTER_CONTAINER_CONTENTS = register("canister_container_contents", builder -> builder.persistent(GasStack.OPTIONAL_CODEC).networkSynchronized(GasStack.OPTIONAL_STREAM_CODEC));
    public static final DataComponentType<ItemContainerContents> GAS_CANISTER_PACK_CONTENTS = register("gas_canister_pack_contents", builder -> builder.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC));

    public static final DataComponentType<Integer> GAS_CANISTER_PACK_FLAGS = register("gas_canister_pack_flags", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DataComponentType<Integer> COMPRESSOR_STORED_HEAT = register("compressor_stored_heat", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DataComponentType<Integer> BREEZE_TIME = register("breeze_time", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DataComponentType<Boolean> BREEZE_CREATIVE = register("breeze_creative", builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static final DataComponentType<AirtightHandheldDrillMiningTemplates> DRILL_MINING_TEMPLATE = register("drill_mining_template", builder -> builder.persistent(AirtightHandheldDrillMiningTemplates.CODEC).networkSynchronized(AirtightHandheldDrillMiningTemplates.STREAM_CODEC));
    public static final DataComponentType<BlockPos> DRILL_MINING_SIZE = register("drill_mining_size", builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));
    public static final DataComponentType<Direction> DRILL_MINING_DIRECTION = register("drill_mining_direction", builder -> builder.persistent(Direction.CODEC).networkSynchronized(Direction.STREAM_CODEC));
    public static final DataComponentType<BlockPos> DRILL_MINING_RELATIVE_POSITION = register("drill_mining_relative_position", builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));

    public static final DataComponentType<ItemContainerContents> AIRTIGHT_UPGRADABLE_INVENTORY = register("airtight_upgradable_inventory", builder -> builder.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC));
    public static final DataComponentType<List<AirtightUpgradeStatus>> AIRTIGHT_UPGRADE_STATUS = register("airtight_upgrade_status", builder -> builder.persistent(AirtightUpgradeStatus.CODEC.listOf()).networkSynchronized(CatnipStreamCodecBuilders.list(AirtightUpgradeStatus.STREAM_CODEC)));

    public static final DataComponentType<Integer> GAS_INJECTION_CHAMBER_FILTER_COLOR = register("gas_injection_chamber_filter_color", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DataComponentType<ResourceLocation> GAS_INJECTION_CHAMBER_FILTER_FAN_PROCESSING_TYPE = register("gas_injection_chamber_filter_fan_processing_type", builder -> builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));

    public static final DataComponentType<BalloonGasContents> BALLOON_GAS_CONTENTS = register("balloon_gas_contents", builder -> builder.persistent(BalloonGasContents.CODEC).networkSynchronized(BalloonGasContents.STREAM_CODEC));
    public static final DataComponentType<Integer> GAS_VIRTUAL_ITEM_COLOR = register("gas_virtual_item_color", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DataComponentType<GasStack> GAS_VIRTUAL_ITEM_TYPE = register("gas_virtual_item_type", builder -> builder.persistent(GasStack.OPTIONAL_CODEC).networkSynchronized(GasStack.OPTIONAL_STREAM_CODEC));
    public static final DataComponentType<GasFilterData> GAS_FILTER_DATA = register("gas_filter_data", builder -> builder.persistent(GasFilterData.CODEC).networkSynchronized(GasFilterData.STREAM_CODEC));

    private static <T> @NotNull DataComponentType<T> register(String name, UnaryOperator<Builder<T>> builder) {
        DataComponentType<T> type = builder.apply(DataComponentType.builder()).build();
        COMPONENTS.register(name, () -> type);
        return type;
    }

    public static void register(IEventBus eventBus) {
        COMPONENTS.register(eventBus);
    }
}