package nesoi.network.NClaim.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;

public class HeadManager {

    static public boolean api = false;

    public HeadManager() {
        api = true;
    }


    public static HashMap<Player, String> textureMap = new HashMap<>();

    public static String offlinePlayerTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VkMWFiYTczZjYzOWY0YmM0MmJkNDgxOTZjNzE1MTk3YmUyNzEyYzNiOTYyYzk3ZWJmOWU5ZWQ4ZWZhMDI1In19fQ==";

    public static ItemStack getPlayerHead(OfflinePlayer player) {

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);

        if (!api) return head;

        String texture = textureMap.get(player);

        if (isVersionAtLeast("1.20.5")) {
            NBT.modifyComponents(head, nbt -> {
                ReadWriteNBT profileNbt = nbt.getOrCreateCompound("minecraft:profile");
                profileNbt.setUUID("id", UUID.randomUUID());
                ReadWriteNBT propertiesNbt = profileNbt.getCompoundList("properties").addCompound();
                propertiesNbt.setString("name", "textures");
                propertiesNbt.setString("value", (texture != null) ? texture : offlinePlayerTexture);
            });
        } else {
            NBT.modify(head, nbt -> {
                ReadWriteNBT skullOwnerCompound = nbt.getOrCreateCompound("SkullOwner");
                skullOwnerCompound.setUUID("Id", UUID.randomUUID());
                skullOwnerCompound.getOrCreateCompound("Properties")
                        .getCompoundList("textures")
                        .addCompound()
                        .setString("Value", (texture != null) ? texture : offlinePlayerTexture);
            });
        }

        return head;
    }

    public static String getFromName(String name) {
        try {
            URL url_0 = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            InputStreamReader reader_0 = new InputStreamReader(url_0.openStream());
            String uuid = new JsonParser().parse(reader_0).getAsJsonObject().get("id").getAsString();

            URL url_1 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
            InputStreamReader reader_1 = new InputStreamReader(url_1.openStream());
            JsonObject textureProperty = new JsonParser().parse(reader_1).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();
            String texture = textureProperty.get("value").getAsString();

            return texture;
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean isVersionAtLeast(String compareVersion) {
        String[] versionParts = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
        int minor = Integer.parseInt(versionParts[1]);
        int patch;
        if(versionParts.length > 2) patch = Integer.parseInt(versionParts[2]); else patch = 0;

        String[] compareParts = compareVersion.split("\\.");
        int compareMinor = Integer.parseInt(compareParts[1]);
        int comparePatch;
        if(compareParts.length > 2) comparePatch = Integer.parseInt(compareParts[2]); else comparePatch = 0;

        if(minor == compareMinor && patch >= comparePatch) {
            return true;
        }
        return minor > compareMinor;
    }
}
