package com.example.voicechair2;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Coach-Buzzer: kleiner Block (kleiner als ein Spielerkopf).
 * Er gehoert zu dem Stuhl, vor dem er auf der umgedrehten Seite steht (max. 3 Bloecke).
 * Rechtsklick: dieser Stuhl dreht sich um,
 * bekommt einen weissen Rand und zeigt den Namen auf der Namensfliese.
 */
public class BuzzerButtonBlock extends Block {
	public static final MapCodec<BuzzerButtonBlock> CODEC = simpleCodec(BuzzerButtonBlock::new);
	private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 3, 11);

	public BuzzerButtonBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
											   Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel serverLevel) {
			BlockPos chairPos = ChairLinks.findChairFor(serverLevel, pos);
			if (chairPos == null) {
				player.sendOverlayMessage(Component.literal("Kein Stuhl gefunden. Stelle den Buzzer auf die Seite, zu der sich der Stuhl umdreht (max. 3 Bloecke Abstand)."));
			} else {
				BlockState chairState = serverLevel.getBlockState(chairPos);
				if (VoiceChairBlock.buzz(serverLevel, chairPos, chairState)) {
					playBuzzerSound(serverLevel, pos);
				} else {
					player.sendOverlayMessage(Component.literal("Dieser Stuhl hat schon gebuzzert."));
				}
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Buzzer-Ton aus Minecraft-Sounds: zwei leicht verstimmte Piepser plus ein harter Schlag. */
	private static void playBuzzerSound(ServerLevel level, BlockPos pos) {
		level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 1.5f, 0.5f);
		level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 1.5f, 0.53f);
		level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.4f, 2.0f);
	}
}
