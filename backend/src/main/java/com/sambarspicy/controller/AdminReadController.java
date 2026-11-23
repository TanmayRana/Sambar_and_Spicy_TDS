package com.sambarspicy.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.sambarspicy.dto.UserDto;
import com.sambarspicy.model.AuditLog;
import com.sambarspicy.model.User;
import com.sambarspicy.repository.AuditLogRepository;
import com.sambarspicy.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AdminReadController {
    private static final Pattern SENSITIVE = Pattern.compile(
            "password|passwd|secret|token|authorization|credential|cvv|(^|_)pan($|_)|hash",
            Pattern.CASE_INSENSITIVE);
    private final UserRepository users;
    private final AuditLogRepository audits;
    private final ObjectMapper mapper;

    public AdminReadController(UserRepository users, AuditLogRepository audits, ObjectMapper mapper) {
        this.users = users;
        this.audits = audits;
        this.mapper = mapper;
    }

    @GetMapping({"/api/admin/users", "/api/v1/admin/users"})
    @Transactional(readOnly = true)
    public List<UserDto> getUsers() {
        return users.findAll(Sort.by("id")).stream().map(this::toUserDto).toList();
    }

    @GetMapping({"/api/admin/audit-logs", "/api/v1/admin/audit-logs"})
    @Transactional(readOnly = true)
    public List<AuditDto> getAuditLogs() {
        return audits.findAll(Sort.by(Sort.Direction.DESC, "createdAt", "id")).stream()
                .map(this::toAuditDto).toList();
    }

    public record AuditDto(
            Long id,
            @JsonProperty("actor_user_id") Integer actorUserId,
            String action,
            @JsonProperty("entity_type") String entityType,
            @JsonProperty("entity_id") String entityId,
            @JsonProperty("old_data") JsonNode oldData,
            @JsonProperty("new_data") JsonNode newData,
            @JsonProperty("ip_address") String ipAddress,
            @JsonProperty("created_at") LocalDateTime createdAt) {}

    private UserDto toUserDto(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getFullName(), user.getRole(),
                user.getIsActive(), user.getLastLoginAt(), user.getCreatedAt(), user.getUpdatedAt());
    }

    private AuditDto toAuditDto(AuditLog log) {
        return new AuditDto(log.getId(), log.getActorUser() == null ? null : log.getActorUser().getId(),
                log.getAction(), log.getEntityType(), log.getEntityId(),
                redactJson(log.getOldData()), redactJson(log.getNewData()), log.getIpAddress(), log.getCreatedAt());
    }

    private JsonNode redactJson(String json) {
        if (json == null) return null;
        try {
            return redact(mapper.readTree(json));
        } catch (Exception ignored) {
            return TextNode.valueOf("[UNAVAILABLE]");
        }
    }

    private JsonNode redact(JsonNode node) {
        if (node == null) return null;
        if (node.isObject()) {
            ObjectNode result = mapper.createObjectNode();
            node.fields().forEachRemaining(entry -> result.set(entry.getKey(),
                    SENSITIVE.matcher(entry.getKey().toLowerCase(Locale.ROOT)).find()
                            ? TextNode.valueOf("[REDACTED]") : redact(entry.getValue())));
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = mapper.createArrayNode();
            node.forEach(value -> result.add(redact(value)));
            return result;
        }
        if (node.isTextual()) {
            String value = node.textValue();
            if (value.startsWith("data:")) {
                return TextNode.valueOf("[IMAGE DATA OMITTED]");
            }
            String trimmed = value.trim();
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                try {
                    return redact(mapper.readTree(trimmed));
                } catch (Exception ignored) {
                    return TextNode.valueOf("[UNAVAILABLE]");
                }
            }
        }
        return node;
    }
}
