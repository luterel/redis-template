package ru.luterel.template.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.luterel.template.api.dto.CreatePostRequest;
import ru.luterel.template.api.dto.PostResponse;
import ru.luterel.template.application.PostService;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@RequestBody @Valid CreatePostRequest request) {
        return postService.create(request);
    }

    @GetMapping("/{id}")
    public PostResponse getById(@PathVariable Long id) {
        return postService.getById(id);
    }

    @PutMapping("/{id}")
    public PostResponse update(
            @PathVariable Long id,
            @RequestBody @Valid CreatePostRequest request
    ) {
        return postService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        postService.delete(id);
    }
}
