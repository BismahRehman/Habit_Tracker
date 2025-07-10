package com.example.habit_tracker;

import java.util.List;

public class HabitModel {
    private String id;
    private String name;
    private String description;
    private long goal;
    private long currentCount;
    private long streak;
    private List<String> days;

    public HabitModel() {} // Required by Firestore

    public HabitModel(String name, String description, long goal, long currentCount, long streak, List<String> days) {
        this.name = name;
        this.description = description;
        this.goal = goal;
        this.currentCount = currentCount;
        this.streak = streak;
        this.days = days;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public String getDescription() { return description; }

    public long getGoal() { return goal; }
    public long getCurrentCount() { return currentCount; }
    public long getStreak() { return streak; }

    public void setCurrentCount(long currentCount) { this.currentCount = currentCount; }
    public void setStreak(long streak) { this.streak = streak; }

    public List<String> getDays() { return days; }
    public void setDays(List<String> days) { this.days = days; }
}
