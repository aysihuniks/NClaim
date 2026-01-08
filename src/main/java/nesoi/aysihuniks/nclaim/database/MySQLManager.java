package nesoi.aysihuniks.nclaim.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import nesoi.aysihuniks.nclaim.Config;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.enums.Permission;
import nesoi.aysihuniks.nclaim.enums.Setting;
import nesoi.aysihuniks.nclaim.model.*;
import org.bukkit.*;
import org.nandayo.dapi.util.Util;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

public class MySQLManager implements DatabaseManager {
    private final HikariDataSource dataSource;
    @Getter private final String database;
    private final Gson gson;

    private static final String CREATE_USERS_TABLE =
        "CREATE TABLE IF NOT EXISTS users (" +
        "uuid VARCHAR(36) NOT NULL PRIMARY KEY, " +
        "balance DOUBLE DEFAULT 0, " +
        "skinTexture TEXT)";

    private static final String CREATE_CLAIMS_TABLE = 
        "CREATE TABLE IF NOT EXISTS claims (" +
        "claim_id VARCHAR(100) PRIMARY KEY, " +
        "name VARCHAR(24) NULL," +
        "slug VARCHAR(24) NULL, " +
        "chunk_world VARCHAR(100), " +
        "chunk_x INT, " +
        "chunk_z INT, " +
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "expired_at TIMESTAMP NULL, " +
        "owner VARCHAR(36), " +
        "claim_block_location TEXT, " +
        "lands TEXT, " +
        "claim_value BIGINT DEFAULT 0," +
        "claim_block_type VARCHAR(50)," +
        "purchased_blocks TEXT," +
        "for_sale BOOLEAN DEFAULT FALSE," +
        "sale_price DOUBLE DEFAULT 0)";

    private static final String CREATE_CLAIM_COOPS_TABLE =
        "CREATE TABLE IF NOT EXISTS claim_coops (" +
        "claim_id VARCHAR(100), " +
        "player_uuid VARCHAR(36), " +
        "joined_at TIMESTAMP, " +
        "permissions TEXT, " +
        "PRIMARY KEY (claim_id, player_uuid))";

    private static final String CREATE_CLAIM_SETTINGS_TABLE =
        "CREATE TABLE IF NOT EXISTS claim_settings (" +
        "claim_id VARCHAR(100) PRIMARY KEY, " +
        "settings TEXT)";

    private static final String SAVE_USER =
        "INSERT INTO users (uuid, balance, skinTexture) VALUES (?, ?, ?) " +
        "ON DUPLICATE KEY UPDATE balance = ?, skinTexture = ?";

    private static final String LOAD_USER = 
        "SELECT balance, skinTexture FROM users WHERE uuid = ?";

    private static final String LOAD_ALL_USERS = 
        "SELECT uuid, balance, skinTexture FROM users";

    private static final String SAVE_CLAIM =
        "INSERT INTO claims (claim_id, name, slug, chunk_world, chunk_x, chunk_z, created_at, expired_at, " +
        "owner, claim_block_location, lands, claim_value, claim_block_type, purchased_blocks, for_sale, sale_price) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
        "ON DUPLICATE KEY UPDATE " +
        "expired_at=?, name=?, slug=?, owner=?, claim_block_location=?, lands=?, claim_value=?, " +
        "claim_block_type=?, purchased_blocks=?,for_sale=?, sale_price=?";

    private static final String SAVE_CLAIM_COOP = 
        "INSERT INTO claim_coops VALUES (?, ?, ?, ?) " +
        "ON DUPLICATE KEY UPDATE joined_at=?, permissions=?";

    private static final String SAVE_CLAIM_SETTINGS = 
        "INSERT INTO claim_settings VALUES (?, ?) " +
        "ON DUPLICATE KEY UPDATE settings=?";

    private static final String LOAD_CLAIM = 
        "SELECT * FROM claims WHERE claim_id = ?";

    private static final String LOAD_ALL_CLAIMS = 
        "SELECT * FROM claims";

