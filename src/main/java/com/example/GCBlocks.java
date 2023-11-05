package com.example;

import com.example.mc.RepoBlock;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GCBlocks {
    public static BlockState[][][] load(String in, Vec3i size, Level level) throws CommandSyntaxException {
        BlockState[][][] blockStates = new BlockState[size.getX()][size.getY()][size.getZ()];

        boolean skip = true;

        for (var line : in.lines().toList()) {
            if (skip) {
                skip = false;
                continue;
            }
            var reader = new StringReader(line);
            int x = reader.readInt();
            reader.read();
            int y = reader.readInt();
            reader.read();
            int z = reader.readInt();
            reader.read();
            var blockState = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), new TagParser(reader).readStruct());
            blockStates[x][y][z] = blockState;
        }

        return blockStates;
    }

    public static void load(Level level, BoundingBox boundingBox, Path path) throws IOException, CommandSyntaxException {
        var size = new Vec3i(boundingBox.getXSpan(), boundingBox.getYSpan(), boundingBox.getZSpan());
        var blocks = load(Files.readString(path), size, level);

        var bp = new BlockPos.MutableBlockPos();
        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    if (blocks[x][y][z] != null) {
                        level.setBlock(bp.set(boundingBox.minX(), boundingBox.minY(), boundingBox.minZ()).move(x, y, z), blocks[x][y][z], 2);
                    } else {
                        level.setBlock(bp.set(boundingBox.minX(), boundingBox.minY(), boundingBox.minZ()).move(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    public static void save(Level level, BoundingBox box, Path path) throws IOException {
        var str = save(level, box);
        Files.writeString(path, str);
    }

    public static String save(Level level, BoundingBox box) {
        var bp = new BlockPos.MutableBlockPos();
        var str = new StringBuilder();

        str.append(box.getXSpan());
        str.append(",");
        str.append(box.getYSpan());
        str.append(",");
        str.append(box.getZSpan());
        str.append("\n");

        for (int x = box.minX(); x < box.maxX() + 1; x++) {
            for (int y = box.minY(); y < box.maxY() + 1; y++) {
                for (int z = box.minZ(); z < box.maxZ() + 1; z++) {
                    var blockState = level.getBlockState(bp.set(x, y, z));
                    if (blockState.getBlock() instanceof RepoBlock) {
                        blockState = Blocks.AIR.defaultBlockState();
                    }
                    str.append(x - box.minX());
                    str.append(",");
                    str.append(y - box.minY());
                    str.append(",");
                    str.append(z - box.minZ());
                    str.append(",");
                    str.append(NbtUtils.writeBlockState(blockState));
                    str.append("\n");
                }
            }
        }
        return str.toString();
    }
}
