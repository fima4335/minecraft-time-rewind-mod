package com.fima.timerewind;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TimeVersionManager {
	private static final String FOLDER = "time_versions";

	public static Path getVersionsDir(PlayerEntity player) {
		if (!(player.getWorld() instanceof ServerWorld serverWorld)) {
			return null;
		}
		Path worldRoot = serverWorld.getServer().getSavePath(WorldSavePath.ROOT);
		Path dir = worldRoot.resolve(FOLDER);
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			TimeRewindMod.LOGGER.error("Cannot create time_versions folder", e);
		}
		return dir;
	}

	public static void save(PlayerEntity player, String name) {
		Path dir = getVersionsDir(player);
		if (dir == null) return;

		TimeVersion v = new TimeVersion(name);
		v.x = player.getX();
		v.y = player.getY();
		v.z = player.getZ();
		v.yaw = player.getYaw();
		v.pitch = player.getPitch();
		v.health = player.getHealth();
		v.foodLevel = player.getHungerManager().getFoodLevel();
		v.saturation = player.getHungerManager().getSaturationLevel();
		v.worldTime = player.getWorld().getTimeOfDay();

		// Inventory snapshot (simple list of item stacks NBT)
		NbtList inv = new NbtList();
		for (int i = 0; i < player.getInventory().size(); i++) {
			var stack = player.getInventory().getStack(i);
			if (!stack.isEmpty()) {
				NbtCompound itemNbt = new NbtCompound();
				itemNbt.putInt("Slot", i);
				stack.writeNbt(itemNbt);
				inv.add(itemNbt);
			}
		}
		v.inventory = inv;

		File file = dir.resolve(sanitize(name) + "_" + v.timestamp + ".dat").toFile();
		try {
			NbtIo.writeCompressed(v.toNbt(), file.toPath());
			TimeRewindMod.LOGGER.info("Saved time version: {}", file.getName());
		} catch (IOException e) {
			TimeRewindMod.LOGGER.error("Failed to save time version", e);
		}
	}

	public static List<TimeVersion> list(PlayerEntity player) {
		List<TimeVersion> result = new ArrayList<>();
		Path dir = getVersionsDir(player);
		if (dir == null || !Files.exists(dir)) return result;

		try (var stream = Files.list(dir)) {
			stream.filter(p -> p.toString().endsWith(".dat"))
				.sorted()
				.forEach(p -> {
					try {
						NbtCompound nbt = NbtIo.readCompressed(p, NbtSizeTracker.ofUnlimitedBytes());
						result.add(TimeVersion.fromNbt(nbt));
					} catch (IOException e) {
						TimeRewindMod.LOGGER.warn("Cannot read version file: {}", p);
					}
				});
		} catch (IOException e) {
			TimeRewindMod.LOGGER.error("Cannot list versions", e);
		}
		return result;
	}

	public static boolean load(PlayerEntity player, int index) {
		List<TimeVersion> versions = list(player);
		if (index < 0 || index >= versions.size()) return false;

		TimeVersion v = versions.get(index);

		if (player instanceof ServerPlayerEntity serverPlayer) {
			serverPlayer.requestTeleport(v.x, v.y, v.z);
			serverPlayer.setYaw(v.yaw);
			serverPlayer.setPitch(v.pitch);
			serverPlayer.setHealth(v.health);
			serverPlayer.getHungerManager().setFoodLevel(v.foodLevel);
			serverPlayer.getHungerManager().setSaturationLevel(v.saturation);

			// Restore world time
			if (serverPlayer.getServerWorld() != null) {
				serverPlayer.getServerWorld().setTimeOfDay(v.worldTime);
			}

			// Restore inventory (clear first)
			serverPlayer.getInventory().clear();
			if (v.inventory != null) {
				for (int i = 0; i < v.inventory.size(); i++) {
					NbtCompound itemNbt = v.inventory.getCompound(i);
					int slot = itemNbt.getInt("Slot");
					var stack = net.minecraft.item.ItemStack.fromNbt(serverPlayer.getRegistryManager(), itemNbt).orElse(net.minecraft.item.ItemStack.EMPTY);
					if (slot >= 0 && slot < serverPlayer.getInventory().size()) {
						serverPlayer.getInventory().setStack(slot, stack);
					}
				}
			}
		}
		return true;
	}

	private static String sanitize(String name) {
		return name.replaceAll("[^a-zA-Z0-9_\-а-яА-ЯёЁ]", "_").substring(0, Math.min(name.length(), 32));
	}
}
