package net.ty.createcraftedbeginning.data;

import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.content.logistics.box.PackageStyles.PackageStyle;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.Item;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonStyles;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBItemBuilderTransformer {
    private CCBItemBuilderTransformer() {
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> gasCanister() {
        return b -> b.model((c, p) -> p.withExistingParent(c.getName(), p.modLoc("block/gas_canister")));
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> creativeGasCanister() {
        return b -> b.model((c, p) -> p.withExistingParent(c.getName(), p.modLoc("block/creative_gas_canister")));
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> balloon(PackageStyle style) {
        return b -> b.properties(p -> p.stacksTo(1)).tag(AllItemTags.PACKAGES.tag).model((c, p) -> p.withExistingParent(c.getName(), p.modLoc("item/balloon/balloon_" + BalloonStyles.getPath(style)))).lang("Balloon").setData(ProviderType.LANG, NonNullBiConsumer.noop());
    }
}
