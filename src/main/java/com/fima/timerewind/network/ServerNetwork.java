package com.fima.timerewind.network;

import com.fima.timerewind.TimeVersion;
import com.fima.timerewind.TimeVersionManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class ServerNetwork {
	private ServerNetwork() {}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(TimePackets.RequestListPayload.ID, TimePackets.RequestListPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(TimePackets.SavePayload.ID, TimePackets.SavePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(TimePackets.LoadPayload.ID, TimePackets.LoadPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(TimePackets.VersionListPayload.ID, TimePackets.VersionListPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(TimePackets.FeedbackPayload.ID, TimePackets.FeedbackPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(TimePackets.RequestListPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			context.server().execute(() -> sendList(player));
		});

		ServerPlayNetworking.registerGlobalReceiver(TimePackets.SavePayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			String name = payload.name();
			if (name == null || name.isBlank()) {
				name = "v_" + new SimpleDateFormat("HH-mm-ss").format(new Date());
			}
			String finalName = name;
			context.server().execute(() -> {
				TimeVersionManager.save(player, finalName);
				ServerPlayNetworking.send(player, new TimePackets.FeedbackPayload("§aВерсия '" + finalName + "' сохранена!"));
				sendList(player);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(TimePackets.LoadPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			int index = payload.index();
			context.server().execute(() -> {
				boolean ok = TimeVersionManager.load(player, index);
				if (ok) {
					ServerPlayNetworking.send(player, new TimePackets.FeedbackPayload("§aВерсия загружена!"));
				} else {
					ServerPlayNetworking.send(player, new TimePackets.FeedbackPayload("§cВерсия не найдена."));
				}
			});
		});
	}

	private static void sendList(ServerPlayerEntity player) {
		List<TimeVersion> versions = TimeVersionManager.listFor(player);
		List<String> entries = new ArrayList<>();
		for (int i = 0; i < versions.size(); i++) {
			TimeVersion v = versions.get(i);
			entries.add(i + "|" + v.name + "|" + v.timestamp);
		}
		ServerPlayNetworking.send(player, new TimePackets.VersionListPayload(entries));
	}
}
