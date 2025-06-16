package nesoi.aysihuniks.nclaim.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

@Getter
@NoArgsConstructor
public class CoopData {

    private Collection<UUID> coopPlayers = new ArrayList<>();
    private HashMap<UUID, Date> joinDates = new HashMap<>();
    private HashMap<UUID, CoopPermission> permissions = new HashMap<>();

    public CoopData(Collection<UUID> coopPlayers,
                    HashMap<UUID, Date> joinDates,
                    HashMap<UUID, CoopPermission> permissions) {
        this.coopPlayers = coopPlayers;
        this.joinDates = joinDates;
        this.permissions = permissions;
    }


}
