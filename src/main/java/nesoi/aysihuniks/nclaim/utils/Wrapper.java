package nesoi.aysihuniks.nclaim.utils;

import lombok.Getter;
import nesoi.aysihuniks.nclaim.NClaim;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Getter
public class Wrapper {

    private final NClaim plugin;
    private final int version;

    public Wrapper(NClaim plugin) {
        this.plugin = plugin;
        version = org.nandayo.dapi.util.Wrapper.getMinecraftVersion();
    }
    
    public void playSound(@NotNull Player player, @NotNull Sound sound, float volume, float pitch) {
        if(version >= 181) {
            player.playSound(player, sound, volume, pitch);
        }else {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
}
