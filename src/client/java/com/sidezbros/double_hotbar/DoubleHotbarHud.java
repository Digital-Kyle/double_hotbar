package com.sidezbros.double_hotbar;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class DoubleHotbarHud {
	private static final Identifier HOTBAR_SPRITE = Identifier.withDefaultNamespace("hud/hotbar");
	private static final Identifier STATUS_SHIFT_LAYER = Identifier.fromNamespaceAndPath("double_hotbar", "status_shift");

	private DoubleHotbarHud() {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.ARMOR_BAR, STATUS_SHIFT_LAYER, (graphics, deltaTracker) -> {
		});
		HudStatusBarHeightRegistry.addLeft(STATUS_SHIFT_LAYER, DoubleHotbarHud::statusShiftHeight);

		HudElementRegistry.replaceElement(VanillaHudElements.HOTBAR, vanilla -> (graphics, deltaTracker) -> {
			Minecraft mc = Minecraft.getInstance();
			Player player = mc.player;
			if (DHModConfig.INSTANCE.disableMod || !DHModConfig.INSTANCE.displayDoubleHotbar || player == null) {
				vanilla.extractRenderState(graphics, deltaTracker);
				return;
			}

			boolean reverse = DHModConfig.INSTANCE.reverseBars;
			int shift = DHModConfig.INSTANCE.shift;
			if (reverse) {
				graphics.pose().pushMatrix();
				graphics.pose().translate(0.0F, (float) -shift);
			}
			vanilla.extractRenderState(graphics, deltaTracker);
			if (reverse) {
				graphics.pose().popMatrix();
			}

			drawExtraHotbarRow(graphics, deltaTracker, player, reverse);
		});
	}

	private static int statusShiftHeight(Player player) {
		if (DHModConfig.INSTANCE.disableMod || !DHModConfig.INSTANCE.displayDoubleHotbar || player == null || player.isSpectator()) {
			return 0;
		}
		return DHModConfig.INSTANCE.shift;
	}

	private static void drawExtraHotbarRow(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Player player, boolean reverse) {
		int shift = DHModConfig.INSTANCE.shift;
		int crop = DHModConfig.INSTANCE.renderCrop;
		int screenCenter = graphics.guiWidth() / 2;
		int baseY = graphics.guiHeight() - 22 - (reverse ? 0 : shift);
		int barHeight = 22 - crop;

		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, screenCenter - 91, baseY, 182, barHeight);

		int itemY = graphics.guiHeight() - 16 - 3 - (reverse ? 0 : shift);
		int invBase = DHModConfig.INSTANCE.inventoryRow * 9;
		int seed = 1;
		for (int i = 0; i < 9; i++) {
			int x = screenCenter - 90 + i * 20 + 2;
			extractSlot(graphics, x, itemY, deltaTracker, player, player.getInventory().getItem(invBase + i), seed++);
		}
	}

	private static void extractSlot(
			GuiGraphicsExtractor graphics,
			int x,
			int y,
			DeltaTracker deltaTracker,
			Player player,
			ItemStack itemStack,
			int seed
	) {
		if (itemStack.isEmpty()) {
			return;
		}
		float pop = itemStack.getPopTime() - deltaTracker.getGameTimeDeltaPartialTick(false);
		if (pop > 0.0F) {
			float squeeze = 1.0F + pop / 5.0F;
			graphics.pose().pushMatrix();
			graphics.pose().translate(x + 8, y + 12);
			graphics.pose().scale(1.0F / squeeze, (squeeze + 1.0F) / 2.0F);
			graphics.pose().translate(-(x + 8), -(y + 12));
		}

		graphics.item(player, itemStack, x, y, seed);
		if (pop > 0.0F) {
			graphics.pose().popMatrix();
		}

		graphics.itemDecorations(Minecraft.getInstance().font, itemStack, x, y);
	}
}
