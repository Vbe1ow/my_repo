package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteIndex;

@Repository
public interface SiteRepository extends JpaRepository<SiteIndex, Long> {
}
