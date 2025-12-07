package net.ty.createcraftedbeginning.data;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.util.OneTimeEventReceiver;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer.Factory;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class CCBBlockEntityBuilder<T extends BlockEntity, P> extends BlockEntityBuilder<T, P> {
    @Nullable
    private NonNullSupplier<Factory<T>> visualFactory;
    private Predicate<@NotNull T> renderNormally;

    protected CCBBlockEntityBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, BlockEntityFactory<T> factory) {
        super(owner, parent, name, callback, factory);
    }

    @Contract("_, _, _, _, _ -> new")
    public static <T extends BlockEntity, P> @NotNull BlockEntityBuilder<T, P> create(@NotNull AbstractRegistrate<?> owner, @NotNull P parent, @NotNull String name, @NotNull BuilderCallback callback, @NotNull BlockEntityFactory<T> factory) {
        return new CCBBlockEntityBuilder<>(owner, parent, name, callback, factory);
    }

    public CCBBlockEntityBuilder<T, P> visual(NonNullSupplier<Factory<T>> visualFactory) {
        return visual(visualFactory, true);
    }

    public CCBBlockEntityBuilder<T, P> visual(NonNullSupplier<Factory<T>> visualFactory, boolean renderNormally) {
        return visual(visualFactory, be -> renderNormally);
    }

    public CCBBlockEntityBuilder<T, P> visual(NonNullSupplier<Factory<T>> visualFactory, Predicate<@NotNull T> renderNormally) {
        if (this.visualFactory == null) {
            CatnipServices.PLATFORM.executeOnClientOnly(() -> this::registerVisualizer);
        }
        this.visualFactory = visualFactory;
        this.renderNormally = renderNormally;
        return this;
    }

    private void registerVisualizer() {
        OneTimeEventReceiver.addModListener(getOwner(), FMLClientSetupEvent.class, event -> {
            NonNullSupplier<Factory<T>> visualFactory = this.visualFactory;
            if (visualFactory == null) {
                return;
            }

            SimpleBlockEntityVisualizer.builder(getEntry()).factory(visualFactory.get()).skipVanillaRender(be -> !renderNormally.test(be)).apply();
        });
    }
}
