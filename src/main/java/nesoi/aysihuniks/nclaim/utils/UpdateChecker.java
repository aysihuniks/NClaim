package nesoi.aysihuniks.nclaim.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.nandayo.dapi.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

public class UpdateChecker {

    private final int resourceId;
    private final Plugin plugin;

    public UpdateChecker(Plugin plugin, int resourceId) {
        this.resourceId = resourceId;
        this.plugin = plugin;
    }

    public void getVersion(final Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try (InputStream is = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId + "/~").openStream();
                 Scanner scann = new Scanner(is)) {
                if (scann.hasNext()) {
                    consumer.accept(scann.next());
                }
            } catch (IOException e) {
                Util.log("&cUnable to check for updates: " + e.getMessage());
            }
        });
    }
}
