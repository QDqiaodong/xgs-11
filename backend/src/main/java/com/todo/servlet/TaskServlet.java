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
    private static final java.util.Set<String> VALID_BATCH_OPS = java.util.Set.of("priority", "status", "dueDate", "assignee");
    private static final int MAX_TEXT_LENGTH = 500;
    private static final int MAX_REASON_LENGTH = 500;

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
        if (task.getOverdueReason() != null && task.getOverdueReason().length() > MAX_REASON_LENGTH) {
            return "逾期原因不能超过 " + MAX_REASON_LENGTH + " 个字符";
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
                    task.setOverdueReason(rs.getString("overdue_reason"));
                    task.setCompletedAt(rs.getTimestamp("completed_at"));
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

        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.startsWith("/batch")) {
            handleBatchUpdate(req, resp, user);
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

    private void handleBatchUpdate(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(req.getReader());
            if (!root.has("taskIds") || !root.has("operation")) {
                resp.setStatus(400);
                resp.getWriter().write("{\"success\":false, \"message\":\"缺少必要参数\"}");
                return;
            }

            String operation = root.get("operation").asText();
            if (!VALID_BATCH_OPS.contains(operation)) {
                resp.setStatus(400);
                resp.getWriter().write("{\"success\":false, \"message\":\"不支持的批量操作类型\"}");
                return;
            }

            List<Integer> taskIds = new ArrayList<>();
            com.fasterxml.jackson.databind.JsonNode idsNode = root.get("taskIds");
            if (!idsNode.isArray()) {
                resp.setStatus(400);
                resp.getWriter().write("{\"success\":false, \"message\":\"taskIds 必须是数组\"}");
                return;
            }
            for (com.fasterxml.jackson.databind.JsonNode idNode : idsNode) {
                taskIds.add(idNode.asInt());
            }

            if (taskIds.isEmpty()) {
                resp.setStatus(400);
                resp.getWriter().write("{\"success\":false, \"message\":\"请选择要操作的任务\"}");
                return;
            }

            if (taskIds.size() > 100) {
                resp.setStatus(400);
                resp.getWriter().write("{\"success\":false, \"message\":\"批量操作最多支持 100 条任务\"}");
                return;
            }

            String value = root.has("value") && !root.get("value").isNull() ? root.get("value").asText() : null;

            if ("priority".equals(operation) && (value == null || !VALID_PRIORITIES.contains(value))) {
                resp.setStatus(400);
                resp.getWriter().write("{\"success\":false, \"message\":\"优先级不合法\"}");
                return;
            }

            if ("status".equals(operation) && (value == null || (!"completed".equals(value) && !"pending".equals(value)))) {
                resp.setStatus(400);
                resp.getWriter().write("{\"success\":false, \"message\":\"状态值不合法\"}");
                return;
            }

            try (Connection conn = DBUtil.getConnection()) {
                List<Integer> allowedIds = new ArrayList<>();
                String placeholders = String.join(",", java.util.Collections.nCopies(taskIds.size(), "?"));
                String checkSql = "SELECT id, user_id, assignee_id FROM tasks WHERE id IN (" + placeholders + ")";
                try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                    for (int i = 0; i < taskIds.size(); i++) {
                        checkPs.setInt(i + 1, taskIds.get(i));
                    }
                    try (ResultSet rs = checkPs.executeQuery()) {
                        while (rs.next()) {
                            int ownerId = rs.getInt("user_id");
                            Integer assigneeId = (Integer) rs.getObject("assignee_id");
                            boolean isOwner = ownerId == user.getId();
                            boolean isAssignee = assigneeId != null && assigneeId == user.getId();
                            if (isOwner || isAssignee) {
                                allowedIds.add(rs.getInt("id"));
                            }
                        }
                    }
                }

                if (allowedIds.isEmpty()) {
                    resp.setStatus(403);
                    resp.getWriter().write("{\"success\":false, \"message\":\"没有权限操作选中的任务\"}");
                    return;
                }

                String updateSql;
                if ("priority".equals(operation)) {
                    updateSql = "UPDATE tasks SET priority = ?, updated_at = CURRENT_TIMESTAMP WHERE id IN (";
                } else if ("status".equals(operation)) {
                    if ("completed".equals(value)) {
                        updateSql = "UPDATE tasks SET completed = TRUE, completed_at = CASE WHEN completed = FALSE THEN CURRENT_TIMESTAMP ELSE completed_at END, updated_at = CURRENT_TIMESTAMP WHERE id IN (";
                    } else {
                        updateSql = "UPDATE tasks SET completed = FALSE, completed_at = NULL, overdue_reason = NULL, updated_at = CURRENT_TIMESTAMP WHERE id IN (";
                    }
                } else if ("dueDate".equals(operation)) {
                    updateSql = "UPDATE tasks SET due_date = ?, updated_at = CURRENT_TIMESTAMP WHERE id IN (";
                } else if ("assignee".equals(operation)) {
                    updateSql = "UPDATE tasks SET assignee_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id IN (";
                } else {
                    resp.setStatus(400);
                    resp.getWriter().write("{\"success\":false, \"message\":\"未知操作类型\"}");
                    return;
                }

                String idPlaceholders = String.join(",", java.util.Collections.nCopies(allowedIds.size(), "?"));
                updateSql += idPlaceholders + ")";

                try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                    int paramIndex = 1;
                    if ("priority".equals(operation)) {
                        updatePs.setString(paramIndex++, value);
                    } else if ("dueDate".equals(operation)) {
                        if (value != null && !value.isEmpty()) {
                            long dueTime = Long.parseLong(value);
                            updatePs.setTimestamp(paramIndex++, new Timestamp(dueTime));
                        } else {
                            updatePs.setNull(paramIndex++, Types.TIMESTAMP);
                        }
                    } else if ("assignee".equals(operation)) {
                        if (value != null && !value.isEmpty()) {
                            updatePs.setInt(paramIndex++, Integer.parseInt(value));
                        } else {
                            updatePs.setNull(paramIndex++, Types.INTEGER);
                        }
                    }

                    for (int i = 0; i < allowedIds.size(); i++) {
                        updatePs.setInt(paramIndex + i, allowedIds.get(i));
                    }

                    int updated = updatePs.executeUpdate();
                    resp.getWriter().write("{\"success\":true, \"updatedCount\":" + updated + ", \"totalCount\":" + allowedIds.size() + "}");
                }
            }
        } catch (com.fasterxml.jackson.databind.JsonMappingException | com.fasterxml.jackson.core.JsonParseException e) {
            System.err.println("JSON Parsing Error (Batch): " + e.getMessage());
            resp.setStatus(400);
            resp.getWriter().write("{\"success\":false, \"message\":\"请求格式错误\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"success\":false, \"message\":\"数据库错误\"}");
        } catch (NumberFormatException e) {
            resp.setStatus(400);
            resp.getWriter().write("{\"success\":false, \"message\":\"参数格式错误\"}");
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"success\":false, \"message\":\"内部服务器错误\"}");
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
            
            String checkSql = "SELECT user_id, assignee_id, completed, overdue_reason, completed_at FROM tasks WHERE id = ?";
            boolean wasCompleted = false;
            String existingOverdueReason = null;
            Timestamp existingCompletedAt = null;
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
                    wasCompleted = rs.getBoolean("completed");
                    existingOverdueReason = rs.getString("overdue_reason");
                    existingCompletedAt = rs.getTimestamp("completed_at");
                }
            }

            Task task = objectMapper.readValue(req.getReader(), Task.class);
            String validationError = validateTask(task, true);
            if (validationError != null) {
                resp.setStatus(400);
                resp.getWriter().write("{\"success\":false, \"message\":\"" + validationError + "\"}");
                return;
            }

            long now = System.currentTimeMillis();
            boolean willComplete = task.isCompleted();
            Timestamp completedAtToStore;
            String overdueReasonToStore;

            if (willComplete && !wasCompleted) {
                boolean overdueAtCompletion = task.getDueDate() != null && task.getDueDate().getTime() < now;
                if (overdueAtCompletion) {
                    String reason = task.getOverdueReason();
                    if (reason == null || reason.trim().isEmpty()) {
                        resp.setStatus(400);
                        resp.getWriter().write("{\"success\":false, \"message\":\"该任务已逾期，完成时请补录逾期原因\"}");
                        return;
                    }
                    overdueReasonToStore = reason.trim();
                } else {
                    overdueReasonToStore = null;
                }
                completedAtToStore = new Timestamp(now);
            } else if (willComplete) {
                completedAtToStore = existingCompletedAt;
                String reason = task.getOverdueReason();
                overdueReasonToStore = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : existingOverdueReason;
            } else {
                completedAtToStore = null;
                overdueReasonToStore = null;
            }

            String sql = "UPDATE tasks SET assignee_id = ?, text = ?, completed = ?, priority = ?, due_date = ?, overdue_reason = ?, completed_at = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setObject(1, task.getAssigneeId());
                ps.setString(2, task.getText());
                ps.setBoolean(3, task.isCompleted());
                ps.setString(4, task.getPriority());
                ps.setTimestamp(5, task.getDueDate());
                ps.setString(6, overdueReasonToStore);
                ps.setTimestamp(7, completedAtToStore);
                ps.setInt(8, taskId);
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
