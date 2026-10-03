package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBOpticalPower extends ConfigBase {
    public final Network network = nested(0, Network::new, "Optical Power Network");
    public final AmethystCollectorPanel amethystCollectorPanel = nested(0, AmethystCollectorPanel::new, "Amethyst Collector Panel");
    public final PhotothermalReceiver photothermalReceiver = nested(0, PhotothermalReceiver::new, "Photothermal Receiver");

    @Override
    public String getName() {
        return "optical_power";
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class Network extends ConfigBase {
        public final ConfigInt maxNetworkPowerLp = i(512, 1, 65536, "max_network_power_lp", "[Unit: LP]", "Maximum optical power distributed simultaneously by one optical fiber network.");

        @Override
        public String getName() {
            return "network";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class AmethystCollectorPanel extends ConfigBase {
        public final ConfigFloat rainOutputMultiplier = f(1, 0, 2, "rain_output_multiplier", "[Unit: multiplier relative to standard behavior; 1 = standard]", "Scales the standard rainy-weather output of 50% of clear-weather output. A value of 0 disables output in rain; 1 preserves the standard reduction; 2 restores clear-weather output.");

        @Override
        public String getName() {
            return "amethyst_collector_panel";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class PhotothermalReceiver extends ConfigBase {
        public final ConfigInt maxReceivedPowerLp = i(48, 1, 48, "max_received_power_lp", "[Unit: LP]", "Maximum combined optical power absorbed by one photothermal receiver. 16 LP targets 50% heat, 32 LP targets 100%, and 48 LP targets superheating at 150%.");

        @Override
        public String getName() {
            return "photothermal_receiver";
        }
    }
}
