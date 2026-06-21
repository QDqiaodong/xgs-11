package com.todo.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todo.model.User;
import com.todo.util.DBUtil;
import org.apache.commons.codec.digest.DigestUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/login", "/api/register", "/api/users", "/api/logout", "/api/me"})
public class UserServlet extends HttpServlet {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String path = req.getServletPath();
        resp.setContentType("application/json;charset=UTF-8");

        if ("/api/login".equals(path)) {
            login(req, resp);
        } else if ("/api/register".equals(path)) {
            register(req, resp);
        } else if ("/api/logout".equals(path)) {
            req.getSession().invalidate();
            resp.getWriter().write("{\"success\":true}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        resp.setContentType("application/json;charset=UTF-8");

        if ("/api/users".equals(path)) {
            getAllUsers(req, resp);
        } else if ("/api/me".equals(path)) {
            getCurrentUser(req, resp);
        }
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            User loginUser = objectMapper.readValue(req.getReader(), User.class);
            if (loginUser.getUsername() == null || loginUser.getPassword() == null) {
                resp.setStatus(400);
                resp.getWriter().write("{\"success\":false,\"message\":\"用户名和密码不能为空\"}");
                return;
            }
            String sql = "SELECT id, username FROM users WHERE username = ? AND password = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, loginUser.getUsername());
                ps.setString(2, DigestUtils.md5Hex(loginUser.getPassword()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        User user = new User(rs.getInt("id"), rs.getString("username"));
                        req.getSession().setAttribute("user", user);
                        Map<String, Object> res = new HashMap<>();
                        res.put("success", true);
                        res.put("user", user);
                        resp.getWriter().write(objectMapper.writeValueAsString(res));
                    } else {
                        resp.setStatus(200);
                        resp.getWriter().write("{\"success\":false,\"message\":\"用户名或密码错误\"}");
                    }
                }
            }
        } catch (com.fasterxml.jackson.databind.JsonMappingException | com.fasterxml.jackson.core.JsonParseException e) {
            System.err.println("Login JSON Error: " + e.getMessage());
            resp.setStatus(200);
            resp.getWriter().write("{\"success\":false,\"message\":\"请求格式不正确\"}");
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"success\":false,\"message\":\"服务器内部错误\"}");
        }
    }

    private void register(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            User regUser = objectMapper.readValue(req.getReader(), User.class);
            if (regUser.getUsername() == null || regUser.getPassword() == null) {
                resp.setStatus(200);
                resp.getWriter().write("{\"success\":false,\"message\":\"注册信息不完整\"}");
                return;
            }
            String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, regUser.getUsername());
                ps.setString(2, DigestUtils.md5Hex(regUser.getPassword()));
                ps.executeUpdate();
                resp.getWriter().write("{\"success\":true}");
            }
        } catch (com.fasterxml.jackson.databind.JsonMappingException | com.fasterxml.jackson.core.JsonParseException e) {
            System.err.println("Register JSON Error: " + e.getMessage());
            resp.setStatus(200);
            resp.getWriter().write("{\"success\":false,\"message\":\"请求格式不正确\"}");
        } catch (SQLException e) {
            resp.setStatus(200);
            resp.getWriter().write("{\"success\":false,\"message\":\"注册失败，用户名可能已存在\"}");
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"success\":false,\"message\":\"服务器内部错误\"}");
        }
    }

    private void getAllUsers(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute("user");
        if (user == null) {
            resp.setStatus(401);
            resp.getWriter().write("{\"success\":false,\"message\":\"未登录，请先登录\"}");
            return;
        }
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username FROM users";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(new User(rs.getInt("id"), rs.getString("username")));
            }
            resp.getWriter().write(objectMapper.writeValueAsString(users));
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
        }
    }

    private void getCurrentUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute("user");
        if (user != null) {
            resp.getWriter().write(objectMapper.writeValueAsString(user));
        } else {
            resp.setStatus(401);
            resp.getWriter().write("{}");
        }
    }
}
