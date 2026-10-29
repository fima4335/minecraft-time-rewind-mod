package com.fima.timerewind.network;

import com.fima.timerewind.TimeRewindMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class TimePackets {
	private TimePackets() {}

	public record RequestListPayload() implements CustomPayload {
		public static final Id<RequestListPayload> ID = new Id<>(Identifier.of(TimeRewindMod.MOD_ID, "request_list"));
		public static final PacketCodec<RegistryByteBuf, RequestListPayload> CODEC =
				PacketCodec.unit(new RequestListPayload());
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record SavePayload(String name) implements CustomPayload {
		public static final Id<SavePayload> ID = new Id<>(Identifier.of(TimeRewindMod.MOD_ID, "save"));
		public static final PacketCodec<RegistryByteBuf, SavePayload> CODEC =
				PacketCodec.tuple(PacketCodecs.STRING, SavePayload::name, SavePayload::new);
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record LoadPayload(int index) implements CustomPayload {
		public static final Id<LoadPayload> ID = new Id<>(Identifier.of(TimeRewindMod.MOD_ID, "load"));
		public static final PacketCodec<RegistryByteBuf, LoadPayload> CODEC =
				PacketCodec.tuple(PacketCodecs.INTEGER, LoadPayload::index, LoadPayload::new);
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record VersionListPayload(List<String> entries) implements CustomPayload {
		public static final Id<VersionListPayload> ID = new Id<>(Identifier.of(TimeRewindMod.MOD_ID, "version_list"));
		public static final PacketCodec<RegistryByteBuf, VersionListPayload> CODEC =
				PacketCodec.tuple(
						PacketCodecs.collection(ArrayList::new, PacketCodecs.STRING),
						VersionListPayload::entries,
						VersionListPayload::new
				);
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record FeedbackPayload(String message) implements CustomPayload {
		public static final Id<FeedbackPayload> ID = new Id<>(Identifier.of(TimeRewindMod.MOD_ID, "feedback"));
		public static final PacketCodec<RegistryByteBuf, FeedbackPayload> CODEC =
				PacketCodec.tuple(PacketCodecs.STRING, FeedbackPayload::message, FeedbackPayload::new);
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}
}
