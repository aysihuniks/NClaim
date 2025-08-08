package nesoi.aysihuniks.nclaim.integrations;

import com.google.common.collect.Sets;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nandayo.dapi.guimanager.button.Button;
import org.nandayo.dapi.guimanager.menu.AnvilMenu;
import org.nandayo.dapi.util.ItemCreator;

import java.util.Set;
import java.util.function.Consumer;

public class AnvilManager extends AnvilMenu {

    private final @NotNull Player player;
    private final @NotNull String title;
    private final @NotNull Consumer<String> onFinish;
    public AnvilManager(@NotNull Player player, @NotNull String title, @NotNull Consumer<String> onFinish) {
        this.player = player;
        this.title = title;
        this.onFinish = onFinish;
        open();
    }

    private void open() {
        createInventory(player, title);

        addButton(new Button() {
            @Override
            protected @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(0);
            }

            @Override
            public @Nullable ItemStack getItem() {
                return ItemCreator.of(Material.PAPER).get();
            }
        });

        addButton(new Button() {
            @Override
            protected @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(2);
            }

            @Override
            public @Nullable ItemStack getItem() {
                return ItemCreator.of(Material.NAME_TAG)
                        .get();
            }

            @Override
            public void onClick(@NotNull Player p, @NotNull ClickType clickType) {
                String text = getText();
                onFinish.accept(text == null ? "" : text);
            }
        });

        displayTo(player);
    }
}
