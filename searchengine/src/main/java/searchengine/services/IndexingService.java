package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingService {
    private ForkJoinPool forkJoinPool = new ForkJoinPool();
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private boolean stopIndex = false;

    public void startIndexing() {
        indexSites();
    }
    public void indexSites() {
            List<Site> sitesList = sites.getSites();
            for (Site site : sitesList) {
                SiteIndex siteIndex = new SiteIndex();
                forkJoinPool.execute(() -> {
                    try {
                        if (siteRepository.existsByUrl(site.getUrl())) {
                            siteRepository.delete(siteRepository.findByUrl(site.getUrl()));
                        }

                        siteIndex.setUrl(site.getUrl());
                        siteIndex.setName(site.getName());
                        siteIndex.setStatusEnum(Status.INDEXING);
                        Document doc = Jsoup.connect(site.getUrl())
                                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                                .referrer("http://www.google.com")
                                .get();
                        siteRepository.save(siteIndex);
                        Elements links = doc.select("a[href]");
                        Elements filteredLinks = new Elements();
                        for (Element link : links) {
                            String href = link.attr("href");
                            if (href.startsWith("/") & !href.equals("/")) {
                                filteredLinks.add(link);
                            }
                        }
                        indexPages(filteredLinks, siteIndex);
                    } catch (Exception e) {
                        siteIndex.setLastError("Error while indexing site: " + site.getUrl() + "; " + e);
                        siteIndex.setStatusEnum(Status.FAILED);
                        siteRepository.save(siteIndex);
                    }
                });
            }
    }
    private void indexPages(Elements links, SiteIndex siteIndex) {

        for (Element link : links) {
            siteIndex.setStatusTime(LocalDateTime.now());
            siteRepository.save(siteIndex);
            Page page = new Page();
            page.setSiteId(siteIndex);
            String href = link.attr("href");
            if (pageRepository.existsByPage(pageRepository.findByPath(href))) {
                Page deletePage = pageRepository.findByPath(href);
                indexRepository.deleteByPageId(deletePage.getId());
                lemmaRepository.deleteLemmasByPageId(deletePage.getId());
                pageRepository.delete(deletePage);
            }


            Page examplePage = new Page();
            examplePage.setPath(href);
            Example<Page> example = Example.of(examplePage);
            if (!pageRepository.exists(example)) {
                page.setPath(href);
                Document doc;
                try {
                    doc = Jsoup.connect(link.absUrl("href"))
                            .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                            .referrer("http://www.google.com")
                            .get();
                    Connection.Response response = Jsoup.connect(link.absUrl("href")).execute();
                    int responseCode = response.statusCode();

                    page.setCode(responseCode);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                page.setContent(doc.toString());
                pageRepository.save(page);
                try {
                    findLemmas(page, siteIndex);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if(link.equals(links.last())) {
                    shutdown(siteIndex);
                    return;
                }
                if(stopIndex) {
                    shutdownNow(siteIndex);
                    return;
                }
            }
        }

    }
    public boolean indexPage(String url){
        String baseUrl = "";
        String decodedUrlString = "";
        String pagePath = "";
        try {
            decodedUrlString = java.net.URLDecoder.decode(url, "UTF-8");
            int pathEndIndex = decodedUrlString.indexOf("/", decodedUrlString.indexOf("//") + 2);
            baseUrl = decodedUrlString.substring(0, pathEndIndex);
            pagePath = decodedUrlString.substring(pathEndIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<Site> sitesList = sites.getSites();
        int equalUrl = 0;
        for (Site site : sitesList) {
            String decodedSiteUrl = "";
            try {
                decodedSiteUrl = java.net.URLDecoder.decode(site.getUrl(), "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (baseUrl.equals(decodedSiteUrl)){
                equalUrl++;
                SiteIndex siteIndex = siteRepository.findByUrl(baseUrl);
                if (pageRepository.existsByPage(pageRepository.findByPath(pagePath))) {
                    Page page = pageRepository.findByPath(pagePath);
                    indexRepository.deleteByPageId(page.getId());
                    lemmaRepository.deleteLemmasByPageId(page.getId());
                    pageRepository.delete(page);
                }
                Page page = new Page();
                page.setSiteId(siteIndex);
                page.setPath(pagePath);
                Document doc;
                try {
                    doc = Jsoup.connect(decodedUrlString)
                            .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                            .referrer("http://www.google.com")
                            .get();
                    Connection.Response response = Jsoup.connect(decodedUrlString).execute();
                    int responseCode = response.statusCode();
                    if (response.statusCode() != 200) {
                        continue;
                    }

                    page.setCode(responseCode);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                page.setContent(doc.toString());
                pageRepository.save(page);
                try {
                    findLemmas(page, siteIndex);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (equalUrl >= 1) {
            return true;
        }
        return false;
    }
    private void findLemmas(Page page, SiteIndex site) throws IOException {
        String content = page.getContent();
        LemmaFinder finder = LemmaFinder.getInstance();
        Map<String, Integer> lemmaMap = finder.collectLemmas(content);
        lemmaMap.forEach((lemma, count) -> {

            if(lemmaRepository.existsByLemma(lemma)) {
            Lemma foundLemma = lemmaRepository.findbyLemma(lemma);
            Index index = new Index();
            index.setLemmaId(foundLemma);
            index.setPageId(page);
            index.setRank(count);
            int newFrequency = foundLemma.getFrequency() + 1;
            foundLemma.setFrequency(newFrequency);
            foundLemma.setSiteId(site);
            foundLemma.setLemma(lemma);
            lemmaRepository.save(foundLemma);
            indexRepository.save(index);
            } else {
                Index index = new Index();
                Lemma newLemma = new Lemma();
                newLemma.setFrequency(1);
                newLemma.setSiteId(site);
                newLemma.setLemma(lemma);
                index.setLemmaId(newLemma);
                index.setPageId(page);
                index.setRank(count);
                lemmaRepository.save(newLemma);
                indexRepository.save(index);
            };
        });

    }
    private void shutdown(SiteIndex site) {
        forkJoinPool.shutdown();
        site.setStatusEnum(Status.INDEXED);
        siteRepository.save(site);
        try {
            forkJoinPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    public void stopIndexing() {
        forkJoinPool.execute(() ->
            stopIndex = true);
    }
    private void shutdownNow(SiteIndex site) {
        forkJoinPool.shutdownNow();
        site.setStatusEnum(Status.FAILED);
        site.setLastError("Индексация остановлена пользователем");
        siteRepository.save(site);
    }

}
