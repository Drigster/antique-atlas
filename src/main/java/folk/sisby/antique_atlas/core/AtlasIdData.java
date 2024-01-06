package folk.sisby.antique_atlas.core;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.world.PersistentState;

/**
 * This class is used to store the next free ID for a new atlas
 */
public class AtlasIdData extends PersistentState {
    public static final String TAG_NEXT_ID = "aaNextID";
    private int nextId = 1;

    public AtlasIdData() {
    }

    public int getNextAtlasId() {
        int id = nextId++;
        markDirty();
        return id;
    }

    public static AtlasIdData fromNbt(NbtCompound compound) {
        AtlasIdData data = new AtlasIdData();
        if (compound.contains(TAG_NEXT_ID, NbtElement.NUMBER_TYPE)) {
            data.nextId = compound.getInt(TAG_NEXT_ID);
        } else {
            data.nextId = 1;
        }
        return data;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound compound) {
        compound.putInt(TAG_NEXT_ID, nextId);
        return compound;
    }
}
