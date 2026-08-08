package net.ty.createcraftedbeginning.registry;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.AirtightBootsMenu;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.AirtightChestplateMenu;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.AirtightHelmetMenu;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.AirtightLeggingsMenu;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillMenu;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackMenu;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeSetGasMenu;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterMenu;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBMenuTypes {
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, CreateCraftedBeginning.MOD_ID);

    private static <C extends AbstractContainerMenu> @NotNull DeferredHolder<MenuType<?>, MenuType<C>> register(String name, IContainerFactory<C> factory) {
        return MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static final DeferredHolder<MenuType<?>, MenuType<AirtightHelmetMenu>> AIRTIGHT_HELMET_MENU = register("airtight_helmet", AirtightHelmetMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<AirtightChestplateMenu>> AIRTIGHT_CHESTPLATE_MENU = register("airtight_chestplate", AirtightChestplateMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<AirtightLeggingsMenu>> AIRTIGHT_LEGGINGS_MENU = register("airtight_leggings", AirtightLeggingsMenu::new);
    public static final DeferredHolder<MenuType<?>, MenuType<AirtightBootsMenu>> AIRTIGHT_BOOTS_MENU = register("airtight_boots", AirtightBootsMenu::new);
    public static final DeferredHolder<MenuType<?>, MenuType<AirtightHandheldDrillMenu>> AIRTIGHT_HANDHELD_DRILL_MENU = register("airtight_handheld_drill", AirtightHandheldDrillMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<GasCanisterPackMenu>> GAS_CANISTER_PACK_MENU = register("gas_canister_pack", GasCanisterPackMenu::new);
    public static final DeferredHolder<MenuType<?>, MenuType<GasFilterMenu>> GAS_FILTER_MENU = register("gas_filter", GasFilterMenu::new);
    public static final DeferredHolder<MenuType<?>, MenuType<GasFactoryGaugeSetGasMenu>> GAS_FACTORY_GAUGE_SET_GAS_MENU = register("gas_factory_gauge_set_gas", GasFactoryGaugeSetGasMenu::new);

    public static void register(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }
}
