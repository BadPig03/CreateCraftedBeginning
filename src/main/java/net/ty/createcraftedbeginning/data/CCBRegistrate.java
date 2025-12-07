package net.ty.createcraftedbeginning.data;

import com.simibubi.create.CreateClient;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.api.registry.registrate.SimpleBuilder;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.builders.BlockEntityBuilder.BlockEntityFactory;
import com.tterrag.registrate.builders.Builder;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.builders.FluidBuilder.FluidTypeFactory;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType.EntityFactory;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Flowing;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.MountedGasStorageType;
import net.ty.createcraftedbeginning.content.fluids.AmethystSuspensionVirtualFluid;
import net.ty.createcraftedbeginning.content.fluids.SlushVirtualFluid;
import net.ty.createcraftedbeginning.registry.CCBRegistries;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class CCBRegistrate extends AbstractRegistrate<CCBRegistrate> {
    private static final Map<RegistryEntry<?, ?>, DeferredHolder<CreativeModeTab, CreativeModeTab>> TAB_LOOKUP = Collections.synchronizedMap(new IdentityHashMap<>());
    private static final ResourceLocation SLUSH = CreateCraftedBeginning.asResource("fluid/slush");
    private static final ResourceLocation AMETHYST_SUSPENSION = CreateCraftedBeginning.asResource("fluid/amethyst_suspension");

    @Nullable
    protected Function<Item, TooltipModifier> currentTooltipModifierFactory;
    protected DeferredHolder<CreativeModeTab, CreativeModeTab> currentTab;

    protected CCBRegistrate(String modId) {
        super(modId);
    }

    @Contract("_ -> new")
    public static @NotNull CCBRegistrate create(String modId) {
        CCBRegistrate registrate = new CCBRegistrate(modId);
        CCBRegistrateRegistrationCallback.provideRegistrate(registrate);
        return registrate;
    }

    public static boolean isOutOfCreativeTab(RegistryEntry<?, ?> entry, DeferredHolder<CreativeModeTab, CreativeModeTab> tab) {
        return TAB_LOOKUP.get(entry) != tab;
    }

    @Contract(pure = true)
    public static <T extends Block> @NotNull NonNullConsumer<? super T> blockModel(Supplier<NonNullFunction<BakedModel, ? extends BakedModel>> func) {
        return entry -> CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> registerBlockModel(entry, func));
    }

    @OnlyIn(Dist.CLIENT)
    private static void registerBlockModel(Block entry, @NotNull Supplier<NonNullFunction<BakedModel, ? extends BakedModel>> func) {
        CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(RegisteredObjectsHelper.getKeyOrThrow(entry), func.get());
    }

    public CCBRegistrate setTooltipModifierFactory(@Nullable Function<Item, TooltipModifier> factory) {
        currentTooltipModifierFactory = factory;
        return self();
    }

    public CCBRegistrate setCreativeTab(DeferredHolder<CreativeModeTab, CreativeModeTab> tab) {
        currentTab = tab;
        return self();
    }

    @Override
    public @NotNull CCBRegistrate registerEventListeners(@NotNull IEventBus bus) {
        return super.registerEventListeners(bus);
    }

    @Override
    protected <R, T extends R> @NotNull RegistryEntry<R, T> accept(@NotNull String name, @NotNull ResourceKey<? extends Registry<R>> type, @NotNull Builder<R, T, ?, ?> builder, @NotNull NonNullSupplier<? extends T> creator, @NotNull NonNullFunction<DeferredHolder<R, T>, ? extends RegistryEntry<R, T>> entryFactory) {
        RegistryEntry<R, T> entry = super.accept(name, type, builder, creator, entryFactory);
        if (type.equals(Registries.ITEM) && currentTooltipModifierFactory != null) {
            Function<Item, TooltipModifier> factory = currentTooltipModifierFactory;
            addRegisterCallback(name, Registries.ITEM, item -> {
                TooltipModifier modifier = factory.apply(item);
                TooltipModifier.REGISTRY.register(item, modifier);
            });
        }
        if (currentTab != null) {
            TAB_LOOKUP.put(entry, currentTab);
        }

        return entry;
    }

    @Override
    public <T extends Entity> @NotNull CCBEntityBuilder<T, CCBRegistrate> entity(@NotNull String name, @NotNull EntityFactory<T> factory, @NotNull MobCategory classification) {
        return entity(self(), name, factory, classification);
    }

    @Override
    public <T extends Entity, P> @NotNull CCBEntityBuilder<T, P> entity(@NotNull P parent, @NotNull String name, @NotNull EntityFactory<T> factory, @NotNull MobCategory classification) {
        return (CCBEntityBuilder<T, P>) entry(name, callback -> CCBEntityBuilder.create(this, parent, name, callback, factory, classification));
    }

    @Override
    public <T extends BlockEntity> @NotNull CCBBlockEntityBuilder<T, CCBRegistrate> blockEntity(@NotNull String name, @NotNull BlockEntityFactory<T> factory) {
        return blockEntity(self(), name, factory);
    }

    @Override
    public <T extends BlockEntity, P> @NotNull CCBBlockEntityBuilder<T, P> blockEntity(@NotNull P parent, @NotNull String name, @NotNull BlockEntityFactory<T> factory) {
        return (CCBBlockEntityBuilder<T, P>) entry(name, callback -> CCBBlockEntityBuilder.create(this, parent, name, callback, factory));
    }

    public <T extends MountedItemStorageType<?>> SimpleBuilder<MountedItemStorageType<?>, T, CCBRegistrate> mountedItemStorage(String name, Supplier<T> supplier) {
        return entry(name, callback -> new SimpleBuilder<>(this, this, name, callback, CreateRegistries.MOUNTED_ITEM_STORAGE_TYPE, supplier).byBlock(MountedItemStorageType.REGISTRY));
    }

    public <T extends MountedGasStorageType<?>> SimpleBuilder<MountedGasStorageType<?>, T, CCBRegistrate> mountedGasStorage(String name, Supplier<T> supplier) {
        return entry(name, callback -> new SimpleBuilder<>(this, this, name, callback, CCBRegistries.MOUNTED_GAS_STORAGE_TYPE, supplier).byBlock(MountedGasStorageType.REGISTRY));
    }

    public FluidBuilder<SlushVirtualFluid, CCBRegistrate> slush_fluid(String name) {
        return entry(name, c -> new CCBVirtualFluidBuilder<>(self(), self(), name, c, SLUSH, SlushVirtualFluid::createSource, SlushVirtualFluid::createFlowing));
    }

    public FluidBuilder<AmethystSuspensionVirtualFluid, CCBRegistrate> amethyst_suspension_fluid(String name) {
        return entry(name, c -> new CCBVirtualFluidBuilder<>(self(), self(), name, c, AMETHYST_SUSPENSION, AmethystSuspensionVirtualFluid::createSource, AmethystSuspensionVirtualFluid::createFlowing));
    }

    public FluidBuilder<Flowing, CCBRegistrate> standardFluid(String name, FluidTypeFactory typeFactory) {
        return fluid(name, CreateCraftedBeginning.asResource("fluid/" + name + "_still"), CreateCraftedBeginning.asResource("fluid/" + name + "_flow"), typeFactory);
    }
}
