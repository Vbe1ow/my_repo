package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.searching.ResponseResults;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteIndex;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class SearchingService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    public ResponseEntity<Map<String, Object>> startSearching(String query, String site){
        Map<String, Object> response = new HashMap<>();
        if (query.equals("")) {
            response.put("result", false);
            response.put("error", "Задан пустой поисковый запрос");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } else {
            response.put("result", true);

            List<ResponseResults> responseResults;
            try {
                if (site == null) {
                    responseResults = searchPagesOnAllSites(query);
                } else {
                    responseResults = searchPagesOnOneSite(query, site);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            response.put("count", responseResults.size());
            response.put("data", responseResults);

            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
    }
    private List<Lemma> getClearedLemmasForOneSite(SiteIndex siteIndex, String query) throws IOException {
        List<Lemma> clearedLemmas = new ArrayList<>();
        LemmaFinder finder = LemmaFinder.getInstance();
        Set<String> lemmas = finder.getLemmaSet(query);
        int maxFrequency = lemmaRepository.findMaxFrequency();
        int threshold = maxFrequency - maxFrequency * 20/100;
        for(String lemma : lemmas) {
            if (lemmaRepository.existsByLemma(lemma) && lemmaRepository.getSiteId(lemma) == siteIndex.getId()) {
                if (lemmaRepository.findbyLemma(lemma).getFrequency() < threshold) {
                    clearedLemmas.add(lemmaRepository.findbyLemma(lemma));
                }
            }
        }
        return clearedLemmas;
    }
    public List<ResponseResults> searchPagesOnOneSite(String query, String site) throws IOException {
        SiteIndex siteIndex = siteRepository.findByUrl(site);
        Comparator<Lemma> frequencyComparator = Comparator.comparingInt(Lemma::getFrequency);
        List<Lemma> clearedLemmas = getClearedLemmasForOneSite(siteIndex, query);
        Collections.sort(clearedLemmas, frequencyComparator);
        List<Page> pages = new ArrayList<>();
        if (!clearedLemmas.isEmpty()) {
            indexRepository.findPagesByLemmaId(clearedLemmas.get(0).getId())
                    .forEach(page -> {
                        if (pageRepository.getSiteId(page) == siteIndex.getId()) {
                            pages.add(pageRepository.findById(page));
                        }
                    });
        } else {
            return new ArrayList<>();
        }
        pagesRemover(clearedLemmas, pages);
        if (pages.size() == 0) {
            return new ArrayList<>();
        }
        Map<Float, Page> rankPages = new HashMap<>();
        List<Float> ranks = new ArrayList<>();
        for (Page page : pages) {
            float totalRank = 0;
            for (Lemma lemma : clearedLemmas) {
                Index index = indexRepository.findById(indexRepository.existsByLemmaAndPageId(lemma.getId(), page.getId()));
                totalRank += index.getRank();
            }
            rankPages.put(totalRank, page);
            ranks.add(totalRank);
        }
        Collections.sort(ranks);
        Collections.reverse(ranks);
        Float maxRank = ranks.get(0);
        LinkedHashMap<Float, Page> relevanceMap = relevanceCounter(rankPages, maxRank);
        return generateResponseForOneSite(relevanceMap, siteIndex, clearedLemmas);
    }
    private void pagesRemover(List<Lemma> clearedLemmas, List<Page> pages) {
        for (int i = 1; i < clearedLemmas.size(); i++) {
            int lemmaId = clearedLemmas.get(i).getId();
            Iterator<Page> iterator = pages.iterator();
            while (iterator.hasNext()) {
                Page page = iterator.next();
                if (indexRepository.existsByLemmaAndPageId(lemmaId, page.getId()) == null) {
                    iterator.remove();
                }
            }
        }
    }

    private List<ResponseResults> generateResponseForOneSite(LinkedHashMap<Float, Page> relevanceMap, SiteIndex siteIndex, List<Lemma> clearedLemmas) {
        List<ResponseResults> results = new ArrayList<>();
        relevanceMap.forEach((aFloat, page) -> {
            ResponseResults result = new ResponseResults();
            result.setRelevance(aFloat);
            result.setUri(page.getPath());
            result.setSite(siteIndex.getUrl());
            result.setSiteName(siteIndex.getName());
            Document doc = Jsoup.parse(page.getContent());
            result.setTitle(doc.title());
            result.setSnippet(generateSnippets(page, clearedLemmas).toString());
            results.add(result);
        });
        return results;
    }

    private List<Lemma> getClearedLemmasForAllSite(String query) throws IOException {
        List<Lemma> clearedLemmas = new ArrayList<>();
        LemmaFinder finder = LemmaFinder.getInstance();
        Set<String> lemmas = finder.getLemmaSet(query);
        int maxFrequency = lemmaRepository.findMaxFrequency();
        int threshold = maxFrequency - maxFrequency * 20/100;
        for(String lemma : lemmas) {
            if (lemmaRepository.existsByLemma(lemma)) {
                if (lemmaRepository.findbyLemma(lemma).getFrequency() < threshold) {
                    clearedLemmas.add(lemmaRepository.findbyLemma(lemma));
                }
            }
        }
        return clearedLemmas;
    }
    public List<ResponseResults> searchPagesOnAllSites(String query) throws IOException {
        List<Lemma> clearedLemmas = getClearedLemmasForAllSite(query);
        Comparator<Lemma> frequencyComparator = Comparator.comparingInt(Lemma::getFrequency);
        Collections.sort(clearedLemmas, frequencyComparator);
        List<Page> pages = new ArrayList<>();
        if (!clearedLemmas.isEmpty()) {
            indexRepository.findPagesByLemmaId(clearedLemmas.get(0).getId())
                    .forEach(page -> pages.add(pageRepository.findById(page)));
        } else {
            return new ArrayList<>();
        }
        pagesRemover(clearedLemmas, pages);
        if (pages.size() == 0) {
            return new ArrayList<>();
        }
        Map<Float, Page> rankPages = new HashMap<>();
        List<Float> ranks = new ArrayList<>();
        for (Page page : pages) {
            float totalRank = 0;
            for (Lemma lemma : clearedLemmas) {
                Index index = indexRepository.findById(indexRepository.existsByLemmaAndPageId(lemma.getId(), page.getId()));
                totalRank += index.getRank();
            }
            rankPages.put(totalRank, page);
            ranks.add(totalRank);
        }
        Collections.sort(ranks);
        Collections.reverse(ranks);
        Float maxRank = ranks.get(0);
        LinkedHashMap<Float, Page> relevanceMap = relevanceCounter(rankPages, maxRank);
        relevanceMap.forEach((aFloat, page) -> log.info(String.valueOf(aFloat)));
        return generateResponseForAllSite(relevanceMap, clearedLemmas);
    }
    private List<ResponseResults> generateResponseForAllSite(LinkedHashMap<Float, Page> relevanceMap, List<Lemma> clearedLemmas) {
        List<ResponseResults> results = new ArrayList<>();
        relevanceMap.forEach((aFloat, page) -> {
            ResponseResults result = new ResponseResults();
            result.setRelevance(aFloat);
            result.setUri(page.getPath());
            SiteIndex site = siteRepository.findById(pageRepository.getSiteId(page.getId()));
            result.setSite(site.getUrl());
            result.setSiteName(site.getName());
            Document doc = Jsoup.parse(page.getContent());
            result.setTitle(doc.title());
            result.setSnippet(generateSnippets(page, clearedLemmas).toString());
            results.add(result);
        });
        return results;
    }
    private LinkedHashMap<Float, Page> relevanceCounter(Map<Float, Page> ranks, Float maxRank){
        Map<Float, Page> relevancePages = new HashMap<>();
        for (Map.Entry<Float, Page> entry : ranks.entrySet()) {
            Float rank = entry.getKey();
            Page page = entry.getValue();
            Float relevance = rank / maxRank;
            relevancePages.put(relevance, page);
        }
        List<Map.Entry<Float, Page>> entries = new ArrayList<>(relevancePages.entrySet());
        Collections.sort(entries, (entry1, entry2) -> Float.compare(entry2.getKey(), entry1.getKey()));
        LinkedHashMap<Float, Page> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<Float, Page> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }
    private List<String> generateSnippets(Page page, List<Lemma> clearedLemmas) {
        String htmlContent = page.getContent();
        List<String> snippets = new ArrayList<>();
        Document doc = Jsoup.parse(htmlContent);
        Elements elements = doc.getAllElements();
        int snippetLength = 3;
        for (Lemma lemma : clearedLemmas) {
            String query = lemma.getLemma();
            for (Element element : elements) {
                String text = element.ownText();
                if (!text.isEmpty() && text.contains(query)) {
                    snippets.add(text.replaceAll(query, "<b>" + query + "</b>"));
                    if (snippets.size() >= snippetLength) {
                        break;
                    }
                }
            }
            if (snippets.size() >= snippetLength) {
                break;
            }
        }
        return snippets;
    }
}
