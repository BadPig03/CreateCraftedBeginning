package net.ty.createcraftedbeginning.compat.jade;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.compat.jade.gas.GasBlockTooltipRenderer;
import net.ty.createcraftedbeginning.compat.jade.gas.GasConstants;
import net.ty.createcraftedbeginning.compat.jade.gas.GasContraptionDataProvider;
import net.ty.createcraftedbeginning.compat.jade.gas.GasDataProvider;
import net.ty.createcraftedbeginning.compat.jade.gas.GasEntityTooltipRenderer;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import org.jetbrains.annotations.NotNull;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class JadePlugin implements IWailaPlugin {
    @Override
    public void register(@NotNull IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(GasDataProvider.INSTANCE, BlockEntity.class);
        registration.registerEntityDataProvider(GasContraptionDataProvider.INSTANCE, AbstractContraptionEntity.class);

        registration.registerBlockDataProvider(BreezeChamberComponentProvider.INSTANCE, BreezeChamberBlockEntity.class);
        registration.registerBlockDataProvider(BreezeCoolerComponentProvider.INSTANCE, BreezeCoolerBlockEntity.class);
        registration.registerFluidStorage(AirCompressorProvider.INSTANCE, AirCompressorBlockEntity.class);
        registration.registerFluidStorage(GasInjectionChamberProvider.INSTANCE, GasInjectionChamberBlockEntity.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void registerClient(@NotNull IWailaClientRegistration registration) {
        registration.addConfig(GasConstants.GAS, true);

        registration.registerBlockComponent((IComponentProvider<BlockAccessor>) GasBlockTooltipRenderer.INSTANCE, Block.class);
        registration.registerEntityComponent((IComponentProvider<EntityAccessor>) GasEntityTooltipRenderer.INSTANCE, AbstractContraptionEntity.class);

        registration.registerBlockComponent(BreezeChamberComponentProvider.INSTANCE, BreezeChamberBlock.class);
        registration.registerBlockComponent(BreezeCoolerComponentProvider.INSTANCE, BreezeCoolerBlock.class);
        registration.registerFluidStorageClient(AirCompressorProvider.INSTANCE);
        registration.registerFluidStorageClient(GasInjectionChamberProvider.INSTANCE);
    }
}
