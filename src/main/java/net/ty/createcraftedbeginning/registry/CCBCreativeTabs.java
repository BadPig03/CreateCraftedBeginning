package net.ty.createcraftedbeginning.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

public class CCBCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS_REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateCraftedBeginning.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB = TABS_REGISTER.register("creative_tab", () -> CreativeModeTab.builder().icon(() -> new ItemStack(CCBBlocks.BREEZE_CHAMBER_BLOCK.get())).title(Component.translatable("creativetab.createcraftedbeginning.creative_tab")).build());

    public static void register(IEventBus modEventBus) {
        TABS_REGISTER.register(modEventBus);
    }

}
