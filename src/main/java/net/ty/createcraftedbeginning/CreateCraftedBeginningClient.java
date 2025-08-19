package net.ty.createcraftedbeginning;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.ty.createcraftedbeginning.content.cinderincinerationblower.CinderIncinerationBlowerOutliner;

@Mod(value = CreateCraftedBeginning.MOD_ID, dist = Dist.CLIENT)
public class CreateCraftedBeginningClient {
    public static final CinderIncinerationBlowerOutliner CINDER_NOZZLE_HANDLER = new CinderIncinerationBlowerOutliner();
}