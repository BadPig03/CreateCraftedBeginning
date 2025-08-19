package net.ty.createcraftedbeginning.compat.jade;

import net.ty.createcraftedbeginning.content.aircompressor.AirCompressorBlockEntity;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.content.gasinjectionchamber.GasInjectionChamberBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(BreezeChamberComponentProvider.INSTANCE, BreezeChamberBlockEntity.class);
        registration.registerFluidStorage(AirCompressorProvider.INSTANCE, AirCompressorBlockEntity.class);
        registration.registerFluidStorage(BreezeChamberProvider.INSTANCE, BreezeChamberBlockEntity.class);
        registration.registerFluidStorage(GasInjectionChamberProvider.INSTANCE, GasInjectionChamberBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(BreezeChamberComponentProvider.INSTANCE, BreezeChamberBlock.class);
        registration.registerFluidStorageClient(AirCompressorProvider.INSTANCE);
        registration.registerFluidStorageClient(BreezeChamberProvider.INSTANCE);
        registration.registerFluidStorageClient(GasInjectionChamberProvider.INSTANCE);
    }
}
