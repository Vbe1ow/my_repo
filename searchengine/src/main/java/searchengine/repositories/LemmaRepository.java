package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.SiteIndex;
import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {
    @Query(value = "SELECT site_id FROM Lemma WHERE lemma = ?1", nativeQuery = true)
    Integer getSiteId(String lemma);
    @Query("SELECT l FROM Lemma l WHERE l.lemma = :lemma")
    Lemma findbyLemma(String lemma);
    boolean existsByLemma(String lemma);
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM lemma WHERE id IN (SELECT lemma_id FROM index_tab WHERE page_id = ?1)", nativeQuery = true)
    void deleteLemmasByPageId(Integer pageId);
    @Query(value = "SELECT * FROM lemma WHERE site_id = :siteId", nativeQuery = true)
    List<Lemma> findAllBySiteId(@Param("siteId") SiteIndex siteId);
    @Query(value = "SELECT MAX(frequency) FROM Lemma")
    Integer findMaxFrequency();
}
