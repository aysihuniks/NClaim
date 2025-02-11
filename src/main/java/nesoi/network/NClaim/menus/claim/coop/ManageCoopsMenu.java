package nesoi.network.NClaim.menus.claim.coop;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.menus.AnvilManager;
import nesoi.network.NClaim.menus.ConfirmMenu;
import nesoi.network.NClaim.menus.claim.ClaimMenu;
import nesoi.network.NClaim.models.ClaimDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.nandayo.DAPI.GUIManager.Button;
import org.nandayo.DAPI.GUIManager.Menu;
import org.nandayo.DAPI.ItemCreator;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static nesoi.network.NClaim.utils.HeadManager.getPlayerHead;

public class ManageCoopsMenu extends Menu {

    private final NCoreMain plugin;
    private final Player player;
    private final Chunk chunk;

    public ManageCoopsMenu(NCoreMain plugin, Player p, Chunk chunk) {
        this.createInventory(9 * 3, "NClaim - Co-op Members");
        this.player = p;
        this.chunk = chunk;
        this.plugin = plugin;

        setup();
        displayTo(p);
    }

    public void setup() {
        ClaimDataManager claimDataManager = NCoreMain.inst().claimDataManager;

        this.addButton(new Button(22) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.NETHER_STAR)
                        .name("{GREEN}Add Co-op Member")
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                new AnvilManager(plugin, p, "Enter a player name",
                        ((text) -> {

                            if (text == null || text.equalsIgnoreCase(player.getName())) {
                                p.closeInventory();
                            }

                            Consumer<String> onFinish = (result) -> {
                                if ("confirmed".equals(result)) {
                                    assert text != null;
                                    Player coopPlayer = Bukkit.getPlayerExact(text);
                                    claimDataManager.addCoopPlayer(p, coopPlayer, chunk);
                                    new ManageCoopsMenu(NCoreMain.inst(), p, chunk);
                                } else if ("declined".equals(result))  {
                                    new ManageCoopsMenu(NCoreMain.inst(), p, chunk);
                                }
                            };

                            new ConfirmMenu(p, "Add to Coop", Arrays.asList("", "{WHITE}If you {GRAY}approve {WHITE}this action,", "{WHITE}the entered {GRAY}player {WHITE}will be","{GRAY}authorized {WHITE}on the {GRAY}Claim{WHITE}."), onFinish);
                        }));
            }
        });



        this.addButton(new Button(18) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.ARROW)
                        .name("{ORANGE}Go Back")
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                new ClaimMenu(p, chunk);
            }
        });

        List<OfflinePlayer> coopPlayers = claimDataManager.getCoopPlayers(chunk);
        String claimKey = chunk.getX() + "_" + chunk.getZ();

        int slot = 0;
        for (OfflinePlayer coopPlayer : coopPlayers) {
            if (slot >= 27) break;


            this.addButton(new Button(slot++) {
                @Override
                public ItemStack getItem() {
                    ItemStack itemStack;
                    if (coopPlayer.isOnline()) {
                        itemStack = getPlayerHead(coopPlayer);
                        itemStack = ItemCreator.of(itemStack)
                                .name("{GREEN}" + coopPlayer.getName())
                                .lore("", "{WHITE}Joining date:", "{GRAY}" + claimDataManager.getCoopJoinedDate(claimKey, coopPlayer.getUniqueId()), "",  "{YELLOW}Click to edit")
                                .get();
                    } else {
                        itemStack = getPlayerHead(coopPlayer);
                        itemStack = ItemCreator.of(itemStack)
                                .name("{GRAY}" + coopPlayer.getName() + " (Offline)")
                                .lore("", "{WHITE}Joining date:", "{GRAY}" + claimDataManager.getCoopJoinedDate(claimKey, coopPlayer.getUniqueId()), "",  "{YELLOW}Click to edit")
                                .get();
                    }
                    return itemStack;
                }

                @Override
                public void onClick(Player p, ClickType clickType) {
                    new ManageCoopPlayerMenu(p, coopPlayer, chunk);
                }
            });
        }
    }
}
