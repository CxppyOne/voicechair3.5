package com.example.voicechair2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Ordnet Coach-Buzzer und Namensfliesen einem Stuhl zu.
 * Sie gehoeren zu dem Stuhl, vor dem sie auf der "umgedrehten Seite" stehen,
 * also auf der Seite, zu der der Stuhl schaut, nachdem er sich umgedreht hat.
 * Das ist die Seite, auf der man beim Platzieren des Stuhls stand.
 */
public final class ChairLinks {
	/** Maximaler Abstand vor dem Stuhl in Bloecken. */
	public static final int REACH = 3;

	private ChairLinks() {}

	/** Richtung, in die der Stuhl schaut, wenn er umgedreht ist. Aendert sich beim Drehen nicht. */
	public static Direction stageDirection(BlockState chair) {
		Direction facing = chair.getValue(VoiceChairBlock.FACING);
		return chair.getValue(VoiceChairBlock.TURNED) ? facing : facing.getOpposite();
	}

	/** Steht "other" auf der umgedrehten Seite vor dem Stuhl (bis REACH Bloecke, hoechstens 1 Block seitlich)? */
	public static boolean isInFront(BlockPos chairPos, BlockState chair, BlockPos other) {
		Direction dir = stageDirection(chair);
		int dx = other.getX() - chairPos.getX();
		int dy = other.getY() - chairPos.getY();
		int dz = other.getZ() - chairPos.getZ();
		int along = dx * dir.getStepX() + dz * dir.getStepZ();
		int lateral = Math.abs(dz * dir.getStepX() - dx * dir.getStepZ());
		return along >= 1 && along <= REACH && lateral <= 1 && Math.abs(dy) <= 2;
	}

	/** Findet die untere Haelfte des Stuhls, zu dem die Position gehoert, oder null. */
	public static BlockPos findChairFor(ServerLevel level, BlockPos pos) {
		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int dx = -REACH; dx <= REACH; dx++) {
			for (int dy = -2; dy <= 2; dy++) {
				for (int dz = -REACH; dz <= REACH; dz++) {
					p.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
					BlockState s = level.getBlockState(p);
					if (s.getBlock() instanceof VoiceChairBlock
						&& s.getValue(VoiceChairBlock.HALF) == DoubleBlockHalf.LOWER
						&& isInFront(p, s, pos)) {
						double d = dx * dx + dy * dy + dz * dz;
						if (d < bestDist) {
							bestDist = d;
							best = p.immutable();
						}
					}
				}
			}
		}
		return best;
	}
}
