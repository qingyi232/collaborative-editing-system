package com.collab.websocket;

import com.collab.entity.Document;
import com.collab.entity.DocumentMember;
import com.collab.mapper.DocumentMapper;
import com.collab.mapper.DocumentMemberMapper;
import com.collab.service.SystemConfigService;
import com.collab.util.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollabWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, Set<WebSocketSession>> documentSessions = new ConcurrentHashMap<>();
    private static final Map<String, Long> sessionDocMap = new ConcurrentHashMap<>();
    private static final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();
    private static final Map<String, String> sessionUsernameMap = new ConcurrentHashMap<>();
    private static final Map<String, String> sessionPermissionMap = new ConcurrentHashMap<>();

    private final JwtUtil jwtUtil;
    private final OTEngine otEngine;
    private final DocumentMapper documentMapper;
    private final DocumentMemberMapper documentMemberMapper;
    private final SystemConfigService systemConfigService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static int getOnlineCount() {
        return sessionDocMap.size();
    }

    public Set<String> getDocumentOnlineUsers(Long docId) {
        Set<WebSocketSession> sessions = documentSessions.get(docId);
        if (sessions == null) {
            return Collections.emptySet();
        }
        Set<String> users = new LinkedHashSet<>();
        for (WebSocketSession session : sessions) {
            String username = sessionUsernameMap.get(session.getId());
            if (username != null) {
                users.add(username);
            }
        }
        return users;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        Map<String, String> params = parseQueryParams(uri);
        String token = params.get("token");
        String docIdStr = params.get("docId");

        if (token == null || docIdStr == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        if (!jwtUtil.validateToken(token)) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);
        Long docId = Long.parseLong(docIdStr);

        String permission = resolvePermission(docId, userId);
        if (permission == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        sessionDocMap.put(session.getId(), docId);
        sessionUserMap.put(session.getId(), userId);
        sessionUsernameMap.put(session.getId(), username);
        sessionPermissionMap.put(session.getId(), permission);
        documentSessions.computeIfAbsent(docId, k -> new CopyOnWriteArraySet<>()).add(session);

        log.info("用户 {}({}) 加入文档 {} [{}], 当前在线: {}", username, userId, docId, permission, getOnlineCount());

        Map<String, Object> joinMsg = new LinkedHashMap<>();
        joinMsg.put("type", "userJoin");
        joinMsg.put("docId", docId);
        joinMsg.put("userId", userId);
        joinMsg.put("username", username);
        joinMsg.put("data", Map.of("onlineUsers", getDocumentOnlineUsers(docId),
                "currentVersion", otEngine.getCurrentVersion(docId)));
        broadcastToDocument(docId, joinMsg, null);

        int heartbeatInterval = 30;
        String hbConfig = systemConfigService.getConfigValue("websocket_heartbeat_interval");
        if (hbConfig != null) {
            try { heartbeatInterval = Integer.parseInt(hbConfig); } catch (NumberFormatException ignored) {}
        }

        Map<String, Object> syncData = new LinkedHashMap<>();
        syncData.put("onlineUsers", getDocumentOnlineUsers(docId));
        syncData.put("currentVersion", otEngine.getCurrentVersion(docId));
        syncData.put("permission", permission);
        syncData.put("heartbeatInterval", heartbeatInterval);

        Map<String, Object> syncMsg = new LinkedHashMap<>();
        syncMsg.put("type", "sync");
        syncMsg.put("docId", docId);
        syncMsg.put("userId", userId);
        syncMsg.put("username", username);
        syncMsg.put("data", syncData);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(syncMsg)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long docId = sessionDocMap.get(session.getId());
        Long userId = sessionUserMap.get(session.getId());
        String username = sessionUsernameMap.get(session.getId());
        String permission = sessionPermissionMap.get(session.getId());

        if (docId == null || userId == null) {
            return;
        }

        Map<String, Object> msg = objectMapper.readValue(message.getPayload(),
                new TypeReference<Map<String, Object>>() {});
        String type = (String) msg.get("type");

        if ("ping".equals(type)) {
            Map<String, Object> pong = new LinkedHashMap<>();
            pong.put("type", "pong");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));
            return;
        }

        if ("operation".equals(type)) {
            if (!canEdit(permission)) {
                Map<String, Object> errMsg = new LinkedHashMap<>();
                errMsg.put("type", "error");
                errMsg.put("data", Map.of("message", "您没有编辑权限"));
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errMsg)));
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) msg.get("data");
            if (data == null) return;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ops = (List<Map<String, Object>>) data.get("ops");
            int baseVersion = ((Number) data.getOrDefault("baseVersion", 0)).intValue();

            OTEngine.Operation result = otEngine.applyOperation(docId, userId, ops, baseVersion);

            Map<String, Object> broadcastMsg = new LinkedHashMap<>();
            broadcastMsg.put("type", "operation");
            broadcastMsg.put("docId", docId);
            broadcastMsg.put("userId", userId);
            broadcastMsg.put("username", username);
            broadcastMsg.put("data", Map.of(
                    "ops", result.getOps(),
                    "version", result.getVersion()));
            broadcastToDocument(docId, broadcastMsg, session.getId());

            Map<String, Object> ack = new LinkedHashMap<>();
            ack.put("type", "ack");
            ack.put("data", Map.of("version", result.getVersion()));
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ack)));
        } else if ("cursor".equals(type)) {
            msg.put("userId", userId);
            msg.put("username", username);
            broadcastToDocument(docId, msg, session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long docId = sessionDocMap.remove(session.getId());
        Long userId = sessionUserMap.remove(session.getId());
        String username = sessionUsernameMap.remove(session.getId());
        sessionPermissionMap.remove(session.getId());

        if (docId != null) {
            Set<WebSocketSession> sessions = documentSessions.get(docId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    documentSessions.remove(docId);
                }
            }

            log.info("用户 {}({}) 离开文档 {}, 当前在线: {}", username, userId, docId, getOnlineCount());

            Map<String, Object> leaveMsg = new LinkedHashMap<>();
            leaveMsg.put("type", "userLeave");
            leaveMsg.put("docId", docId);
            leaveMsg.put("userId", userId);
            leaveMsg.put("username", username);
            leaveMsg.put("data", Map.of("onlineUsers", getDocumentOnlineUsers(docId)));
            broadcastToDocument(docId, leaveMsg, null);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket传输异常, sessionId: {}", session.getId(), exception);
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException ignored) {
        }
    }

    private String resolvePermission(Long docId, Long userId) {
        Document doc = documentMapper.selectById(docId);
        if (doc == null || doc.getStatus() == 0) {
            return null;
        }
        if (doc.getOwnerId().equals(userId)) {
            return "OWNER";
        }
        LambdaQueryWrapper<DocumentMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentMember::getDocumentId, docId)
                .eq(DocumentMember::getUserId, userId);
        DocumentMember member = documentMemberMapper.selectOne(wrapper);
        if (member != null) {
            return member.getPermission();
        }
        if (doc.getIsPublic() != null && doc.getIsPublic() == 1) {
            return "VIEW";
        }
        return null;
    }

    private boolean canEdit(String permission) {
        return "OWNER".equals(permission) || "ADMIN".equals(permission) || "EDIT".equals(permission);
    }

    private void broadcastToDocument(Long docId, Map<String, Object> message, String excludeSessionId) {
        Set<WebSocketSession> sessions = documentSessions.get(docId);
        if (sessions == null) return;

        try {
            String json = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);
            for (WebSocketSession s : sessions) {
                if (s.isOpen() && !s.getId().equals(excludeSessionId)) {
                    try {
                        synchronized (s) {
                            s.sendMessage(textMessage);
                        }
                    } catch (IOException e) {
                        log.error("发送消息失败, sessionId: {}", s.getId(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("序列化消息失败", e);
        }
    }

    private Map<String, String> parseQueryParams(URI uri) {
        Map<String, String> params = new HashMap<>();
        try {
            UriComponentsBuilder.fromUri(uri).build().getQueryParams()
                    .forEach((key, values) -> {
                        if (!values.isEmpty()) {
                            params.put(key, values.get(0));
                        }
                    });
        } catch (Exception e) {
            log.warn("解析WebSocket URI参数失败: {}", uri, e);
        }
        return params;
    }
}
