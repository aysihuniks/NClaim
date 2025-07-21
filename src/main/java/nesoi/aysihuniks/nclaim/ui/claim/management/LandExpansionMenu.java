package nesoi.aysihuniks.nclaim.ui.claim.management;

import com.google.common.collect.Sets;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.ui.shared.BackgroundMenu;
import nesoi.aysihuniks.nclaim.ui.shared.BaseMenu;
import nesoi.aysihuniks.nclaim.ui.shared.ConfirmMenu;
import nesoi.aysihuniks.nclaim.ui.claim.admin.AdminClaimManagementMenu;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.utils.LangManager;
import nesoi.aysihuniks.nclaim.utils.MessageType;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.guimanager.Button;
import org.nandayo.dapi.guimanager.Menu;
import org.nandayo.dapi.ItemCreator;
import org.nandayo.dapi.guimanager.MenuType;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LandExpansionMenu extends BaseMenu {

    private final @NotNull Claim claim;
    private final @NotNull Collection<Chunk> allClaimChunks;
    private final boolean admin;
    private final int height;
    private final int width;
    private final int rows;

    public LandExpansionMenu(@NotNull Player player, @NotNull Claim claim, boolean admin) {
        super("claim_expand_menu");
        this.claim = claim;
        this.allClaimChunks = claim.getAllChunks();
        this.admin = admin;
        this.height = Math.max(1, Math.min(NClaim.inst().getNconfig().getExpandMenuHeight(), 3));
        this.width = Math.max(1, Math.min(NClaim.inst().getNconfig().getExpandMenuWidth(), 3));
        this.rows = Math.min((height * 2 + 1), 6);
        setupMenu();
        displayTo(player);
    }

    private void setupMenu() {
        MenuType menuType;
        switch (rows) {
            case 1:
                menuType = MenuType.CHEST_1_ROW;
                break;
            case 2:
                menuType = MenuType.CHEST_2_ROWS;
                break;
            case 3:
                menuType = MenuType.CHEST_3_ROWS;
                break;
            case 4:
                menuType = MenuType.CHEST_4_ROWS;
                break;
            case 5:
                menuType = MenuType.CHEST_5_ROWS;
                break;
            case 6:
                menuType = MenuType.CHEST_6_ROWS;
                break;
            default:
                menuType = MenuType.CHEST_3_ROWS;
                break;
        }
        createInventory(menuType, getString("title"));
        setBackgroundButton(BackgroundMenu::getButton);

        this.addButton(new Button() {

            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet((rows * 9 - 1) / 2);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(claim.getClaimBlockType())
                        .name(getString("center.display_name"))
                        .lore(getStringList("center.lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_BACK.playSound(player);
                if (!admin) {
                    new ClaimManagementMenu(player, claim);
                } else {
                    new AdminClaimManagementMenu(player, claim);
                }
            }
        });

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = row * 9 + col;
                if (slot == (rows * 9 - 1) / 2) continue;
                if (isWithinRange(row, col)) {
                    addDirtButton(slot);
                }
            }
        }
    }

    private boolean isWithinRange(int row, int col) {
        int centerRow = rows / 2;
        int centerCol = 4;
        int deltaX = Math.abs(col - centerCol);
        int deltaZ = Math.abs(row - centerRow);
        return deltaX <= width && deltaZ <= height;
    }

    private void addDirtButton(int slot) {
        Chunk thatChunk = calculateNewChunk(slot);
        Claim thatClaim = Claim.getClaim(thatChunk);

        String configPath;
        Material material;
        boolean clickable = false;

        if (admin) {
            if (thatClaim == null) {
                configPath = "expand";
                material = Material.BROWN_WOOL;
            } else {
                configPath = "claimed";
                material = Material.LIME_WOOL;
            }
            clickable = true;
        } else {
            if (!isAdjacentToClaim(thatChunk)) {
                configPath = "not_adjacent";
                material = Material.BLACK_WOOL;
            } else if (thatClaim == null) {
                configPath = "expand";
                material = Material.BROWN_WOOL;
                clickable = true;
            } else if (claim.getLands().contains(NClaim.serializeChunk(thatChunk))) {
                configPath = "claimed";
                material = Material.LIME_WOOL;
                clickable = true;
            } else {
                configPath = "claimed_another_player";
                material = Material.RED_WOOL;
            }
        }

        addButton(createButton(slot, configPath, material, thatChunk, clickable));
    }

    private Button createButton(int slot, String configPath, Material material, Chunk thatChunk, boolean clickable) {
        return new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(slot);
            }

            @Override
            public ItemStack getItem() {
                double landPrice = calculateChunkPrice(thatChunk);
                String displayName = getString(configPath + ".display_name");
                List<String> lore = new ArrayList<>(getStringList(configPath + ".lore"));
                if (configPath.equals("expand")) {
                    lore.replaceAll(s -> s.replace("{price}", String.valueOf(landPrice)));
                }
                return ItemCreator.of(material)
                        .name(displayName)
                        .lore(lore)
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                if (!clickable) return;
                if (clickType.isLeftClick() && getInvItem(slot).getType() == Material.BROWN_WOOL) {
                    Consumer<String> onFinish = (result) -> {
                        if ("confirmed".equals(result)) {
                            NClaim.inst().getClaimService().buyLand(claim, player, thatChunk);
                            new LandExpansionMenu(player, claim, admin);
                        } else if ("declined".equals(result)) {
                            new LandExpansionMenu(player, claim, admin);
                        }
                    };

                    new ConfirmMenu(player,
                            NClaim.inst().getGuiLangManager().getString("confirm_menu.children.claim_expand.display_name"),
                            NClaim.inst().getGuiLangManager().getStringList("confirm_menu.children.claim_expand.lore")
                                    .stream()
                                    .map(s -> s.replace("{price}", String.valueOf(calculateChunkPrice(thatChunk))))
                                    .collect(Collectors.toList()),
                            onFinish);
                } else if (clickType.isRightClick() && getInvItem(slot).getType() == Material.LIME_WOOL || clickType.isRightClick() && getInvItem(slot).getType() == Material.BROWN_WOOL) {
                    player.closeInventory();
                    NClaim.inst().getClaimVisualizerService().showClaimBorders(player, thatChunk);
                }
            }
        };
    }

    private double calculateChunkPrice(Chunk targetChunk) {
        int currentChunkCount = 1 + claim.getLands().size();
        int nextChunkNumber = currentChunkCount + 1;

        return NClaim.inst().getNconfig().getTieredPrice(nextChunkNumber);
    }

    private boolean isAdjacentToClaim(@NotNull Chunk thatChunk) {
        return allClaimChunks.stream()
                .anyMatch(c -> c != null && NClaim.isChunkAdjacent(c, thatChunk, 2));
    }

    private Chunk calculateNewChunk(int slot) {
        int chunkX = claim.getChunk().getX();
        int chunkZ = claim.getChunk().getZ();

        int centerRow = rows / 2;
        int centerCol = 4;

        int row = slot / 9;
        int col = slot % 9;

        int deltaX = col - centerCol;
        int deltaZ = row - centerRow;

        chunkX += deltaX;
        chunkZ += deltaZ;

        return claim.getChunk().getWorld().getChunkAt(chunkX, chunkZ);
    }
}