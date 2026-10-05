package namidevelopment.kiriyaga.nami.impl.feature.visuals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static namidevelopment.kiriyaga.api.NamiApi.CONFIG_SERVICE;
import static namidevelopment.kiriyaga.api.NamiApi.LOGGER;
import static namidevelopment.kiriyaga.api.NamiApi.MC;

@RegisterFeature
public class PlayerIntelFeature extends Feature {
    public static final int HEAT_CELL_SIZE = 16;
    private static final int SAMPLE_TICKS = 20;
    private static final long HEAT_INTERVAL_MILLIS = 30_000L;
    private static final long SAVE_INTERVAL_MILLIS = 30_000L;
    private static final int MAX_SIGHTINGS_PER_PLAYER = 128;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path dataFile = CONFIG_SERVICE.getDirectoryProvider().getBaseDir().toPath().resolve("player-intel.json");
    private final IntelData intelData = new IntelData();
    private int ticks;
    private long lastSaveMillis;
    private boolean dirty;

    public PlayerIntelFeature() {
        super("PlayerIntel", "Logs player sightings and saved base locations.", FeatureCategory.of("Render"));
        loadData();
    }

    @Override
    public void onDisable() {
        saveData();
    }

    @SubscribeEvent
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null || MC.level == null || ++ticks % SAMPLE_TICKS != 0) {
            return;
        }

        long now = System.currentTimeMillis();
        WorldIntel world = getCurrentWorldIntel();
        if (world == null) {
            return;
        }

        for (Player player : MC.level.players()) {
            if (player == MC.player || player.isRemoved()) {
                continue;
            }

            String playerName = player.getGameProfile().name();
            int x = player.blockPosition().getX();
            int y = player.blockPosition().getY();
            int z = player.blockPosition().getZ();
            int cellX = Math.floorDiv(x, HEAT_CELL_SIZE);
            int cellZ = Math.floorDiv(z, HEAT_CELL_SIZE);
            String cellKey = cellKey(cellX, cellZ);

            PlayerHistory history = world.players.computeIfAbsent(playerName, ignored -> new PlayerHistory());
            PlayerSighting previous = history.latest();
            if (previous == null
                    || !cellKey.equals(cellKey(Math.floorDiv(previous.x, HEAT_CELL_SIZE), Math.floorDiv(previous.z, HEAT_CELL_SIZE)))
                    || now - previous.timestamp >= HEAT_INTERVAL_MILLIS) {
                history.sightings.add(new PlayerSighting(x, y, z, now));
                if (history.sightings.size() > MAX_SIGHTINGS_PER_PLAYER) {
                    history.sightings.remove(0);
                }
                dirty = true;
            } else {
                previous.timestamp = now;
                previous.x = x;
                previous.y = y;
                previous.z = z;
            }

            HeatCell heatCell = world.heat.computeIfAbsent(cellKey, ignored -> new HeatCell(cellX, cellZ));
            if (now - heatCell.lastVisit >= HEAT_INTERVAL_MILLIS) {
                heatCell.visits++;
                heatCell.lastVisit = now;
                dirty = true;
            }
        }

        if (dirty && now - lastSaveMillis >= SAVE_INTERVAL_MILLIS) {
            saveData();
        }
    }

    public WorldIntel getCurrentWorldIntel() {
        if (MC.level == null) {
            return null;
        }

        ServerData server = MC.getCurrentServer();
        String serverKey = server == null ? "singleplayer" : server.ip.toLowerCase(Locale.ROOT);
        String dimension = MC.level.dimension().toString();
        String worldKey = serverKey + "|" + dimension;

        return intelData.worlds.computeIfAbsent(worldKey, ignored -> new WorldIntel());
    }

    public boolean addBase(String name) {
        if (MC.player == null) {
            return false;
        }
        WorldIntel world = getCurrentWorldIntel();
        if (world == null) {
            return false;
        }

        String normalizedName = name.trim();
        if (normalizedName.isEmpty()) {
            return false;
        }
        world.bases.put(normalizedName.toLowerCase(Locale.ROOT),
                new BaseMarker(normalizedName, MC.player.blockPosition().getX(),
                        MC.player.blockPosition().getY(), MC.player.blockPosition().getZ()));
        saveData();
        return true;
    }

    public boolean removeBase(String name) {
        WorldIntel world = getCurrentWorldIntel();
        if (world == null || world.bases.remove(name.trim().toLowerCase(Locale.ROOT)) == null) {
            return false;
        }
        saveData();
        return true;
    }

    public void saveData() {
        try {
            Files.createDirectories(dataFile.getParent());
            Files.writeString(dataFile, gson.toJson(intelData), StandardCharsets.UTF_8);
            dirty = false;
            lastSaveMillis = System.currentTimeMillis();
        } catch (IOException e) {
            LOGGER.error("Failed to save player intel data to " + dataFile, e);
        }
    }

    private void loadData() {
        if (!Files.exists(dataFile)) {
            return;
        }

        try {
            IntelData loaded = gson.fromJson(Files.readString(dataFile, StandardCharsets.UTF_8), IntelData.class);
            if (loaded != null && loaded.worlds != null) {
                intelData.worlds.putAll(loaded.worlds);
                intelData.worlds.values().forEach(WorldIntel::ensureCollections);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to load player intel data from " + dataFile, e);
        }
    }

    private static String cellKey(int cellX, int cellZ) {
        return cellX + "," + cellZ;
    }

    public static class WorldIntel {
        public Map<String, PlayerHistory> players = new HashMap<>();
        public Map<String, HeatCell> heat = new HashMap<>();
        public Map<String, BaseMarker> bases = new LinkedHashMap<>();

        private void ensureCollections() {
            if (players == null) players = new HashMap<>();
            if (heat == null) heat = new HashMap<>();
            if (bases == null) bases = new LinkedHashMap<>();
            players.values().forEach(history -> {
                if (history.sightings == null) history.sightings = new ArrayList<>();
            });
        }
    }

    public static class PlayerHistory {
        public List<PlayerSighting> sightings = new ArrayList<>();

        public PlayerSighting latest() {
            return sightings.isEmpty() ? null : sightings.get(sightings.size() - 1);
        }
    }

    public static class PlayerSighting {
        public int x;
        public int y;
        public int z;
        public long timestamp;

        public PlayerSighting(int x, int y, int z, long timestamp) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.timestamp = timestamp;
        }
    }

    public static class HeatCell {
        public int cellX;
        public int cellZ;
        public int visits;
        public long lastVisit;

        public HeatCell(int cellX, int cellZ) {
            this.cellX = cellX;
            this.cellZ = cellZ;
        }
    }

    public static class BaseMarker {
        public String name;
        public int x;
        public int y;
        public int z;
        public long addedAt;

        public BaseMarker(String name, int x, int y, int z) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.addedAt = System.currentTimeMillis();
        }
    }

    private static class IntelData {
        public Map<String, WorldIntel> worlds = new HashMap<>();
    }
}
