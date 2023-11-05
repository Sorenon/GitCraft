package com.example;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RepoScreen extends Screen {

    private final String name;
    private final OpenRepoGuiPacket.CommitDetails[] commits;

    protected RepoScreen(Component component, String name, OpenRepoGuiPacket.CommitDetails[] commits) {
        super(component);
        this.name = name;
        this.commits = commits;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Change Remote"), button -> {}).pos(this.width - 130, 40).width(110).build());
        addRenderableWidget(Button.builder(Component.literal("Commit"), button -> {}).pos(this.width - 70, 40 + 30).width(50).build());
        addRenderableWidget(Button.builder(Component.literal("Push"), button -> {}).pos(this.width - 70, 100 + 30).width(50).build());
        addRenderableWidget(Button.builder(Component.literal("Pull"), button -> {}).pos(this.width - 70, 130 + 30).width(50).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
    }
}
