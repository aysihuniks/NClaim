package nesoi.network.NClaim.integrations;

import nesoi.network.NClaim.NCoreMain;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.function.Consumer;

public class AnvilManager {

    private final NCoreMain plugin;
    private final Player player;
    private final String title;
    private final Consumer<String> onFinish;

    public AnvilManager(NCoreMain plugin, Player player, String title, Consumer<String> onFinish) {
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
                .onClick((slot, stateSnapshot) -> Arrays.asList(
                        AnvilGUI.ResponseAction.run(() -> {
                            onFinish.accept(stateSnapshot.getText());
                        })))
                .title(title)
                .open(player);
    }
}
