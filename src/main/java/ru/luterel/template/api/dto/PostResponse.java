package ru.luterel.template.api.dto;

import java.time.Instant;

public record PostResponse(
        Long id,
        String title,
        String body,
        String status,
        String tags,
        Instant updatedAt
) {
}
