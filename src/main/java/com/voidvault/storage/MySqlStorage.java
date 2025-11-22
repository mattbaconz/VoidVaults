package com.voidvault.storage;

import com.voidvault.model.PlayerVaultData;
import com.voidvault.model.VaultPage;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MySQL-based storage implementation for vault data.
 * Uses HikariCP for efficient connection pooling and supports async operations.
 * <p>
 * Tables:
 * - voidvault_players: Stores player metadata (custom slots/pages)
 * - voidvault_items: Stores individual items with page and slot information
 */
public class MySqlStorage implements StorageManager {

    private final Plugin plugin;
    private final Logger logger;
    private final DataCache dataCache;
    private final ExecutorService asyncExecutor;
    private HikariDataSource dataSource;

    // Connection retry configuration
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000;

    /**
     * Creates a new MySqlStorage instance.
     *
     * @param plugin    The plugin instance
     * @param dataCache The data cache for managing in-memory data
     */
    public MySqlStorage(Plugin plugin, DataCache dataCache) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dataCache = dataCache;
        this.asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                setupConnectionPool();
                createTables();
                logger.info("MySQL storage initialized successfully");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to initialize MySQL storage", e);
                throw new RuntimeException("Failed to initialize MySQL storage", e);
            }
        }, asyncExecutor);
    }

    /**
     * Sets up the HikariCP connection pool from configuration.
     */
    private void setupConnectionPool() {
        FileConfiguration config = plugin.getConfig();
        
        String host = config.getString("mysql.host", "localhost");
        int port = config.getInt("mysql.port", 3306);
        String database = config.getString("mysql.database", "voidvault");
        String username = config.getString("mysql.username", "root");
        String password = config.getString("mysql.password", "password");
        int poolSize = config.getInt("mysql.pool-size", 10);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(poolSize);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setPoolName("VoidVault-Pool");
        
        // MySQL-specific optimizations
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");

        this.dataSource = new HikariDataSource(hikariConfig);
        logger.info("HikariCP connection pool established");
    }

    /**
     * Creates the necessary database tables if they don't exist.
     */
    private void createTables() throws SQLException {
        String createPlayersTable = """
            CREATE TABLE IF NOT EXISTS voidvault_players (
                player_id VARCHAR(36) PRIMARY KEY,
                custom_slots INT DEFAULT 0,
                custom_pages INT DEFAULT 0,
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

        String createItemsTable = """
            CREATE TABLE IF NOT EXISTS voidvault_items (
                id INT AUTO_INCREMENT PRIMARY KEY,
                player_id VARCHAR(36) NOT NULL,
                page_number INT NOT NULL,
                slot_number INT NOT NULL,
                item_data MEDIUMBLOB NOT NULL,
                FOREIGN KEY (player_id) REFERENCES voidvault_players(player_id) ON DELETE CASCADE,
                UNIQUE KEY unique_slot (player_id, page_number, slot_number),
                INDEX idx_player_page (player_id, page_number)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createPlayersTable);
            stmt.execute(createItemsTable);
            logger.info("Database tables created/verified");
        }
    }

    @Override
    public CompletableFuture<PlayerVaultData> loadPlayerData(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            return executeWithRetry(() -> loadPlayerDataInternal(playerId), "load data for " + playerId);
        }, asyncExecutor);
    }

    /**
     * Internal method to load player data from the database.
     */
    private PlayerVaultData loadPlayerDataInternal(UUID playerId) throws SQLException {
        try (Connection conn = getConnection()) {
            // Load player metadata
            int customSlots = 0;
            int customPages = 0;
            
            String selectPlayer = "SELECT custom_slots, custom_pages FROM voidvault_players WHERE player_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(selectPlayer)) {
                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        customSlots = rs.getInt("custom_slots");
                        customPages = rs.getInt("custom_pages");
                    } else {
                        // Player doesn't exist yet, return empty data
                        logger.fine("No database record found for player " + playerId + ", creating empty data");
                        return PlayerVaultData.createEmpty(playerId);
                    }
                }
            }

            // Load items grouped by page
            Map<Integer, VaultPage> pages = new HashMap<>();
            String selectItems = "SELECT page_number, slot_number, item_data FROM voidvault_items WHERE player_id = ? ORDER BY page_number, slot_number";
            
            try (PreparedStatement stmt = conn.prepareStatement(selectItems)) {
                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    Map<Integer, Map<Integer, ItemStack>> pageItems = new HashMap<>();
                    
                    while (rs.next()) {
                        int pageNumber = rs.getInt("page_number");
                        int slotNumber = rs.getInt("slot_number");
                        byte[] itemData = rs.getBytes("item_data");
                        
                        try {
                            ItemStack item = deserializeItemStack(itemData);
                            pageItems.computeIfAbsent(pageNumber, k -> new HashMap<>())
                                    .put(slotNumber, item);
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Failed to deserialize item for player " + playerId + 
                                    " page " + pageNumber + " slot " + slotNumber, e);
                        }
                    }
                    
                    // Convert to VaultPage objects
                    for (Map.Entry<Integer, Map<Integer, ItemStack>> entry : pageItems.entrySet()) {
                        int pageNumber = entry.getKey();
                        Map<Integer, ItemStack> items = entry.getValue();
                        
                        ItemStack[] contents = new ItemStack[50];
                        for (Map.Entry<Integer, ItemStack> itemEntry : items.entrySet()) {
                            int slot = itemEntry.getKey();
                            if (slot >= 0 && slot < contents.length) {
                                contents[slot] = itemEntry.getValue();
                            }
                        }
                        
                        pages.put(pageNumber, new VaultPage(pageNumber, contents));
                    }
                }
            }

            logger.fine("Loaded data for player " + playerId + " with " + pages.size() + " pages");
            return new PlayerVaultData(playerId, pages, customSlots, customPages);
        }
    }

    @Override
    public CompletableFuture<Void> savePlayerData(UUID playerId, PlayerVaultData data) {
        return CompletableFuture.runAsync(() -> {
            executeWithRetry(() -> {
                savePlayerDataInternal(playerId, data);
                return null;
            }, "save data for " + playerId);
        }, asyncExecutor);
    }

    /**
     * Internal method to save player data to the database.
     * Uses REPLACE INTO for items to avoid DELETE + INSERT overhead.
     */
    private void savePlayerDataInternal(UUID playerId, PlayerVaultData data) throws SQLException, IOException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Upsert player metadata
                String upsertPlayer = """
                    INSERT INTO voidvault_players (player_id, custom_slots, custom_pages)
                    VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE custom_slots = VALUES(custom_slots), custom_pages = VALUES(custom_pages)
                    """;
                
                try (PreparedStatement stmt = conn.prepareStatement(upsertPlayer)) {
                    stmt.setString(1, playerId.toString());
                    stmt.setInt(2, data.customSlots());
                    stmt.setInt(3, data.customPages());
                    stmt.executeUpdate();
                }

                // Delete items that no longer exist (slots that are now empty)
                // First, get all existing item slots
                Set<String> existingSlots = new HashSet<>();
                String selectExisting = "SELECT page_number, slot_number FROM voidvault_items WHERE player_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(selectExisting)) {
                    stmt.setString(1, playerId.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            existingSlots.add(rs.getInt("page_number") + ":" + rs.getInt("slot_number"));
                        }
                    }
                }

                // Build set of current item slots
                Set<String> currentSlots = new HashSet<>();
                for (Map.Entry<Integer, VaultPage> pageEntry : data.pages().entrySet()) {
                    int pageNumber = pageEntry.getKey();
                    VaultPage page = pageEntry.getValue();
                    for (int slot = 0; slot < page.getSize(); slot++) {
                        if (page.getItem(slot) != null) {
                            currentSlots.add(pageNumber + ":" + slot);
                        }
                    }
                }

                // Delete slots that no longer have items
                existingSlots.removeAll(currentSlots);
                if (!existingSlots.isEmpty()) {
                    String deleteItem = "DELETE FROM voidvault_items WHERE player_id = ? AND page_number = ? AND slot_number = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(deleteItem)) {
                        for (String slotKey : existingSlots) {
                            String[] parts = slotKey.split(":");
                            stmt.setString(1, playerId.toString());
                            stmt.setInt(2, Integer.parseInt(parts[0]));
                            stmt.setInt(3, Integer.parseInt(parts[1]));
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }

                // Upsert all current items
                String upsertItem = """
                    INSERT INTO voidvault_items (player_id, page_number, slot_number, item_data)
                    VALUES (?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE item_data = VALUES(item_data)
                    """;
                try (PreparedStatement stmt = conn.prepareStatement(upsertItem)) {
                    int batchCount = 0;
                    for (Map.Entry<Integer, VaultPage> pageEntry : data.pages().entrySet()) {
                        int pageNumber = pageEntry.getKey();
                        VaultPage page = pageEntry.getValue();
                        
                        for (int slot = 0; slot < page.getSize(); slot++) {
                            ItemStack item = page.getItem(slot);
                            if (item != null) {
                                stmt.setString(1, playerId.toString());
                                stmt.setInt(2, pageNumber);
                                stmt.setInt(3, slot);
                                stmt.setBytes(4, serializeItemStack(item));
                                stmt.addBatch();
                                batchCount++;
                                
                                // Execute batch every 100 items to avoid memory issues
                                if (batchCount % 100 == 0) {
                                    stmt.executeBatch();
                                }
                            }
                        }
                    }
                    // Execute remaining batch
                    if (batchCount % 100 != 0) {
                        stmt.executeBatch();
                    }
                }

                conn.commit();
                logger.fine("Saved data for player " + playerId);
                
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public CompletableFuture<Void> saveAll() {
        return CompletableFuture.runAsync(() -> {
            Set<UUID> dirtyPlayers = dataCache.getDirtyPlayers();
            logger.info("Saving " + dirtyPlayers.size() + " dirty player(s) to MySQL");
            
            List<CompletableFuture<Void>> saveFutures = new ArrayList<>();
            
            for (UUID playerId : dirtyPlayers) {
                dataCache.get(playerId).ifPresent(data -> {
                    CompletableFuture<Void> saveFuture = savePlayerData(playerId, data)
                            .thenRun(() -> dataCache.clearDirty(playerId))
                            .exceptionally(ex -> {
                                logger.log(Level.SEVERE, "Failed to save data for player " + playerId, ex);
                                return null;
                            });
                    saveFutures.add(saveFuture);
                });
            }
            
            // Wait for all saves to complete
            CompletableFuture.allOf(saveFutures.toArray(new CompletableFuture[0])).join();
            logger.info("Completed saving all dirty players to MySQL");
            
        }, asyncExecutor);
    }

    @Override
    public void close() {
        logger.info("Closing MySQL storage...");
        
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("HikariCP connection pool closed");
        }
        
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Gets a connection from the pool.
     */
    private Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Data source is not available");
        }
        return dataSource.getConnection();
    }

    /**
     * Executes a database operation with exponential backoff retry logic.
     */
    private <T> T executeWithRetry(SQLOperation<T> operation, String operationName) {
        int attempt = 0;
        long delay = INITIAL_RETRY_DELAY_MS;
        
        while (attempt < MAX_RETRIES) {
            try {
                return operation.execute();
            } catch (SQLException | IOException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    logger.log(Level.SEVERE, "Failed to " + operationName + " after " + MAX_RETRIES + " attempts", e);
                    throw new RuntimeException("Database operation failed: " + operationName, e);
                }
                
                logger.warning("Failed to " + operationName + " (attempt " + attempt + "/" + MAX_RETRIES + "), retrying in " + delay + "ms");
                
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
                
                delay *= 2; // Exponential backoff
            }
        }
        
        throw new RuntimeException("Unexpected error in retry logic");
    }

    /**
     * Serializes an ItemStack to a byte array.
     */
    private byte[] serializeItemStack(ItemStack item) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            
            dataOutput.writeObject(item);
            return outputStream.toByteArray();
        }
    }

    /**
     * Deserializes an ItemStack from a byte array.
     */
    private ItemStack deserializeItemStack(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            
            return (ItemStack) dataInput.readObject();
        }
    }

    /**
     * Functional interface for SQL operations that can throw SQLException or IOException.
     */
    @FunctionalInterface
    private interface SQLOperation<T> {
        T execute() throws SQLException, IOException;
    }
}
