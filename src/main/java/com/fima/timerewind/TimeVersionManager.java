package com.fima.timerewind;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TimeVersionManager {
	private static final String FOLDER = "time_versions";

	public static Path getPlayerDir(MinecraftServer server, UUID playerId) {
		if (server == null) return null;
		Path dir = server.getSavePath(WorldSavePath.ROOT).resolve(FOLDER).resolve(playerId.toString());
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			TimeRewindMod.LOGGER.error("Cannot create time_versions folder", e);
		}
		return dir;
	}

	public static void save(ServerPlayerEntity player, String name) {
		MinecraftServer server = player.getServer();
		Path dir = getPlayerDir(server, player.getUuid());
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
		v.worldTime = player.getServerWorld().getTimeOfDay();

		RegistryWrapper.WrapperLookup registries = server.getRegistryManager();
		NbtList inv = new NbtList();
		for (int i = 0; i < player.getInventory().size(); i++) {
			ItemStack stack = player.getInventory().getStack(i);
			if (!stack.isEmpty()) {
				NbtCompound itemNbt = new NbtCompound();
				itemNbt.putInt("Slot", i);
				itemNbt.put("Item", stack.encode(registries));
				inv.add(itemNbt);
			}
		}
		v.inventory = inv;

		Path file = dir.resolve(sanitize(name) + "_" + v.timestamp + ".dat");
		try {
			NbtIo.writeCompressed(v.toNbt(), file);
			TimeRewindMod.LOGGER.info("Saved time version for {}: {}", player.getName().getString(), file.getFileName());
		} catch (IOException e) {
			TimeRewindMod.LOGGER.error("Failed to save time version", e);
		}
	}

	public static List<TimeVersion> list(MinecraftServer server, UUID playerId) {
		List<TimeVersion> result = new ArrayList<>();
		Path dir = getPlayerDir(server, playerId);
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

	public static List<TimeVersion> list(MinecraftServer server) {
		return new ArrayList<>();
	}

	public static List<TimeVersion> listFor(ServerPlayerEntity player) {
		return list(player.getServer(), player.getUuid());
	}

	public static boolean load(ServerPlayerEntity player, int index) {
		MinecraftServer server = player.getServer();
		List<TimeVersion> versions = listFor(player);
		if (index < 0 || index >= versions.size()) return false;

		TimeVersion v = versions.get(index);
		RegistryWrapper.WrapperLookup registries = server.getRegistryManager();

		player.requestTeleport(v.x, v.y, v.z);
		player.setYaw(v.yaw);
		player.setPitch(v.pitch);
		player.setHealth(v.health);
		player.getHungerManager().setFoodLevel(v.foodLevel);
		player.getHungerManager().setSaturationLevel(v.saturation);

		if (server.isSingleplayer() || player.hasPermissionLevel(2)) {
			ServerWorld world = player.getServerWorld();
			if (world != null) {
				world.setTimeOfDay(v.worldTime);
			}
		}

		player.getInventory().clear();
		if (v.inventory != null) {
			for (int i = 0; i < v.inventory.size(); i++) {
				NbtCompound itemNbt = v.inventory.getCompound(i);
				int slot = itemNbt.getInt("Slot");
				if (itemNbt.contains("Item")) {
					ItemStack stack = ItemStack.fromNbt(registries, itemNbt.getCompound("Item")).orElse(ItemStack.EMPTY);
					if (slot >= 0 && slot < player.getInventory().size()) {
						player.getInventory().setStack(slot, stack);
					}
				}
			}
		}
		player.currentScreenHandler.sendContentUpdates();
		return true;
	}

	private static String sanitize(String name) {
		String s = name.replaceAll("[^a-zA-Z0-9_\\-а-яА-ЯёЁ]", "_");
		if (s.isEmpty()) s = "version";
		return s.substring(0, Math.min(s.length(), 32));
	}
}
