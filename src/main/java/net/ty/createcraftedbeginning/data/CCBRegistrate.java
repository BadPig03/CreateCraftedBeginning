package net.ty.createcraftedbeginning.data;

import com.simibubi.create.CreateClient;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.api.registry.registrate.SimpleBuilder;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.builders.Builder;
import com.tterrag.registrate.builders.FluidBuilder;
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
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.fluids.AmethystSuspensionVirtualFluid;
import net.ty.createcraftedbeginning.content.fluids.CompressedAirFakeFluid;
import net.ty.createcraftedbeginning.content.fluids.CoolingTimeVirtualFluid;
import net.ty.createcraftedbeginning.content.fluids.SlushVirtualFluid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.ty.createcraftedbeginning.data.CCBRegistrateRegistrationCallbackImpl.CALLBACKS_VIEW;

public class CCBRegistrate extends AbstractRegistrate<CCBRegistrate> {
    private static final Map<RegistryEntry<?, ?>, DeferredHolder<CreativeModeTab, CreativeModeTab>> TAB_LOOKUP = Collections.synchronizedMap(new IdentityHashMap<>());

    @Nullable
    protected Function<Item, TooltipModifier> currentTooltipModifierFactory;
    protected DeferredHolder<CreativeModeTab, CreativeModeTab> currentTab;

    protected CCBRegistrate(String modId) {
        super(modId);
    }

    public static CCBRegistrate create(String modId) {
        return new CCBRegistrate(modId);
    }

