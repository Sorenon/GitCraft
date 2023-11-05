package com.example.mc;

import com.example.GCBlocks;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Vector3i;

import java.io.IOException;
import java.nio.file.Path;

public class DebugItem extends Item {
    public DebugItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {

        var path = Path.of("blocks.gc");

        if (level instanceof ServerLevel) {
            if (player.isCrouching()) {
//                try {
//                    GCBlocks.load(level, new BlockPos(0, 0, 0), new Vector3i(10, 10, 10), path);
//                } catch (IOException | CommandSyntaxException e) {
//                    throw new RuntimeException(e);
//                }
            } else {
//                try {
//                    GCBlocks.save(level, new BlockPos(0, 0, 0), new BlockPos(10, 10, 10), path);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
            }
        }

        return super.use(level, player, interactionHand);
    }
}
