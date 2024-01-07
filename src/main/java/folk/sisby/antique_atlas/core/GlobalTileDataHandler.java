package folk.sisby.antique_atlas.core;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used to store tiles, which are shared between ALL atlases
 * Also, this data overwrites the result of ITileDetector instances.
 * <p>
 * Use this class for world gen structures and other important but unique tiles.
 */
public class GlobalTileDataHandler {
    private static final String DATA_KEY = "aAtlasTiles";

    private final Map<RegistryKey<World>, TileDataStorage> globalTileData =
        new ConcurrentHashMap<>(2, 0.75f, 2);

    public void onWorldLoad(MinecraftServer server, ServerWorld world) {
        globalTileData.put(world.getRegistryKey(), world.getPersistentStateManager().getOrCreate(TileDataStorage::readNbt, () -> {
            TileDataStorage data = new TileDataStorage();
            data.markDirty();
            return data;
        }, DATA_KEY));
    }

    public TileDataStorage getData(World world) {
        return getData(world.getRegistryKey());
    }

    public TileDataStorage getData(RegistryKey<World> world) {
        return globalTileData.computeIfAbsent(world,
            k -> new TileDataStorage());
    }

    public void onPlayerLogin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        globalTileData.forEach((world, tileData) -> tileData.syncToPlayer(handler.getPlayer(), world));
    }

}
