package net.ty.createcraftedbeginning;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.ty.createcraftedbeginning.content.cindernozzle.CinderNozzleOutliner;

@Mod(value = CreateCraftedBeginning.MOD_ID, dist = Dist.CLIENT)
public class CreateCraftedBeginningClient {
        public static final CinderNozzleOutliner CINDER_NOZZLE_HANDLER = new CinderNozzleOutliner();
}