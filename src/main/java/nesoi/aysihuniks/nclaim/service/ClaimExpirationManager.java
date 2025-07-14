package nesoi.aysihuniks.nclaim.service;

import lombok.RequiredArgsConstructor;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.enums.RemoveCause;
import nesoi.aysihuniks.nclaim.model.Claim;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.nandayo.dapi.HexUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class ClaimExpirationManager {
    private final NClaim plugin;
    private BukkitTask expirationTask;

    public void startExpirationChecker() {
        long checkInterval = 5 * 60 * 20;
        
        expirationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkExpiredClaims, checkInterval, checkInterval);
    }

    public void stopExpirationChecker() {
        if (expirationTask != null) {
            expirationTask.cancel();
            expirationTask = null;
        }
    }

    public void checkExpiredClaims() {
        Date currentDate = new Date();
        for (Claim claim : new ArrayList<>(Claim.claims)) {
            if (claim.getExpiredAt().before(currentDate)) {
                if (plugin.getNconfig().isDatabaseEnabled()) {
                    plugin.getDatabaseManager().deleteClaim(claim.getClaimId());
                }
                claim.remove(RemoveCause.REMOVED);
            }
        }
    }

    public void extendClaimExpiration(Claim claim, int days, int hours, int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(claim.getExpiredAt());

        calendar.add(Calendar.DAY_OF_MONTH, days);
        calendar.add(Calendar.HOUR_OF_DAY, hours);
        calendar.add(Calendar.MINUTE, minutes);

        claim.setExpiredAt(calendar.getTime());

        if (plugin.getNconfig().isDatabaseEnabled()) {
            plugin.getDatabaseManager().saveClaim(claim);
        }
    }

    public String getFormattedTimeLeft(Claim claim) {
        long diffInMillis = claim.getExpiredAt().getTime() - new Date().getTime();
        long diffDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
        long diffHours = TimeUnit.HOURS.convert(diffInMillis, TimeUnit.MILLISECONDS) % 24;
        long diffMinutes = TimeUnit.MINUTES.convert(diffInMillis, TimeUnit.MILLISECONDS) % 60;
        long diffSeconds = TimeUnit.SECONDS.convert(diffInMillis, TimeUnit.MILLISECONDS) % 60;

        String daySymbol = plugin.getLangManager().getString("hologram.time_left.d");
        String hourSymbol = plugin.getLangManager().getString("hologram.time_left.h");
        String minuteSymbol = plugin.getLangManager().getString("hologram.time_left.m");
        String secondSymbol = plugin.getLangManager().getString("hologram.time_left.s");

        String timeLeft;
        if (diffDays > 0) {
            timeLeft = String.format("%d%s, %d%s", diffDays, daySymbol, diffHours, hourSymbol);
        } else if (diffHours > 0) {
            timeLeft = String.format("%d%s, %d%s", diffHours, hourSymbol, diffMinutes, minuteSymbol);
        } else if (diffMinutes > 0) {
            timeLeft = String.format("%d%s, %d%s", diffMinutes, minuteSymbol, diffSeconds, secondSymbol);
        } else {
            timeLeft = String.format("%d%s", diffSeconds, secondSymbol);
        }

        String color;
        if (diffDays >= 2) {
            color = "{GREEN}";
        } else if (diffDays >= 1) {
            color = "{YELLOW}";
        } else {
            color = "{RED}";
        }

        return HexUtil.parse(color + timeLeft);
    }

    public boolean isExpired(Claim claim) {
        return claim.getExpiredAt().before(new Date());
    }

    public boolean isExpiringSoon(Claim claim, int warningHours) {
        long diffInMillis = claim.getExpiredAt().getTime() - new Date().getTime();
        long diffHours = TimeUnit.HOURS.convert(diffInMillis, TimeUnit.MILLISECONDS);
        return diffHours <= warningHours;
    }
}