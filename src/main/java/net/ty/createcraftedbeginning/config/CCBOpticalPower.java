package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBOpticalPower extends ConfigBase {
    public final Network network = nested(0, Network::new, "Optical Power Network");
    public final AmethystCollectorPanel amethystCollectorPanel = nested(0, AmethystCollectorPanel::new, "Amethyst Collector Panel");
    public final LaserReceiver laserReceiver = nested(0, LaserReceiver::new, "Laser Receiver");

    @Override
    public String getName() {
        return "optical_power";
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class Network extends ConfigBase {
        public final ConfigInt maxNetworkPowerSu = i(32768, 1024, 16777216, "max_network_power_su", "[Unit: equivalent SU]", "Maximum Optical Power distributed simultaneously by one Optical Fiber network. Rounded down to a multiple of 256 SU.");

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
    public static final class LaserReceiver extends ConfigBase {
        public final ConfigInt maxReceivedPowerSu = i(8192, 256, 4194304, "max_received_power_su", "[Unit: equivalent SU]", "Maximum combined Optical Power accepted by one Laser Receiver. Multiple beams add together up to this limit. Rounded down to a multiple of 256 SU; final mechanical output also depends on the configured stress capacity.");

        @Override
        public String getName() {
            return "laser_receiver";
        }
    }
}
