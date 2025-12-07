package net.ty.createcraftedbeginning.registry;

import com.tterrag.registrate.builders.MenuBuilder.ForgeMenuFactory;
import com.tterrag.registrate.builders.MenuBuilder.ScreenFactory;
import com.tterrag.registrate.util.entry.MenuEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillMenu;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillScreen;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackMenu;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackScreen;
import org.jetbrains.annotations.NotNull;

public class CCBMenuTypes {
    public static final MenuEntry<AirtightHandheldDrillMenu> AIRTIGHT_HANDHELD_DRILL_MENU = register("airtight_handheld_drill", AirtightHandheldDrillMenu::new, () -> AirtightHandheldDrillScreen::new);
    public static final MenuEntry<GasCanisterPackMenu> GAS_CANISTER_PACK_MENU = register("gas_canister_pack", GasCanisterPackMenu::new, () -> GasCanisterPackScreen::new);

    private static <C extends AbstractContainerMenu, S extends Screen & MenuAccess<C>> @NotNull MenuEntry<C> register(String name, ForgeMenuFactory<C> factory, NonNullSupplier<ScreenFactory<C, S>> screenFactory) {
        return CreateCraftedBeginning.registrate().menu(name, factory, screenFactory).register();
    }

    public static void register() {
    }
}
