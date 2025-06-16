package nesoi.aysihuniks.nclaim.database;

import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface DatabaseManager {

    void saveUser(User user);
    User loadUser(UUID uuid);
    List<User> loadAllUsers();
    void saveClaim(Claim claim);
    void saveClaimsBatch(List<Claim> claims);
    Claim loadClaim(String claimId);
    List<Claim> loadAllClaims();
    void deleteClaim(String claimId);
    Connection getConnection() throws SQLException;
    void close();

    int getClaimCount();
    int getUserCount();

}
