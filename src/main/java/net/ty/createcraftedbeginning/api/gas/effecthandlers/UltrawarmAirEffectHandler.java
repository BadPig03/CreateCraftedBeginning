package net.ty.createcraftedbeginning.api.gas.effecthandlers;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasOpenPipeEffectHandler;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.List;

public class UltrawarmAirEffectHandler implements GasOpenPipeEffectHandler {
    @Override
    public void apply(@NotNull Level level, AABB area, Gas gas) {
        if (level.dimensionType().ultraWarm()) {
            return;
        }

        List<Entity> entities = level.getEntities((Entity) null, area, entity -> entity instanceof AbstractPiglin || entity instanceof HoglinBase);
        for (Entity entity : entities) {
            if (entity instanceof AbstractPiglin piglin) {
                modifyPiglinTime(piglin);
            }
        }
    }

    private static void modifyPiglinTime(AbstractPiglin piglin) {
        try {
            Field timeField = AbstractPiglin.class.getDeclaredField("timeInOverworld");
            timeField.setAccessible(true);
            timeField.setInt(piglin, 0);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
    }
}
