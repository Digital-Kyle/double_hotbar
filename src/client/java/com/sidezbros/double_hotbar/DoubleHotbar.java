package com.sidezbros.double_hotbar;

import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.platform.InputConstants;

public class DoubleHotbar implements ClientModInitializer {
	public static final Logger LOGGER = LogManager.getLogger("double_hotbar");
	private static KeyMapping keyBinding;
	private final boolean[] hotbarKeys = new boolean[10];
	private final long[] timer = new long[10];
	private boolean alreadySwapped = false;

	public static final Identifier WOOSH_SOUND_ID = Identifier.fromNamespaceAndPath("double_hotbar", "woosh");
	private static final KeyMapping.Category KEYBIND_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("double_hotbar", "keybinds"));
	public static final SoundEvent WOOSH_SOUND_EVENT = SoundEvent.createVariableRangeEvent(WOOSH_SOUND_ID);

	@Override
	public void onInitializeClient() {
		DHModConfig.init();
		Registry.register(BuiltInRegistries.SOUND_EVENT, WOOSH_SOUND_ID, WOOSH_SOUND_EVENT);
		DoubleHotbarHud.register();

		keyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.double_hotbar.swap",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_R,
				KEYBIND_CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (DHModConfig.INSTANCE.holdToSwap) {
				if (keyBinding.isDown() != this.hotbarKeys[9]) {
					this.hotbarKeys[9] = keyBinding.isDown();
					if (keyBinding.isDown()) {
						this.timer[9] = Instant.now().toEpochMilli();
					} else {
						if (Instant.now().toEpochMilli() - this.timer[9] < DHModConfig.INSTANCE.holdTime) {
							this.swapStack(client.player, !DHModConfig.INSTANCE.holdToSwapBar, client.player.getInventory().getSelectedSlot());
						} else {
							this.alreadySwapped = false;
						}
					}
				}
				if (!this.alreadySwapped && keyBinding.isDown()
						&& Instant.now().toEpochMilli() - this.timer[9] > DHModConfig.INSTANCE.holdTime) {
					this.swapStack(client.player, DHModConfig.INSTANCE.holdToSwapBar, client.player.getInventory().getSelectedSlot());
					this.alreadySwapped = true;
				}
			} else {
				while (keyBinding.consumeClick()) {
					this.swapStack(client.player, true, 0);
				}
			}
			if (DHModConfig.INSTANCE.allowDoubleTap) {
				for (int i = 0; i < 9; i++) {
					if (client.options.keyHotbarSlots[i].isDown() != this.hotbarKeys[i]) {
						this.hotbarKeys[i] = client.options.keyHotbarSlots[i].isDown();
						if (client.options.keyHotbarSlots[i].isDown()) {
							if (Instant.now().toEpochMilli() - this.timer[i] < DHModConfig.INSTANCE.doubleTapWindow) {
								this.swapStack(client.player, false, i);
								this.timer[i] = 0;
							} else {
								this.timer[i] = Instant.now().toEpochMilli();
							}
						}
					}
				}
			}
		});
	}

	public void swapStack(Player player, boolean fullRow, int slot) {
		@SuppressWarnings("resource")
		MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
		int inventoryRow = DHModConfig.INSTANCE.inventoryRow * 9;
		boolean playSound = false;

		if (gameMode == null || DHModConfig.INSTANCE.disableMod) {
			return;
		}

		if (fullRow) {
			for (int i = 0; i < 9; i++) {
				ItemStack a = player.getInventory().getItem(i);
				ItemStack b = player.getInventory().getItem(inventoryRow + i);
				if (!ItemStack.matches(a, b)) {
					gameMode.handleContainerInput(player.containerMenu.containerId, inventoryRow + i, i, ContainerInput.SWAP, player);
					playSound = true;
				}
			}
		} else {
			ItemStack a = player.getInventory().getItem(slot);
			ItemStack b = player.getInventory().getItem(inventoryRow + slot);
			if (!ItemStack.matches(a, b)) {
				gameMode.handleContainerInput(player.containerMenu.containerId, inventoryRow + slot, slot, ContainerInput.SWAP, player);
				playSound = true;
			}
		}

		if (playSound) {
			player.playSound(WOOSH_SOUND_EVENT, 0.01f * DHModConfig.INSTANCE.wooshVolume, 1f);
		}
	}
}
