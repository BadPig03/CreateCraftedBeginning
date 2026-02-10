package net.ty.createcraftedbeginning.registry;

import com.mojang.serialization.Codec;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponentType.Builder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.templates.AirtightHandheldDrillMiningTemplates;
import net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateContents;
import net.ty.createcraftedbeginning.recipe.SequencedAssemblyWithGasRecipe.SequencedAssemblyWithGas;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.UnaryOperator;

public class CCBDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, CreateCraftedBeginning.MOD_ID);

    public static final DataComponentType<List<GasStack>> CANISTER_CONTAINER_CONTENTS = register("canister_container_contents", builder -> builder.persistent(GasStack.OPTIONAL_CODEC.listOf()).networkSynchronized(CatnipStreamCodecBuilders.list(GasStack.OPTIONAL_STREAM_CODEC)));

    public static final DataComponentType<List<Long>> CANISTER_CONTAINER_CAPACITIES = register("canister_container_capacities", builder -> builder.persistent(Codec.LONG.listOf()).networkSynchronized(CatnipStreamCodecBuilders.list(ByteBufCodecs.VAR_LONG)));

    public static final DataComponentType<List<CompoundTag>> CANISTER_PACK_CONTAINER_COMPOUNDS = register("canister_pack_container_compounds", builder -> builder.persistent(CompoundTag.CODEC.listOf()).networkSynchronized(CatnipStreamCodecBuilders.list(ByteBufCodecs.COMPOUND_TAG)));

    public static final DataComponentType<List<Boolean>> CANISTER_PACK_CONTAINER_CREATIVES = register("canister_pack_container_creatives", builder -> builder.persistent(Codec.BOOL.listOf()).networkSynchronized(CatnipStreamCodecBuilders.list(ByteBufCodecs.BOOL)));

    public static final DataComponentType<SturdyCrateContents> STURDY_CRATE_CONTENTS = register("sturdy_crate_contents", builder -> builder.persistent(SturdyCrateContents.CODEC).networkSynchronized(SturdyCrateContents.STREAM_CODEC));

    public static final DataComponentType<Integer> BREEZE_TIME = register("breeze_time", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DataComponentType<String> AIR_COMPRESSOR_OVERHEAT_STATE = register("air_compressor_overheat_state", builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DataComponentType<SequencedAssemblyWithGas> SEQUENCED_ASSEMBLY_WITH_GAS = register("sequenced_assembly_with_gas", builder -> builder.persistent(SequencedAssemblyWithGas.CODEC).networkSynchronized(SequencedAssemblyWithGas.STREAM_CODEC));

    public static final DataComponentType<Integer> DRILL_OPTION_FLAGS = register("drill_option_flags", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DataComponentType<AirtightHandheldDrillMiningTemplates> DRILL_MINING_TEMPLATE = register("drill_mining_template", builder -> builder.persistent(AirtightHandheldDrillMiningTemplates.CODEC).networkSynchronized(AirtightHandheldDrillMiningTemplates.STREAM_CODEC));

    public static final DataComponentType<BlockPos> DRILL_MINING_SIZE = register("drill_mining_size", builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));

    public static final DataComponentType<Direction> DRILL_MINING_DIRECTION = register("drill_mining_direction", builder -> builder.persistent(Direction.CODEC).networkSynchronized(Direction.STREAM_CODEC));

    public static final DataComponentType<BlockPos> DRILL_MINING_RELATIVE_POSITION = register("drill_mining_relative_position", builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));

    public static final DataComponentType<ItemContainerContents> DRILL_INVENTORY = register("drill_inventory", builder -> builder.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC));

    public static final DataComponentType<Integer> GAS_CANISTER_PACK_FLAGS = register("gas_canister_pack_flags", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DataComponentType<Integer> GAS_VIRTUAL_ITEM_COLOR = register("gas_virtual_item_color", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DataComponentType<GasStack> GAS_VIRTUAL_ITEM_TYPE = register("gas_virtual_item_type", builder -> builder.persistent(GasStack.OPTIONAL_CODEC).networkSynchronized(GasStack.OPTIONAL_STREAM_CODEC));

    private static <T> @NotNull DataComponentType<T> register(String name, @NotNull UnaryOperator<Builder<T>> builder) {
        DataComponentType<T> type = builder.apply(DataComponentType.builder()).build();
        COMPONENTS.register(name, () -> type);
        return type;
    }

    public static void register(IEventBus eventBus) {
        COMPONENTS.register(eventBus);
    }
}