    private static final String LOAD_CLAIM_COOPS = 
        "SELECT * FROM claim_coops WHERE claim_id = ?";

    private static final String LOAD_CLAIM_SETTINGS = 
        "SELECT * FROM claim_settings WHERE claim_id = ?";

    private static final String DELETE_CLAIM = 
        "DELETE FROM claims WHERE claim_id = ?";

    private static final String DELETE_CLAIM_COOPS = 
        "DELETE FROM claim_coops WHERE claim_id = ?";

    private static final String DELETE_CLAIM_SETTINGS = 
        "DELETE FROM claim_settings WHERE claim_id = ?";

    public MySQLManager(Config config) {
        this.database = config.getMysqlDatabase();
        this.gson = new Gson();
        createDatabaseIfNotExists(config);
        this.dataSource = setupDataSource(config);
        initializeDatabase();
    }

    private void createDatabaseIfNotExists(Config config) {
        String host = config.getMysqlHost();
        int port = config.getMysqlPort();
        String user = config.getMysqlUser();
        String password = config.getMysqlPassword();
        String dbName = config.getMysqlDatabase();
        String rootUrl = String.format("jdbc:mysql://%s:%d/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port);
        try (Connection conn = DriverManager.getConnection(rootUrl, user, password);
            Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + dbName + "`");
            Util.log("&aDatabase '" + dbName + "' checked/created successfully.");
        }
        catch (SQLException e) {
                    throw new RuntimeException("Failed to initialize database '" + dbName + "': " + e.getMessage(), e);
        }
    }

    private HikariDataSource setupDataSource(Config config) {
        String host = config.getMysqlHost();
        int port = config.getMysqlPort();
        String database = config.getMysqlDatabase();
        String user = config.getMysqlUser();
        String password = config.getMysqlPassword();
        String jdbcUrl = String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            host, port, database
        );
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(user);
        hikariConfig.setPassword(password);

