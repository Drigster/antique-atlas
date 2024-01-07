package folk.sisby.antique_atlas.network.packet.s2c.play;

import dev.architectury.networking.NetworkManager;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.AntiqueAtlasClient;
import folk.sisby.antique_atlas.marker.MarkersData;
import folk.sisby.antique_atlas.network.packet.c2s.play.DeleteMarkerC2SPacket;
import folk.sisby.antique_atlas.network.packet.s2c.S2CPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Deletes a marker. A client sends a {@link DeleteMarkerC2SPacket}
 * to the server as a request, and the server sends this back to all players as a response, including the
 * original sender.
 * @author Hunternif
 * @author Haven King
 */
public class DeleteMarkerS2CPacket extends S2CPacket {
	public static final Identifier ID = AntiqueAtlas.id("packet", "s2c", "marker", "delete");

	private static final int GLOBAL = -1;

	public DeleteMarkerS2CPacket(int atlasID, int markerID) {
		this.writeVarInt(atlasID);
		this.writeVarInt(markerID);
	}

	@Override
	public Identifier getId() {
		return ID;
	}

	@Environment(EnvType.CLIENT)
	public static void apply(PacketByteBuf buf, NetworkManager.PacketContext context) {
		int atlasID = buf.readVarInt();
		int markerID = buf.readVarInt();

		context.queue(() -> {
			MarkersData data = atlasID == GLOBAL ?
					AntiqueAtlas.globalMarkersData.getData() :
					AntiqueAtlas.markersData.getMarkersData(atlasID, context.getPlayer().getEntityWorld());
			data.removeMarker(markerID);

			AntiqueAtlasClient.getAtlasGUI().updateBookmarkerList();
		});
	}
}
