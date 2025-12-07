package net.ty.createcraftedbeginning.data;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.builders.EntityBuilder;
import com.tterrag.registrate.util.OneTimeEventReceiver;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer;
import dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer.Factory;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType.EntityFactory;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class CCBEntityBuilder<T extends Entity, P> extends EntityBuilder<T, P> {
    @Nullable
    private NonNullSupplier<Factory<T>> visualFactory;
    private Predicate<@NotNull T> renderNormally;

    public CCBEntityBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, EntityFactory<T> factory, MobCategory classification) {
        super(owner, parent, name, callback, factory, classification);
    }

    public static <T extends Entity, P> @NotNull EntityBuilder<T, P> create(@NotNull AbstractRegistrate<?> owner, @NotNull P parent, @NotNull String name, @NotNull BuilderCallback callback, @NotNull EntityFactory<T> factory, @NotNull MobCategory classification) {
        return new CCBEntityBuilder<>(owner, parent, name, callback, factory, classification).defaultLang();
    }

    public CCBEntityBuilder<T, P> visual(NonNullSupplier<Factory<T>> visualFactory) {
        return visual(visualFactory, true);
    }

    public CCBEntityBuilder<T, P> visual(NonNullSupplier<Factory<T>> visualFactory, boolean renderNormally) {
        return visual(visualFactory, entity -> renderNormally);
    }

    public CCBEntityBuilder<T, P> visual(NonNullSupplier<Factory<T>> visualFactory, Predicate<@NotNull T> renderNormally) {
        if (this.visualFactory == null) {
            CatnipServices.PLATFORM.executeOnClientOnly(() -> this::registerVisualizer);
        }

        this.visualFactory = visualFactory;
        this.renderNormally = renderNormally;

        return this;
    }

    protected void registerVisualizer() {
        OneTimeEventReceiver.addModListener(getOwner(), FMLClientSetupEvent.class, $ -> {
            NonNullSupplier<Factory<T>> visualFactory = this.visualFactory;
            if (visualFactory == null) {
                return;
            }

            SimpleEntityVisualizer.builder(getEntry()).factory(visualFactory.get()).skipVanillaRender(entity -> !renderNormally.test(entity)).apply();
        });
    }
}
