package nesoi.network.NClaim.menus.claim;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.menus.claim.inside.ClaimMenu;
import nesoi.network.NClaim.model.Claim;
import nesoi.network.NClaim.model.User;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.nandayo.DAPI.guimanager.Button;
import org.nandayo.DAPI.guimanager.LazyButton;
import org.nandayo.DAPI.guimanager.Menu;
import org.nandayo.DAPI.ItemCreator;
import org.nandayo.DAPI.object.DMaterial;

import java.util.*;

public class ListMenu extends Menu {

    private static final int[] playerSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30, 37, 38, 39};
    private static final int[] coopSlots = {14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43};
    private static final int PAGE_SIZE = playerSlots.length;

    private final Player player;
    private int page = 0;

    public ListMenu(Player p, int page) {
        this.player = p;
        this.page = page;
        createInventory(9 * 6, "NClaim - All Claims");
        setup();
        displayTo(p);
    }

    private void setup() {
        List<Integer> glassPaneSlots = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 13, 17, 18, 22, 26, 27, 31, 35, 36, 40, 44, 46, 47, 48, 49, 50, 51, 52));

        List<Claim> playerClaims = new ArrayList<>(User.getUser(player.getUniqueId()).getPlayerClaims());
        List<Claim> coopClaims = new ArrayList<>(User.getUser(player.getUniqueId()).getCoopClaims());

        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, playerClaims.size());

        for (int i = startIndex, slotIndex = 0; i < endIndex; i++, slotIndex++) {
            Claim claim = playerClaims.get(i);
            addClaimButton(claim, playerSlots[slotIndex], true);
        }

        startIndex = page * PAGE_SIZE;
        endIndex = Math.min(startIndex + PAGE_SIZE, coopClaims.size());

        for (int i = startIndex, slotIndex = 0; i < endIndex; i++, slotIndex++) {
            Claim claim = coopClaims.get(i);
            addClaimButton(claim, coopSlots[slotIndex], false);
        }

        addPaginationButtons(playerClaims.size(), coopClaims.size(), glassPaneSlots);

        addLazyButton(new LazyButton(glassPaneSlots) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").get();
            }
        });
    }

    private void addClaimButton(Claim claim, int slot, boolean isOwner) {
        Chunk chunk = claim.getChunk();
        ItemStack item = ItemCreator.of(isOwner ? Material.GRASS_BLOCK : Material.OAK_SIGN)
                .name(isOwner ? "{BROWN}Claim: " + claim.getClaimId() : "{BROWN}Coop Claim: " + claim.getClaimId())
                .lore(
                        "",
                        "{WHITE}World: {GRAY}" + chunk.getWorld().getName(),
                        "{WHITE}Coordinates: {GRAY}" + NCoreMain.getCoordinates(chunk),
                        isOwner ? "{WHITE}Owner: {GRAY}" + player.getName() : "{WHITE}Claim Owner: {GRAY}" + getOwnerName(claim),
                        "",
                        isOwner ? "{YELLOW}Click to view or manage this Claim." : "{YELLOW}You can only overview this Claim."
                )
                .get();

        addButton(new Button(slot) {
            @Override
            public ItemStack getItem() {
                return item;
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                if (isOwner) new ClaimMenu(p, claim);
            }
        });
    }

    private void addPaginationButtons(int totalPlayerClaims, int totalCoopClaims, List<Integer> glassPaneSlots) {
        boolean hasNextPage = (page + 1) * PAGE_SIZE < Math.max(totalPlayerClaims, totalCoopClaims);
        boolean hasPreviousPage = page > 0;

        if (hasPreviousPage) {
            addButton(new Button(45) {
                @Override
                public ItemStack getItem() {
                    return ItemCreator.of(Material.ARROW).name("{YELLOW}Previous Page").get();
                }

                @Override
                public void onClick(Player p, ClickType clickType) {
                    new ListMenu(p, page - 1);
                }
            });
            glassPaneSlots.remove(Integer.valueOf(45));
        } else {
            addButton(new Button(45) {
                @Override
                public ItemStack getItem() {
                    return ItemCreator.of(Material.BARRIER).name("{YELLOW}Go Back").get();
                }

                @Override
                public void onClick(Player p, ClickType clickType) {
                    new MainMenu(p);
                }
            });
        }

        if (hasNextPage) {
            addButton(new Button(53) {
                @Override
                public ItemStack getItem() {
                    return ItemCreator.of(Material.ARROW).name("{YELLOW}Next Page").get();
                }

                @Override
                public void onClick(Player p, ClickType clickType) {
                    new ListMenu(p, page + 1);
                }
            });
            glassPaneSlots.remove(Integer.valueOf(53));
        }
    }

    private String getOwnerName(Claim claim) {
        UUID ownerUUID = claim.getOwner();
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);
        return owner.getName() != null ? owner.getName() : "Unknown";
    }
}
