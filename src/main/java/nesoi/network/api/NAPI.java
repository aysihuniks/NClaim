package nesoi.network.api;

import nesoi.network.api.web.Setting;
import nesoi.network.api.web.WebCreator;
import org.bukkit.plugin.Plugin;
import org.nandayo.DAPI.Util;

import java.io.IOException;

public final class NAPI {

    public final Plugin plugin;
    public static NAPI instance;

    public NAPI(Plugin plugin) throws IOException {
        instance = this;
        this.plugin = plugin;

        if (plugin != null) {
            Util.PREFIX = "&8[<#fa8443>NAPI&8]&r ";
            new WebCreator();

            this.add("Prefix",
                    "[NCORE] > Tag at the beginning of messages",
                    "string",
                    "NCore")
                    .add("Check for Updates",
                        "When enabled, the plugin will notify the console if an update is available.",
                        "boolean",
                        true);
        }
    }

    public NAPI add(String title, String description, String type, Object value) {
        Setting.add(title, description, type, value);
        return this;
    }


}
