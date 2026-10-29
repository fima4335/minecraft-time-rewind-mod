package com.fima.timerewind;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.StringArgumentType;

public class TimeRewindMod implements ModInitializer {
	public static final String MOD_ID = "timerewind";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Time Rewind mod loaded!");

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("timeversion")
				.then(CommandManager.literal("save")
					.executes(ctx -> {
						ServerCommandSource source = ctx.getSource();
						if (source.getPlayer() != null) {
							TimeVersionManager.save(source.getPlayer(), "auto_" + System.currentTimeMillis());
							source.sendFeedback(() -> Text.literal("§aВерсия сохранена!"), false);
						}
						return 1;
					})
					.then(CommandManager.argument("name", StringArgumentType.greedyString())
						.executes(ctx -> {
							ServerCommandSource source = ctx.getSource();
							if (source.getPlayer() != null) {
								String name = StringArgumentType.getString(ctx, "name");
								TimeVersionManager.save(source.getPlayer(), name);
								source.sendFeedback(() -> Text.literal("§aВерсия '" + name + "' сохранена!"), false);
							}
							return 1;
						})
					)
				)
				.then(CommandManager.literal("list")
					.executes(ctx -> {
						ServerCommandSource source = ctx.getSource();
						if (source.getPlayer() != null) {
							var list = TimeVersionManager.list(source.getPlayer());
							if (list.isEmpty()) {
								source.sendFeedback(() -> Text.literal("§eНет сохранённых версий."), false);
							} else {
								source.sendFeedback(() -> Text.literal("§bСписок версий:"), false);
								for (int i = 0; i < list.size(); i++) {
									int idx = i;
									source.sendFeedback(() -> Text.literal("§7[" + idx + "] §f" + list.get(idx).name), false);
								}
							}
						}
						return 1;
					})
				)
				.then(CommandManager.literal("load")
					.then(CommandManager.argument("id", StringArgumentType.word())
						.executes(ctx -> {
							ServerCommandSource source = ctx.getSource();
							if (source.getPlayer() != null) {
								try {
									int id = Integer.parseInt(StringArgumentType.getString(ctx, "id"));
									if (TimeVersionManager.load(source.getPlayer(), id)) {
										source.sendFeedback(() -> Text.literal("§aВерсия загружена!"), false);
									} else {
										source.sendFeedback(() -> Text.literal("§cВерсия не найдена."), false);
									}
								} catch (NumberFormatException e) {
									source.sendFeedback(() -> Text.literal("§cID должен быть числом."), false);
								}
							}
							return 1;
						})
					)
				)
			);
		});
	}
}
