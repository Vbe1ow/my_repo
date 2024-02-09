package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;

import javax.transaction.Transactional;

@Repository
public interface IndexRepository extends JpaRepository<Index, Long> {
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM index_tab WHERE page_id = ?1", nativeQuery = true)
    void deleteByPageId(Integer pageId);
}
