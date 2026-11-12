package com.example.voicechair2;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * Jurystuhl, zwei Bloecke hoch (wie eine Tuer).
 * HALF = LOWER: Sitzflaeche, HALF = UPPER: hohe Rueckenlehne.
 *
 * Zustaende:
 * - TURNED=false, BUZZED=false: roter Stuhl, nicht umgedreht
 * - TURNED=true,  BUZZED=false: umgedreht (z. B. durch das Lied-Ende-Item), rot, kein Name
 * - TURNED=true,  BUZZED=true:  vom Coach-Buzzer gedrueckt, weisser Rand, Name sichtbar
 *
 * Wichtig: Alle Logik arbeitet mit der UNTEREN Haelfte. Die obere wird immer mitgezogen.
 */
public class VoiceChairBlock extends Block {
	public static final MapCodec<VoiceChairBlock> CODEC = simpleCodec(VoiceChairBlock::new);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
	public static final BooleanProperty TURNED = BooleanProperty.create("turned");
	public static final BooleanProperty BUZZED = BooleanProperty.create("buzzed");
	private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

	public VoiceChairBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any()
			.setValue(FACING, Direction.NORTH)
			.setValue(HALF, DoubleBlockHalf.LOWER)
			.setValue(TURNED, false)
			.setValue(BUZZED, false));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, HALF, TURNED, BUZZED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		Level level = context.getLevel();
		// Platz fuer die obere Haelfte noetig
		if (level.isOutsideBuildHeight(pos.above())) return null;
		if (!level.getBlockState(pos.above()).canBeReplaced(context)) return null;
		return defaultBlockState()
			.setValue(FACING, context.getHorizontalDirection())
			.setValue(HALF, DoubleBlockHalf.LOWER)
			.setValue(TURNED, false)
			.setValue(BUZZED, false);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
	}

	/** Beim Abbauen immer beide Haelften entfernen, es faellt genau ein Stuhl. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide()) {
			boolean lower = state.getValue(HALF) == DoubleBlockHalf.LOWER;
			BlockPos other = lower ? pos.above() : pos.below();
			BlockState otherState = level.getBlockState(other);
			if (otherState.is(this) && otherState.getValue(HALF) != state.getValue(HALF)) {
				if (lower) {
					level.setBlock(other, Blocks.AIR.defaultBlockState(), 35);
				} else {
					level.destroyBlock(other, !player.isCreative());
				}
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		// Die obere Haelfte ist nur Rueckenlehne und soll den Klick-Ray nicht
		// blockieren. Die untere Haelfte bleibt anklickbar, um sich hinzusetzen.
		// Die physische Kollision beider Haelften bleibt in getCollisionShape().
		return state.getValue(HALF) == DoubleBlockHalf.UPPER ? Shapes.empty() : SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
										   CollisionContext context) {
		return SHAPE;
	}

	private static BlockPos basePos(BlockState state, BlockPos pos) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
										  Player player, InteractionHand hand, BlockHitResult hit) {
		if (stack.getItem() instanceof BuzzerItem) {
			if (level instanceof ServerLevel serverLevel) {
				BlockPos base = basePos(state, pos);
				BlockState baseState = serverLevel.getBlockState(base);
				if (baseState.is(this)) toggleOne(serverLevel, base, baseState);
			}
			return InteractionResult.SUCCESS;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
											   Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel serverLevel) {
			BlockPos base = basePos(state, pos);
			if (serverLevel.getBlockState(base).is(this)) {
				SeatManager.sit(serverLevel, base, player);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * Setzt den Zustand der UNTEREN Haelfte (state) und zieht die obere mit.
	 * Dreht sich der Stuhl (turned aendert sich), wird die Blickrichtung umgekehrt.
	 * Gibt true zurueck, wenn sich etwas geaendert hat. Aktualisiert keine Fliesen.
	 */
	public static boolean apply(ServerLevel level, BlockPos pos, BlockState state,
								boolean turned, boolean buzzed, boolean sound) {
		boolean curTurned = state.getValue(TURNED);
		boolean curBuzzed = state.getValue(BUZZED);
		if (curTurned == turned && curBuzzed == buzzed) return false;

		Direction facing = state.getValue(FACING);
		if (curTurned != turned) facing = facing.getOpposite();

		BlockState newLower = state.setValue(TURNED, turned).setValue(BUZZED, buzzed).setValue(FACING, facing);
		level.setBlock(pos, newLower, 3);

		BlockPos up = pos.above();
		if (level.getBlockState(up).is(state.getBlock())) {
			level.setBlock(up, newLower.setValue(HALF, DoubleBlockHalf.UPPER), 3);
		}

		if (curTurned != turned) {
			SeatManager.rotate(pos, facing.toYRot());
			if (sound) {
				level.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1.0f, 0.7f);
			}
		}
		return true;
	}

	/** Item auf einen einzelnen Stuhl: normal umdrehen bzw. zuruecksetzen (ohne weissen Rand, ohne Name). */
	public static void toggleOne(ServerLevel level, BlockPos pos, BlockState state) {
		apply(level, pos, state, !state.getValue(TURNED), false, true);
		NameTileBlock.refreshAround(level, pos);
	}

	/** Coach-Buzzer: Stuhl dreht sich (falls noetig), bekommt weissen Rand, Name erscheint. */
	public static boolean buzz(ServerLevel level, BlockPos pos, BlockState state) {
		boolean changed = apply(level, pos, state, true, true, false);
		if (changed) NameTileBlock.refreshAround(level, pos);
		return changed;
	}
}
