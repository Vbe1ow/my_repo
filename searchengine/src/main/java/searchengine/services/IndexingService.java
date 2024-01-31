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
import searchengine.model.Page;
import searchengine.model.SiteIndex;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
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

                        siteIndex.setUrl(site.getUrl());
                        siteRepository.delete(siteIndex);
                        siteIndex.setName(site.getName());
                        siteIndex.setStatusEnum(Status.INDEXING);
                        Document doc = Jsoup.connect(site.getUrl())
                                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                                .referrer("http://www.google.com")
                                .get();
                        siteRepository.save(siteIndex);
                        indexPages(doc, siteIndex);
                    } catch (Exception e) {
                        siteIndex.setLastError("Error while indexing site: " + site.getUrl() + "; " + e);
                        siteIndex.setStatusEnum(Status.FAILED);
                        siteRepository.save(siteIndex);
                    }
                });
            }
    }
    private void indexPages(Document doc, SiteIndex siteIndex) {
        Elements links = doc.select("a[href]");
        Elements filteredLinks = new Elements();
        for (Element link : links) {
            String href = link.attr("href");
            if (href.startsWith("/")) {
                filteredLinks.add(link);
            }
        }
        for (Element link : filteredLinks) {
            siteIndex.setStatusTime(LocalDateTime.now());
            siteRepository.save(siteIndex);
            Page page = new Page();
            page.setSiteId(siteIndex);
            pageRepository.delete(page);
            String href = link.attr("href");
            Page examplePage = new Page();
            examplePage.setPath(href);
            Example<Page> example = Example.of(examplePage);
            if (!pageRepository.exists(example)) {
                page.setPath(href);
                try {
                    Connection.Response response = Jsoup.connect(link.absUrl("href")).execute();
                    int responseCode = response.statusCode();

                    page.setCode(responseCode);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                page.setContent(doc.toString());
                pageRepository.save(page);
                if(link.equals(filteredLinks.last())) {
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
    private void shutdown(SiteIndex site) {
        forkJoinPool.shutdown();
        site.setStatusEnum(Status.INDEXED);
        siteRepository.save(site);
        try {
            forkJoinPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            log.error("Error while shutting down ForkJoinPool", e);
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
