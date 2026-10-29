package com.fima.timerewind;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;

public class TimeVersion {
	public String name;
	public long timestamp;
	public double x, y, z;
	public float yaw, pitch;
	public float health;
	public int foodLevel;
	public float saturation;
	public long worldTime;
	public NbtList inventory;

	public TimeVersion() {}

	public TimeVersion(String name) {
		this.name = name;
		this.timestamp = System.currentTimeMillis();
	}

	public NbtCompound toNbt() {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("name", name);
		nbt.putLong("timestamp", timestamp);
		nbt.putDouble("x", x);
		nbt.putDouble("y", y);
		nbt.putDouble("z", z);
		nbt.putFloat("yaw", yaw);
		nbt.putFloat("pitch", pitch);
		nbt.putFloat("health", health);
		nbt.putInt("foodLevel", foodLevel);
		nbt.putFloat("saturation", saturation);
		nbt.putLong("worldTime", worldTime);
		if (inventory != null) {
			nbt.put("inventory", inventory);
		}
		return nbt;
	}

	public static TimeVersion fromNbt(NbtCompound nbt) {
		TimeVersion v = new TimeVersion();
		v.name = nbt.getString("name");
		v.timestamp = nbt.getLong("timestamp");
		v.x = nbt.getDouble("x");
		v.y = nbt.getDouble("y");
		v.z = nbt.getDouble("z");
		v.yaw = nbt.getFloat("yaw");
		v.pitch = nbt.getFloat("pitch");
		v.health = nbt.getFloat("health");
		v.foodLevel = nbt.getInt("foodLevel");
		v.saturation = nbt.getFloat("saturation");
		v.worldTime = nbt.getLong("worldTime");
		if (nbt.contains("inventory", NbtElement.LIST_TYPE)) {
			v.inventory = nbt.getList("inventory", NbtElement.COMPOUND_TYPE);
		}
		return v;
	}
}
