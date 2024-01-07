package folk.sisby.antique_atlas.network.packet.c2s.play;

import dev.architectury.networking.NetworkManager;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.api.AtlasAPI;
import folk.sisby.antique_atlas.network.packet.c2s.C2SPacket;
import folk.sisby.antique_atlas.util.Log;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Deletes a marker. A client sends this packet to the server as a request,
 * and the server sends to all players as a response, including the
 * original sender.
 * @author Hunternif
 */
public class DeleteMarkerC2SPacket extends C2SPacket {
	public static final Identifier ID = AntiqueAtlas.id("packet", "c2s", "marker", "delete");

	private static final int GLOBAL = -1;

	public DeleteMarkerC2SPacket(int atlasID, int markerID) {
		this.writeVarInt(atlasID);
		this.writeVarInt(markerID);
	}

	@Override
	public Identifier getId() {
		return ID;
	}
	public static void apply(PacketByteBuf buf, NetworkManager.PacketContext context) {
		int atlasID = buf.readVarInt();
		int markerID = buf.readVarInt();

		context.queue(() -> {
			if (AtlasAPI.getPlayerAtlasId(context.getPlayer()) != atlasID) {
				Log.warn("Player %s attempted to delete marker from someone else's Atlas #%d",
						context.getPlayer().getName(), atlasID);
				return;
			}

			AtlasAPI.getMarkerAPI().deleteMarker(context.getPlayer().getEntityWorld(), atlasID, markerID);
		});
	}
}
