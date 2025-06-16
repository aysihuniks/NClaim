package nesoi.aysihuniks.nclaim.api.events;

import lombok.Getter;
import nesoi.aysihuniks.nclaim.model.Claim;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class ClaimCoopAddEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled = false;

    private final @NotNull Player owner;
    private final @NotNull Player coopPlayer;
    private final @NotNull Claim claim;

    public ClaimCoopAddEvent(@NotNull Player owner, @NotNull Player coopPlayer, @NotNull Claim claim) {
        this.owner = owner;
        this.coopPlayer = coopPlayer;
        this.claim = claim;
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
