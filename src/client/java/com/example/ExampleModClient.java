package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;

public class ExampleModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(ExampleMod.OPEN_SCREEN, (client, handler, buf, responseSender) -> {
			String name = buf.readUtf();
			var commits = new OpenRepoGuiPacket.CommitDetails[buf.readInt()];
			for (int i = 0; i < commits.length; i++) {
				commits[i] = new OpenRepoGuiPacket.CommitDetails(buf.readUtf(), buf.readInt(), buf.readUtf());
			}

			client.execute(() -> client.setScreen(new RepoScreen(Component.literal("Git Repository"), name, commits)));
		});
	}
}