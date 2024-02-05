package folk.sisby.antique_atlas.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.api.AtlasAPI;
import folk.sisby.antique_atlas.structure.StructurePieceTile;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.JigsawJunction;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.pool.SinglePoolElement;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StructureTiles extends JsonDataLoader implements IdentifiableResourceReloadListener {
    private static final StructureTiles INSTANCE = new StructureTiles();

    public static final Identifier ID = AntiqueAtlas.id("structures");
    public static final int VERSION = 2;

    public static StructureTiles getInstance() {
        return INSTANCE;
    }

    public static Collection<ChunkPos> ALWAYS(World world, BlockBox box) {
        return Collections.singleton(new ChunkPos(box.getCenter()));
    }

    public interface PieceMatcher {
        Collection<ChunkPos> matches(World world, BlockBox box);
    }

    public static Optional<ChunkPos> chunkPosIfX(StructurePiece piece) {
        if (piece instanceof PoolStructurePiece poolPiece) {
            List<JigsawJunction> junctions = poolPiece.getJunctions();
            if (junctions.size() == 2) {
                if (junctions.get(0).getSourceX() == junctions.get(1).getSourceX() || junctions.get(0).getSourceZ() != junctions.get(1).getSourceZ()) {
                    return Optional.of(new ChunkPos(piece.getBoundingBox().getCenter()));
                }
            } else {
                return Optional.of(new ChunkPos(piece.getBoundingBox().getCenter()));
            }
        }
        return Optional.empty();
    }

    public static Optional<ChunkPos> chunkPosIfZ(StructurePiece piece) {
        if (piece instanceof PoolStructurePiece poolPiece) {
            List<JigsawJunction> junctions = poolPiece.getJunctions();
            if (junctions.size() == 2) {
                if (junctions.get(0).getSourceZ() == junctions.get(1).getSourceZ() || junctions.get(0).getSourceX() != junctions.get(1).getSourceX()) {
                    return Optional.of(new ChunkPos(piece.getBoundingBox().getCenter()));
                }
            } else {
                return Optional.of(new ChunkPos(piece.getBoundingBox().getCenter()));
            }
        }
        return Optional.empty();
    }

    private final Map<Identifier, Pair<Identifier, Text>> structureMarkers = new HashMap<>(); // Structure Start
    private final Map<TagKey<Structure>, Pair<Identifier, Text>> structureTagMarkers = new HashMap<>(); // Structure Start

    private final Multimap<Identifier, Pair<Identifier, PieceMatcher>> structurePieceTiles = HashMultimap.create();
    private final Multimap<Identifier, StructurePieceTile> jigsawTiles = HashMultimap.create();

    private final Map<Identifier, Integer> structurePiecePriority = new HashMap<>();

    private final Set<Triple<Integer, Integer, Identifier>> VISITED_STRUCTURES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public StructureTiles() {
        super(new Gson(), "atlas/structures");
    }

    public void registerTile(StructurePieceType structurePieceType, int priority, Identifier textureId, PieceMatcher pieceMatcher) {
        Identifier id = Registries.STRUCTURE_PIECE.getId(structurePieceType);
        structurePieceTiles.put(id, new Pair<>(textureId, pieceMatcher));
        structurePiecePriority.put(textureId, priority);
    }

    public void registerJigsawTile(Identifier id, StructurePieceTile tile) {
        jigsawTiles.put(id, tile);
        structurePiecePriority.put(tile.tileX(), tile.priority());
        structurePiecePriority.put(tile.tileZ(), tile.priority());
    }

    public void registerMarker(StructureType<?> structureFeature, Identifier markerType, Text name) {
        structureMarkers.put(Registries.STRUCTURE_TYPE.getId(structureFeature), new Pair<>(markerType, name));
    }

    public void registerMarker(TagKey<Structure> structureTag, Identifier markerType, Text name) {
        structureTagMarkers.put(structureTag, new Pair<>(markerType, name));
    }

    public void resolve(StructurePiece structurePiece, ServerWorld world) {
        if (structurePiece.getType() == StructurePieceType.JIGSAW) {
            if (structurePiece instanceof PoolStructurePiece pool && pool.getPoolElement() instanceof SinglePoolElement element) {
                Optional<Identifier> jigsawId = element.location.left();
                if (jigsawId.isPresent()) {
                    for (StructurePieceTile pieceTile : jigsawTiles.get(jigsawId.get())) {
                        chunkPosIfX(structurePiece).ifPresent(pos -> put(world, pos.x, pos.z, pieceTile.tileX()));
                        chunkPosIfZ(structurePiece).ifPresent(pos -> put(world, pos.x, pos.z, pieceTile.tileZ()));
                    }
                }
            }
            return;
        }

        Identifier structurePieceId = Registries.STRUCTURE_PIECE.getId(structurePiece.getType());
        if (structurePieceTiles.containsKey(structurePieceId)) {
            for (Pair<Identifier, PieceMatcher> entry : structurePieceTiles.get(structurePieceId)) {
                for (ChunkPos pos : entry.getRight().matches(world, structurePiece.getBoundingBox())) {
                    put(world, pos.x, pos.z, entry.getLeft());
                }
            }
        }
    }

    public void resolve(StructureStart structureStart, ServerWorld world) {
        Identifier structureId = Registries.STRUCTURE_TYPE.getId(structureStart.getStructure().getType());

        Pair<Identifier, Text> foundMarker = null;

        if (structureMarkers.containsKey(structureId)) {
            foundMarker = structureMarkers.get(structureId);
        } else {
            Registry<Structure> structureRegistry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
            RegistryEntry<Structure> structureTag = structureRegistry.entryOf(structureRegistry.getKey(structureStart.getStructure()).orElse(null));
            for (Map.Entry<TagKey<Structure>, Pair<Identifier, Text>> entry : structureTagMarkers.entrySet()) {
                if (structureTag.isIn(entry.getKey())) {
                    foundMarker = entry.getValue();
                    break;
                }
            }
        }

        if (foundMarker != null) {
            Triple<Integer, Integer, Identifier> key = Triple.of(
                structureStart.getBoundingBox().getCenter().getX(),
                structureStart.getBoundingBox().getCenter().getY(),
                structureId);

            if (VISITED_STRUCTURES.contains(key)) return;
            VISITED_STRUCTURES.add(key);

            AtlasAPI.getMarkerAPI().putGlobalMarker(
                world,
                false,
                foundMarker.getLeft(),
                foundMarker.getRight(),
                structureStart.getBoundingBox().getCenter().getX(),
                structureStart.getBoundingBox().getCenter().getZ()
            );
        }
    }

    private int getPriority(Identifier structurePieceId) {
        return structurePiecePriority.getOrDefault(structurePieceId, Integer.MAX_VALUE);
    }

    private void put(World world, int chunkX, int chunkZ, Identifier textureId) {
        Identifier existingTile = AtlasAPI.getTileAPI().getGlobalTile(world, chunkX, chunkZ);

        if (getPriority(textureId) < getPriority(existingTile)) {
            AtlasAPI.getTileAPI().putGlobalTile(world, textureId, chunkX, chunkZ);
        }
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        Map<Identifier, StructurePieceTile> outMap = new HashMap<>();
        for (Map.Entry<Identifier, JsonElement> fileEntry : prepared.entrySet()) {
            Identifier fileId = fileEntry.getKey();
            try {
                JsonObject fileJson = fileEntry.getValue().getAsJsonObject();

                StructurePieceTile structurePieceTile;
                int version = fileJson.getAsJsonPrimitive("version").getAsInt();
                if (version == 1) {
                    structurePieceTile = new StructurePieceTile(
                        Identifier.tryParse(fileJson.get("tile").getAsString()),
                        Identifier.tryParse(fileJson.get("tile").getAsString()),
                        fileJson.get("priority").getAsInt()
                    );
                } else if (version == VERSION) {
                    structurePieceTile = new StructurePieceTile(
                        Identifier.tryParse(fileJson.get("tile_x").getAsString()),
                        Identifier.tryParse(fileJson.get("tile_z").getAsString()),
                        fileJson.get("priority").getAsInt()
                    );
                } else {
                    throw new RuntimeException("Incompatible version (" + VERSION + " != " + version + ")");
                }

                outMap.put(fileId, structurePieceTile);
            } catch (Exception e) {
                AntiqueAtlas.LOG.warn("Error reading structure piece config from " + fileId + "!", e);
            }
        }

        outMap.forEach(this::registerJigsawTile);
    }

    @Override
    public Identifier getFabricId() {
        return ID;
    }
}
