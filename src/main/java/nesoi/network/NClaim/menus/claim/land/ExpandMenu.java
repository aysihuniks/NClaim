package nesoi.network.NClaim.menus.claim.land;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.menus.claim.admin.inside.ManageClaimMenu;
import nesoi.network.NClaim.menus.claim.inside.ClaimMenu;
import nesoi.network.NClaim.model.Claim;
import nesoi.network.NClaim.utils.ChunkBorderManager;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.nandayo.DAPI.guimanager.Button;
import org.nandayo.DAPI.guimanager.LazyButton;
import org.nandayo.DAPI.guimanager.Menu;
import org.nandayo.DAPI.ItemCreator;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ExpandMenu extends Menu {

    private final @NotNull Claim claim;
    List<Integer> frameSlots = Arrays.asList(0, 8, 9, 17, 18, 26, 27, 35, 36, 44);
    Collection<Chunk> allClaimChunks;
    private boolean admin = false;

    public ExpandMenu(Player p, @NotNull Claim claim, Boolean admin) {
        this.claim = claim;
        this.createInventory(9 * 5, "NClaim - Expand Claim");
        this.allClaimChunks = claim.getAllChunks();
        this.admin = admin;
        setupMenu(p);
        displayTo(p);
    }

    private void setupMenu(Player p) {
        this.addLazyButton(new LazyButton(Arrays.asList(0, 8, 9, 17, 18, 26, 27, 35, 36, 44)) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").get();
            }
        });

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
                if (!admin) {
                    new ClaimMenu(player, claim);
                } else {
                    new ManageClaimMenu(player, claim);
                }
            }
        });

        for (int i = 0; i < 45; i++) {
            if (frameSlots.contains(i) || i == 22)
                continue;
            addDirtButton(i);
        }
    }

    private void addDirtButton(int slot) {
        Chunk thatChunk = calculateNewChunk(slot);
        Claim thatClaim = Claim.getClaim(thatChunk);

        if (admin) {
            if (thatClaim == null) {
                addButton(new Button(slot) {
                    @Override
                    public ItemStack getItem() {
                        return ItemCreator.of(Material.BROWN_WOOL)
                                .name("{BROWN}Expand Claim")
                                .lore("", "{GRAY}Click to add this chunk to your claim.")
                                .get();
                    }

                    @Override
                    public void onClick(Player p, ClickType clickType) {
                        if (clickType.isLeftClick()) {
                            claim.buyLand(p, thatChunk);
                            new ExpandMenu(p, claim, admin);
                        } else {
                            p.closeInventory();
                            ChunkBorderManager chunkBorderManager = NCoreMain.inst().chunkBorderManager;
                            chunkBorderManager.showChunkBorder(p, thatChunk);
                        }
                    }
                });
            } else {
                addButton(new Button(slot) {
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
        } else {
            if (!isAdjacentToClaim(thatChunk)) {
                addButton(new Button(slot) {
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
            } else if (thatClaim == null) {
                addButton(new Button(slot) {
                    @Override
                    public ItemStack getItem() {
                        return ItemCreator.of(Material.BROWN_WOOL)
                                .name("{BROWN}Expand Claim")
                                .lore("", "{GRAY}Click to add this chunk to your claim.")
                                .get();
                    }

                    @Override
                    public void onClick(Player p, ClickType clickType) {
                        if (clickType.isLeftClick()) {
                            claim.buyLand(p, thatChunk);
                            new ExpandMenu(p, claim, admin);
                        } else {
                            p.closeInventory();
                            ChunkBorderManager chunkBorderManager = NCoreMain.inst().chunkBorderManager;
                            chunkBorderManager.showChunkBorder(p, thatChunk);
                        }
                    }
                });
            } else if (claim.getLands().contains(NCoreMain.serializeChunk(thatChunk))) {
                addButton(new Button(slot) {
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
            } else {
                addButton(new Button(slot) {
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
    }

    private boolean isAdjacentToClaim(@NotNull Chunk thatChunk) {
        return allClaimChunks.stream()
                .anyMatch(c -> c != null && NCoreMain.isChunkAdjacent(c, thatChunk, 2));
    }

    private Chunk calculateNewChunk(int slot) {
        int chunkX = claim.getChunk().getX();
        int chunkZ = claim.getChunk().getZ();

        int centerRow = 3;
        int centerCol = 5;

        int row = (slot + 1) / 9 + 1;
        int col = (slot + 1) % 9;
        if (col == 0) col = 9;

        int deltaX = col - centerCol;
        int deltaZ = row - centerRow;

        chunkX += deltaX;
        chunkZ += deltaZ;

        return claim.getChunk().getWorld().getChunkAt(chunkX, chunkZ);
    }
}