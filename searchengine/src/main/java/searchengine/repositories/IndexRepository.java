package searchengine.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<Index, Long> {
    Index findById(Integer id);
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM index_tab WHERE page_id = ?1", nativeQuery = true)
    void deleteByPageId(Integer pageId);
    @Query(value = "SELECT page_id FROM index_tab WHERE lemma_id = ?1", nativeQuery = true)
    List<Integer> findPagesByLemmaId(Integer lemmaId);

    @Query(value = "SELECT id FROM index_tab WHERE lemma_id = ?1 AND page_id = ?2", nativeQuery = true)
    Integer existsByLemmaAndPageId(Integer lemmaId, Integer pageId);

}
