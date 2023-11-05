package com.example.mc;

import com.example.ExampleMod;
import com.example.GCBlocks;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Stack;

public class RepoBlockEntity extends BlockEntity {

    public BoundingBox boundingBox;
    public String name;
    public Git git;
    public boolean powered;

    public int cooldown = 0;
    public Stack<RevCommit> replayingCommits = null;

    public RepoBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ExampleMod.REPO_BLOCK_ENTITY, blockPos, blockState);
    }

    @Override
    public void load(CompoundTag compoundTag) {
        if (compoundTag.contains("name")) {
            boundingBox = new BoundingBox(
                    compoundTag.getInt("x1"),
                    compoundTag.getInt("y1"),
                    compoundTag.getInt("z1"),
                    compoundTag.getInt("x2"),
                    compoundTag.getInt("y2"),
                    compoundTag.getInt("z2")
            );
            name = compoundTag.getString("name");
        }
        powered = compoundTag.getBoolean("powered");
        this.setLevel(level);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level instanceof ServerLevel && this.name != null) {
            if (git == null) {
                try {
                    git = Git.open(this.getPath().toFile());
                } catch (IOException e) {
                    System.err.println(e);
                }
                System.out.println(git);
            }
        }
    }

    public Path getPath() {
        return ExampleMod.repoDir((ServerLevel) this.level, this.name);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        if (this.boundingBox != null) {
            tag.putInt("x1", boundingBox.minX());
            tag.putInt("x2", boundingBox.maxX());
            tag.putInt("y1", boundingBox.minY());
            tag.putInt("y2", boundingBox.maxY());
            tag.putInt("z1", boundingBox.minZ());
            tag.putInt("z2", boundingBox.maxZ());
            tag.putString("name", name);
        }
        tag.putBoolean("powered", powered);
    }

    public void openScreen(ServerPlayer serverPlayer) {
        if (this.git == null) return;

//        var commits = new ArrayList<OpenRepoGuiPacket.CommitDetails>();
//
//        try {
//            this.git.log().call().forEach(revCommit -> commits.add(new OpenRepoGuiPacket.CommitDetails(revCommit.getName(), revCommit.getCommitTime(), revCommit.getFullMessage())));
//        } catch (GitAPIException e) {
//            throw new RuntimeException(e);
//        }
//
//        var buf = PacketByteBufs.create();
//        buf.writeUtf(this.name);
//        buf.writeInt(commits.size());
//        for (var commit : commits) {
//            buf.writeUtf(commit.id());
//            buf.writeInt(commit.time());
//            buf.writeUtf(commit.message());
//        }
//
//        ServerPlayNetworking.send(serverPlayer, ExampleMod.OPEN_SCREEN, buf);

        try {
            replayCommits();
        } catch (GitAPIException | IOException | CommandSyntaxException e) {
            System.err.println(e);
        }
    }

    public void replayCommits() throws GitAPIException, IOException, CommandSyntaxException {
        replayingCommits = new Stack<>();

        this.git.log().call().forEach(replayingCommits::add);

        if (replayingCommits.size() == 0) {
            replayingCommits = null;
            return;
        }

        stepReplay();
    }

    public void stepReplay() throws GitAPIException {
        cooldown = 5;
        if (replayingCommits.size() <= 1) {
            replayingCommits = null;
            try {
                GCBlocks.load(level, this.boundingBox, this.getPath().resolve("blocks.gc"));
            } catch (IOException | CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        try {
            git.branchDelete().setBranchNames("tmp_replay_hack").call();
            git.checkout().setCreateBranch(true).setName("tmp_replay_hack").setStartPoint(replayingCommits.pop()).call();

            GCBlocks.load(level, this.boundingBox, this.getPath().resolve("blocks.gc"));
        } catch (Exception e) {
            System.err.println(e);
        } finally {
            git.checkout().setName("master").call();
            git.branchDelete().setBranchNames("tmp_replay_hack").call();
        }
    }

    public void saveAndCommit() {
        if (this.git == null || this.level.isClientSide || this.replayingCommits != null) {
            return;
        }

        try {
            GCBlocks.save(level, this.boundingBox, this.getPath().resolve("blocks.gc"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            git.add().addFilepattern("blocks.gc").call();
            if (!git.status().call().isClean()) {
                git.commit().setMessage("Auto Commit").call();
            }
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    public void setPowered(boolean hasNeighborSignal) {
        if (hasNeighborSignal == this.powered) return;
        this.powered = hasNeighborSignal;
        if (!this.powered) return;
        saveAndCommit();
    }

    public void tick() {
        if (this.replayingCommits != null && cooldown-- < 0) {
            try {
                stepReplay();
            } catch (GitAPIException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
