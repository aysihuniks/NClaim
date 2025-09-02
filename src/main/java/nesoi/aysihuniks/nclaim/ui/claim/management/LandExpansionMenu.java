package nesoi.aysihuniks.nclaim.ui.claim.management;

import com.google.common.collect.Sets;
import lombok.Getter;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.ui.shared.BackgroundMenu;
import nesoi.aysihuniks.nclaim.ui.shared.BaseMenu;
import nesoi.aysihuniks.nclaim.ui.shared.ConfirmMenu;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.utils.MessageType;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nandayo.dapi.guimanager.button.Button;
import org.nandayo.dapi.guimanager.button.SingleSlotButton;
import org.nandayo.dapi.util.ItemCreator;
import org.nandayo.dapi.guimanager.MenuType;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LandExpansionMenu extends BaseMenu {

    private final @NotNull Claim claim;
    private final @NotNull Collection<Chunk> allClaimChunks;
    private final boolean admin;
    private final int baseSlot;

    private LandExpansionMenu(@NotNull Player player, @NotNull Claim claim, boolean admin, int baseSlot) {
        super("claim_expand_menu");
        this.claim = claim;
        this.allClaimChunks = claim.getAllChunks();
        this.admin = admin;
        this.baseSlot = baseSlot;
        setupMenu(player);
    }
    public LandExpansionMenu(@NotNull Player player, @NotNull Claim claim, boolean admin) {
        this(player, claim, admin, 22);
    }

    @Override
    public Function<Integer, @Nullable SingleSlotButton> backgroundButtonFunction() {
        return BackgroundMenu::getButton;
    }

    private void setupMenu(Player player) {
        createInventory(MenuType.CHEST_6_ROWS, getString("title"));

        this.addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(baseSlot);
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
                new ClaimManagementMenu(player, claim, admin);
            }
        });

        for(Scroller scroller : Scroller.SCROLLERS) {
            addButton(new Button() {
                @Override
                protected @NotNull Set<Integer> getSlots() {
                    return Sets.newHashSet(scroller.getSlot());
                }

                @Override
                public @Nullable ItemStack getItem() {
                    String directionKey = "scroll_button.directions." + scroller.getType().name();
                    String localizedDirection = NClaim.inst().getGuiLangManager().getString(directionKey);
                    return ItemCreator.of(Material.OAK_BUTTON)
                            .name(getString("scroll_button.display_name").replace("{direction}", localizedDirection))
                            .get();
                }

                @Override
                public void onClick(@NotNull Player p, @NotNull ClickType clickType) {
                    if(!canScroll(scroller)) return;
                    new LandExpansionMenu(p, claim, admin, baseSlot + scroller.getSlotAddition());
                }
            });
        }

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = row * 9 + col;
                if (slot == baseSlot || slot % 9 == 0 || slot % 9 == 8) continue;
                addChunkButton(slot, player);
            }
        }
        displayTo(player);
    }

    private void addChunkButton(int slot, Player player) {
        Chunk thatChunk = findChunkFromSlot(slot, player);
        Claim thatClaim = Claim.getClaim(thatChunk);

        String configPath;
        ItemStack material;
        boolean clickable = false;

        if (admin) {
            if (thatClaim == null) {
                configPath = "expand";
                material = getMaterial("expand");
                clickable = true;
            } else {
                if (thatClaim.equals(this.claim)) {
                    configPath = "claimed";
                    material = getMaterial("claimed");
                } else {
                    configPath = "claimed_another_player";
                    material = getMaterial("claimed_another_player");
                }
                clickable = true;
            }
        } else {
            if (!isAdjacentToClaim(thatChunk)) {
                configPath = "not_adjacent";
                material = getMaterial("not_adjacent");
            } else if (thatClaim == null) {
                configPath = "expand";
                material = getMaterial("expand");
                clickable = true;
            } else if (claim.getLands().contains(NClaim.serializeChunk(thatChunk))) {
                configPath = "claimed";
                material = getMaterial("claimed");
                clickable = true;
            } else {
                configPath = "claimed_another_player";
                material = getMaterial("claimed_another_player");
            }
        }

        addButton(createButton(slot, configPath, material, thatChunk, clickable));
    }

    private Button createButton(int slot, String configPath, ItemStack material, Chunk thatChunk, boolean clickable) {
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
                    String priceStr = admin ? NClaim.inst().getGuiLangManager().getString("free") : landPrice < 0 ? "N/A" : String.valueOf(landPrice);
                    lore.replaceAll(s -> s.replace("{price}", priceStr));
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
                            NClaim.inst().getClaimService().buyLand(claim, player, thatChunk, admin);
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
                } else if (clickType.isRightClick() && getInvItem(slot).getType() == Material.GREEN_WOOL || clickType.isRightClick() && getInvItem(slot).getType() == Material.BROWN_WOOL) {
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

    private Chunk findChunkFromSlot(int slot, Player player) {
        int chunkX = claim.getChunk().getX();
        int chunkZ = claim.getChunk().getZ();

        int centerRow = baseSlot / 9;
        int centerCol = baseSlot % 9;

        int row = slot / 9;
        int col = slot % 9;

        int deltaX = col - centerCol;
        int deltaZ = row - centerRow;

        float yaw = player.getLocation().getYaw();
        yaw = (yaw % 360 + 360) % 360;

        int rotatedX, rotatedZ;

        if (yaw >= 315 || yaw < 45) {
            rotatedX = deltaX;
            rotatedZ = -deltaZ;
        } else if (yaw >= 45 && yaw < 135) {
            rotatedX = -deltaZ;
            rotatedZ = -deltaX;
        } else if (yaw >= 135 && yaw < 225) {
            rotatedX = -deltaX;
            rotatedZ = deltaZ;
        } else {
            rotatedX = deltaZ;
            rotatedZ = deltaX;
        }

        chunkX += rotatedX;
        chunkZ += rotatedZ;

        return claim.getChunk().getWorld().getChunkAt(chunkX, chunkZ);
    }


    private boolean canScroll(Scroller scroller) {
        if(scroller == null) return false;
        switch (scroller.getType()) {
            case LEFT:
                return baseSlot % 9 < 7;
            case RIGHT:
                return baseSlot % 9 > 1;
            case UP:
                return baseSlot / 9 < 4;
            case DOWN:
                return baseSlot / 9 > 0;
        }
        return false;
    }


    @Getter
    private static class Scroller {

        public static final Scroller LEFT = new Scroller(Type.LEFT, 46, +1);
        public static final Scroller RIGHT = new Scroller(Type.RIGHT, 52, -1);
        public static final Scroller UP = new Scroller(Type.UP, 50, +9);
        public static final Scroller DOWN = new Scroller(Type.DOWN, 48, -9);

        public static final Scroller[] SCROLLERS = new Scroller[] {LEFT, RIGHT, UP, DOWN};


        private final Type type;
        private final int slot;
        private final int slotAddition;
        protected Scroller(Type type, int slot, int slotAddition) {
            this.type = type;
            this.slot = slot;
            this.slotAddition = slotAddition;
        }


        enum Type { LEFT, RIGHT, UP, DOWN}
    }
}