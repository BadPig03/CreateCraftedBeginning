package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.api.stress.BlockStressValues.GeneratedRpm;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.config.CCBStress;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver.AirtightAssemblyDriverCore;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBStressProviders {
    private CCBStressProviders() {
    }

    public static void register(CCBStress stress) {
        BlockStressValues.IMPACTS.registerProvider(stress::getImpact);
        BlockStressValues.CAPACITIES.registerProvider(stress::getCapacity);
        BlockStressValues.RPM.registerProvider(CCBStressProviders::getGeneratorSpeed);
    }

    private static @Nullable GeneratedRpm getGeneratorSpeed(Block block) {
        if (block == CCBBlocks.AIRTIGHT_ENGINE_BLOCK.get()) {
            int speed = AirtightAssemblyDriverCore.MAX_LEVEL * AirtightEngineBlockEntity.BASE_ROTATION_SPEED;
            return new GeneratedRpm(speed, true);
        }

        if (block == CCBBlocks.TESLA_TURBINE_BLOCK.get()) {
            int speed = TeslaTurbineUtils.MAX_LEVEL * TeslaTurbineUtils.BASE_ROTATION_SPEED;
            return new GeneratedRpm(speed, true);
        }

        return null;
    }
}
