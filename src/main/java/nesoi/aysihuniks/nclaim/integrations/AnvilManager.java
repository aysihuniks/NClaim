package nesoi.aysihuniks.nclaim.integrations;

import nesoi.aysihuniks.nclaim.NClaim;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class AnvilManager {

    private final NClaim plugin;
    private final Player player;
    private final String title;
    private final Consumer<String> onFinish;

    public AnvilManager(NClaim plugin, Player player, String title, Consumer<String> onFinish) {
        this.plugin = plugin;
        this.player = player;
        this.title = title;
        this.onFinish = onFinish;

        open();
    }

    public void open() {
        new AnvilGUI.Builder()
                .plugin(plugin)
                .itemLeft(new ItemStack(Material.PAPER))
                .itemOutput(new ItemStack(Material.NAME_TAG))
                .interactableSlots(AnvilGUI.Slot.OUTPUT)
                .onClick((slot, stateSnapshot) -> Collections.singletonList(
                        AnvilGUI.ResponseAction.run(() -> onFinish.accept(stateSnapshot.getText()))))
                .title(title)
                .open(player);
    }
}
