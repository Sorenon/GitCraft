package com.example;

import com.example.mc.RepoBlock;
import com.example.mc.RepoBlockEntity;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.LevelResource;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public class ExampleMod implements ModInitializer {
    public static final ResourceLocation OPEN_SCREEN = new ResourceLocation("gitcraft", "open_screen");

    public static final RepoBlock REPO_BLOCK = new RepoBlock(BlockBehaviour.Properties.of());
    public static final BlockEntityType<RepoBlockEntity> REPO_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(RepoBlockEntity::new, REPO_BLOCK).build();
    public static final BlockItem REPO_ITEM = new BlockItem(REPO_BLOCK, new Item.Properties());
    public static final Item RESET = new Item(new Item.Properties());
    public static final Item STATUS = new Item(new Item.Properties());

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("git_init").then(
                            Commands.argument("from", BlockPosArgument.blockPos()).then(
                                    Commands.argument("to", BlockPosArgument.blockPos())
                                            .executes(
                                                    commandContext -> createRepo(commandContext.getSource(), BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(commandContext, "from"), BlockPosArgument.getLoadedBlockPos(commandContext, "to")), null)
                                            ))));
            dispatcher.register(
                    Commands.literal("git_clone").then(
                            Commands.argument("remote", StringArgumentType.string()).then(
                                    Commands.argument("pos", BlockPosArgument.blockPos())
                                            .executes(
                                                    commandContext -> cloneRepo(commandContext.getSource(), BlockPosArgument.getLoadedBlockPos(commandContext, "pos"), StringArgumentType.getString(commandContext, "remote")))
                            )));
        });


        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation("gitcraft", "reset"), RESET);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation("gitcraft", "status"), STATUS);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation("gitcraft", "repo"), REPO_ITEM);
        Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation("gitcraft", "repo"), REPO_BLOCK);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, new ResourceLocation("gitcraft", "repo"), REPO_BLOCK_ENTITY);
    }

    public static int createRepo(CommandSourceStack commandSourceStack, BoundingBox boundingBox, String remote) {
        var player = commandSourceStack.getPlayer();
        if (player != null) {
            var name = String.valueOf(new Random().nextLong());

            var stack = new ItemStack(REPO_ITEM);
            var tag = new CompoundTag();
            tag.putInt("x1", boundingBox.minX());
            tag.putInt("x2", boundingBox.maxX());
            tag.putInt("y1", boundingBox.minY());
            tag.putInt("y2", boundingBox.maxY());
            tag.putInt("z1", boundingBox.minZ());
            tag.putInt("z2", boundingBox.maxZ());
            tag.putString("name", name);
            BlockItem.setBlockEntityData(stack, REPO_BLOCK_ENTITY, tag);
            player.addItem(stack);

            try {
                Git.init().setDirectory(repoDir(commandSourceStack.getLevel(), name).toFile()).call().close();
                commandSourceStack.sendSystemMessage(Component.literal("Repo created with id: " + name));
            } catch (GitAPIException e) {
                throw new RuntimeException(e);
            }
        }
        return 1;
    }

    public static int cloneRepo(CommandSourceStack commandSourceStack, BlockPos pos, String remote) {
        var player = commandSourceStack.getPlayer();
        if (player != null) {
            var name = String.valueOf(new Random().nextLong());
            var path = repoDir(commandSourceStack.getLevel(), name);

            try {
                Git.cloneRepository().setURI(remote).setDirectory(path.toFile()).call().close();
                commandSourceStack.sendSystemMessage(Component.literal("Repo created with id: " + name + " from: " + remote));
            } catch (GitAPIException e) {
                System.err.println(e);
                throw new RuntimeException(e);
            }

            try {
                StringReader reader = new StringReader(Files.readString(path.resolve("blocks.gc")).lines().findFirst().get());
                int x = reader.readInt();
                reader.read();
                int y = reader.readInt();
                reader.read();
                int z = reader.readInt();

                var stack = new ItemStack(REPO_ITEM);
                var tag = new CompoundTag();
                tag.putInt("x1", pos.getX());
                tag.putInt("x2", pos.getX() + x);
                tag.putInt("y1", pos.getY());
                tag.putInt("y2", pos.getY() + y);
                tag.putInt("z1", pos.getZ());
                tag.putInt("z2", pos.getZ() + z);
                tag.putString("name", name);
                BlockItem.setBlockEntityData(stack, REPO_BLOCK_ENTITY, tag);
                player.addItem(stack);

            } catch (IOException | CommandSyntaxException e) {
                throw new RuntimeException(e);
            }

        }
        return 1;
    }

    public static Path repoDir(ServerLevel serverLevel, String name) {
        return serverLevel.getServer().getWorldPath(LevelResource.ROOT).getParent().resolve("repos/" + name);
    }
}