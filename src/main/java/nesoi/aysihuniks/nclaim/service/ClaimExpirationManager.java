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
        
        expirationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            checkExpiredClaims();
        }, checkInterval, checkInterval);
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
                    plugin.getMySQLManager().deleteClaim(claim.getClaimId());
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
            plugin.getMySQLManager().saveClaim(claim);
        }
    }

    public String getFormattedTimeLeft(Claim claim) {
        long diffInMillis = claim.getExpiredAt().getTime() - new Date().getTime();
        long diffDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
        long diffHours = TimeUnit.HOURS.convert(diffInMillis, TimeUnit.MILLISECONDS) % 24;
        long diffMinutes = TimeUnit.MINUTES.convert(diffInMillis, TimeUnit.MILLISECONDS) % 60;
        long diffSeconds = TimeUnit.SECONDS.convert(diffInMillis, TimeUnit.MILLISECONDS) % 60;

        String timeLeft;
        if (diffDays > 0) {
            timeLeft = String.format("%dd, %dh", diffDays, diffHours);
        } else if (diffHours > 0) {
            timeLeft = String.format("%dh, %dm", diffHours, diffMinutes);
        } else if (diffMinutes > 0) {
            timeLeft = String.format("%dm, %ds", diffMinutes, diffSeconds);
        } else {
            timeLeft = String.format("%ds", diffSeconds);
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