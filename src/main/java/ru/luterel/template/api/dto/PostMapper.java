package ru.luterel.template.api.dto;

import org.springframework.stereotype.Component;
import ru.luterel.template.domain.PostEntity;

@Component
public class PostMapper {

    public PostResponse fromEntityToResponse(PostEntity entity) {
        return new PostResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getBody(),
                entity.getStatus(),
                entity.getTags(),
                entity.getUpdatedAt()
        );
    }
}
