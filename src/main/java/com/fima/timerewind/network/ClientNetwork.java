package com.fima.timerewind.network;

import com.fima.timerewind.client.TimeMenuScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ClientNetwork {
	private static Consumer<List<VersionEntry>> listCallback;

	private ClientNetwork() {}

	public record VersionEntry(int index, String name, long timestamp) {}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(TimePackets.VersionListPayload.ID, (payload, context) -> {
			List<VersionEntry> list = new ArrayList<>();
			for (String e : payload.entries()) {
				String[] p = e.split("\\|", 3);
				if (p.length >= 3) {
					try {
						list.add(new VersionEntry(Integer.parseInt(p[0]), p[1], Long.parseLong(p[2])));
					} catch (NumberFormatException ignored) {}
				}
			}
			context.client().execute(() -> {
				if (listCallback != null) listCallback.accept(list);
				if (context.client().currentScreen instanceof TimeMenuScreen screen) {
					screen.setVersionsFromServer(list);
				}
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(TimePackets.FeedbackPayload.ID, (payload, context) -> {
			context.client().execute(() -> {
				if (context.client().player != null) {
					context.client().player.sendMessage(Text.literal(payload.message()), false);
				}
			});
		});
	}

	public static void requestList(Consumer<List<VersionEntry>> callback) {
		listCallback = callback;
		if (ClientPlayNetworking.canSend(TimePackets.RequestListPayload.ID)) {
			ClientPlayNetworking.send(new TimePackets.RequestListPayload());
		}
	}

	public static void save(String name) {
		if (ClientPlayNetworking.canSend(TimePackets.SavePayload.ID)) {
			ClientPlayNetworking.send(new TimePackets.SavePayload(name == null ? "" : name));
		}
	}

	public static void load(int index) {
		if (ClientPlayNetworking.canSend(TimePackets.LoadPayload.ID)) {
			ClientPlayNetworking.send(new TimePackets.LoadPayload(index));
		}
	}
}
