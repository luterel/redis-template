package ru.luterel.template.infrastructure.cache;

import ru.luterel.template.api.dto.PostResponse;

import java.util.Optional;

public interface PostCacheService {

    Optional<PostResponse> getById(Long id);

    void put(PostResponse post);

    void evictById(Long id);
}
