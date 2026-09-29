package net.ty.createcraftedbeginning.gametests;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig.Entry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.config.CCBConfig;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConfigurationGameTests {
    private ConfigurationGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void stressRangesAllowZeroAndRejectInvalidNumbers(GameTestHelper helper) {
        Map<String, List<String>> requiredKeys = Map.of("stress_impact", List.of("end_incineration_blower", "airtight_regulator_pump", "airtight_valve", "end_sculk_silencer_structural", "airtight_reactor_kettle_structural_cog", "end_incineration_blower_structural", "end_sculk_silencer", "airtight_pump", "airtight_forging_press_structural_shaft"), "stress_capacity", List.of("airtight_engine", "laser_receiver", "tesla_turbine", "pneumatic_engine"));
        for (String group : List.of("stress_impact", "stress_capacity")) {
            ModConfigSpec specification = CCBConfig.server().specification;
            if (specification == null) {
                continue;
            }

            UnmodifiableConfig values = specification.getSpec().get(List.of("kinetics", group));
            if (values == null) {
                throw new NullPointerException("Expected stress configuration group 'kinetics." + group + "'.");
            }

            helper.assertTrue(!values.entrySet().isEmpty(), "Expected nonempty stress configuration group '" + group + "'.");
            for (String key : requiredKeys.get(group)) {
                helper.assertTrue(values.contains(List.of(key)), "Missing required stress configuration '" + group + '.' + key + "'.");
            }
            for (Entry entry : values.entrySet()) {
                ValueSpec spec = entry.getRawValue();
                if (spec == null) {
                    throw new NullPointerException("Expected stress configuration specification for '" + entry.getKey() + "'.");
                }

                String key = group + '.' + entry.getKey();
                helper.assertTrue(spec.test(0.0), "Expected zero to remain valid for '" + key + "'.");
                helper.assertTrue(spec.test(spec.getDefault()), "Expected a valid default for '" + key + "'.");
                helper.assertTrue(spec.test(Double.MAX_VALUE), "Expected the finite upper limit to remain valid for '" + key + "'.");
                helper.assertTrue(!spec.test(-1.0), "Expected negative values to be rejected for '" + key + "'.");
                helper.assertTrue(!spec.test(Double.NaN), "Expected NaN to be rejected for '" + key + "'.");
                helper.assertTrue(!spec.test(Double.POSITIVE_INFINITY), "Expected infinity to be rejected for '" + key + "'.");
            }
        }

        helper.succeed();
    }
}
