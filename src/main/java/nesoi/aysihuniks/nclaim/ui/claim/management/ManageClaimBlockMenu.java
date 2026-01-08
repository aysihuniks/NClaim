package nesoi.aysihuniks.nclaim.ui.claim.management;

import com.google.common.collect.Sets;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.enums.Balance;
import nesoi.aysihuniks.nclaim.enums.Permission;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.model.User;
import nesoi.aysihuniks.nclaim.service.ClaimBlockManager;
import nesoi.aysihuniks.nclaim.ui.shared.BaseMenu;
import nesoi.aysihuniks.nclaim.ui.shared.ConfirmMenu;
import nesoi.aysihuniks.nclaim.utils.MessageType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nandayo.dapi.util.ItemCreator;
import org.nandayo.dapi.guimanager.button.Button;
import org.nandayo.dapi.guimanager.MenuType;
import org.nandayo.dapi.message.ChannelType;
import org.nandayo.dapi.object.DParticle;
import org.nandayo.dapi.object.DSound;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ManageClaimBlockMenu extends BaseMenu {

    private final @NotNull Claim claim;
    private final @NotNull Player player;
    private final ClaimBlockManager blockManager;
    private int page = 0;

    private static final int[] BLOCK_SLOTS = {28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private static final int NEXT_PAGE_SLOT = 16;
    private static final int PREV_PAGE_SLOT = 10;

    private List<ClaimBlockManager.ClaimBlockInfo> allowedBlocks() {
        List<ClaimBlockManager.ClaimBlockInfo> result = new ArrayList<>();
        for (ClaimBlockManager.ClaimBlockInfo info : blockManager.getAllBlockInfos()) {
            if (info.enabled) result.add(info);
        }
        return result;
    }

    public ManageClaimBlockMenu(@NotNull Claim claim, @NotNull Player player, int page) {
        super("claim_block_management_menu");
        this.claim = claim;
        this.player = player;
        this.blockManager = NClaim.inst().getClaimBlockManager();
        this.page = page;

        setup();
        displayTo(player);
    }

    private void setup() {
        createInventory(MenuType.CHEST_6_ROWS, getString("title"));

        addButton(new Button() {
            @Override
            protected @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(12);
            }

            @Override
            public @Nullable ItemStack getItem() {
                return ItemCreator.of(getMaterial("teleport"))
                        .name(getString("teleport.display_name"))
                        .lore(getStringList("teleport.lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player p, @NotNull ClickType clickType) {
                if(!claim.isSafeToTeleport()) {
                    ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("claim.teleport.failed"));
                    return;
                }

                claim.teleport(player);
                ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("claim.teleport.success"));
                player.closeInventory();
            }
        });

        addButton(new Button() {
            @Override
            protected @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(14);
            }

            @Override
            public @Nullable ItemStack getItem() {
                return ItemCreator.of(getMaterial("move_claim_block"))
                        .name(getString("move_claim_block.display_name"))
                        .lore(getStringList("move_claim_block.lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player p, @NotNull ClickType clickType) {
                if (!NClaim.inst().getClaimCoopManager().hasPermission(player, claim, Permission.MOVE_CLAIM_BLOCK)) {
                    ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.permission_denied"));
                    return;
                }

                Location playerLocation = player.getLocation().getBlock().getLocation();
                if(!claim.getChunk().equals(playerLocation.getChunk())) {
                    ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("claim.move.outside_of_chunk"));
                    return;
                }

                new ConfirmMenu(player,
                        NClaim.inst().getGuiLangManager().getString("confirm_menu.children.move_claim_block.display_name"),
                        NClaim.inst().getGuiLangManager().getStringList("confirm_menu.children.move_claim_block.lore"),
                        result -> {
                            if ("confirmed".equals(result)) {
                                player.closeInventory();
                                claim.moveClaimBlock(playerLocation);
                                ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("claim.move.success"));
                            } else if ("declined".equals(result)) {
                                new ManageClaimBlockMenu(claim, player, page);
                            }
                        });
            }
        });


        if(NClaim.inst().getClaimCoopManager().hasPermission(player, claim, Permission.MANAGE_CLAIM_BLOCK_TYPES)) {
            List<ClaimBlockManager.ClaimBlockInfo> blockInfos = allowedBlocks();
            int start = page * BLOCK_SLOTS.length;
            int end = Math.min(blockInfos.size(), start + BLOCK_SLOTS.length);

            for (int i = start, j = 0; i < end; i++, j++) {
                final ClaimBlockManager.ClaimBlockInfo info = blockInfos.get(i);
                final int slot = BLOCK_SLOTS[j];

                addButton(new Button() {
                    @Override
                    public @NotNull Set<Integer> getSlots() {
                        return Sets.newHashSet(slot);
                    }

                    @Override
                    public @Nullable ItemStack getItem() {
                        ItemCreator creator = ItemCreator.of(info.material)
                                .name("&e" + info.displayName);

                        if (info.lore != null && !info.lore.isEmpty()) {

                            double playerBalance;
                            if (NClaim.inst().getBalanceSystem() == Balance.VAULT) {
                                playerBalance = NClaim.inst().getEconomy().getBalance(player);
                            } else {
                                playerBalance = User.getUser(player.getUniqueId()).getBalance();
                            }

                            DecimalFormat df = new DecimalFormat("#.##");

                            List<String> updatedLore = info.lore.stream()
                                    .map(line -> {
                                        if (line.contains("{cost}") || line.contains("{need_balance}")) {
                                            String balanceColor = playerBalance >= info.price ? "&a" : "&c";

                                            String formattedPrice = df.format(info.price);
                                            String formattedNeededBalance = df.format(Math.min(playerBalance, info.price));

                                            return line.replace("{cost}", formattedPrice)
                                                    .replace("{need_balance}", balanceColor + formattedNeededBalance);
                                        }
                                        return line;
                                    })
                                    .collect(Collectors.toList());
                            creator.lore(updatedLore);
                        }

                        if (claim.getClaimBlockType() == info.material) {
                            creator.enchant(Enchantment.MENDING, 1);
                            creator.flags(ItemFlag.values());
                        }
                        return creator.get();
                    }


                    @Override
                    public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                        if (info.permission != null && !info.permission.isEmpty() && !player.hasPermission(info.permission)) {
                            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.permission_denied"));
                            return;
                        }

                        if (!claim.getPurchasedBlockTypes().contains(info.material)) {
                            double balance;
                            boolean useVault = NClaim.inst().getBalanceSystem() == Balance.VAULT;

                            if (useVault) {
                                balance = NClaim.inst().getEconomy().getBalance(player);
                            } else {
                                balance = User.getUser(player.getUniqueId()).getBalance();
                            }

                            if (balance < info.price) {
                                ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.balance.not_enough"));
                                return;
                            }

                            if (useVault) {
                                NClaim.inst().getEconomy().withdrawPlayer(player, info.price);
                            } else {
                                User.getUser(player.getUniqueId()).setBalance(balance - info.price);
                            }

                            claim.getPurchasedBlockTypes().add(info.material);
                            NClaim.inst().getClaimStorageManager().saveClaim(claim);
                            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("claim.block.purchased")
                                    .replace("{block}", info.displayName)
                                    .replace("{price}", String.valueOf(info.price)));
                        }

                        if (claim.getClaimBlockType() == info.material) {
                            return;
                        }

                        Material oldBlockType = claim.getClaimBlockType();
                        ClaimBlockManager.ClaimBlockInfo oldBlockInfo = NClaim.inst().getClaimBlockManager().getBlockInfo(oldBlockType);
                        String oldBlockDisplayName = oldBlockInfo != null ? oldBlockInfo.displayName : oldBlockType.name();

                        player.playSound(player.getLocation(), DSound.BLOCK_BEACON_POWER_SELECT.parseSound(), 0.5f, 0.7f);

                        claim.setClaimBlockType(info.material);
                        claim.getClaimBlockLocation().getBlock().setType(info.material);
                        NClaim.inst().getClaimStorageManager().saveClaim(claim);

                        ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("claim.block.changed")
                                .replace("{old_block}", oldBlockDisplayName)
                                .replace("{new_block}", info.displayName));

                        player.closeInventory();

                        claim.getClaimBlockLocation().getWorld().spawnParticle(NClaim.inst().getParticle(DParticle.TOTEM_OF_UNDYING, DParticle.TOTEM), claim.getClaimBlockLocation(), 1);
                    }

                });
            }

            if (blockInfos.size() > (page + 1) * BLOCK_SLOTS.length) {
                addButton(new Button() {
                    @Override
                    public @NotNull Set<Integer> getSlots() {
                        return Sets.newHashSet(NEXT_PAGE_SLOT);
                    }

                    @Override
                    public @Nullable ItemStack getItem() {
                        return ItemCreator.of(getMaterialFullPath("next_page"))
                                .name(NClaim.inst().getGuiLangManager().getString("next_page.display_name"))
                                .get();
                    }

                    @Override
                    public void onClick(@NotNull Player clicker, @NotNull ClickType clickType) {
                        if(!NClaim.inst().getClaimCoopManager().hasPermission(player, claim, Permission.MANAGE_CLAIM_BLOCK_TYPES)) {
                            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.permission_denied"));
                            return;
                        }

                        MessageType.MENU_FORWARD.playSound(player);
                        new ManageClaimBlockMenu(claim, clicker, page + 1);
                    }
                });
            }
        }

        addButton(new Button() {
            final String buttonPath = page == 0 ? "back" : "previous_page";

            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(PREV_PAGE_SLOT);
            }

            @Override
            public @Nullable ItemStack getItem() {
                return ItemCreator.of(getMaterialFullPath(buttonPath))
                        .name(NClaim.inst().getGuiLangManager().getString(buttonPath + ".display_name"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player clicker, @NotNull ClickType clickType) {
                MessageType.MENU_BACK.playSound(player);
                if (page == 0) {
                    new ClaimManagementMenu(player, claim, false);
                } else {
                    new ManageClaimBlockMenu(claim, clicker, page - 1);
                }

            }
        });
    }
}