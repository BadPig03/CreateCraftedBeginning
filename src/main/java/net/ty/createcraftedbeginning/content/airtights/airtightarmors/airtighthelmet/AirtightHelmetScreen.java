package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorScreen;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class AirtightHelmetScreen extends AirtightArmorScreen<AirtightHelmetMenu> {
    public AirtightHelmetScreen(AirtightHelmetMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }
}
