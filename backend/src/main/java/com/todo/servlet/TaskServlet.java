package com.todo.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todo.model.Task;
import com.todo.model.User;
import com.todo.util.DBUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@WebServlet(urlPatterns = {"/api/tasks", "/api/tasks/*"})
public class TaskServlet extends HttpServlet {
    private ObjectMapper objectMapper = new ObjectMapper();
    private static final java.util.Set<String> VALID_PRIORITIES = java.util.Set.of("high", "medium", "low");
    private static final int MAX_TEXT_LENGTH = 500;

    private String validateTask(Task task, boolean isUpdate) {
        if (task.getText() == null || task.getText().trim().isEmpty()) {
            return "任务内容不能为空";
        }
        if (task.getText().length() > MAX_TEXT_LENGTH) {
            return "任务内容不能超过 " + MAX_TEXT_LENGTH + " 个字符";
        }
        String priority = task.getPriority();
        if (priority != null && !priority.isEmpty() && !VALID_PRIORITIES.contains(priority)) {
            return "优先级必须是 high、medium 或 low";
        }
        if (task.getDueDate() != null) {
            long now = System.currentTimeMillis();
            long dueTime = task.getDueDate().getTime();
            long tenYearsMs = 10L * 365 * 24 * 60 * 60 * 1000;
            if (dueTime < now - tenYearsMs || dueTime > now + tenYearsMs) {
                return "截止时间不合法";
            }
        }
        return null;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        User user = (User) req.getSession().getAttribute("user");
        if (user == null) {
            resp.setStatus(401);
            return;
        }

        List<Task> tasks = new ArrayList<>();
        // Fetch tasks created by or assigned to the user
        String sql = "SELECT t.*, u.username as creator_name, au.username as assignee_name " +
                     "FROM tasks t " +
                     "JOIN users u ON t.user_id = u.id " +
                     "LEFT JOIN users au ON t.assignee_id = au.id " +
                     "WHERE t.user_id = ? OR t.assignee_id = ? " +
                     "ORDER BY t.created_at DESC";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, user.getId());
            ps.setInt(2, user.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Task task = new Task();
                    task.setId(rs.getInt("id"));
                    task.setUserId(rs.getInt("user_id"));
                    task.setAssigneeId((Integer) rs.getObject("assignee_id"));
                    task.setText(rs.getString("text"));
                    task.setCompleted(rs.getBoolean("completed"));
                    task.setPriority(rs.getString("priority"));
                    task.setDueDate(rs.getTimestamp("due_date"));
                    task.setCreatedAt(rs.getTimestamp("created_at"));
                    task.setUpdatedAt(rs.getTimestamp("updated_at"));
                    task.setUsername(rs.getString("creator_name"));
                    task.setAssigneeName(rs.getString("assignee_name"));
                    tasks.add(task);
                }
            }
            resp.getWriter().write(objectMapper.writeValueAsString(tasks));
        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(500);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        User user = (User) req.getSession().getAttribute("user");
        if (user == null) {
            resp.setStatus(401);
            return;
        }

        try {
            Task task = objectMapper.readValue(req.getReader(), Task.class);
            String validationError = validateTask(task, false);
            if (validationError != null) {
                resp.setStatus(400);
                resp.getWriter().write("{\"success\":false, \"message\":\"" + validationError + "\"}");
                return;
            }
            String sql = "INSERT INTO tasks (user_id, assignee_id, text, completed, priority, due_date) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, user.getId());
                ps.setObject(2, task.getAssigneeId());
                ps.setString(3, task.getText());
                ps.setBoolean(4, false);
                ps.setString(5, task.getPriority() != null ? task.getPriority() : "medium");
                ps.setTimestamp(6, task.getDueDate());
                ps.executeUpdate();
                resp.getWriter().write("{\"success\":true}");
            }
        } catch (com.fasterxml.jackson.databind.JsonMappingException | com.fasterxml.jackson.core.JsonParseException e) {
            System.err.println("JSON Parsing Error: " + e.getMessage());
            resp.setStatus(400);
            resp.getWriter().write("{\"success\":false, \"message\":\"Invalid request format\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"success\":false, \"message\":\"Database error\"}");
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"success\":false, \"message\":\"Internal server error\"}");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        User user = (User) req.getSession().getAttribute("user");
        if (user == null) {
            resp.setStatus(401);
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 2) {
            resp.setStatus(400);
            resp.getWriter().write("{\"success\":false, \"message\":\"Invalid request\"}");
            return;
        }
        
        try {
            int taskId = Integer.parseInt(pathInfo.substring(1));
            
            String checkSql = "SELECT user_id, assignee_id FROM tasks WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setInt(1, taskId);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (!rs.next()) {
                        resp.setStatus(404);
                        resp.getWriter().write("{\"success\":false, \"message\":\"Task not found\"}");
                        return;
                    }
                    int ownerId = rs.getInt("user_id");
                    Integer assigneeId = (Integer) rs.getObject("assignee_id");
                    boolean isOwner = ownerId == user.getId();
                    boolean isAssignee = assigneeId != null && assigneeId == user.getId();
                    if (!isOwner && !isAssignee) {
                        resp.setStatus(403);
                        resp.getWriter().write("{\"success\":false, \"message\":\"Permission denied: only creator or assignee can update this task\"}");
                        return;
                    }
                }
            }
            
            Task task = objectMapper.readValue(req.getReader(), Task.class);
            String validationError = validateTask(task, true);
            if (validationError != null) {
                resp.setStatus(400);
                resp.getWriter().write("{\"success\":false, \"message\":\"" + validationError + "\"}");
                return;
            }
            String sql = "UPDATE tasks SET assignee_id = ?, text = ?, completed = ?, priority = ?, due_date = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setObject(1, task.getAssigneeId());
                ps.setString(2, task.getText());
                ps.setBoolean(3, task.isCompleted());
                ps.setString(4, task.getPriority());
                ps.setTimestamp(5, task.getDueDate());
                ps.setInt(6, taskId);
                ps.executeUpdate();
                resp.getWriter().write("{\"success\":true}");
            }
        } catch (NumberFormatException e) {
            resp.setStatus(400);
            resp.getWriter().write("{\"success\":false, \"message\":\"Invalid task ID\"}");
        } catch (com.fasterxml.jackson.databind.JsonMappingException | com.fasterxml.jackson.core.JsonParseException e) {
            System.err.println("JSON Parsing Error (PUT): " + e.getMessage());
            resp.setStatus(400);
            resp.getWriter().write("{\"success\":false, \"message\":\"Invalid request format\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"success\":false, \"message\":\"Database error\"}");
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"success\":false, \"message\":\"Internal server error\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        User user = (User) req.getSession().getAttribute("user");
        if (user == null) {
            resp.setStatus(401);
            resp.getWriter().write("{\"success\":false, \"message\":\"Unauthorized\"}");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 2) {
            resp.setStatus(400);
            resp.getWriter().write("{\"success\":false, \"message\":\"Invalid request\"}");
            return;
        }

        try {
            int taskId = Integer.parseInt(pathInfo.substring(1));

            String checkSql = "SELECT user_id, assignee_id FROM tasks WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setInt(1, taskId);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (!rs.next()) {
                        resp.setStatus(404);
                        resp.getWriter().write("{\"success\":false, \"message\":\"Task not found\"}");
                        return;
                    }
                    int ownerId = rs.getInt("user_id");
                    Integer assigneeId = (Integer) rs.getObject("assignee_id");
                    boolean isOwner = ownerId == user.getId();
                    boolean isAssignee = assigneeId != null && assigneeId == user.getId();
                    if (!isOwner && !isAssignee) {
                        resp.setStatus(403);
                        resp.getWriter().write("{\"success\":false, \"message\":\"Permission denied: only creator or assignee can delete this task\"}");
                        return;
                    }
                }
            }

            String sql = "DELETE FROM tasks WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, taskId);
                ps.executeUpdate();
                resp.getWriter().write("{\"success\":true}");
            }
        } catch (NumberFormatException e) {
            resp.setStatus(400);
            resp.getWriter().write("{\"success\":false, \"message\":\"Invalid task ID\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"success\":false, \"message\":\"Database error\"}");
        }
    }
}
