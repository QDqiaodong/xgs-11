package com.todo.model;

import java.sql.Timestamp;

public class Task {
    private int id;
    private int userId;
    private Integer assigneeId;
    private String text;
    private boolean completed;
    private String priority; // high, medium, low
    private Timestamp dueDate;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String overdueReason;
    private Timestamp completedAt;

    // Extra fields for frontend
    private String username; // Creator's name
    private String assigneeName;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public Integer getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Integer assigneeId) { this.assigneeId = assigneeId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Timestamp getDueDate() { return dueDate; }
    public void setDueDate(Timestamp dueDate) { this.dueDate = dueDate; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public String getOverdueReason() { return overdueReason; }
    public void setOverdueReason(String overdueReason) { this.overdueReason = overdueReason; }
    public Timestamp getCompletedAt() { return completedAt; }
    public void setCompletedAt(Timestamp completedAt) { this.completedAt = completedAt; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }
}
