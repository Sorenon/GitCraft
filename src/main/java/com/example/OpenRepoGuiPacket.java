package com.example;

public class OpenRepoGuiPacket {

    public CommitDetails[] commits;

    public record CommitDetails(String id, int time, String message) {
    }
}
