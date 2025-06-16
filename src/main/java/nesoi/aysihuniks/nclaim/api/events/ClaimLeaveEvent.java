package nesoi.aysihuniks.nclaim.api.events;

import lombok.Getter;
import nesoi.aysihuniks.nclaim.model.Claim;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class ClaimLeaveEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final @NotNull Player sender;
    private final @NotNull Claim claim;

    public ClaimLeaveEvent(@NotNull Player sender, @NotNull Claim claim) {
        this.sender = sender;
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
}
