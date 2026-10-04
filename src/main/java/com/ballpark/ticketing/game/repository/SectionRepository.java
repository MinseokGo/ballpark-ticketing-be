package com.ballpark.ticketing.game.repository;

import com.ballpark.ticketing.game.Section;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionRepository extends JpaRepository<Section, Long> {

	boolean existsByName(String name);
}
