package net.ty.createcraftedbeginning.registry;

import com.tterrag.registrate.builders.MenuBuilder.ForgeMenuFactory;
import com.tterrag.registrate.builders.MenuBuilder.ScreenFactory;
import com.tterrag.registrate.util.entry.MenuEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.AirtightBootScreen;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.AirtightBootsMenu;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.AirtightChestplateMenu;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.AirtightChestplateScreen;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.AirtightHelmetMenu;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.AirtightHelmetScreen;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.AirtightLeggingsMenu;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.AirtightLeggingsScreen;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillMenu;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillScreen;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackMenu;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackScreen;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterMenu;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterScreen;
import org.jetbrains.annotations.NotNull;

public class CCBMenuTypes {
    public static final MenuEntry<AirtightHelmetMenu> AIRTIGHT_HELMET_MENU = register("airtight_helmet", AirtightHelmetMenu::new, () -> AirtightHelmetScreen::new);
    public static final MenuEntry<AirtightChestplateMenu> AIRTIGHT_CHESTPLATE_MENU = register("airtight_chestplate", AirtightChestplateMenu::new, () -> AirtightChestplateScreen::new);
    public static final MenuEntry<AirtightLeggingsMenu> AIRTIGHT_LEGGINGS_MENU = register("airtight_leggings", AirtightLeggingsMenu::new, () -> AirtightLeggingsScreen::new);
    public static final MenuEntry<AirtightBootsMenu> AIRTIGHT_BOOTS_MENU = register("airtight_boots", AirtightBootsMenu::new, () -> AirtightBootScreen::new);
    public static final MenuEntry<AirtightHandheldDrillMenu> AIRTIGHT_HANDHELD_DRILL_MENU = register("airtight_handheld_drill", AirtightHandheldDrillMenu::new, () -> AirtightHandheldDrillScreen::new);
    public static final MenuEntry<GasCanisterPackMenu> GAS_CANISTER_PACK_MENU = register("gas_canister_pack", GasCanisterPackMenu::new, () -> GasCanisterPackScreen::new);
    public static final MenuEntry<GasFilterMenu> GAS_FILTER_MENU = register("gas_filter", GasFilterMenu::new, () -> GasFilterScreen::new);

    private static <C extends AbstractContainerMenu, S extends Screen & MenuAccess<C>> @NotNull MenuEntry<C> register(String name, ForgeMenuFactory<C> factory, NonNullSupplier<ScreenFactory<C, S>> screenFactory) {
        return CreateCraftedBeginning.registrate().menu(name, factory, screenFactory).register();
    }

    public static void register() {
    }
}
