package com.example.voicechair2;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.List;

/** Findet alle Stuehle und Fliesen im Umkreis und dreht sie gemeinsam. */
public final class ChairNetwork {
	/** Suchradius horizontal und vertikal in Bloecken. Bei Bedarf vergroessern. */
	public static final int RADIUS = 48;
	public static final int HEIGHT = 16;

	private ChairNetwork() {}

	/**
	 * reset=false: alle noch nicht umgedrehten Stuehle drehen sich um (rot, ohne Namen).
	 *              Stuehle, die schon gebuzzert haben, bleiben weiss mit Namen.
	 * reset=true:  alle Stuehle zurueck auf rot, Namen weg.
	 */
	public static int turnAll(ServerLevel level, BlockPos center, boolean reset) {
		int changed = 0;
		List<BlockPos> tiles = new ArrayList<>();
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dy = -HEIGHT; dy <= HEIGHT; dy++) {
				for (int dz = -RADIUS; dz <= RADIUS; dz++) {
					p.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
					if (!level.hasChunkAt(p)) continue;
					BlockState state = level.getBlockState(p);
					if (state.getBlock() instanceof VoiceChairBlock) {
						if (state.getValue(VoiceChairBlock.HALF) != DoubleBlockHalf.LOWER) continue;
						boolean did = reset
							? VoiceChairBlock.apply(level, p.immutable(), state, false, false, false)
							: VoiceChairBlock.apply(level, p.immutable(), state, true, state.getValue(VoiceChairBlock.BUZZED), false);
						if (did) changed++;
					} else if (state.getBlock() instanceof NameTileBlock) {
						tiles.add(p.immutable());
					}
				}
			}
		}
		for (BlockPos tile : tiles) {
			NameTileBlock.refresh(level, tile);
		}
		return changed;
	}
}
