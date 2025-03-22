package nesoi.network.NClaim.menus.claim.coop;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.integrations.AnvilManager;
import nesoi.network.NClaim.menus.ConfirmMenu;
import nesoi.network.NClaim.menus.claim.admin.inside.ManageClaimMenu;
import nesoi.network.NClaim.menus.claim.inside.ClaimMenu;
import nesoi.network.NClaim.model.Claim;
import nesoi.network.NClaim.model.CoopPermission;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.nandayo.DAPI.guimanager.Button;
import org.nandayo.DAPI.guimanager.Menu;
import org.nandayo.DAPI.ItemCreator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static nesoi.network.NClaim.utils.HeadManager.getPlayerHead;

public class ManageMenu extends Menu {

    private final @NotNull Claim claim;

    private final boolean admin;

    public ManageMenu(Player p, @NotNull Claim claim, Boolean admin) {
        createInventory(9 * 3, "NClaim - Co-op Members");
        this.claim = claim;
        this.admin = admin;
        setup();
        displayTo(p);
    }

    public void setup() {
        addButton(new Button(22) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.NETHER_STAR)
                        .name("{GREEN}Add Co-op Member")
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                new AnvilManager(NCoreMain.inst(), p, "Enter a player name",
                        ((text) -> {
                            Consumer<String> onFinish = (result) -> {
                                if ("confirmed".equals(result)) {
                                    if (text == null || text.equalsIgnoreCase(p.getName())) {
                                        p.sendMessage(NCoreMain.inst().langManager.getMsg("messages.error.not-found"));
                                        p.closeInventory();
                                        return;
                                    }

                                    Player player = Bukkit.getPlayerExact(text);

                                    if (player == null) {
                                        p.sendMessage(NCoreMain.inst().langManager.getMsg("messages.error.no-player"));
                                        p.closeInventory();
                                        return;
                                    }

                                    p.closeInventory();
                                    claim.addCoop(player);
                                } else if ("declined".equals(result))  {
                                    new ManageMenu(p, claim, admin);
                                }
                            };

                            new ConfirmMenu(p, "Add to Coop", Arrays.asList("", "{WHITE}If you {GRAY}approve {WHITE}this action,", "{WHITE}the entered {GRAY}player {WHITE}will be","{GRAY}authorized {WHITE}on the {GRAY}Claim{WHITE}."), onFinish);
                        }));
            }
        });

        addButton(new Button(18) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.ARROW)
                        .name("{YELLOW}Go Back")
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                if (!admin) {
                    new ClaimMenu(p, claim);
                } else {
                    new ManageClaimMenu(p, claim);
                }

            }
        });

        int slot = 0;
        for (UUID coopPlayerUUID : claim.getCoopPlayers()) {
            if (slot >= 8) break;

            OfflinePlayer coopPlayer = Bukkit.getOfflinePlayer(coopPlayerUUID);
            String playerName = coopPlayer.getName() != null ? coopPlayer.getName() : "Unknown";

            CoopPermission cp = claim.getCoopPermissions().get(coopPlayer.getUniqueId());
            int yes = (int) Arrays.stream(CoopPermission.Permission.values())
                    .filter(cp::isEnabled)
                    .count();
            int no = CoopPermission.Permission.values().length - yes;

            addButton(new Button(slot++) {
                @Override
                public ItemStack getItem() {
                    ItemStack itemStack = getPlayerHead(coopPlayer);

                    List<String> loreList = new ArrayList<>();
                    loreList.add("");
                    loreList.add("{WHITE}Joining date:");
                    loreList.add("{GRAY}" + NCoreMain.serializeDate(claim.getCoopPlayerJoinDate().get(coopPlayerUUID)));
                    loreList.add("");

                    if (admin) {
                        loreList.add("{WHITE}Permission Status: {GREEN}" + yes + "{GRAY}/{RED}" + no);
                        loreList.add("");
                    }

                    loreList.add("{YELLOW}Click to edit");

                    itemStack = ItemCreator.of(itemStack).name(
                                    coopPlayer.isOnline() ? "{GREEN}" + playerName : "{GRAY}" + playerName + " (Offline)")
                            .lore(loreList)
                            .get();

                    return itemStack;
                }

                @Override
                public void onClick(Player p, ClickType clickType) {
                    new PermissionMenu(p, coopPlayer, claim, admin);
                }
            });
        }
    }
}
