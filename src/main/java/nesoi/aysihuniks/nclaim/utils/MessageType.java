package nesoi.aysihuniks.nclaim.utils;

import nesoi.aysihuniks.nclaim.NClaim;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nandayo.dapi.object.DSound;

import static nesoi.aysihuniks.nclaim.NClaim.getSound;

public abstract class MessageType {
    public static final MessageType CONFIRM = new MessageType() {
        @Override
        public void playSound(Player player) {
            NClaim.inst().getWrapper().playSound(player, getSound(DSound.BLOCK_NOTE_BLOCK_CHIME, DSound.BLOCK_NOTE_BLOCK_PLING), 1f, 1f);
            Bukkit.getScheduler().runTaskLater(NClaim.inst(), task -> {
                NClaim.inst().getWrapper().playSound(player, getSound(DSound.BLOCK_NOTE_BLOCK_CHIME, DSound.BLOCK_NOTE_BLOCK_PLING), 1f, 2f);
            }, 2L);
        }
    };

    public static final MessageType FAIL = new MessageType() {
        @Override
        public void playSound(Player player) {
            NClaim.inst().getWrapper().playSound(player, getSound(DSound.ENTITY_ENDERMAN_TELEPORT, DSound.ENTITY_GHAST_HURT), 0.5f, 0.5f);
            Bukkit.getScheduler().runTaskLater(NClaim.inst(), task -> {
                NClaim.inst().getWrapper().playSound(player, getSound(DSound.BLOCK_ANVIL_LAND, DSound.BLOCK_ANVIL_PLACE), 0.3f, 0.5f);
            }, 2L);
        }
    };

    public static final MessageType WARN = new MessageType() {
        @Override
        public void playSound(Player player) {
            NClaim.inst().getWrapper().playSound(player, getSound(DSound.BLOCK_NOTE_BLOCK_BASS, DSound.BLOCK_NOTE_BLOCK_BASEDRUM), 1f, 0.5f);
            Bukkit.getScheduler().runTaskLater(NClaim.inst(), task -> {
                NClaim.inst().getWrapper().playSound(player, getSound(DSound.BLOCK_NOTE_BLOCK_BASS, DSound.BLOCK_NOTE_BLOCK_BASEDRUM), 1f, 0.6f);
            }, 2L);
        }
    };

    public static final MessageType VALUE_INCREASE = new MessageType() {
        @Override
        public void playSound(Player player) {
            NClaim.inst().getWrapper().playSound(player, getSound(DSound.UI_BUTTON_CLICK, DSound.BLOCK_STONE_BUTTON_CLICK_ON), 0.4f, 1.8f);
            NClaim.inst().getWrapper().playSound(player, getSound(DSound.BLOCK_NOTE_BLOCK_PLING, DSound.BLOCK_NOTE_BLOCK_BELL), 0.3f, 1.6f);
        }
    };

    public static final MessageType VALUE_DECREASE = new MessageType() {
        @Override
        public void playSound(Player player) {
            NClaim.inst().getWrapper().playSound(player, getSound(DSound.UI_BUTTON_CLICK, DSound.BLOCK_STONE_BUTTON_CLICK_ON), 0.4f, 1.4f);
            NClaim.inst().getWrapper().playSound(player, getSound(DSound.BLOCK_NOTE_BLOCK_PLING, DSound.BLOCK_NOTE_BLOCK_BELL), 0.3f, 0.8f);
        }
    };


    public static final MessageType MENU_REFRESH = new MessageType() {
        @Override
        public void playSound(Player player) {
            NClaim.inst().getWrapper().playSound(player, getSound(DSound.BLOCK_PISTON_CONTRACT, DSound.BLOCK_PISTON_EXTEND), 0.3f, 1.5f);

            Bukkit.getScheduler().runTaskLater(NClaim.inst(), task -> {
                NClaim.inst().getWrapper().playSound(player, getSound(DSound.BLOCK_METAL_PRESSURE_PLATE_CLICK_ON, DSound.BLOCK_STONE_PRESSURE_PLATE_CLICK_ON), 0.4f, 1.6f);
            }, 1L);

            Bukkit.getScheduler().runTaskLater(NClaim.inst(), task -> {
                NClaim.inst().getWrapper().playSound(player, getSound(DSound.BLOCK_IRON_TRAPDOOR_CLOSE, DSound.BLOCK_WOODEN_TRAPDOOR_CLOSE), 0.3f, 2.0f);
            }, 3L);
        }
    };



    public static final MessageType SEARCH_OPEN = new MessageType() {
        @Override
        public void playSound(Player player) {
            NClaim.inst().getWrapper().playSound(player, getSound(DSound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, DSound.ITEM_BOOK_PUT), 0.6f, 1.2f);


            Bukkit.getScheduler().runTaskLater(NClaim.inst(), task -> {
                NClaim.inst().getWrapper().playSound(player, getSound(DSound.ITEM_BOOK_PAGE_TURN, DSound.ITEM_BOOK_PUT), 0.4f, 1.5f);

            }, 1L);

            Bukkit.getScheduler().runTaskLater(NClaim.inst(), task -> {
                NClaim.inst().getWrapper().playSound(player, getSound(DSound.BLOCK_NOTE_BLOCK_HAT, DSound.BLOCK_NOTE_BLOCK_SNARE), 0.3f, 1.8f);

            }, 2L);
        }
    };


