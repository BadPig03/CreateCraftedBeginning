package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.api.registry.registrate.SimpleBuilder;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.Builder;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.builders.FluidBuilder.FluidTypeFactory;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Flowing;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.airtights.gas.mounted.MountedGasStorageType;
import net.ty.createcraftedbeginning.content.fluids.amethystsuspension.AmethystSuspensionVirtualFluid;
import net.ty.createcraftedbeginning.content.fluids.slush.SlushVirtualFluid;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabLayout.CCBCreativeTabSection;
import net.ty.createcraftedbeginning.registry.CCBRegistries;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBRegistrate extends AbstractRegistrate<CCBRegistrate> {
    private static final Map<RegistryEntry<?, ?>, CCBCreativeTabSection> SECTION_LOOKUP = Collections.synchronizedMap(new IdentityHashMap<>());
    private static final ResourceLocation SLUSH = CCBAPI.asResource("fluid/slush");
    private static final ResourceLocation AMETHYST_SUSPENSION = CCBAPI.asResource("fluid/amethyst_suspension");

    @Nullable
    protected Function<Item, TooltipModifier> currentTooltipModifierFactory;
    protected CCBCreativeTabSection currentCreativeSection;

    protected CCBRegistrate(String modId) {
        super(modId);
    }

    @Contract("_ -> new")
    public static CCBRegistrate create(String modId) {
        CCBRegistrate registrate = new CCBRegistrate(modId);
        CCBRegistrateRegistrationCallback.provideRegistrate(registrate);
        return registrate;
    }

    public static boolean isOutOfCreativeSection(RegistryEntry<?, ?> entry, CCBCreativeTabSection section) {
        return SECTION_LOOKUP.get(entry) != section;
    }

    public CCBRegistrate setTooltipModifierFactory(@Nullable Function<Item, TooltipModifier> factory) {
        currentTooltipModifierFactory = factory;
        return self();
    }

    public CCBRegistrate setCreativeSection(CCBCreativeTabSection section) {
        currentCreativeSection = section;
        return self();
    }

    @Override
    public CCBRegistrate registerEventListeners(IEventBus bus) {
        return super.registerEventListeners(bus);
    }

    @SuppressWarnings("ObjectEqualsCanBeEquality")
    @Override
    protected <R, T extends R> @NotNull RegistryEntry<R, T> accept(String name, ResourceKey<? extends Registry<R>> type, Builder<R, T, ?, ?> builder, NonNullSupplier<? extends T> creator, NonNullFunction<DeferredHolder<R, T>, ? extends RegistryEntry<R, T>> entryFactory) {
        RegistryEntry<R, T> entry = super.accept(name, type, builder, creator, entryFactory);
        if (type.equals(Registries.ITEM) && currentTooltipModifierFactory != null) {
            Function<Item, TooltipModifier> factory = currentTooltipModifierFactory;
            addRegisterCallback(name, Registries.ITEM, item -> {
                TooltipModifier modifier = factory.apply(item);
                TooltipModifier.REGISTRY.register(item, modifier);
            });
        }
        if (currentCreativeSection == null) {
            return entry;
        }

        SECTION_LOOKUP.put(entry, currentCreativeSection);
        return entry;
    }

    public <T extends MountedItemStorageType<?>> SimpleBuilder<MountedItemStorageType<?>, T, CCBRegistrate> mountedItemStorage(String name, Supplier<T> supplier) {
        return entry(name, callback -> new SimpleBuilder<>(this, this, name, callback, CreateRegistries.MOUNTED_ITEM_STORAGE_TYPE, supplier).byBlock(MountedItemStorageType.REGISTRY));
    }

    public <T extends MountedGasStorageType<?>> SimpleBuilder<MountedGasStorageType<?>, T, CCBRegistrate> mountedGasStorage(String name, Supplier<T> supplier) {
        return entry(name, callback -> new SimpleBuilder<>(this, this, name, callback, CCBRegistries.MOUNTED_GAS_STORAGE_TYPE, supplier).byBlock(MountedGasStorageType.REGISTRY));
    }

    public FluidBuilder<SlushVirtualFluid, CCBRegistrate> slush_fluid(String name) {
        return entry(name, callback -> new CCBVirtualFluidBuilder<>(self(), self(), name, callback, SLUSH, SlushVirtualFluid::createSource, SlushVirtualFluid::createFlowing));
    }

    public FluidBuilder<AmethystSuspensionVirtualFluid, CCBRegistrate> amethyst_suspension_fluid(String name) {
        return entry(name, callback -> new CCBVirtualFluidBuilder<>(self(), self(), name, callback, AMETHYST_SUSPENSION, AmethystSuspensionVirtualFluid::createSource, AmethystSuspensionVirtualFluid::createFlowing));
    }

    public FluidBuilder<Flowing, CCBRegistrate> standardFluid(String name, FluidTypeFactory typeFactory) {
        return fluid(name, CCBAPI.asResource("fluid/" + name + "_still"), CCBAPI.asResource("fluid/" + name + "_flow"), typeFactory);
    }
}
