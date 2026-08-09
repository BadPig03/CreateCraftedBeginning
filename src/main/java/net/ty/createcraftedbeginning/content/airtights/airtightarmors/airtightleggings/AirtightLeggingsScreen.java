package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings;

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
public class AirtightLeggingsScreen extends AirtightArmorScreen<AirtightLeggingsMenu> {
    public AirtightLeggingsScreen(AirtightLeggingsMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }
}
