package nesoi.aysihuniks.nclaim.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import lombok.Getter;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.enums.HeadProvider;
import nesoi.aysihuniks.nclaim.model.User;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nandayo.dapi.DAPI;
import org.nandayo.dapi.Util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Getter
public class HeadManager {
    private final HeadProvider headProvider;
    private final int version;
    private final SkinsRestorer skinsRestorer;
    private final boolean skinRestorerHooked;
    private final Map<UUID, String> skinTextureCache;
    private final Map<UUID, ItemStack> headCache;
    private final Map<UUID, Long> textureFetchCooldown;
    private static final long COOLDOWN_MS = 3600 * 1000;

    public HeadManager() {
        version = DAPI.getInstance().getWrapper().getMinecraftVersion();
        skinsRestorer = getSkinsRestorerInstance();
        skinRestorerHooked = skinsRestorer != null;
        headProvider = skinRestorerHooked ? HeadProvider.SKINRESTORER : HeadProvider.NBTAPI;
        skinTextureCache = new HashMap<>();
        headCache = new HashMap<>();
        textureFetchCooldown = new HashMap<>();
        Util.log("&aHeadManager enabled successfully! Using " + headProvider.name() + " for heads.");
    }

    private SkinsRestorer getSkinsRestorerInstance() {
        try {
            return (SkinsRestorer) Bukkit.getPluginManager().getPlugin("SkinsRestorer");
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public String getSkinTextureValue(@NotNull UUID uuid, boolean forceFetch) {
        Long lastFetched = textureFetchCooldown.get(uuid);
        if (!forceFetch && lastFetched != null && System.currentTimeMillis() - lastFetched < COOLDOWN_MS) {
            User user = User.getUser(uuid);
            if (user != null && user.getSkinTexture() != null) {
                return user.getSkinTexture();
            }
            return skinTextureCache.get(uuid);
        }

        User user = User.getUser(uuid);
        if (user != null && user.getSkinTexture() != null) {
            return user.getSkinTexture();
        }

        String cachedTexture = skinTextureCache.get(uuid);
        if (cachedTexture != null || skinTextureCache.containsKey(uuid)) {
            return cachedTexture;
        }

        String texture = null;
        if (skinRestorerHooked) {
            OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(uuid);
            PlayerStorage storage = skinsRestorer.getPlayerStorage();
            try {
                Optional<SkinProperty> property = storage.getSkinForPlayer(uuid, offPlayer.getName());
                if (property.isPresent()) {
                    String textureURL = PropertyUtils.getSkinTextureUrl(property.get());
                    String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + textureURL + "\"}}}";
                    texture = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {}
        } else {
            try {
                URL url_0 = new URL("https://api.mojang.com/users/profiles/minecraft/" + Bukkit.getOfflinePlayer(uuid).getName());
                InputStreamReader reader_0 = new InputStreamReader(url_0.openStream());
                String id = new JsonParser().parse(reader_0).getAsJsonObject().get("id").getAsString();

                URL url_1 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + id + "?unsigned=false");
                InputStreamReader reader_1 = new InputStreamReader(url_1.openStream());
                JsonObject textureProperty = new JsonParser().parse(reader_1).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();
                texture = textureProperty.get("value").getAsString();
            } catch (Exception ignored) {}
        }

        textureFetchCooldown.put(uuid, System.currentTimeMillis());
        skinTextureCache.put(uuid, texture);
        if (user != null && texture != null) {
            user.setSkinTexture(texture);
            User.saveUser(uuid);
        }

        return texture;
    }

    public ItemStack createHead(OfflinePlayer player) {
        UUID uuid = player.getUniqueId();

        ItemStack cachedHead = headCache.get(uuid);
        if (cachedHead != null) {
            return cachedHead.clone();
        }

        String texture = getSkinTextureValue(uuid, false);
        if (texture == null) {
            ItemStack defaultHead = new ItemStack(Material.PLAYER_HEAD);
            headCache.put(uuid, defaultHead);
            return defaultHead.clone();
        }

        ItemStack head = createHeadWithTexture(texture);
        headCache.put(uuid, head);
        return head.clone();
    }

    public ItemStack createHeadWithTexture(final String texture) {
        try {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            if (version > 204) {
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
            return head;
        } catch (Exception e) {
            Util.log("HeadManager error creating head with NBTAPI: " + e.getMessage());
            return new ItemStack(Material.PLAYER_HEAD);
        }
    }

    public void preloadTexturesAsync(Collection<UUID> uuids) {
        Bukkit.getScheduler().runTaskAsynchronously(NClaim.inst(), () -> {
            for (UUID uuid : uuids) {
                String texture = getSkinTextureValue(uuid, false);
                if (texture != null) {
                    ItemStack head = createHeadWithTexture(texture);
                    headCache.put(uuid, head);
                } else {
                    ItemStack defaultHead = new ItemStack(Material.PLAYER_HEAD);
                    headCache.put(uuid, defaultHead);
                }
            }
        });
    }
}