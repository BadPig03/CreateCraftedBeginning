package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBOpticalPower extends ConfigBase {
    public final AmethystLoader amethystLoader = nested(0, AmethystLoader::new, "Amethyst Loader");
    public final Network network = nested(0, Network::new, "Optical Power Network");
    public final AmethystCollectorPanel amethystCollectorPanel = nested(0, AmethystCollectorPanel::new, "Amethyst Collector Panel");
    public final PhotothermalReceiver photothermalReceiver = nested(0, PhotothermalReceiver::new, "Photothermal Receiver");

    @Override
    public String getName() {
        return "optical_power";
    }

    public static final class AmethystLoader extends ConfigBase {
        public final ConfigBool enableChunkLoading = b(true, "enable_chunk_loading", "Allow Amethyst Loaders to keep their own current chunk ticking without power or nearby players, including Create contraptions, trains and Sable structures. Sable also retains its native physics loading around the structure. Disabling releases loader tickets, including their Sable structure tickets. After re-enabling, trains resume automatically; other unloaded devices or structures resume when loaded again.");

        @Override
        public String getName() {
            return "amethyst_loader";
        }
    }

    public static final class Network extends ConfigBase {
        public final ConfigFloat maxNetworkFluxLm = f(512, 1, 65536, "max_network_flux_lm", "[Unit: lm]", "Maximum luminous flux distributed simultaneously by one optical fiber network.");
        public final ConfigFloat maxConsumerFluxLm = f(48, 1, 512, "max_consumer_flux_lm", "[Unit: lm]", "Maximum luminous flux allocated to each consumer in an optical fiber network. Actual allocation also depends on source output and the network limit.");

        @Override
        public String getName() {
            return "network";
        }
    }

    public static final class AmethystCollectorPanel extends ConfigBase {
        public final ConfigFloat rainOutputMultiplier = f(1, 0, 2, "rain_output_multiplier", "[Unit: multiplier relative to standard behavior; 1 = standard]", "Scales the standard rainy-weather output of 50% of clear-weather output. A value of 0 disables output in rain; 1 preserves the standard reduction; 2 restores clear-weather output.");

        @Override
        public String getName() {
            return "amethyst_collector_panel";
        }
    }

    public static final class PhotothermalReceiver extends ConfigBase {
        public final ConfigFloat maxReceivedFluxLm = f(48, 1, 512, "max_received_flux_lm", "[Unit: lm]", "Maximum combined luminous flux admitted to one photothermal receiver inventory. Heating targets depend on local surface illuminance and coverage, not summed input flux.");

        @Override
        public String getName() {
            return "photothermal_receiver";
        }
    }
}