    @SuppressWarnings("removal")
    public static FluidType defaultFluidType(FluidType.Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture) {
        return new FluidType(properties) {
            @Override
            public void initializeClient(@NotNull Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    @Override
                    public @NotNull ResourceLocation getStillTexture() {
                        return stillTexture;
                    }

                    @Override
                    public @NotNull ResourceLocation getFlowingTexture() {
                        return flowingTexture;
                    }
                });
            }
        };
    }

    public static <T extends Block> NonNullConsumer<? super T> blockModel(Supplier<NonNullFunction<BakedModel, ? extends BakedModel>> func) {
        return entry -> CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> registerBlockModel(entry, func));
    }

    @OnlyIn(Dist.CLIENT)
    private static void registerBlockModel(Block entry, Supplier<NonNullFunction<BakedModel, ? extends BakedModel>> func) {
        CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(RegisteredObjectsHelper.getKeyOrThrow(entry), func.get());
    }

    public CCBRegistrate setTooltipModifierFactory(@Nullable Function<Item, TooltipModifier> factory) {
        currentTooltipModifierFactory = factory;
        return self();
    }

    public void setCreativeTab(DeferredHolder<CreativeModeTab, CreativeModeTab> tab) {
        currentTab = tab;
    }

    @Override
    public @NotNull CCBRegistrate registerEventListeners(@NotNull IEventBus bus) {
        return super.registerEventListeners(bus);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <R, T extends R> @NotNull RegistryEntry<R, T> accept(@NotNull String name, @NotNull ResourceKey<? extends Registry<R>> type, @NotNull Builder<R, T, ?, ?> builder, @NotNull NonNullSupplier<? extends T> creator, @NotNull NonNullFunction<DeferredHolder<R, T>, ? extends RegistryEntry<R, T>> entryFactory) {
        RegistryEntry<R, T> entry = super.accept(name, type, builder, creator, entryFactory);
        if (type.equals(Registries.ITEM) && currentTooltipModifierFactory != null) {
            Function<Item, TooltipModifier> factory = currentTooltipModifierFactory;
            this.addRegisterCallback(name, Registries.ITEM, item -> {
                TooltipModifier modifier = factory.apply(item);
                TooltipModifier.REGISTRY.register(item, modifier);
            });
        }
        if (currentTab != null) {
            TAB_LOOKUP.put(entry, currentTab);
        }

        for (CCBRegistrateRegistrationCallbackImpl.CallbackImpl<?> callback : CALLBACKS_VIEW) {
            String modId = callback.id().getNamespace();
            String entryId = callback.id().getPath();
            if (callback.registry().equals(type) && getModid().equals(modId) && name.equals(entryId)) {
                ((Consumer<T>) callback.callback()).accept(entry.get());
            }
        }

        return entry;
    }

    @Override
    public <T extends Entity> @NotNull CCBEntityBuilder<T, CCBRegistrate> entity(@NotNull String name, EntityType.@NotNull EntityFactory<T> factory, @NotNull MobCategory classification) {
        return this.entity(self(), name, factory, classification);
    }

    @Override
    public <T extends Entity, P> @NotNull CCBEntityBuilder<T, P> entity(@NotNull P parent, @NotNull String name, EntityType.@NotNull EntityFactory<T> factory, @NotNull MobCategory classification) {
        return (CCBEntityBuilder<T, P>) this.entry(name, (callback) -> CCBEntityBuilder.create(this, parent, name, callback, factory, classification));
    }

    @Override
    public <T extends BlockEntity> @NotNull CCBBlockEntityBuilder<T, CCBRegistrate> blockEntity(@NotNull String name, @NotNull BlockEntityBuilder.BlockEntityFactory<T> factory) {
        return blockEntity(self(), name, factory);
    }

    @Override
    public <T extends BlockEntity, P> @NotNull CCBBlockEntityBuilder<T, P> blockEntity(@NotNull P parent, @NotNull String name, BlockEntityBuilder.@NotNull BlockEntityFactory<T> factory) {
        return (CCBBlockEntityBuilder<T, P>) entry(name, callback -> CCBBlockEntityBuilder.create(this, parent, name, callback, factory));
    }

    public <T extends MountedItemStorageType<?>> SimpleBuilder<MountedItemStorageType<?>, T, CCBRegistrate> mountedItemStorage(String name, Supplier<T> supplier) {
        return this.entry(name, callback -> new SimpleBuilder<>(this, this, name, callback, CreateRegistries.MOUNTED_ITEM_STORAGE_TYPE, supplier).byBlock(MountedItemStorageType.REGISTRY));
    }

    public <T extends MountedFluidStorageType<?>> SimpleBuilder<MountedFluidStorageType<?>, T, CCBRegistrate> mountedFluidStorage(String name, Supplier<T> supplier) {
        return this.entry(name, callback -> new SimpleBuilder<>(this, this, name, callback, CreateRegistries.MOUNTED_FLUID_STORAGE_TYPE, supplier).byBlock(MountedFluidStorageType.REGISTRY));
    }

    public FluidBuilder<CompressedAirFakeFluid, CCBRegistrate> compressed_air_fluid(String name) {
        return entry(name, c -> new CCBVirtualFluidBuilder<>(self(), self(), name, c, ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "fluid/compressed_air"), ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "fluid/compressed_air"), CCBRegistrate::defaultFluidType, CompressedAirFakeFluid::createSource, CompressedAirFakeFluid::createFlowing));
    }

    public FluidBuilder<SlushVirtualFluid, CCBRegistrate> slush_fluid(String name) {
        return entry(name, c -> new CCBVirtualFluidBuilder<>(self(), self(), name, c, ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "fluid/slush"), ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "fluid/slush"), CCBRegistrate::defaultFluidType, SlushVirtualFluid::createSource, SlushVirtualFluid::createFlowing));
    }

    public FluidBuilder<AmethystSuspensionVirtualFluid, CCBRegistrate> amethyst_suspension_fluid(String name) {
        return entry(name, c -> new CCBVirtualFluidBuilder<>(self(), self(), name, c, ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "fluid/amethyst_suspension"), ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "fluid/amethyst_suspension"), CCBRegistrate::defaultFluidType, AmethystSuspensionVirtualFluid::createSource, AmethystSuspensionVirtualFluid::createFlowing));
    }

    public FluidBuilder<CoolingTimeVirtualFluid, CCBRegistrate> cooling_time(String name) {
        return entry(name, c -> new CCBVirtualFluidBuilder<>(self(), self(), name, c, ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "fluid/compressed_air"), ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "fluid/compressed_air"), CCBRegistrate::defaultFluidType, CoolingTimeVirtualFluid::createSource, CoolingTimeVirtualFluid::createFlowing));
    }
}
