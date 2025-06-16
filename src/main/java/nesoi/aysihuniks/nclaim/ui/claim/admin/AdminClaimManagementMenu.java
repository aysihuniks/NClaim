package nesoi.aysihuniks.nclaim.ui.claim.admin;

import com.google.common.collect.Sets;
import nesoi.aysihuniks.nclaim.enums.RemoveCause;
import nesoi.aysihuniks.nclaim.ui.shared.BackgroundMenu;
import nesoi.aysihuniks.nclaim.ui.shared.BaseMenu;
import nesoi.aysihuniks.nclaim.ui.shared.ConfirmMenu;
import nesoi.aysihuniks.nclaim.ui.claim.coop.CoopListMenu;
import nesoi.aysihuniks.nclaim.ui.claim.management.LandExpansionMenu;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.utils.MessageType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.guimanager.Button;
import org.nandayo.dapi.ItemCreator;
import org.nandayo.dapi.guimanager.MenuType;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Consumer;

public class AdminClaimManagementMenu extends BaseMenu {
    private final @NotNull Claim claim;

    public AdminClaimManagementMenu(@NotNull Player player, @NotNull Claim claim) {
        super("menu.admin.manage_claim_menu");
        this.claim = claim;

        setupMenu();
        displayTo(player);
    }

    private void setupMenu() {
        String ownerName = Bukkit.getOfflinePlayer(claim.getOwner()).getName() != null
                ? Bukkit.getOfflinePlayer(claim.getOwner()).getName() : "Unknown";
        createInventory(MenuType.CHEST_3_ROWS, getString("title").replace("{owner}", ownerName));
        setBackgroundButton(BackgroundMenu::getButton);

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(10);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.OAK_DOOR)
                        .name(langManager.getString("menu.back.display_name"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_BACK.playSound(player);
                new AdminAllClaimMenu(player, null, true, 0, new ArrayList<>());
            }
        });

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(12);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.BEEHIVE)
                        .name(getString("manage_coop.display_name"))
                        .lore(getStringList("manage_coop.lore"))
                        .hideFlag(ItemFlag.values())
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_FORWARD.playSound(player);
                new CoopListMenu(player, claim, true);
            }
        });

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(13);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.BRICKS)
                        .name(getString("manage_lands.display_name"))
                        .lore(getStringList("manage_lands.lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_FORWARD.playSound(player);
                new LandExpansionMenu(player, claim, true);
            }
        });

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(14);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.CLOCK)
                        .name(getString("manage_expiration.display_name"))
                        .lore(getStringList("manage_expiration.lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_FORWARD.playSound(player);
                new AdminTimeManagementMenu(player, 0, 0, 0, 0, claim);
            }
        });

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(15);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.TNT)
                        .name(getString("delete_claim.display_name"))
                        .lore(getStringList("delete_claim.lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                Consumer<String> onFinish = (result) -> {
                    if ("confirmed".equals(result)) {
                        claim.remove(RemoveCause.REMOVED_BY_ADMIN);
                        new AdminAllClaimMenu(player, null, true, 0, new ArrayList<>());
                    } else if ("declined".equals(result)) {
                        new AdminClaimManagementMenu(player, claim);
                    }
                };

                new ConfirmMenu(player, langManager.getString("menu.confirm_menu.delete_claim.display_name"),
                        langManager.getStringList("menu.confirm_menu.delete_claim.lore"), onFinish);
            }
        });
    }
}