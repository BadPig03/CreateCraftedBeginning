package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;

public class CCBItems {
    private static final CreateRegistrate CREATE_REGISTRATE = CreateCraftedBeginning.registrate();

    public static final ItemEntry<Item> CINDER_ALLOY = CREATE_REGISTRATE.item("cinder_alloy", Item::new)
        .properties(p -> p.stacksTo(64).fireResistant())
        .tag(CCBItemTags.CINDER_ALLOY.tag)
        .register();

    public static final ItemEntry<Item> AMETHYST_CRYSTAL_SHEET = CREATE_REGISTRATE.item("amethyst_crystal_sheet", Item::new)
        .properties(p -> p.stacksTo(64))
        .register();

    public static void register() {}
}
