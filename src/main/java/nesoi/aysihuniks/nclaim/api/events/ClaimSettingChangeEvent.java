package nesoi.aysihuniks.nclaim.api.events;

import lombok.Getter;
import lombok.Setter;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.model.ClaimSetting;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class ClaimSettingChangeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    private final @NotNull Claim claim;
    private final @NotNull ClaimSetting setting;
    private final @NotNull Player player;
    private final boolean newState;

    public ClaimSettingChangeEvent(@NotNull Claim claim, @NotNull ClaimSetting setting, @NotNull Player player, boolean newState) {
        this.claim = claim;
        this.setting = setting;
        this.player = player;
        this.newState = newState;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
