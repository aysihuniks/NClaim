package nesoi.network.NClaim.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;

public class HeadManager {

    public static ItemStack getPlayerHead(OfflinePlayer player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        String[] textureData = getFromName(player.getName()); // Fetch the texture data

        if (textureData != null) {
            String texture = textureData[0];

            if (isVersionAtLeast("1.20.5")) {
                NBT.modifyComponents(head, nbt -> {
                    ReadWriteNBT profileNbt = nbt.getOrCreateCompound("minecraft:profile");
                    profileNbt.setUUID("id", UUID.randomUUID());
                    ReadWriteNBT propertiesNbt = profileNbt.getCompoundList("properties").addCompound();
                    propertiesNbt.setString("name", "textures");
                    propertiesNbt.setString("value", texture);
                });
            } else {
                NBT.modify(head, nbt -> {
                    ReadWriteNBT skullOwnerCompound = nbt.getOrCreateCompound("SkullOwner");

                    skullOwnerCompound.setUUID("Id", UUID.randomUUID());
                    skullOwnerCompound.getOrCreateCompound("Properties")
                            .getCompoundList("textures")
                            .addCompound()
                            .setString("Value", texture);
                });
            }
        }

        return head;
    }



    public static String[] getFromName(String name) {
        try {
            URL url_0 = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            InputStreamReader reader_0 = new InputStreamReader(url_0.openStream());
            String uuid = new JsonParser().parse(reader_0).getAsJsonObject().get("id").getAsString();

            URL url_1 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
            InputStreamReader reader_1 = new InputStreamReader(url_1.openStream());
            JsonObject textureProperty = new JsonParser().parse(reader_1).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();
            String texture = textureProperty.get("value").getAsString();
            String signature = textureProperty.get("signature").getAsString();

            return new String[] {texture, signature};
        } catch (IOException e) {
            System.err.println("Could not get skin data from session servers!");
            e.printStackTrace();
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
