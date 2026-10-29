package com.fima.timerewind.client;

import com.fima.timerewind.network.ClientNetwork;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TimeMenuScreen extends Screen {
	private List<ClientNetwork.VersionEntry> versions = new ArrayList<>();
	private int selectedIndex = -1;
	private int scrollOffset = 0;
	private static final int MAX_VISIBLE = 8;
	private boolean waitingList = true;

	public TimeMenuScreen() {
		super(Text.literal("Меню времени"));
	}

	public void setVersionsFromServer(List<ClientNetwork.VersionEntry> list) {
		this.versions = list != null ? list : new ArrayList<>();
		this.waitingList = false;
		if (selectedIndex >= versions.size()) selectedIndex = versions.size() - 1;
	}

	@Override
	protected void init() {
		super.init();
		waitingList = true;
		ClientNetwork.requestList(this::setVersionsFromServer);

		int centerX = this.width / 2;
		int bottomY = this.height - 40;

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Сохранить версию"), btn -> {
			String name = "v" + (versions.size() + 1) + "_" + new SimpleDateFormat("HH-mm-ss").format(new Date());
			ClientNetwork.save(name);
			waitingList = true;
		}).dimensions(centerX - 160, bottomY, 100, 20).build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Загрузить"), btn -> {
			if (selectedIndex >= 0 && selectedIndex < versions.size()) {
				ClientNetwork.load(versions.get(selectedIndex).index());
				this.close();
			}
		}).dimensions(centerX - 50, bottomY, 100, 20).build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), btn -> this.close())
			.dimensions(centerX + 60, bottomY, 100, 20).build());

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

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Обновить"), btn -> {
			waitingList = true;
			ClientNetwork.requestList(this::setVersionsFromServer);
		}).dimensions(centerX - 50, bottomY - 55, 100, 20).build());
	}

	private void ensureVisible() {
		if (selectedIndex < scrollOffset) scrollOffset = Math.max(0, selectedIndex);
		if (selectedIndex >= scrollOffset + MAX_VISIBLE) scrollOffset = selectedIndex - MAX_VISIBLE + 1;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);

		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
		context.drawCenteredTextWithShadow(this.textRenderer,
			Text.literal("Одиночная и сервер (мод на сервере + клиенте)"),
			this.width / 2, 26, 0xAAAAAA);

		int listY = 48;
		int listX = this.width / 2 - 150;

		if (waitingList) {
			context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Загрузка списка с сервера..."), this.width / 2, listY + 40, 0xFFFF55);
		} else if (versions.isEmpty()) {
			context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Нет версий. Нажми «Сохранить»"), this.width / 2, listY + 40, 0xFFAA00);
		} else {
			for (int i = 0; i < MAX_VISIBLE; i++) {
				int idx = scrollOffset + i;
				if (idx >= versions.size()) break;
				ClientNetwork.VersionEntry v = versions.get(idx);
				boolean selected = idx == selectedIndex;
				int color = selected ? 0x55FF55 : 0xFFFFFF;
				String timeStr = new SimpleDateFormat("dd.MM HH:mm:ss").format(new Date(v.timestamp()));
				String line = String.format("[%d] %s  (%s)", v.index(), v.name(), timeStr);
				context.drawTextWithShadow(this.textRenderer, line, listX, listY + i * 14, color);
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && !versions.isEmpty()) {
			int listY = 48;
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
		if (versions.size() > MAX_VISIBLE) {
			scrollOffset = (int) Math.max(0, Math.min(versions.size() - MAX_VISIBLE, scrollOffset - (int) verticalAmount));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
