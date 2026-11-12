package com.example.voicechair2;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Namensfliese. Gehoert zu dem Stuhl, vor dem sie auf der umgedrehten Seite steht (max. 3 Bloecke).
 * Der Name steht als unsichtbarer Ruestungsstaender mit Namensschild ueber der Fliese
 * und wird nur angezeigt, solange der Stuhl umgedreht ist.
 * Name aendern: Rechtsklick mit einem benannten Namensschild (im Amboss umbenennen).
 */
public class NameTileBlock extends Block {
	public static final MapCodec<NameTileBlock> CODEC = simpleCodec(NameTileBlock::new);
	public static final String TAG = "voicechair2_name";
	public static final int RADIUS = 3;

	/** Hoehe des Staender-Fusses relativ zur Fliese. Der Name schwebt ca. 0,3 Bloecke ueber der Fliese. */
	private static final double NAME_Y_OFFSET = -2.1;
	private static final String DEFAULT_NAME = "Name";
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

	public NameTileBlock(Properties properties) {
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
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (level instanceof ServerLevel serverLevel) {
			refresh(serverLevel, pos);
		}
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (level instanceof ServerLevel serverLevel) {
			ArmorStand stand = findStand(serverLevel, pos);
			if (stand != null) stand.discard();
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
										  Player player, InteractionHand hand, BlockHitResult hit) {
		if (stack.is(Items.NAME_TAG)) {
			if (level instanceof ServerLevel serverLevel) {
				Component name = stack.get(DataComponents.CUSTOM_NAME);
				if (name == null) {
					player.sendOverlayMessage(
						Component.literal("Dieses Namensschild hat keinen Namen. Benenne es zuerst im Amboss."));
				} else {
					ArmorStand stand = findOrCreateStand(serverLevel, pos);
					stand.setCustomName(name);
					refresh(serverLevel, pos);
					serverLevel.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.7f, 1.5f);
					player.sendOverlayMessage(Component.literal("Name gesetzt: ").append(name));
				}
			}
			return InteractionResult.SUCCESS;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
											   Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			player.sendOverlayMessage(
				Component.literal("Rechtsklick mit einem benannten Namensschild, um den Namen zu aendern."));
		}
		return InteractionResult.SUCCESS;
	}

	// ---------- Hilfsfunktionen ----------

	static ArmorStand findStand(ServerLevel level, BlockPos pos) {
		AABB box = new AABB(pos).inflate(0.3, 3.0, 0.3);
		List<ArmorStand> found = level.getEntitiesOfClass(ArmorStand.class, box, s ->
			s.entityTags().contains(TAG)
				&& Math.abs(s.getX() - (pos.getX() + 0.5)) < 0.1
				&& Math.abs(s.getZ() - (pos.getZ() + 0.5)) < 0.1);
		return found.isEmpty() ? null : found.get(0);
	}

	static ArmorStand findOrCreateStand(ServerLevel level, BlockPos pos) {
		ArmorStand stand = findStand(level, pos);
		if (stand != null) return stand;
		stand = new ArmorStand(level, pos.getX() + 0.5, pos.getY() + NAME_Y_OFFSET, pos.getZ() + 0.5);
		stand.setInvisible(true);
		stand.setNoGravity(true);
		stand.setInvulnerable(true);
		stand.addTag(TAG);
		stand.setCustomName(Component.literal(DEFAULT_NAME));
		stand.setCustomNameVisible(false);
		level.addFreshEntity(stand);
		return stand;
	}

	/** Zeigt den Namen genau dann, wenn der Stuhl, vor dem die Fliese steht, gebuzzert hat. */
	public static void refresh(ServerLevel level, BlockPos tilePos) {
		BlockPos chairPos = ChairLinks.findChairFor(level, tilePos);
		boolean buzzed = chairPos != null
			&& level.getBlockState(chairPos).getValue(VoiceChairBlock.BUZZED);
		findOrCreateStand(level, tilePos).setCustomNameVisible(buzzed);
	}

	/** Aktualisiert alle Fliesen im Umkreis eines Stuhls. */
	public static void refreshAround(ServerLevel level, BlockPos chairPos) {
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dy = -2; dy <= 2; dy++) {
				for (int dz = -RADIUS; dz <= RADIUS; dz++) {
					p.set(chairPos.getX() + dx, chairPos.getY() + dy, chairPos.getZ() + dz);
					if (level.getBlockState(p).getBlock() instanceof NameTileBlock) {
						refresh(level, p.immutable());
					}
				}
			}
		}
	}
}
