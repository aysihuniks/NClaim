package nesoi.aysihuniks.nclaim.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Setter;
import nesoi.aysihuniks.nclaim.Config;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.enums.Permission;
import nesoi.aysihuniks.nclaim.enums.Setting;
import nesoi.aysihuniks.nclaim.model.*;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.nandayo.dapi.Util;

import java.io.File;
import java.lang.reflect.Type;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class SQLiteManager implements DatabaseManager {
    private final HikariDataSource dataSource;
    private final Gson gson;

    // SQL statements as constants
    private static final class SQLStatements {
        // Create tables
        static final String CREATE_USERS_TABLE =
                "CREATE TABLE IF NOT EXISTS users (uuid TEXT PRIMARY KEY, balance REAL DEFAULT 0, skinTexture TEXT)";

        static final String CREATE_CLAIMS_TABLE =
                "CREATE TABLE IF NOT EXISTS claims (" +
                        "claim_id TEXT PRIMARY KEY, chunk_world TEXT, chunk_x INTEGER, chunk_z INTEGER, " +
                        "created_at TEXT DEFAULT CURRENT_TIMESTAMP, expired_at TEXT NULL, owner TEXT, " +
                        "bedrock_location TEXT, lands TEXT, claim_value INTEGER DEFAULT 0)";

        static final String CREATE_CLAIM_COOPS_TABLE =
                "CREATE TABLE IF NOT EXISTS claim_coops (" +
                        "claim_id TEXT, player_uuid TEXT, joined_at TEXT, permissions TEXT, " +
                        "PRIMARY KEY (claim_id, player_uuid))";

        static final String CREATE_CLAIM_SETTINGS_TABLE =
                "CREATE TABLE IF NOT EXISTS claim_settings (claim_id TEXT PRIMARY KEY, settings TEXT)";

        // CRUD operations
        static final String SAVE_USER = "INSERT OR REPLACE INTO users (uuid, balance, skinTexture) VALUES (?, ?, ?)";
        static final String LOAD_USER = "SELECT balance, skinTexture FROM users WHERE uuid = ?";
        static final String LOAD_ALL_USERS = "SELECT uuid, balance, skinTexture FROM users";

        static final String SAVE_CLAIM =
                "INSERT OR REPLACE INTO claims (claim_id, chunk_world, chunk_x, chunk_z, created_at, " +
                        "expired_at, owner, bedrock_location, lands, claim_value) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        static final String LOAD_CLAIM = "SELECT * FROM claims WHERE claim_id = ?";
        static final String LOAD_ALL_CLAIMS = "SELECT * FROM claims";
        static final String DELETE_CLAIM = "DELETE FROM claims WHERE claim_id = ?";

        static final String SAVE_CLAIM_COOP =
                "INSERT OR REPLACE INTO claim_coops (claim_id, player_uuid, joined_at, permissions) VALUES (?, ?, ?, ?)";
        static final String LOAD_CLAIM_COOPS = "SELECT * FROM claim_coops WHERE claim_id = ?";
        static final String DELETE_CLAIM_COOPS = "DELETE FROM claim_coops WHERE claim_id = ?";

        static final String SAVE_CLAIM_SETTINGS =
                "INSERT OR REPLACE INTO claim_settings (claim_id, settings) VALUES (?, ?)";
        static final String LOAD_CLAIM_SETTINGS = "SELECT * FROM claim_settings WHERE claim_id = ?";
        static final String DELETE_CLAIM_SETTINGS = "DELETE FROM claim_settings WHERE claim_id = ?";
    }

    public SQLiteManager(Config config) {
        this.dataSource = setupDataSource(config);
        this.gson = new Gson();
        initializeDatabase();
    }

    private HikariDataSource setupDataSource(Config config) {
        File databaseFile = new File(NClaim.inst().getDataFolder(), config.getSqliteFile());
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        hikariConfig.setMaximumPoolSize(config.getMaximumPoolSize());
        hikariConfig.setMinimumIdle(config.getMinimumIdle());

        hikariConfig.addDataSourceProperty("foreign_keys", "on");
        hikariConfig.addDataSourceProperty("journal_mode", "WAL");
        hikariConfig.addDataSourceProperty("synchronous", "NORMAL");

        return new HikariDataSource(hikariConfig);
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection()) {
            executeUpdate(conn, SQLStatements.CREATE_USERS_TABLE);
            executeUpdate(conn, SQLStatements.CREATE_CLAIMS_TABLE);
            executeUpdate(conn, SQLStatements.CREATE_CLAIM_COOPS_TABLE);
            executeUpdate(conn, SQLStatements.CREATE_CLAIM_SETTINGS_TABLE);
            Util.log("&aSQLite tables initialized successfully");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize SQLite tables", e);
        }
    }

    private void executeUpdate(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    protected Timestamp getTimestamp(Date date) {
        if (date == null) return null;
        return Timestamp.valueOf(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date));
    }

    @Override
    public int getClaimCount() {
        String sql = "SELECT COUNT(*) FROM claims";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int getUserCount() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // User operations
    @Override
    public void saveUser(User user) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQLStatements.SAVE_USER)) {
            stmt.setString(1, user.getUuid().toString());
            stmt.setDouble(2, user.getBalance());
            stmt.setString(3, user.getSkinTexture());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logError("save user data", e);
        }
    }

    @Override
    public User loadUser(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQLStatements.LOAD_USER)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return createUserFromResultSet(uuid, rs);
            }
        } catch (SQLException e) {
            logError("load user data", e);
        }
        return null;
    }

    @Override
    public List<User> loadAllUsers() {
        List<User> users = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQLStatements.LOAD_ALL_USERS)) {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                users.add(createUserFromResultSet(
                    UUID.fromString(rs.getString("uuid")),
                    rs
                ));
            }
        } catch (SQLException e) {
            logError("load all users", e);
        }
        return users;
    }

    private User createUserFromResultSet(UUID uuid, ResultSet rs) throws SQLException {
        return new User(
            uuid,
            rs.getDouble("balance"),
            rs.getString("skinTexture"),
            new ArrayList<>(),
            new ArrayList<>()
        );
    }

    // Claim operations
    @Override
    public void saveClaim(Claim claim) {
        try (Connection conn = getConnection()) {
            saveClaimData(conn, claim);
            saveClaimCoops(conn, claim);
            saveClaimSettings(conn, claim);
        } catch (SQLException e) {
            logError("save claim data", e);
        }
    }

    @Override
    public void saveClaimsBatch(List<Claim> claims) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (Claim claim : claims) {
                    saveClaimData(conn, claim);
                    saveClaimCoops(conn, claim);
                    saveClaimSettings(conn, claim);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logError("save claims batch", e);
        }
    }

    private void saveClaimData(Connection conn, Claim claim) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQLStatements.SAVE_CLAIM)) {
            prepareClaimStatement(stmt, claim);
            stmt.executeUpdate();
        }
    }

    private void prepareClaimStatement(PreparedStatement stmt, Claim claim) throws SQLException {
        stmt.setString(1, claim.getClaimId());
        stmt.setString(2, claim.getChunk().getWorld().getName());
        stmt.setInt(3, claim.getChunk().getX());
        stmt.setInt(4, claim.getChunk().getZ());
        stmt.setString(5, getTimestamp(claim.getCreatedAt()).toString());
        stmt.setString(6, getTimestamp(claim.getExpiredAt()).toString());
        stmt.setString(7, claim.getOwner().toString());
        stmt.setString(8, NClaim.serializeLocation(claim.getBedrockLocation()));
        stmt.setString(9, gson.toJson(claim.getLands()));
    }

    private void saveClaimCoops(Connection conn, Claim claim) throws SQLException {
        executeUpdate(conn, SQLStatements.DELETE_CLAIM_COOPS, claim.getClaimId());

        try (PreparedStatement stmt = conn.prepareStatement(SQLStatements.SAVE_CLAIM_COOP)) {
            for (UUID coopPlayer : claim.getCoopPlayers()) {
                Map<String, Boolean> permissions = serializePermissions(claim.getCoopPermissions().get(coopPlayer));
                stmt.setString(1, claim.getClaimId());
                stmt.setString(2, coopPlayer.toString());
                stmt.setString(3, getTimestamp(claim.getCoopPlayerJoinDate().get(coopPlayer)).toString());
                stmt.setString(4, gson.toJson(permissions));
                stmt.executeUpdate();
            }
        }
    }

    private Map<String, Boolean> serializePermissions(CoopPermission coopPerm) {
        Map<String, Boolean> permissions = new HashMap<>();
        for (Permission perm : Permission.values()) {
            permissions.put(perm.name(), coopPerm.isEnabled(perm));
        }
        return permissions;
    }

    private void saveClaimSettings(Connection conn, Claim claim) throws SQLException {
        Map<String, Boolean> settings = new HashMap<>();
        for (Setting setting : Setting.values()) {
            settings.put(setting.name(), claim.getSettings().isEnabled(setting));
        }

        try (PreparedStatement stmt = conn.prepareStatement(SQLStatements.SAVE_CLAIM_SETTINGS)) {
            stmt.setString(1, claim.getClaimId());
            stmt.setString(2, gson.toJson(settings));
            stmt.executeUpdate();
        }
    }

    @Override
    public Claim loadClaim(String claimId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQLStatements.LOAD_CLAIM)) {
            stmt.setString(1, claimId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return createClaimFromResultSet(rs, conn);
            }
        } catch (SQLException e) {
            logError("load claim", e);
        }
        return null;
    }

    @Override
    public List<Claim> loadAllClaims() {
        List<Claim> claims = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQLStatements.LOAD_ALL_CLAIMS)) {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Claim claim = createClaimFromResultSet(rs, conn);
                if (claim != null) {
                    claims.add(claim);
                }
            }
        } catch (SQLException e) {
            logError("load all claims", e);
        }
        return claims;
    }

    private Claim createClaimFromResultSet(ResultSet rs, Connection conn) throws SQLException {
        World world = Bukkit.getWorld(rs.getString("chunk_world"));
        if (world == null) return null;

        Chunk chunk = world.getChunkAt(rs.getInt("chunk_x"), rs.getInt("chunk_z"));
        String claimId = rs.getString("claim_id");

        Date createdAt = Timestamp.valueOf(rs.getString("created_at"));
        Date expiredAt = Timestamp.valueOf(rs.getString("expired_at"));

        UUID owner = UUID.fromString(rs.getString("owner"));
        Location bedrockLocation = NClaim.deserializeLocation(rs.getString("bedrock_location"));
        long claimValue = 0;
        try {
            claimValue = rs.getLong("claim_value");
        } catch (SQLException e) {
            Util.log("&cFailed to read claim_value for claim " + claimId + ": " + e.getMessage());
        }

        Type landType = new TypeToken<Collection<String>>(){}.getType();
        Collection<String> lands = gson.fromJson(rs.getString("lands"), landType);

        CoopData coopData = loadClaimCoops(conn, claimId);
        ClaimSetting settings = loadClaimSettings(conn, claimId);



        return new Claim(
                claimId,
                chunk,
                createdAt,
                expiredAt,
                owner,
                bedrockLocation,
                claimValue,
                lands,
                coopData.getCoopPlayers(),
                coopData.getJoinDates(),
                coopData.getPermissions(),
                settings
        );
    }

    private CoopData loadClaimCoops(Connection conn, String claimId) throws SQLException {
        CoopData coopData = new CoopData();

        try (PreparedStatement stmt = conn.prepareStatement(SQLStatements.LOAD_CLAIM_COOPS)) {
            stmt.setString(1, claimId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                coopData.getCoopPlayers().add(playerUuid);
                coopData.getJoinDates().put(playerUuid, Timestamp.valueOf(rs.getString("joined_at")));

                Type permType = new TypeToken<Map<String, Boolean>>(){}.getType();
                Map<String, Boolean> permMap = gson.fromJson(rs.getString("permissions"), permType);

                CoopPermission coopPerm = new CoopPermission();
                for (Map.Entry<String, Boolean> entry : permMap.entrySet()) {
                    coopPerm.setEnabled(Permission.valueOf(entry.getKey()), entry.getValue());
                }
                coopData.getPermissions().put(playerUuid, coopPerm);
            }
        }

        return coopData;
    }

    private ClaimSetting loadClaimSettings(Connection conn, String claimId) throws SQLException {
        ClaimSetting settings = new ClaimSetting();

        try (PreparedStatement stmt = conn.prepareStatement(SQLStatements.LOAD_CLAIM_SETTINGS)) {
            stmt.setString(1, claimId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Type settingsType = new TypeToken<Map<String, Boolean>>(){}.getType();
                Map<String, Boolean> settingsMap = gson.fromJson(rs.getString("settings"), settingsType);

                for (Map.Entry<String, Boolean> entry : settingsMap.entrySet()) {
                    settings.set(Setting.valueOf(entry.getKey()), entry.getValue());
                }
            }
        }

        return settings;
    }

    @Override
    public void deleteClaim(String claimId) {
        try (Connection conn = getConnection()) {
            executeUpdate(conn, SQLStatements.DELETE_CLAIM_COOPS, claimId);
            executeUpdate(conn, SQLStatements.DELETE_CLAIM_SETTINGS, claimId);
            executeUpdate(conn, SQLStatements.DELETE_CLAIM, claimId);
        } catch (SQLException e) {
            logError("delete claim", e);
        }
    }

    private void executeUpdate(Connection conn, String sql, String claimId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, claimId);
            stmt.executeUpdate();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void logError(String operation, SQLException e) {
        Util.log("&cFailed to " + operation + ": " + e.getMessage());
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}