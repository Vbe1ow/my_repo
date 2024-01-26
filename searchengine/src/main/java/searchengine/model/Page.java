package searchengine.model;

import javax.persistence.*;

@Entity
@Table(name = "page", indexes = {@Index(name = "path_index", columnList = "path")})

public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;
    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SiteIndex siteId;
    @Column(name = "path", columnDefinition = "text", nullable = false)
    private String path;
    @Column(name = "code")
    private int code;
    @Column(name = "content", columnDefinition = "mediumtext")
    private String content;
}
