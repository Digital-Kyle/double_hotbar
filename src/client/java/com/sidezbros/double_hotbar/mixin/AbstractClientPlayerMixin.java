package com.sidezbros.double_hotbar.mixin;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

	private static final UUID DOUBLE_HOTBAR_DEV_CAPE_UUID = UUID.fromString("f2d832c6-c3b4-41ed-937e-f49cd71c98a7");

	@Shadow
	protected abstract PlayerInfo getPlayerInfo();

	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void double_hotbar$elytraSkin(CallbackInfoReturnable<PlayerSkin> cir) {
		try {
			PlayerInfo playerInfo = this.getPlayerInfo();
			if (playerInfo != null && DOUBLE_HOTBAR_DEV_CAPE_UUID.equals(playerInfo.getProfile().id())) {
				PlayerSkin skin = cir.getReturnValue();
				ClientAsset.ResourceTexture elytraTexture = new ClientAsset.ResourceTexture(Identifier.fromNamespaceAndPath("double_hotbar", "elytra"));
				cir.setReturnValue(skin.with(new PlayerSkin.Patch(
						Optional.empty(),
						Optional.of(elytraTexture),
						Optional.of(elytraTexture),
						Optional.empty()
				)));
			}
		} catch (Exception ignored) {
		}
	}
}
