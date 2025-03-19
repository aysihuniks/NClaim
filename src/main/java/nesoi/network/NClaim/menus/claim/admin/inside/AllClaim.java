package nesoi.network.NClaim.menus.claim.admin.inside;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.integrations.AnvilManager;
import nesoi.network.NClaim.menus.claim.admin.AdminMenu;
import nesoi.network.NClaim.model.Claim;
import nesoi.network.NClaim.model.ClaimSetting;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.nandayo.DAPI.guimanager.Button;
import org.nandayo.DAPI.guimanager.LazyButton;
import org.nandayo.DAPI.guimanager.Menu;
import org.nandayo.DAPI.ItemCreator;
import org.nandayo.DAPI.object.DMaterial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AllClaim extends Menu {

    private OfflinePlayer target;
    private boolean sortByNewest = true;
    private int currentPage = 0;
    private static final int CLAIMS_PER_PAGE = 36;

    private final List<Claim> selectedClaims;

    public AllClaim(Player p, OfflinePlayer target, boolean sortByNewest, int page, List<Claim> selectedClaims) {
        createInventory(9 * 6, "NClaim - Manage All Claims");
        this.target = target;
        this.sortByNewest = sortByNewest;
        this.currentPage = page;
        this.selectedClaims = new ArrayList<>(selectedClaims);
        setup();
        displayTo(p);
    }

    private void setup() {
        addLazyButton(new LazyButton(Arrays.asList(36, 37, 38, 39, 40, 41, 42, 43, 44)) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").get();
            }
        });

        if (target != null || currentPage > 0 || !selectedClaims.isEmpty()) {
            addButton(new Button(51) {
                @Override
                public ItemStack getItem() {
                    return ItemCreator.of(Material.BOOK)
                            .name("{RED}Reset Settings").get();
                }

                @Override
                public void onClick(Player player, ClickType clickType) {
                    new AllClaim(player, null, true, 0, new ArrayList<>());
                }
            });
        }

        addButton(new Button(49) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.NETHER_STAR)
                        .name("{GREEN}Search for a Player")
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                new AnvilManager(NCoreMain.inst(), p, "Enter a player name",
                        ((text) -> {
                            if (text == null) {
                                p.closeInventory();
                                return;
                            }

                            Player onlinePlayer = Bukkit.getPlayerExact(text);
                            if (onlinePlayer != null) {
                                new AllClaim(p, onlinePlayer, sortByNewest, currentPage, selectedClaims);
                                return;
                            }

                            OfflinePlayer searchedPlayer = null;
                            for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                                if (offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(text)) {
                                    searchedPlayer = offlinePlayer;
                                    break;
                                }
                            }

                            new AllClaim(p, searchedPlayer, sortByNewest, currentPage, selectedClaims);
                        }));
            }
        });

        List<Claim> allClaims = Claim.getClaims().stream()
                .filter(claim -> target == null || claim.getOwner().equals(target.getUniqueId()))
                .sorted((c1, c2) -> sortByNewest
                        ? Long.compare(c2.getCreatedAt().getTime(), c1.getCreatedAt().getTime())
                        : Long.compare(c1.getCreatedAt().getTime(), c2.getCreatedAt().getTime())
                )
                .collect(Collectors.toList());

        int maxPage = (int) Math.ceil((double) allClaims.size() / CLAIMS_PER_PAGE);
        if (currentPage >= maxPage) currentPage = Math.max(0, maxPage - 1);

        List<Claim> claims = allClaims.stream()
                .skip((long) currentPage * CLAIMS_PER_PAGE)
                .limit(CLAIMS_PER_PAGE)
                .collect(Collectors.toList());

        int slot = 0;
        for (Claim claim : claims) {
            Chunk chunk = claim.getChunk();
            OfflinePlayer owner = Bukkit.getOfflinePlayer(claim.getOwner());

            ClaimSetting cs = claim.getSettings();
            int yes = (int) Arrays.stream(ClaimSetting.Setting.values())
                    .filter(cs::isEnabled)
                    .count();
            int no = ClaimSetting.Setting.values().length - yes;

            addButton(new Button(slot) {
                @Override
                public ItemStack getItem() {
                    if (selectedClaims.contains(claim)) {
                        return ItemCreator.of(NCoreMain.getMaterial(DMaterial.GLOW_ITEM_FRAME, DMaterial.LIME_DYE))
                                .name("{GREEN}" + owner.getName() + "'s Claim (Selected)")
                                .lore("",
                                        "{WHITE}Created At: {GRAY}" + NCoreMain.serializeDate(claim.getCreatedAt()),
                                        "{WHITE}Coordinates: {GRAY}" + chunk.getWorld().getName() + "," + NCoreMain.getCoordinates(chunk),
                                        "{WHITE}Land Size: {GRAY}" + claim.getLands().size(),
                                        "{WHITE}Coop Count: {GRAY}" + claim.getCoopPlayers().size(),
                                        "{WHITE}Setting Status: {GREEN}" + yes + "{GRAY}/{RED}" + no,
                                        "",
                                        "{YELLOW}Click to manage Claim",
                                        "{YELLOW}Shift click to teleport Claim",
                                        "{YELLOW}Right click to deselect Claim")
                                .get();
                    } else {
                        return ItemCreator.of(Material.ITEM_FRAME)
                                .name("{BROWN}" + owner.getName() + "'s Claim")
                                .lore("",
                                        "{WHITE}Created At: {GRAY}" + NCoreMain.serializeDate(claim.getCreatedAt()),
                                        "{WHITE}Coordinates: {GRAY}" + chunk.getWorld().getName() + "," + NCoreMain.getCoordinates(chunk),
                                        "{WHITE}Land Size: {GRAY}" + claim.getLands().size(),
                                        "{WHITE}Coop Count: {GRAY}" + claim.getCoopPlayers().size(),
                                        "{WHITE}Setting Status: {GREEN}" + yes + "{GRAY}/{RED}" + no,
                                        "",
                                        "{YELLOW}Click to manage Claim",
                                        "{YELLOW}Shift click to teleport Claim",
                                        "{YELLOW}Right click to select Claim")
                                .get();
                    }
                }

                @Override
                public void onClick(Player player, ClickType clickType) {
                    if (clickType == ClickType.RIGHT) {
                        if (selectedClaims.contains(claim)) {
                            selectedClaims.remove(claim);
                        } else {
                            selectedClaims.add(claim);
                        }
                        new AllClaim(player, target, sortByNewest, currentPage, selectedClaims);
                    } else if (clickType == ClickType.LEFT) {
                        new ManageClaimMenu(player, claim);
                    } else if (clickType == ClickType.SHIFT_LEFT) {
                        Location bedrockLoc = claim.getBedrockLocation();
                        World world = bedrockLoc.getWorld();
                        if (world == null) return;

                        Location safeLoc = new Location(world, bedrockLoc.getX() + 0.5, bedrockLoc.getY() + 1, bedrockLoc.getZ() + 0.5);

                        while (safeLoc.getY() < world.getMaxHeight() && !safeLoc.getBlock().getType().isSolid()) {
                            safeLoc.setY(safeLoc.getY() + 1);
                        }

                        if (!safeLoc.getBlock().getType().isSolid()) {
                            safeLoc.setY(world.getHighestBlockYAt(safeLoc) + 1);
                        }

                        if (safeLoc.getY() < bedrockLoc.getY()) {
                            safeLoc.setY(bedrockLoc.getY() + 1);
                        }

                        player.teleport(safeLoc);
                        player.closeInventory();
                    }
                }
            });
            slot++;
        }

        if (!selectedClaims.isEmpty()) {
            addButton(new Button(46) {
                @Override
                public ItemStack getItem() {
                    return ItemCreator.of(Material.BARRIER)
                            .name("{RED}Delete selected Claims")
                            .get();
                }

                @Override
                public void onClick(Player player, ClickType clickType) {
                    for (Claim selected : selectedClaims) {
                        selected.remove();
                    }
                    selectedClaims.clear();
                    new AllClaim(player, target, sortByNewest, currentPage, selectedClaims);
                }
            });
        }

        if (currentPage > 0) {
            addButton(new Button(45) {
                @Override
                public ItemStack getItem() {
                    return ItemCreator.of(Material.ARROW)
                            .name("{YELLOW}Previous Page")
                            .get();
                }

                @Override
                public void onClick(Player player, ClickType clickType) {
                    new AllClaim(player, target, sortByNewest, currentPage - 1, selectedClaims);
                }
            });
        } else{
            addButton(new Button(45) {
                @Override
                public ItemStack getItem() {
                    return ItemCreator.of(Material.ARROW)
                            .name("{YELLOW}Go Back")
                            .get();
                }

                @Override
                public void onClick(Player player, ClickType clickType) {
                    new AdminMenu(player);
                }
            });
        }

        addButton(new Button(47) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.GOLD_BLOCK)
                        .name("{GREEN}Sort by Date")
                        .lore("",
                                "{WHITE}- " + (sortByNewest ? "{YELLOW}Newest First" : "{GRAY}Newest First"),
                                "{WHITE}- " + (!sortByNewest ? "{YELLOW}Oldest First" : "{GRAY}Oldest First")
                        )
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                new AllClaim(p, target, sortByNewest = !sortByNewest, currentPage, selectedClaims);
            }
        });

        if (currentPage < maxPage - 1) {
            addButton(new Button(53) {
                @Override
                public ItemStack getItem() {
                    return ItemCreator.of(Material.ARROW)
                            .name("{YELLOW}Next Page")
                            .get();
                }

                @Override
                public void onClick(Player player, ClickType clickType) {
                    new AllClaim(player, target, sortByNewest, currentPage + 1, selectedClaims);
                }
            });
        }
    }
}
