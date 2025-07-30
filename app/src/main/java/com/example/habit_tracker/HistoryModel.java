package com.example.habit_tracker;

public class HistoryModel {
    private long count;
    private long goal;
    private long timestamp;

    public HistoryModel() {} // Required for Firestore

    public HistoryModel(long count, long goal, long timestamp) {
        this.count = count;
        this.goal = goal;
        this.timestamp = timestamp;
    }

    public long getCount() { return count; }
    public long getGoal() { return goal; }
    public long getTimestamp() { return timestamp; }
}
