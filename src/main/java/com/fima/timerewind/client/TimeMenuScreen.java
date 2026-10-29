package com.fima.timerewind.client;

import com.fima.timerewind.TimeVersion;
import com.fima.timerewind.TimeVersionManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class TimeMenuScreen extends Screen {
	private List<TimeVersion> versions;
	private int selectedIndex = -1;
	private int scrollOffset = 0;
	private static final int MAX_VISIBLE = 8;

	public TimeMenuScreen() {
		super(Text.literal("Меню времени"));
	}

	@Override
	protected void init() {
		super.init();
		if (client != null && client.player != null) {
			versions = TimeVersionManager.list(client.player);
		} else {
			versions = List.of();
		}

		int centerX = this.width / 2;
		int bottomY = this.height - 40;

		// Save button
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Сохранить версию"), btn -> {
			if (client != null && client.player != null) {
				String name = "v" + (versions.size() + 1) + "_" + new SimpleDateFormat("HH-mm-ss").format(new Date());
				TimeVersionManager.save(client.player, name);
				versions = TimeVersionManager.list(client.player);
				selectedIndex = versions.size() - 1;
			}
		}).dimensions(centerX - 160, bottomY, 100, 20).build());

		// Load button
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Загрузить"), btn -> {
			if (selectedIndex >= 0 && client != null && client.player != null) {
				TimeVersionManager.load(client.player, selectedIndex);
				this.close();
			}
		}).dimensions(centerX - 50, bottomY, 100, 20).build());

		// Close
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), btn -> this.close())
			.dimensions(centerX + 60, bottomY, 100, 20).build());

		// Rewind buttons
		this.addDrawableChild(ButtonWidget.builder(Text.literal("◀ Назад"), btn -> {
			if (versions.isEmpty()) return;
			if (selectedIndex <= 0) selectedIndex = 0;
			else selectedIndex--;
			ensureVisible();
		}).dimensions(centerX - 110, bottomY - 30, 100, 20).build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Вперёд ▶"), btn -> {
			if (versions.isEmpty()) return;
			if (selectedIndex < 0) selectedIndex = 0;
			else if (selectedIndex < versions.size() - 1) selectedIndex++;
			ensureVisible();
		}).dimensions(centerX + 10, bottomY - 30, 100, 20).build());
	}

	private void ensureVisible() {
		if (selectedIndex < scrollOffset) scrollOffset = selectedIndex;
		if (selectedIndex >= scrollOffset + MAX_VISIBLE) scrollOffset = selectedIndex - MAX_VISIBLE + 1;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);

		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Выбери версию и загрузи (или ◀ / ▶)"), this.width / 2, 30, 0xAAAAAA);

		int listY = 50;
		int listX = this.width / 2 - 150;

		if (versions == null || versions.isEmpty()) {
			context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Нет сохранённых версий. Нажми «Сохранить»"), this.width / 2, listY + 40, 0xFFAA00);
		} else {
			for (int i = 0; i < MAX_VISIBLE; i++) {
				int idx = scrollOffset + i;
				if (idx >= versions.size()) break;
				TimeVersion v = versions.get(idx);
				boolean selected = idx == selectedIndex;
				int color = selected ? 0x55FF55 : 0xFFFFFF;
				String timeStr = new SimpleDateFormat("dd.MM HH:mm:ss").format(new Date(v.timestamp));
				String line = String.format("[%d] %s  (%s)", idx, v.name, timeStr);
				context.drawTextWithShadow(this.textRenderer, line, listX, listY + i * 14, color);
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && versions != null) {
			int listY = 50;
			int listX = this.width / 2 - 150;
			for (int i = 0; i < MAX_VISIBLE; i++) {
				int idx = scrollOffset + i;
				if (idx >= versions.size()) break;
				int y = listY + i * 14;
				if (mouseX >= listX && mouseX <= listX + 300 && mouseY >= y && mouseY <= y + 12) {
					selectedIndex = idx;
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (versions != null && versions.size() > MAX_VISIBLE) {
			scrollOffset = (int) Math.max(0, Math.min(versions.size() - MAX_VISIBLE, scrollOffset - (int) verticalAmount));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean shouldPause() {
		return false; // temporary menu, game continues
	}
}
