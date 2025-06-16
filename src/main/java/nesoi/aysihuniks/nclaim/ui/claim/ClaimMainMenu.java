package nesoi.aysihuniks.nclaim.ui.claim;

import com.google.common.collect.Sets;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.ui.shared.BackgroundMenu;
import nesoi.aysihuniks.nclaim.ui.shared.BaseMenu;
import nesoi.aysihuniks.nclaim.ui.shared.ConfirmMenu;
import nesoi.aysihuniks.nclaim.ui.claim.admin.AdminDashboardMenu;
import nesoi.aysihuniks.nclaim.model.User;
import nesoi.aysihuniks.nclaim.utils.HeadManager;
import nesoi.aysihuniks.nclaim.utils.MessageType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.ItemCreator;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.nandayo.dapi.guimanager.Button;
import org.nandayo.dapi.guimanager.MenuType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ClaimMainMenu extends BaseMenu {

    public ClaimMainMenu(Player player) {
        super("menu.main_menu");

        setupMenu(player);
        displayTo(player);
    }

    private void setupMenu(Player player) {
        createInventory(MenuType.CHEST_3_ROWS, getString("title"));
        setBackgroundButton(BackgroundMenu::getButton);
        addBuyClaimButton();
        addManageClaimsButton();
        
        if (player.hasPermission("nclaim.admin")) {
            addAdminButton();
        }
    }

    private void addBuyClaimButton() {
        addButton(new Button() {
            final String buttonPath = "buy_claim";

            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(11);
            }

            @Override
            public ItemStack getItem() {

                List<String> lore = getStringList(buttonPath + ".lore");
                lore.replaceAll(l -> l.replace("{price}", String.valueOf(NClaim.inst().getNconfig().getClaimBuyPrice())));
                
                return ItemCreator.of(Material.EMERALD)
                        .name(getString(buttonPath + ".display_name"))
                        .lore(lore)
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                if (clickType == ClickType.LEFT) {
                    handleBuyClaimClick(player);
                } else if(clickType == ClickType.RIGHT) {
                    NClaim.inst().getClaimVisualizerService().showClaimBorders(player);
                }

            }
        });
    }

    private void handleBuyClaimClick(Player player) {
        Consumer<String> onFinish = (result) -> {
            if ("confirmed".equals(result)) {
                player.closeInventory();
                NClaim.inst().getClaimService().buyNewClaim(player);
            } else if ("declined".equals(result)) {
                new ClaimMainMenu(player);
            }
        };

        new ConfirmMenu(player,
                langManager.getString("menu.confirm_menu.buy_claim.display_name"),
                langManager.getStringList("menu.confirm_menu.buy_claim.lore")
                        .stream()
                        .map(s -> s.replace("{price}", String.valueOf(NClaim.inst().getNconfig().getClaimBuyPrice())))
                        .collect(Collectors.toList()),
                onFinish);
    }

    private void addManageClaimsButton() {
        addButton(new Button() {
            final String buttonPath = "manage_claims";

            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(15);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.CHEST)
                        .name(getString(buttonPath + ".display_name"))
                        .lore(getStringList(buttonPath + ".lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                handleManageClaimsClick(player);
            }
        });
    }

    private void handleManageClaimsClick(Player player) {
        User user = User.getUser(player.getUniqueId());
        if (!user.getPlayerClaims().isEmpty() || !user.getCoopClaims().isEmpty()) {
            new ClaimListMenu(player, 0);
        } else {
            player.closeInventory();
            player.sendMessage(langManager.getString("claim.not_found"));
            MessageType.WARN.playSound(player);
        }
    }

    private void addAdminButton() {
        addButton(new Button() {
            final String buttonPath = "admin";

            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(13);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.COMMAND_BLOCK)
                        .name(getString(buttonPath + ".display_name"))
                        .lore(getStringList(buttonPath + ".lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                new AdminDashboardMenu(player);
            }
        });
    }
}