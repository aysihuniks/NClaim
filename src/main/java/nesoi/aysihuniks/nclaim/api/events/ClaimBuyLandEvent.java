package nesoi.aysihuniks.nclaim.api.events;

import lombok.Getter;
import nesoi.aysihuniks.nclaim.model.Claim;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Getter
public class ClaimBuyLandEvent extends ClaimBuyEvent {

    private final @NotNull Chunk land;

    public ClaimBuyLandEvent(@NotNull Player sender, @NotNull Claim claim, @NotNull Chunk land) {
        super(sender, claim);
        this.land = land;
    }
}
