package nesoi.aysihuniks.nclaim.commands.root;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.commands.BaseCommand;
import nesoi.aysihuniks.nclaim.model.Claim;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nandayo.dapi.message.ChannelType;

import java.util.*;

public class NameCommand extends BaseCommand {

    private static final int MAX_NAME_LEN = 24;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            ChannelType.CHAT.send(sender, NClaim.inst().getLangManager().getString("command.must_be_player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nclaim.name") && !player.hasPermission("nclaim.use")) {
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.permission_denied"));
            return true;
        }

        if (args.length < 3) {
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.wrong_usage"));
            return true;
        }

        String identifier = args[1];
        String displayNameRaw = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim();

        Claim claim;
        if ("here".equalsIgnoreCase(identifier)) {
            claim = Claim.getClaim(player.getLocation().getChunk());
        } else {
            claim = Claim.getClaimByIdentifier(player.getUniqueId(), identifier);
        }

        if (claim == null) {
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("claim.not_found"));
            return true;
        }

        if (!claim.getOwner().equals(player.getUniqueId()) && !player.hasPermission("nclaim.admin")) {
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.permission_denied"));
            return true;
        }

        String oldSlug = claim.getSlug();
        boolean isCleared = false;

        if (displayNameRaw.equalsIgnoreCase("off") || displayNameRaw.equalsIgnoreCase("none")) {
            claim.setDisplayName(null);
            claim.setSlug(Claim.toSlug(claim.getClaimId()));
            isCleared = true;
        } else {
            if (displayNameRaw.length() > MAX_NAME_LEN) {
                String msg = NClaim.inst().getLangManager().getString("claim.name_too_long")
                        .replace("{max}", String.valueOf(MAX_NAME_LEN))
                        .replace("{current}", String.valueOf(displayNameRaw.length()));
                ChannelType.CHAT.send(player, msg);
                return true;
            }

            claim.setDisplayName(displayNameRaw);
            claim.setSlug(Claim.toSlug(displayNameRaw));
        }

        ensureUniqueSlugForOwner(claim);
        String newSlug = claim.getSlug();

        NClaim.inst().getClaimStorageManager().saveClaim(claim);

        if (isCleared) {
            String msg = NClaim.inst().getLangManager().getString("claim.name_cleared")
                    .replace("{slug}", newSlug);
            ChannelType.CHAT.send(player, msg);
        } else if (oldSlug.equalsIgnoreCase(newSlug)) {
            String msg = NClaim.inst().getLangManager().getString("claim.name_changed_same_slug")
                    .replace("{slug}", newSlug);
            ChannelType.CHAT.send(player, msg);
        } else {
            String msg = NClaim.inst().getLangManager().getString("claim.name_changed_new_slug")
                    .replace("{old_slug}", oldSlug)
                    .replace("{new_slug}", newSlug);
            ChannelType.CHAT.send(player, msg);
        }

        return true;
    }

    private void ensureUniqueSlugForOwner(Claim claim) {
        UUID owner = claim.getOwner();
        String base = claim.getSlug();

        String candidate = base;
        int i = 2;

        while (true) {
            final String cand = candidate;

            Claim existing = Claim.claims.stream()
                    .filter(c -> owner.equals(c.getOwner()))
                    .filter(c -> c.getSlug().equalsIgnoreCase(cand))
                    .findFirst()
                    .orElse(null);

            if (existing == null || existing.getClaimId().equalsIgnoreCase(claim.getClaimId())) {
                claim.setSlug(candidate);
                return;
            }

            String suffix = "-" + i++;
            int max = MAX_NAME_LEN - suffix.length();
            String prefix = base.length() > max ? base.substring(0, Math.max(1, max)) : base;
            candidate = prefix + suffix;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) return null;
        Player player = (Player) sender;

        if (!player.hasPermission("nclaim.use") && !player.hasPermission("nclaim.admin")) {
            return null;
        }

        if (args.length == 2) {
            Set<String> suggestions = new LinkedHashSet<>();
            suggestions.add("here");

            boolean isAdmin = player.hasPermission("nclaim.admin");

            for (Claim c : Claim.claims) {
                if (!isAdmin && !player.getUniqueId().equals(c.getOwner())) {
                    continue;
                }

                if (!c.getSlug().isEmpty()) {
                    suggestions.add(c.getSlug());
                }
            }

            return new ArrayList<>(suggestions);
        }

        if (args.length == 3) {
            return Arrays.asList("off", "none", "<display_name>");
        }

        return null;
    }
}