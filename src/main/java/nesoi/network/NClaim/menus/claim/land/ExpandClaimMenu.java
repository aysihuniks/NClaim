package nesoi.network.NClaim.menus.claim.land;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.menus.claim.ClaimMenu;
import nesoi.network.NClaim.models.ClaimDataManager;
import nesoi.network.NClaim.models.PlayerDataManager;
import nesoi.network.NClaim.utils.ChunkBorderManager;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.nandayo.DAPI.GUIManager.Button;
import org.nandayo.DAPI.GUIManager.LazyButton;
import org.nandayo.DAPI.GUIManager.Menu;
import org.nandayo.DAPI.ItemCreator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExpandClaimMenu extends Menu {

    private final Chunk centerChunk;
    private final Set<String> claimedChunks = new HashSet<>();
    List<Integer> frameSlots = Arrays.asList(0, 8, 9, 17, 18, 26, 27, 35, 36, 44);

    public ExpandClaimMenu(Player p, Chunk centerChunk) {
        this.centerChunk = centerChunk;
        this.addLazyButton(new LazyButton(Set.of(0, 8, 9, 17, 18, 26, 27, 35, 36, 44)) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.TINTED_GLASS).name(" ").get();
            }
        });
        this.createInventory(9 * 5, "Expand Your Claim");

        loadClaimedChunks();
        setupMenu(p);
        displayTo(p);
    }

    private void loadClaimedChunks() {
        ClaimDataManager claimDataManager = NCoreMain.inst().claimDataManager;
        String centerChunkKey = centerChunk.getX() + "_" + centerChunk.getZ();
        claimedChunks.add(centerChunkKey);

        List<String> lands = claimDataManager.getLands(centerChunk);
        claimedChunks.addAll(lands);
    }


    private void setupMenu(Player p) {


        this.addButton(new Button(22) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.BEDROCK)
                        .name("{BROWN}Center Chunk")
                        .lore(
                                "",
                                "{WHITE}This is your current claim center chunk."
                        )
                        .get();
            }

            @Override
            public void onClick(Player player, ClickType clickType) {
                new ClaimMenu(player, centerChunk);
            }
        });

        for(int i = 0; i < 45; i++) {
            if (frameSlots.contains(i) || i == 22 )
                continue;
            addDirtButton(i, p);

        }
    }

    private void addDirtButton(int slot, Player p) {
        String playerUUID = p.getUniqueId().toString();
        Chunk newChunk = calculateNewChunk(slot);
        ClaimDataManager claimDataManager = NCoreMain.inst().claimDataManager;
        String owner = claimDataManager.getClaimOwner(newChunk);
        String centerChunkKey = claimDataManager.getCenterChunk(newChunk);

        //Invalid
        if (!isAdjacentToClaim(newChunk) && ExpandClaimMenu.this.getInvItem(slot) == null) {
            this.addButton(new Button(slot) {
                @Override
                public ItemStack getItem() {
                    return ItemCreator.of(Material.BARRIER)
                            .name("{RED}Invalid Chunk")
                            .lore("", "{GRAY}You can't claim chunks that are not adjacent.")
                            .get();
                }

                @Override
                public void onClick(Player p, ClickType clickType) {
                }
            });
        }

        //unowned
        else if (owner == null) {
            this.addButton(new Button(slot) {
                @Override
                public ItemStack getItem() {
                    return ItemCreator.of(Material.BROWN_WOOL)
                            .name("{BROWN}Expand Claim")
                            .lore("", "{GRAY}Click to add this chunk to your claim.")
                            .get();
                }

                @Override
                public void onClick(Player p, ClickType clickType) {
                    if (NCoreMain.isInCooldown(p, 3000)) {
                        if (NCoreMain.shouldSendMessage(p, 5000)) {
                            p.sendMessage(NCoreMain.inst().langManager.getMsg("messages.cooldown"));
                        }
                        return;
                    }

                    if (clickType.isLeftClick()) {
                        if (!claimDataManager.isChunkClaimedByAnotherPlayer(newChunk, playerUUID)) {
                            expandClaim(p, slot);
                            new ExpandClaimMenu(p, centerChunk);
                        } else {
                            p.sendMessage(NCoreMain.inst().langManager.getMsg("messages.chunk-already-claimed"));
                        }
                    } else if (clickType.isRightClick()) {
                        p.closeInventory();
                        ChunkBorderManager chunkBorderManager = new ChunkBorderManager();
                        chunkBorderManager.showChunkBorder(p, newChunk);
                    }
                }
            });
        }

        //owned
        else if (centerChunkKey != null && centerChunkKey.equals(centerChunk.getX() + "_" + centerChunk.getZ()) && owner.equals(p.getUniqueId().toString())) {
            this.addButton(new Button(slot) {
                @Override
                public ItemStack getItem() {
                    return ItemCreator.of(Material.LIME_WOOL)
                            .name("{GREEN}Claimed Chunk")
                            .lore("", "{GRAY}This chunk is already claimed.")
                            .get();
                }

                @Override
                public void onClick(Player p, ClickType clickType) {

                }
            });
        }

        //owned by other
        else {
            this.addButton(new Button(slot) {
                @Override
                public ItemStack getItem() {
                    return ItemCreator.of(Material.RED_WOOL)
                            .name("{RED}Claimed by Another Player")
                            .lore("", "{GRAY}This chunk is already claimed by another player.")
                            .get();
                }

                @Override
                public void onClick(Player p, ClickType clickType) {

                }
            });
        }
    }

    private void expandClaim(Player p, int slot) {
        Chunk newChunk = calculateNewChunk(slot);
        ClaimDataManager claimDataManager = NCoreMain.inst().claimDataManager;
        PlayerDataManager playerDataManager = NCoreMain.pdCache.get(p);
        claimDataManager.addLandToClaim(centerChunk, newChunk, playerDataManager);

        String newChunkKey = newChunk.getX() + "_" + newChunk.getZ();
        claimedChunks.add(newChunkKey);

        addDirtButton(slot, p);
        displayTo(p);
    }

    private boolean isAdjacentToClaim(Chunk newChunk) {
        int x = newChunk.getX();
        int z = newChunk.getZ();

        for (String claimed : claimedChunks) {
            String[] parts = claimed.split("_");
            int claimedX = Integer.parseInt(parts[0]);
            int claimedZ = Integer.parseInt(parts[1]);

            if (Math.abs(claimedX - x) <= 1 && Math.abs(claimedZ - z) <= 1) {
                return true;
            }
        }
        return false;
    }


    private Chunk calculateNewChunk(int slot) {
        int chunkX = centerChunk.getX();
        int chunkZ = centerChunk.getZ();

        int centerRow = 3;
        int centerCol = 5;

        int row = (slot + 1) / 9 + 1;
        int col = (slot + 1) % 9;
        if (col == 0) col = 9;

        int deltaX = col - centerCol;
        int deltaZ = row - centerRow;

        chunkX += deltaX;
        chunkZ += deltaZ;

        return centerChunk.getWorld().getChunkAt(chunkX, chunkZ);
    }

}
