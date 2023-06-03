package ru.store.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.store.catalog.domain.Image;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
}
