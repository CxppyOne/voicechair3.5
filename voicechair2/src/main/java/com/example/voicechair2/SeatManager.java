package com.example.voicechair2;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Sitzplaetze: ein unsichtbarer Ruestungsstaender, auf dem der Spieler "reitet". */
public final class SeatManager {
	public static final String TAG = "voicechair2_seat";

	/**
	 * Hoehe des Staenders relativ zum Stuhl. Sitzt man zu hoch oder zu tief,
	 * einfach diesen Wert aendern (kleiner = tiefer).
	 */
	private static final double SEAT_Y_OFFSET = -0.9;

	private record Seat(ArmorStand stand, BlockPos pos) {}

	private static final List<Seat> SEATS = new ArrayList<>();

	private SeatManager() {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> tick());

		// Uebrig gebliebene Sitze aus einer frueheren Spielsitzung entfernen.
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof ArmorStand stand
				&& stand.entityTags().contains(TAG)
				&& SEATS.stream().noneMatch(s -> s.stand() == stand)) {
				world.getServer().execute(stand::discard);
			}
		});
	}

	public static void sit(ServerLevel level, BlockPos pos, Player player) {
		if (player.isPassenger()) return;
		for (Seat seat : SEATS) {
			if (seat.pos().equals(pos) && !seat.stand().isRemoved()) return; // Platz belegt
		}
		BlockState state = level.getBlockState(pos);
		ArmorStand stand = new ArmorStand(level, pos.getX() + 0.5, pos.getY() + SEAT_Y_OFFSET, pos.getZ() + 0.5);
		stand.setInvisible(true);
		stand.setNoGravity(true);
		stand.setInvulnerable(true);
		stand.addTag(TAG);
		stand.setYRot(state.getValue(VoiceChairBlock.FACING).toYRot());
		SEATS.add(new Seat(stand, pos));
		level.addFreshEntity(stand);
		if (player.startRiding(stand)) {
			level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
		}
	}

	/** Dreht den Sitz mit, wenn sich der Stuhl dreht. */
	public static void rotate(BlockPos pos, float yaw) {
		for (Seat seat : SEATS) {
			if (seat.pos().equals(pos) && !seat.stand().isRemoved()) {
				seat.stand().setYRot(yaw);
			}
		}
	}

	private static void tick() {
		SEATS.removeIf(seat -> {
			ArmorStand stand = seat.stand();
			if (stand.isRemoved()) return true;
			boolean chairGone = !(stand.level().getBlockState(seat.pos()).getBlock() instanceof VoiceChairBlock);
			if (stand.getPassengers().isEmpty() || chairGone) {
				stand.ejectPassengers();
				stand.discard();
				return true;
			}
			return false;
		});
	}
}
