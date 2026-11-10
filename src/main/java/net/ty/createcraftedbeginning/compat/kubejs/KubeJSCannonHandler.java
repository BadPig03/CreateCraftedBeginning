package net.ty.createcraftedbeginning.compat.kubejs;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Function;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonShotContext;
import net.ty.createcraftedbeginning.api.cannonhandlers.DefaultCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.visual.CannonAnimationType;
import net.ty.createcraftedbeginning.api.cannonhandlers.visual.CannonModelType;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightCannonHandlerEvent.ExplodeCannonHandler;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightCannonHandlerEvent.IconCannonHandler;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightCannonHandlerEvent.ParticlesCannonHandler;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightCannonHandlerEvent.TextCannonHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class KubeJSCannonHandler extends DefaultCannonHandler {
    private final float directHitDamage;
    private final double consumptionMultiplier;
    private final float rotationSpeed;
    private final ResourceLocation texture;
    private final CannonModelType modelType;
    private final CannonAnimationType animationType;
    @Nullable
    private final IconCannonHandler icon;
    @Nullable
    private final ParticlesCannonHandler particles;
    @Nullable
    private final ExplodeCannonHandler explode;
    @Nullable
    private final ExplodeCannonHandler afterExplode;
    @Nullable
    private final TextCannonHandler text;

    public KubeJSCannonHandler(Context context, @Nullable Object options) {
        if (!(options instanceof Map<?, ?> fields)) {
            throw new IllegalArgumentException("Cannon handler options must be an object.");
        }

        for (Object key : fields.keySet()) {
            switch (String.valueOf(key)) {
                case "directHitDamage", "consumptionMultiplier", "rotationMultiplier", "texture", "modelType", "animationType", "icon", "particles", "explode", "afterExplode", "text" -> {}
                default -> throw new IllegalArgumentException("Unknown cannon handler field '" + key + "'.");
            }
        }

        if (fields.containsKey("explode") && fields.containsKey("afterExplode")) {
            throw new IllegalArgumentException("Cannon handler fields 'explode' and 'afterExplode' are mutually exclusive.");
        }

        directHitDamage = (float) readNumber(fields, "directHitDamage", INSTANCE.getDirectHitDamage(), 0, Float.MAX_VALUE);
        consumptionMultiplier = readNumber(fields, "consumptionMultiplier", INSTANCE.getGasConsumptionMultiplier(), 0, Double.MAX_VALUE);
        float rotationMultiplier = (float) readNumber(fields, "rotationMultiplier", 1, -Float.MAX_VALUE, Float.MAX_VALUE);
        rotationSpeed = INSTANCE.getRotationSpeed() * rotationMultiplier;
        if (!Float.isFinite(rotationSpeed)) {
            throw new IllegalArgumentException("Cannon handler field 'rotationMultiplier' produces a non-finite rotation speed.");
        }

        if (fields.containsKey("texture")) {
            Object value = fields.get("texture");
            if (!(value instanceof CharSequence path)) {
                throw new IllegalArgumentException("Cannon handler field 'texture' must be a resource location string.");
            }

            ResourceLocation location = ResourceLocation.tryParse(path.toString());
            if (location == null) {
                throw new IllegalArgumentException("Invalid cannon handler texture: " + value);
            }

            texture = location;
        }
        else {
            texture = INSTANCE.getTextureLocation();
        }
        modelType = readEnum(fields, "modelType", INSTANCE.getModelType());
        animationType = readEnum(fields, "animationType", INSTANCE.getAnimationType());
        icon = readCallback(context, fields, "icon", IconCannonHandler.class);
        particles = readCallback(context, fields, "particles", ParticlesCannonHandler.class);
        explode = readCallback(context, fields, "explode", ExplodeCannonHandler.class);
        afterExplode = readCallback(context, fields, "afterExplode", ExplodeCannonHandler.class);
        text = readCallback(context, fields, "text", TextCannonHandler.class);
    }

    private static double readNumber(Map<?, ?> fields, String name, double fallback, double minimum, double maximum) {
        if (!fields.containsKey(name)) {
            return fallback;
        }

        Object value = fields.get(name);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Cannon handler field '" + name + "' must be a number; got " + value + '.');
        }

        double amount = number.doubleValue();
        if (!Double.isFinite(amount) || amount < minimum || amount > maximum) {
            throw new IllegalArgumentException("Cannon handler field '" + name + "' must be finite and in [" + minimum + ", " + maximum + "]; got " + value + '.');
        }

        return amount;
    }

    private static <T extends Enum<T>> T readEnum(Map<?, ?> fields, String name, T fallback) {
        if (!fields.containsKey(name)) {
            return fallback;
        }

        Object value = fields.get(name);
        if (!(value instanceof CharSequence choice)) {
            throw new IllegalArgumentException("Cannon handler field '" + name + "' must be an enum name string.");
        }

        try {
            return Enum.valueOf(fallback.getDeclaringClass(), choice.toString());
        }
        catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Unknown cannon handler field '" + name + "' value: " + value, failure);
        }
    }

    @Nullable
    private static <T> T readCallback(Context context, Map<?, ?> fields, String name, Class<T> type) {
        if (!fields.containsKey(name)) {
            return null;
        }

        Object callback = fields.get(name);
        if (!(callback instanceof Function)) {
            throw new IllegalArgumentException("Cannon handler field '" + name + "' must be a function.");
        }

        return type.cast(context.jsToJava(callback, TypeInfo.of(type)));
    }

    @Override
    public float getDirectHitDamage() {
        return directHitDamage;
    }

    @Override
    public double getGasConsumptionMultiplier() {
        return consumptionMultiplier;
    }

    @Override
    public float getRotationSpeed() {
        return rotationSpeed;
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return texture;
    }

    @Override
    public CannonModelType getModelType() {
        return modelType;
    }

    @Override
    public CannonAnimationType getAnimationType() {
        return animationType;
    }

    @Override
    public ItemStack getRenderIcon(Level level) {
        if (icon == null) {
            return super.getRenderIcon(level);
        }

        return new ItemStack(BuiltInRegistries.ITEM.getOptional(icon.apply(level)).orElse(Items.BARRIER));
    }

    @Override
    public void renderTrailParticles(Level level, Vec3 pos, Vec3 velocity) {
        if (particles == null) {
            super.renderTrailParticles(level, pos, velocity);
            return;
        }

        particles.apply(level, pos, velocity);
    }

    @Override
    public void explode(Level level, Vec3 pos, AirtightCannonShotContext context) {
        if (explode != null) {
            explode.apply(level, pos, context);
            return;
        }

        super.explode(level, pos, context);
        if (level.isClientSide || afterExplode == null) {
            return;
        }

        try {
            afterExplode.apply(level, pos, context);
        }
        catch (RuntimeException failure) {
            CCBAPI.LOGGER.error("Failed to apply airtight cannon afterExplode for gas '{}' at {}.", context.gasHolder().value().getResourceLocation(), pos, failure);
        }
    }

    @Override
    public void appendHoverText(ItemStack cannon, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (text == null) {
            super.appendHoverText(cannon, context, tooltip, flag);
            return;
        }

        text.apply(cannon, context, tooltip, flag);
    }
}
