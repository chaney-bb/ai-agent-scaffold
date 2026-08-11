/**
 * 前端全局配置（对话页 index.html 会读取）
 *
 * API_BASE：后端 Spring Boot 根地址（不要末尾斜杠）
 * 改端口时只改这里即可，例如 http://127.0.0.1:8091
 *
 * 对应设计文档：docs/studyDocs/2-19-chat-ui-前端对接设计文档.md
 */
window.APP_CONFIG = {
  API_BASE: "http://127.0.0.1:8091",
};
