package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "lemma")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;
    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SiteIndex siteId;
    @Column(name = "lemma", columnDefinition = "varchar(255)", nullable = false)
    private String lemma;
    @Column(name = "frequency", nullable = false)
    private int frequency;
}
