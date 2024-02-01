package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "index")
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;
    @ManyToOne
    @JoinColumn(name = "page_id", nullable = false)
    private Page pageId;
    @ManyToOne
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemmaId;
    @Column(name = "rank", nullable = false)
    private Float rank;
}
