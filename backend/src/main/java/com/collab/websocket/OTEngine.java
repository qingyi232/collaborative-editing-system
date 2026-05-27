package com.collab.websocket;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class OTEngine {

    private final Map<Long, List<Operation>> operationHistory = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> documentVersions = new ConcurrentHashMap<>();

    @Data
    public static class Operation {
        private Long userId;
        private List<Map<String, Object>> ops;
        private int version;
        private long timestamp;

        public Operation(Long userId, List<Map<String, Object>> ops, int version) {
            this.userId = userId;
            this.ops = ops;
            this.version = version;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public synchronized Operation applyOperation(Long docId, Long userId,
                                                  List<Map<String, Object>> ops, int baseVersion) {
        AtomicInteger currentVersion = documentVersions.computeIfAbsent(docId, k -> new AtomicInteger(0));
        List<Operation> history = operationHistory.computeIfAbsent(docId, k -> new CopyOnWriteArrayList<>());

        int serverVersion = currentVersion.get();
        List<Map<String, Object>> transformedOps = new ArrayList<>(ops);

        if (baseVersion < serverVersion) {
            for (int i = baseVersion; i < serverVersion && i < history.size(); i++) {
                Operation concurrentOp = history.get(i);
                if (!concurrentOp.getUserId().equals(userId)) {
                    transformedOps = transformDelta(transformedOps, concurrentOp.getOps());
                }
            }
        }

        int newVersion = currentVersion.incrementAndGet();
        Operation operation = new Operation(userId, transformedOps, newVersion);
        history.add(operation);

        if (history.size() > 1000) {
            history.subList(0, history.size() - 500).clear();
        }

        return operation;
    }

    /**
     * Quill Delta OT transform: transform ops1 against ops2.
     * Quill Delta ops use keys: "insert" (String/Object), "retain" (Number), "delete" (Number).
     * Optional "attributes" for formatting.
     */
    private List<Map<String, Object>> transformDelta(List<Map<String, Object>> clientOps,
                                                      List<Map<String, Object>> serverOps) {
        try {
            List<Map<String, Object>> result = new ArrayList<>();
            Iterator<Map<String, Object>> serverIt = serverOps.iterator();
            Map<String, Object> serverOp = serverIt.hasNext() ? serverIt.next() : null;
            int serverRemaining = 0;

            for (Map<String, Object> clientOp : clientOps) {
                if (clientOp.containsKey("insert")) {
                    result.add(new LinkedHashMap<>(clientOp));
                } else if (clientOp.containsKey("retain")) {
                    int clientRetain = ((Number) clientOp.get("retain")).intValue();
                    int remaining = clientRetain;

                    while (remaining > 0 && serverOp != null) {
                        if (serverOp.containsKey("insert")) {
                            int insertLen = getInsertLength(serverOp);
                            Map<String, Object> r = new LinkedHashMap<>();
                            r.put("retain", insertLen);
                            result.add(r);
                            serverOp = serverIt.hasNext() ? serverIt.next() : null;
                        } else if (serverOp.containsKey("retain")) {
                            int serverRetain = serverRemaining > 0 ? serverRemaining
                                    : ((Number) serverOp.get("retain")).intValue();
                            serverRemaining = 0;
                            int minLen = Math.min(remaining, serverRetain);
                            Map<String, Object> r = new LinkedHashMap<>(clientOp);
                            r.put("retain", minLen);
                            result.add(r);
                            remaining -= minLen;
                            if (serverRetain > minLen) {
                                serverRemaining = serverRetain - minLen;
                            } else {
                                serverOp = serverIt.hasNext() ? serverIt.next() : null;
                            }
                        } else if (serverOp.containsKey("delete")) {
                            int serverDelete = ((Number) serverOp.get("delete")).intValue();
                            remaining = Math.max(0, remaining - serverDelete);
                            serverOp = serverIt.hasNext() ? serverIt.next() : null;
                        } else {
                            break;
                        }
                    }

                    if (remaining > 0) {
                        Map<String, Object> r = new LinkedHashMap<>(clientOp);
                        r.put("retain", remaining);
                        result.add(r);
                    }
                } else if (clientOp.containsKey("delete")) {
                    int clientDelete = ((Number) clientOp.get("delete")).intValue();
                    int remaining = clientDelete;

                    while (remaining > 0 && serverOp != null) {
                        if (serverOp.containsKey("insert")) {
                            int insertLen = getInsertLength(serverOp);
                            Map<String, Object> r = new LinkedHashMap<>();
                            r.put("retain", insertLen);
                            result.add(r);
                            serverOp = serverIt.hasNext() ? serverIt.next() : null;
                        } else if (serverOp.containsKey("retain")) {
                            int serverRetain = serverRemaining > 0 ? serverRemaining
                                    : ((Number) serverOp.get("retain")).intValue();
                            serverRemaining = 0;
                            int minLen = Math.min(remaining, serverRetain);
                            Map<String, Object> r = new LinkedHashMap<>();
                            r.put("delete", minLen);
                            result.add(r);
                            remaining -= minLen;
                            if (serverRetain > minLen) {
                                serverRemaining = serverRetain - minLen;
                            } else {
                                serverOp = serverIt.hasNext() ? serverIt.next() : null;
                            }
                        } else if (serverOp.containsKey("delete")) {
                            int serverDelete = ((Number) serverOp.get("delete")).intValue();
                            remaining = Math.max(0, remaining - serverDelete);
                            serverOp = serverIt.hasNext() ? serverIt.next() : null;
                        } else {
                            break;
                        }
                    }

                    if (remaining > 0) {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("delete", remaining);
                        result.add(r);
                    }
                } else {
                    result.add(new LinkedHashMap<>(clientOp));
                }
            }

            while (serverOp != null) {
                if (serverOp.containsKey("insert")) {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("retain", getInsertLength(serverOp));
                    result.add(r);
                }
                serverOp = serverIt.hasNext() ? serverIt.next() : null;
            }

            return result.isEmpty() ? clientOps : result;
        } catch (Exception e) {
            log.warn("OT transform failed, returning original ops: {}", e.getMessage());
            return clientOps;
        }
    }

    private int getInsertLength(Map<String, Object> op) {
        Object insert = op.get("insert");
        if (insert instanceof String) {
            return ((String) insert).length();
        }
        return 1;
    }

    public int getCurrentVersion(Long docId) {
        return documentVersions.computeIfAbsent(docId, k -> new AtomicInteger(0)).get();
    }

    public void removeDocument(Long docId) {
        operationHistory.remove(docId);
        documentVersions.remove(docId);
    }
}
