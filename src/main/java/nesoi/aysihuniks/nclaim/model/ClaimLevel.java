package nesoi.aysihuniks.nclaim.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

@Getter
@Setter
public class ClaimLevel {
    private Material material;
    private int value;

    public ClaimLevel(Material material, int value) {
        this.material = material;
        this.value = value;
    }
}