        hikariConfig.setMaximumPoolSize(config.getMaximumPoolSize());
        hikariConfig.setMinimumIdle(config.getMinimumIdle());
        hikariConfig.setIdleTimeout(config.getIdleTimeout());
        hikariConfig.setMaxLifetime(config.getMaxLifetime());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout());

        return new HikariDataSource(hikariConfig);
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection()) {
            conn.prepareStatement(CREATE_USERS_TABLE).executeUpdate();
            conn.prepareStatement(CREATE_CLAIMS_TABLE).executeUpdate();
            conn.prepareStatement(CREATE_CLAIM_COOPS_TABLE).executeUpdate();
            conn.prepareStatement(CREATE_CLAIM_SETTINGS_TABLE).executeUpdate();
            ensureClaimSaleColumns(conn);

            Util.log("&aDatabase tables initialized successfully.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database tables", e);
        }
    }

    private void ensureClaimSaleColumns(Connection conn) {
        try (Statement st = conn.createStatement()) {
            try {
                st.executeUpdate("ALTER TABLE claims ADD COLUMN name VARCHAR(24) NULL");
            } catch (SQLException ignored) {
            }
            try {
                st.executeUpdate("ALTER TABLE claims ADD COLUMN slug VARCHAR(24) NULL");
            } catch (SQLException ignored) {
            }
            try {
                st.executeUpdate("ALTER TABLE claims ADD COLUMN for_sale BOOLEAN DEFAULT FALSE");
            } catch (SQLException ignored) {
            }
            try {
                st.executeUpdate("ALTER TABLE claims ADD COLUMN sale_price DOUBLE DEFAULT 0");
            } catch (SQLException ignored) {
            }
        } catch (SQLException e) {
            Util.log("&cFailed to ensure claim sale columns: " + e.getMessage());
        }
    }

    public void saveUser(User user) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SAVE_USER)) {
            stmt.setString(1, user.getUuid().toString());
            stmt.setDouble(2, user.getBalance());
            stmt.setString(3, user.getSkinTexture());
            stmt.setDouble(4, user.getBalance());
            stmt.setString(5, user.getSkinTexture());
            stmt.executeUpdate();
        } catch (SQLException e) {
            Util.log("&cFailed to save user data: " + e.getMessage());
        }
    }

    public User loadUser(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(LOAD_USER)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new User(
                    uuid,
                    rs.getDouble("balance"),
                    null,
                    new ArrayList<>(),
                    new ArrayList<>()
                );
            }
        } catch (SQLException e) {
            Util.log("&cFailed to load user data: " + e.getMessage());
        }
        return null;
    }

    public List<User> loadAllUsers() {
        List<User> users = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(LOAD_ALL_USERS)) {
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                users.add(new User(
                    UUID.fromString(rs.getString("uuid")),
                    rs.getDouble("balance"),
                    rs.getString("skinTexture"),
                    new ArrayList<>(),
                    new ArrayList<>()
                ));
            }
        } catch (SQLException e) {
            Util.log("&cFailed to load all users: " + e.getMessage());
        }
        return users;
    }

    public void saveClaim(Claim claim) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(SAVE_CLAIM)) {
                // INSERT (1..16)
                stmt.setString(1, claim.getClaimId());
                stmt.setString(2, claim.getDisplayName());
                stmt.setString(3, claim.getSlug());

                stmt.setString(4, claim.getChunk().getWorld().getName());
                stmt.setInt(5, claim.getChunk().getX());
                stmt.setInt(6, claim.getChunk().getZ());
                stmt.setTimestamp(7, new Timestamp(claim.getCreatedAt().getTime()));
                stmt.setTimestamp(8, claim.getExpiredAt() != null ? new Timestamp(claim.getExpiredAt().getTime()) : null);
                stmt.setString(9, claim.getOwner().toString());
                stmt.setString(10, NClaim.serializeLocation(claim.getClaimBlockLocation()));
                stmt.setString(11, gson.toJson(claim.getLands()));
                stmt.setLong(12, claim.getClaimValue());
                stmt.setString(13, claim.getClaimBlockType().name());

                List<String> purchasedBlockNames = claim.getPurchasedBlockTypes().stream()
                        .map(Material::name)
                        .collect(Collectors.toList());
                stmt.setString(14, gson.toJson(purchasedBlockNames));

                stmt.setBoolean(15, claim.isForSale());
                stmt.setDouble(16, claim.getSalePrice());

                // UPDATE (17..27)
                stmt.setTimestamp(17, claim.getExpiredAt() != null ? new Timestamp(claim.getExpiredAt().getTime()) : null);
                stmt.setString(18, claim.getDisplayName());
                stmt.setString(19, claim.getSlug());
                stmt.setString(20, claim.getOwner().toString());
                stmt.setString(21, NClaim.serializeLocation(claim.getClaimBlockLocation()));
                stmt.setString(22, gson.toJson(claim.getLands()));
                stmt.setLong(23, claim.getClaimValue());
                stmt.setString(24, claim.getClaimBlockType().name());
                stmt.setString(25, gson.toJson(purchasedBlockNames));
                stmt.setBoolean(26, claim.isForSale());
                stmt.setDouble(27, claim.getSalePrice());

                stmt.executeUpdate();
            }

            saveClaimCoops(conn, claim);
            saveClaimSettings(conn, claim);

        } catch (SQLException e) {
            Util.log("&cFailed to save claim: " + e.getMessage());
        }
    }


    public void saveClaimsBatch(List<Claim> claims) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try {
                for (Claim claim : claims) {
                    try (PreparedStatement stmt = conn.prepareStatement(SAVE_CLAIM)) {
                        stmt.setString(1, claim.getClaimId());
                        stmt.setString(2, claim.getDisplayName());
                        stmt.setString(3, claim.getSlug());

                        stmt.setString(4, claim.getChunk().getWorld().getName());
                        stmt.setInt(5, claim.getChunk().getX());
                        stmt.setInt(6, claim.getChunk().getZ());
                        stmt.setTimestamp(7, new Timestamp(claim.getCreatedAt().getTime()));
                        stmt.setTimestamp(8, new Timestamp(claim.getExpiredAt().getTime()));
                        stmt.setString(9, claim.getOwner().toString());
                        stmt.setString(10, NClaim.serializeLocation(claim.getClaimBlockLocation()));
                        stmt.setString(11, gson.toJson(claim.getLands()));
                        stmt.setLong(12, claim.getClaimValue());
                        stmt.setString(13, claim.getClaimBlockType().name());

                        List<String> purchasedBlockNames = claim.getPurchasedBlockTypes().stream()
                                .map(Material::name)
                                .collect(Collectors.toList());
                        stmt.setString(14, gson.toJson(purchasedBlockNames));

                        stmt.setBoolean(15, claim.isForSale());
                        stmt.setDouble(16, claim.getSalePrice());

                        stmt.setTimestamp(17, new Timestamp(claim.getExpiredAt().getTime()));
                        stmt.setString(18, claim.getDisplayName());
                        stmt.setString(19, claim.getSlug());
                        stmt.setString(20, claim.getOwner().toString());
                        stmt.setString(21, NClaim.serializeLocation(claim.getClaimBlockLocation()));
                        stmt.setString(22, gson.toJson(claim.getLands()));
                        stmt.setLong(23, claim.getClaimValue());
                        stmt.setString(24, claim.getClaimBlockType().name());
                        stmt.setString(25, gson.toJson(purchasedBlockNames));
                        stmt.setBoolean(26, claim.isForSale());
                        stmt.setDouble(27, claim.getSalePrice());

                        stmt.executeUpdate();
                    }

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
            Util.log("&cFailed to save claims batch: " + e.getMessage());
        }
    }

    private void saveClaimCoops(Connection conn, Claim claim) throws SQLException {
        try (PreparedStatement deleteStmt = conn.prepareStatement(DELETE_CLAIM_COOPS)) {
            deleteStmt.setString(1, claim.getClaimId());
            deleteStmt.executeUpdate();
        }

        try (PreparedStatement stmt = conn.prepareStatement(SAVE_CLAIM_COOP)) {
            for (UUID coopPlayer : claim.getCoopPlayers()) {
                Map<String, Boolean> permissions = new HashMap<>();
                CoopPermission coopPerm = claim.getCoopPermissions().get(coopPlayer);
                
                for (Permission perm : Permission.values()) {
                    permissions.put(perm.name(), coopPerm.isEnabled(perm));
                }

                stmt.setString(1, claim.getClaimId());
                stmt.setString(2, coopPlayer.toString());
                stmt.setTimestamp(3, new Timestamp(claim.getCoopPlayerJoinDate().get(coopPlayer).getTime()));
                stmt.setString(4, gson.toJson(permissions));
                stmt.setTimestamp(5, new Timestamp(claim.getCoopPlayerJoinDate().get(coopPlayer).getTime()));
                stmt.setString(6, gson.toJson(permissions));
                
                stmt.executeUpdate();
            }
        }
    }

    private void saveClaimSettings(Connection conn, Claim claim) throws SQLException {
        Map<String, Boolean> settings = new HashMap<>();
        for (Setting setting : Setting.values()) {
            settings.put(setting.name(), claim.getSettings().isEnabled(setting));
        }

        try (PreparedStatement stmt = conn.prepareStatement(SAVE_CLAIM_SETTINGS)) {
            stmt.setString(1, claim.getClaimId());
            stmt.setString(2, gson.toJson(settings));
            stmt.setString(3, gson.toJson(settings));
            stmt.executeUpdate();
        }
    }

    public Claim loadClaim(String claimId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(LOAD_CLAIM)) {
            
            stmt.setString(1, claimId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return createClaimFromResultSet(rs, conn);
            }
        } catch (SQLException e) {
            Util.log("&cFailed to load claim: " + e.getMessage());
        }
        return null;
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
            
        }
        return 0;
    }

    public List<Claim> loadAllClaims() {
        List<Claim> claims = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(LOAD_ALL_CLAIMS)) {
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Claim claim = createClaimFromResultSet(rs, conn);
                if (claim != null) {
                    claims.add(claim);
                }
            }
        } catch (SQLException e) {
            Util.log("&cFailed to load claims: " + e.getMessage());
        }
        return claims;
    }

    private Claim createClaimFromResultSet(ResultSet rs, Connection conn) throws SQLException {
        World world = Bukkit.getWorld(rs.getString("chunk_world"));
        if (world == null) return null;

        Chunk chunk = world.getChunkAt(rs.getInt("chunk_x"), rs.getInt("chunk_z"));
        String claimId = rs.getString("claim_id");

        String displayName = null;
        String slug = null;
        try {
            displayName = rs.getString("name");
        } catch (SQLException ignored) {
        }
        try {
            slug = rs.getString("slug");
        } catch (SQLException ignored) {
        }

        Date createdAt = new Date(rs.getTimestamp("created_at").getTime());
        Date expiredAt = new Date(rs.getTimestamp("expired_at").getTime());
        UUID owner = UUID.fromString(rs.getString("owner"));
        Location claimBlockLocation = NClaim.deserializeLocation(rs.getString("claim_block_location"));
        long claimValue = rs.getLong("claim_value");
        Material claimBlockType = Material.valueOf(rs.getString("claim_block_type"));

        boolean forSale = false;
        double salePrice = 0D;
        try {
            forSale = rs.getBoolean("for_sale");
            salePrice = rs.getDouble("sale_price");
        } catch (SQLException ignored) {
        }

        Set<Material> purchasedBlocks = new HashSet<>();
        String purchasedBlocksJson = rs.getString("purchased_blocks");
        if (purchasedBlocksJson != null && !purchasedBlocksJson.isEmpty()) {
            Type blockListType = new TypeToken<List<String>>(){}.getType();
            List<String> blockNames = gson.fromJson(purchasedBlocksJson, blockListType);
            for (String blockName : blockNames) {
                try {
                    purchasedBlocks.add(Material.valueOf(blockName));
                } catch (IllegalArgumentException e) {
                    Util.log("&cInvalid material in purchased blocks for claim " + claimId + ": " + blockName);
                }
            }
        }

        Type landType = new TypeToken<Collection<String>>(){}.getType();
        Collection<String> lands = gson.fromJson(rs.getString("lands"), landType);

        CoopData coopData = loadClaimCoops(conn, claimId);
        ClaimSetting settings = loadClaimSettings(conn, claimId);

        return new Claim(
                claimId,
                displayName,
                slug,
                chunk,
                createdAt,
                expiredAt,
                owner,
                claimBlockLocation,
                claimValue,
                claimBlockType,
                lands,
                coopData.getCoopPlayers(),
                coopData.getJoinDates(),
                coopData.getPermissions(),
                settings,
                purchasedBlocks,
                forSale,
                salePrice
        );
    }

    private CoopData loadClaimCoops(Connection conn, String claimId) throws SQLException {
        CoopData coopData = new CoopData();

        try (PreparedStatement stmt = conn.prepareStatement(LOAD_CLAIM_COOPS)) {
            stmt.setString(1, claimId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                coopData.getCoopPlayers().add(playerUuid);
                coopData.getJoinDates().put(playerUuid, new Date(rs.getTimestamp("joined_at").getTime()));
                
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
        
        try (PreparedStatement stmt = conn.prepareStatement(LOAD_CLAIM_SETTINGS)) {
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

    public void deleteClaim(String claimId) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement coopsStmt = conn.prepareStatement(DELETE_CLAIM_COOPS)) {
                coopsStmt.setString(1, claimId);
                coopsStmt.executeUpdate();
            }

            try (PreparedStatement settingsStmt = conn.prepareStatement(DELETE_CLAIM_SETTINGS)) {
                settingsStmt.setString(1, claimId);
                settingsStmt.executeUpdate();
            }

            try (PreparedStatement claimStmt = conn.prepareStatement(DELETE_CLAIM)) {
                claimStmt.setString(1, claimId);
                claimStmt.executeUpdate();
            }
        } catch (SQLException e) {
            Util.log("&cFailed to delete claim: " + e.getMessage());
        }

    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}