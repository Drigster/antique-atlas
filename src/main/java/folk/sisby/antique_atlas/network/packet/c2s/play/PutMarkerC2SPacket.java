package folk.sisby.antique_atlas.network.packet.c2s.play;

import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.api.AtlasAPI;
import folk.sisby.antique_atlas.network.packet.c2s.C2SPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * A request from a client to create a new marker. In order to prevent griefing,
 * the marker has to be local.
 *
 * @author Hunternif
 * @author Haven King
 */
public class PutMarkerC2SPacket extends C2SPacket {
    public static final Identifier ID = AntiqueAtlas.id("packet.c2s.marker.put");

    public PutMarkerC2SPacket(int atlasID, Identifier markerType, int x, int z, boolean visibleBeforeDiscovery, Text label) {
        this.writeVarInt(atlasID);
        this.writeIdentifier(markerType);
        this.writeVarInt(x);
        this.writeVarInt(z);
        this.writeBoolean(visibleBeforeDiscovery);
        this.writeText(label);
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    public static void apply(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        int atlasID = buf.readVarInt();
        Identifier markerType = buf.readIdentifier();
        int x = buf.readVarInt();
        int z = buf.readVarInt();
        boolean visibleBeforeDiscovery = buf.readBoolean();
        Text label = buf.readText();

        if (AtlasAPI.getPlayerAtlasId(player) != atlasID) {
            AntiqueAtlas.LOG.warn("Player {} attempted to put marker into someone else's Atlas #{}}", player.getName(), atlasID);
            return;
        }

        AtlasAPI.getMarkerAPI().putMarker(player.getEntityWorld(), visibleBeforeDiscovery, atlasID, markerType, label, x, z);
    }
}
