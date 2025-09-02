package nesoi.aysihuniks.nclaim.commands.admin;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.commands.BaseCommand;
import nesoi.aysihuniks.nclaim.utils.LangManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.message.ChannelType;

import java.util.*;
import java.util.stream.Collectors;

public class ChangeCommand extends BaseCommand {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        LangManager langManager = NClaim.inst().getLangManager();
        if (!(sender instanceof Player)) {
            ChannelType.CHAT.send(sender, langManager.getString("command.must_be_player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nclaim.change") && !player.hasPermission("nclaim.admin")) {
            ChannelType.CHAT.send(player, langManager.getString("command.permission.denied"));
            return true;
        }

        if (args.length < 3) {
            ChannelType.CHAT.send(player, langManager.getString("command.wrong_usage"));
            return true;
        }

        if (args[1].equalsIgnoreCase("lang")) {
            String lang = args[2];
            handleLangChange(player, lang);
        } else if (args[1].equalsIgnoreCase("blockvalue")) {
            if (args.length < 4) {
                ChannelType.CHAT.send(player, langManager.getString("command.wrong_usage"));
                return true;
            }
            String materialName = args[2];
            String valueStr = args[3];
            handleBlockValueChange(player, materialName, valueStr);
        } else {
            ChannelType.CHAT.send(player, langManager.getString("command.wrong_usage"));
        }

        return true;
    }

    private void handleLangChange(Player player, String lang) {
        if (NClaim.inst().getNconfig().isValidLanguage(lang) && !lang.equalsIgnoreCase(NClaim.inst().getNconfig().getDefaultLanguage())) {
            NClaim.inst().getNconfig().setDefaultLanguage(lang);
            NClaim.inst().getNconfig().save();
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.change.lang_changed").replace("{lang}", lang));
        } else {
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.change.invalid_lang"));
        }
    }

    private void handleBlockValueChange(Player player, String materialName, String valueStr) {
        LangManager langManager = NClaim.inst().getLangManager();

        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            ChannelType.CHAT.send(player, langManager.getString("command.change.invalid_material").replace("{material}", materialName));
            return;
        }

        int value;
        try {
            value = Integer.parseInt(valueStr);
            if (value <= 0) {
                ChannelType.CHAT.send(player, langManager.getString("command.change.invalid_value"));
                return;
            }
        } catch (NumberFormatException e) {
            ChannelType.CHAT.send(player, langManager.getString("command.change.invalid_value"));
            return;
        }

        int oldValue = NClaim.inst().getBlockValueManager().getBlockValue(material);

        if (oldValue == value) {
            ChannelType.CHAT.send(player, langManager.getString("command.change.blockvalue_not_changed"));
            return;
        }

        NClaim.inst().getBlockValueManager().setBlockValue(material, value);

        if (oldValue > 0) {
            ChannelType.CHAT.send(player, langManager.getString("command.change.blockvalue_updated")
                    .replace("{material}", material.name())
                    .replace("{old_value}", String.valueOf(oldValue))
                    .replace("{new_value}", String.valueOf(value)));
        } else {
            ChannelType.CHAT.send(player, langManager.getString("command.change.blockvalue_added")
                    .replace("{material}", material.name())
                    .replace("{value}", String.valueOf(value)));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if ((!(sender instanceof Player)) || (!sender.hasPermission("nclaim.change") && !sender.hasPermission("nclaim.admin"))) {
            return null;
        }

        if (args.length == 2) {
            return Arrays.asList("lang", "blockvalue");
        }

        if (args.length == 3) {
            if (args[1].equalsIgnoreCase("lang")) {
                return Arrays.asList("tr-TR", "en-US", "fr-FR");
            } else if (args[1].equalsIgnoreCase("blockvalue")) {
                Set<Material> ymlMaterials = NClaim.inst().getBlockValueManager().getBlockValues().keySet();
                return ymlMaterials.stream()
                        .map(Material::name)
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 4 && args[1].equalsIgnoreCase("blockvalue")) {
            Material material;
            try {
                material = Material.valueOf(args[2].toUpperCase());
                int currentValue = NClaim.inst().getBlockValueManager().getBlockValue(material);
                if (currentValue > 0) {
                    return Collections.singletonList(String.valueOf(currentValue));
                }
            } catch (IllegalArgumentException ignored) {
            }

            return Arrays.asList("1", "5", "10", "25", "50", "100");
        }

        return null;
    }
}