package net.ty.createcraftedbeginning.content.mobeffects;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class JetpackFlightEffect extends MobEffect {
    private static final ResourceLocation ID = CreateCraftedBeginning.asResource("jetpack_flight_effect");

    public JetpackFlightEffect(MobEffectCategory category, int color) {
        super(category, color);
        addAttributeModifier(NeoForgeMod.CREATIVE_FLIGHT, ID, 1, Operation.ADD_VALUE);
    }
}
