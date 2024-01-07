package folk.sisby.antique_atlas.network.packet.c2s.play;

import dev.architectury.networking.NetworkManager;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.api.AtlasAPI;
import folk.sisby.antique_atlas.network.packet.c2s.C2SPacket;
import folk.sisby.antique_atlas.util.Log;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Puts biome tile into one atlas. When sent to server, forwards it to every
 * client that has this atlas' data synced.
 * @author Hunternif
 * @author Haven King
 */
public class PutTileC2SPacket extends C2SPacket {
	public static final Identifier ID = AntiqueAtlas.id("packet", "c2s", "tile", "put");

	public PutTileC2SPacket(int atlasID, int x, int z, Identifier tile) {
		this.writeInt(atlasID);
		this.writeVarInt(x);
		this.writeVarInt(z);
		this.writeIdentifier(tile);
	}

	@Override
	public Identifier getId() {
		return ID;
	}

	public static void apply(PacketByteBuf buf, NetworkManager.PacketContext context) {
		int atlasID = buf.readVarInt();
		int x = buf.readVarInt();
		int z = buf.readVarInt();
		Identifier tile = buf.readIdentifier();

		context.queue(() -> {
			if (AtlasAPI.getPlayerAtlasId(context.getPlayer()) != atlasID) {
				Log.warn("Player %s attempted to modify someone else's Atlas #%d",
						context.getPlayer().getName(), atlasID);
				return;
			}

			AtlasAPI.getTileAPI().putTile(context.getPlayer().getEntityWorld(), atlasID, tile, x, z);
		});
	}
}
