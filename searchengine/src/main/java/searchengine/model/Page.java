package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "page")

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
