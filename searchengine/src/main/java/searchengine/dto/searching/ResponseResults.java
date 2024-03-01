package searchengine.dto.searching;

import lombok.Data;

@Data
public class ResponseResults {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private double relevance;
}
