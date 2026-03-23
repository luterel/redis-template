package ru.luterel.template.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.luterel.template.domain.PostEntity;

public interface PostRepository extends JpaRepository<PostEntity, Long> {
}
