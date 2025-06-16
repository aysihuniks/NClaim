package nesoi.aysihuniks.nclaim.api.events;

import lombok.Getter;
import nesoi.aysihuniks.nclaim.model.Claim;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Getter
public class ClaimCreateEvent extends ClaimBuyEvent {

    public ClaimCreateEvent(@NotNull Player sender, @NotNull Claim claim) {
        super(sender, claim);
    }
}
