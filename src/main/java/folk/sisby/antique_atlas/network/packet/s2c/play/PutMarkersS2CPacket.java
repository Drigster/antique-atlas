package folk.sisby.antique_atlas.network.packet.s2c.play;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import dev.architectury.networking.NetworkManager;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.AntiqueAtlasClient;
import folk.sisby.antique_atlas.marker.Marker;
import folk.sisby.antique_atlas.marker.MarkersData;
import folk.sisby.antique_atlas.network.packet.s2c.S2CPacket;
import folk.sisby.antique_atlas.registry.MarkerType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.List;

/**
 * Sends markers set via API from server to client.
 * Only one dimension per packet.
 * The markers in one packet are either all global or all local.
 *
 * @author Hunternif
 * @author Haven King
 */
public class PutMarkersS2CPacket extends S2CPacket {
    public static final Identifier ID = AntiqueAtlas.id("packet", "s2c", "marker", "put");

    private static final int GLOBAL = -1;

    public PutMarkersS2CPacket(int atlasID, RegistryKey<World> world, Collection<Marker> markers) {
        ListMultimap<Identifier, Marker> markersByType = ArrayListMultimap.create();
        for (Marker marker : markers) {
            markersByType.put(marker.getType(), marker);
        }

        this.writeVarInt(atlasID);
        this.writeIdentifier(world.getValue());
        this.writeVarInt(markersByType.keySet().size());

        for (Identifier type : markersByType.keySet()) {
            this.writeIdentifier(type);
            List<Marker> markerList = markersByType.get(type);
            this.writeVarInt(markerList.size());
            for (Marker marker : markerList) {
                marker.write(this);
            }
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @Environment(EnvType.CLIENT)
    public static void apply(PacketByteBuf buf, NetworkManager.PacketContext context) {
        int atlasID = buf.readVarInt();
        RegistryKey<World> world = RegistryKey.of(Registry.WORLD_KEY, buf.readIdentifier());
        int typesLength = buf.readVarInt();

        ListMultimap<Identifier, Marker.Precursor> markersByType = ArrayListMultimap.create();
        for (int i = 0; i < typesLength; ++i) {
            Identifier type = buf.readIdentifier();
            int markersLength = buf.readVarInt();
            for (int j = 0; j < markersLength; ++j) {
                markersByType.put(type, new Marker.Precursor(buf));
            }
        }

        context.queue(() -> {
            MarkersData markersData = atlasID == GLOBAL
                    ? AntiqueAtlas.globalMarkersData.getData()
                    : AntiqueAtlas.markersData.getMarkersDataCached(atlasID, world);

            for (Identifier type : markersByType.keys()) {
                MarkerType markerType = MarkerType.REGISTRY.get(type);
                for (Marker.Precursor precursor : markersByType.get(type)) {
                    markersData.loadMarker(new Marker(MarkerType.REGISTRY.getId(markerType), world, precursor));
                }
            }

            AntiqueAtlasClient.getAtlasGUI().updateBookmarkerList();
        });
    }
}
