# 极简任务管理系统 (Todo Application)

这是一个基于 Java Servlet 和 Vue 3 (CDN) 开发的任务管理系统，采用现代 UI 设计，支持完整的任务生命周期管理。

## 🛠 技术栈
- **Frontend**: Vue 3 (Composition API) + SFC Loader + Axios + Vanilla CSS
- **Backend**: Java Servlet + Maven + MySQL 8.0
- **Deployment**: Docker + Docker Compose + Tomcat 9

## 运⾏说明
1. 确保已安装并启动 Docker Desktop。
2. 在项目根目录下执行：
   ```bash
   docker compose up --build
   ```
3. 等待容器启动完成。首次启动时，MySQL 容器会自动初始化数据库并插入演示数据。
4. 启动后，通过浏览器访问：[http://localhost:18083](http://localhost:18083)

## 测试账号
- **用户名**: `admin`
- **密码**: `123456`
- 也可点击注册按钮创建新账号。

## Services
- **前端页面**: [http://localhost:18083](http://localhost:18083)
- **数据库**: `localhost:14001` (用户: `root`, 密码: `root`)

## Verification
1. **登录注册**: 验证表单校验，体验基于 MD5 的安全认证。
2. **任务管理**: 创建、编辑、删除任务。支持设置优先级（高/中/低）和截止日期。
3. **统计看板**: 实时查看总任务、已完成、待处理及即将到期（24小时内）的任务统计。
4. **任务指派**: 在添加或编辑任务时，可以选择其他系统用户进行指派。
5. **筛选与排序**: 支持按关键字、完成状态、优先级、角色（创建/受托）进行复合筛选；支持按时间、优先级排序。
6. **视觉提示**: 逾期任务自动红框高亮显示。

## 📁 目录结构
```text
├── backend
│   ├── Dockerfile
│   ├── pom.xml
│   └── src
│       └── main
│           ├── java/com/todo/... (后端源码)
│           └── webapp
│               ├── components/ (Vue 组件)
│               ├── css/ (样式)
│               ├── js/ (逻辑)
│               └── index.html (入口)
├── database
│   └── init.sql (数据库初始化脚本)
└── docker-compose.yml
```