    public static final MessageType MENU_BACK = new MessageType() {
        @Override
        public void playSound(Player player) {
            NClaim.inst().getWrapper().playSound(player, getSound(DSound.ITEM_BUNDLE_DROP_CONTENTS, DSound.ENTITY_ITEM_PICKUP), 0.5f, 1.2f);

            Bukkit.getScheduler().runTaskLater(NClaim.inst(), task -> {
                NClaim.inst().getWrapper().playSound(player, getSound(DSound.BLOCK_NOTE_BLOCK_HAT, DSound.BLOCK_NOTE_BLOCK_SNARE), 0.3f, 0.8f);

            }, 2L);
        }
    };

    public static final MessageType MENU_FORWARD = new MessageType() {
        @Override
        public void playSound(Player player) {
            NClaim.inst().getWrapper().playSound(player, getSound(DSound.BLOCK_ENCHANTMENT_TABLE_USE, DSound.BLOCK_BEACON_ACTIVATE), 0.5f, 1.5f);

            Bukkit.getScheduler().runTaskLater(NClaim.inst(), task -> {
                NClaim.inst().getWrapper().playSound(player, getSound(DSound.ENTITY_EXPERIENCE_ORB_PICKUP, DSound.ENTITY_PLAYER_LEVELUP), 0.3f, 1.8f);

            }, 2L);
        }
    };

    public static final MessageType MENU_SELECT = new MessageType() {
        @Override
        public void playSound(Player player) {
            NClaim.inst().getWrapper().playSound(player, getSound(DSound.UI_BUTTON_CLICK, DSound.BLOCK_STONE_BUTTON_CLICK_ON), 0.7f, 1.2f);

            Bukkit.getScheduler().runTaskLater(NClaim.inst(), task -> {
                NClaim.inst().getWrapper().playSound(player, getSound(DSound.ENTITY_PLAYER_LEVELUP, DSound.ENTITY_EXPERIENCE_ORB_PICKUP), 0.4f, 2.0f);

            }, 2L);
            Bukkit.getScheduler().runTaskLater(NClaim.inst(), task -> {
                NClaim.inst().getWrapper().playSound(player, getSound(DSound.BLOCK_NOTE_BLOCK_BELL, DSound.BLOCK_NOTE_BLOCK_CHIME), 0.3f, 1.8f);

            }, 3L);
        }
    };

    public static final MessageType MENU_DESELECT = new MessageType() {
        @Override
        public void playSound(Player player) {
            NClaim.inst().getWrapper().playSound(player, getSound(DSound.UI_BUTTON_CLICK, DSound.BLOCK_STONE_BUTTON_CLICK_ON), 0.7f, 0.8f);

            Bukkit.getScheduler().runTaskLater(NClaim.inst(), task -> {
                NClaim.inst().getWrapper().playSound(player, getSound(DSound.BLOCK_WOODEN_BUTTON_CLICK_OFF, DSound.BLOCK_STONE_BUTTON_CLICK_OFF), 0.4f, 0.7f);

            }, 1L);
            Bukkit.getScheduler().runTaskLater(NClaim.inst(), task -> {
                NClaim.inst().getWrapper().playSound(player, getSound(DSound.BLOCK_NOTE_BLOCK_HAT, DSound.BLOCK_NOTE_BLOCK_SNARE), 0.3f, 0.6f);

            }, 2L);
        }
    };

    public static final MessageType TELEPORT = new MessageType() {
        @Override
        public void playSound(Player player) {
            NClaim.inst().getWrapper().playSound(player, getSound(DSound.BLOCK_ENCHANTMENT_TABLE_USE, DSound.BLOCK_BEACON_ACTIVATE), 0.7f, 1.0f);
            NClaim.inst().getWrapper().playSound(player, getSound(DSound.BLOCK_BEACON_AMBIENT, DSound.BLOCK_BEACON_ACTIVATE), 0.4f, 2.0f);


            Bukkit.getScheduler().runTaskLater(NClaim.inst(), task -> {
                NClaim.inst().getWrapper().playSound(player, getSound(DSound.ENTITY_EXPERIENCE_ORB_PICKUP, DSound.ENTITY_PLAYER_LEVELUP), 0.5f, 1.8f);

            }, 2L);

            Bukkit.getScheduler().runTaskLater(NClaim.inst(), task -> {
                NClaim.inst().getWrapper().playSound(player, getSound(DSound.BLOCK_AMETHYST_BLOCK_CHIME, DSound.BLOCK_NOTE_BLOCK_CHIME), 0.3f, 1.5f);

            }, 4L);
        }
    };

    public abstract void playSound(Player player);

    public static MessageType silent() {
        return new MessageType() {
            @Override
            public void playSound(Player player) {}
        };
    }
}