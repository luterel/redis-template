package ru.luterel.template.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePostRequest(
        @NotBlank String title,
        @NotBlank String body,
        @NotBlank String status,
        @NotBlank String tags
) {
}
