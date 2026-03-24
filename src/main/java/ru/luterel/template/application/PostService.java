package ru.luterel.template.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.luterel.template.api.dto.CreatePostRequest;
import ru.luterel.template.api.dto.PostMapper;
import ru.luterel.template.api.dto.PostResponse;
import ru.luterel.template.domain.PostEntity;
import ru.luterel.template.infrastructure.cache.PostCacheService;
import ru.luterel.template.infrastructure.persistence.PostRepository;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final PostCacheService postCacheService;

    public PostResponse create(CreatePostRequest request) {
        PostEntity post = new PostEntity(
                request.title(),
                request.body(),
                request.status(),
                request.tags());
        PostEntity savedPost = postRepository.save(post);
        return postMapper.fromEntityToResponse(savedPost);
    }

    public PostResponse getById(Long id) {
        Optional<PostResponse> cachedPost = postCacheService.getById(id);

        if (cachedPost.isPresent()) {
            return cachedPost.get();
        }

        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Post not found: " + id));
        PostResponse response = postMapper.fromEntityToResponse(post);
        postCacheService.put(response);
        return response;
    }

    public PostResponse update(Long id, CreatePostRequest request) {
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Post not found: " + id));

        post.update(
                request.title(),
                request.body(),
                request.status(),
                request.tags()
        );

        PostEntity updatedPost = postRepository.save(post);
        postCacheService.evictById(id);
        return postMapper.fromEntityToResponse(updatedPost);
    }

    public void delete(Long id) {
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Post not found: " + id));
        postRepository.delete(post);
        postCacheService.evictById(id);
    }
}
