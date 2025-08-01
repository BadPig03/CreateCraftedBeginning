package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

public class CCBItems {
    private static final CreateRegistrate CREATE_REGISTRATE = CreateCraftedBeginning.registrate();

    public static final ItemEntry<Item> CINDER_DOUGH = CREATE_REGISTRATE.item("cinder_dough", Item::new)
        .properties(p -> p.stacksTo(64).fireResistant())
        .register();

    public static final ItemEntry<Item> POLISHED_AMETHYST_SHARD = CREATE_REGISTRATE.item("polished_amethyst_shard", Item::new)
        .properties(p -> p.stacksTo(64))
        .register();

    public static void register() {}
}
