package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.templates;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.ty.createcraftedbeginning.data.CCBLang;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public enum AirtightHandheldDrillMiningTemplates implements StringRepresentable {
    CUBOID(new CuboidTemplate()),
    HOLLOW_CUBOID(new HollowCuboidTemplate()),
    FRAME_CUBOID(new FrameCuboidTemplate());

    public static final List<Component> TEMPLATE_OPTIONS = generateOptions();
    public static final Codec<AirtightHandheldDrillMiningTemplates> CODEC = StringRepresentable.fromValues(AirtightHandheldDrillMiningTemplates::values);
    public static final StreamCodec<ByteBuf, AirtightHandheldDrillMiningTemplates> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(AirtightHandheldDrillMiningTemplates.class);

    private final BaseTemplate template;

    AirtightHandheldDrillMiningTemplates(BaseTemplate template) {
        this.template = template;
    }

    private static @NotNull List<Component> generateOptions() {
        String prefix = "gui.airtight_handheld_drill.template";
        String[] keys = Arrays.stream(values()).map(AirtightHandheldDrillMiningTemplates::getSerializedName).toArray(String[]::new);
        return CCBLang.translatedOptions(prefix, keys);
    }

    public BaseTemplate getTemplate() {
        return template;
    }

    public @NotNull Component getSizeLabel(int index, @NotNull Direction direction) {
        return switch (direction) {
            case UP, DOWN -> switch (index) {
                case 0 -> CCBLang.translateDirect("gui.airtight_handheld_drill.size.width");
                case 1 -> CCBLang.translateDirect("gui.airtight_handheld_drill.size.length");
                default -> CCBLang.translateDirect("gui.airtight_handheld_drill.size.height");
            };
            default -> switch (index) {
                case 0 -> CCBLang.translateDirect("gui.airtight_handheld_drill.size.width");
                case 1 -> CCBLang.translateDirect("gui.airtight_handheld_drill.size.height");
                default -> CCBLang.translateDirect("gui.airtight_handheld_drill.size.length");
            };
        };
    }

    public @NotNull Component getRelativeLabel(int index, @NotNull Direction direction) {
        return switch (direction) {
            case UP, DOWN -> switch (index) {
                case 0 -> CCBLang.translateDirect("gui.airtight_handheld_drill.relative_position.width");
                case 1 -> CCBLang.translateDirect("gui.airtight_handheld_drill.relative_position.length");
                default -> CCBLang.translateDirect("gui.airtight_handheld_drill.relative_position.height");
            };
            default -> switch (index) {
                case 0 -> CCBLang.translateDirect("gui.airtight_handheld_drill.relative_position.width");
                case 1 -> CCBLang.translateDirect("gui.airtight_handheld_drill.relative_position.height");
                default -> CCBLang.translateDirect("gui.airtight_handheld_drill.relative_position.length");
            };
        };
    }

    @Override
    public @NotNull String getSerializedName() {
        return Lang.asId(name());
    }
}
