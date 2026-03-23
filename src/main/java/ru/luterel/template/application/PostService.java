package ru.luterel.template.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.luterel.template.api.dto.CreatePostRequest;
import ru.luterel.template.api.dto.PostMapper;
import ru.luterel.template.api.dto.PostResponse;
import ru.luterel.template.domain.PostEntity;
import ru.luterel.template.infrastructure.persistence.PostRepository;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostMapper postMapper;

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
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Post not found: " + id));
        return postMapper.fromEntityToResponse(post);
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
        return postMapper.fromEntityToResponse(updatedPost);
    }

    public void delete(Long id) {
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Post not found: " + id));
        postRepository.delete(post);
    }
}
