package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.SiteIndex;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    Page findByPath(String pagePath);
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Page p WHERE p = :page")
    boolean existsByPage(Page page);
    @Query(value = "SELECT * FROM page WHERE site_id = :siteId", nativeQuery = true)
    List<Page> findAllBySiteId(@Param("siteId") SiteIndex siteId);
}
