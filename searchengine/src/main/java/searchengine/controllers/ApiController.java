package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.searching.ResponseResults;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchingService;
import searchengine.services.StatisticsService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchingService searchingService;
    private boolean isIndexingInProgress = false;
    @Autowired

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SearchingService searchingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchingService = searchingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
    @GetMapping("/startIndexing")
    public ResponseEntity<Map<String, Object>> startIndexing() {
        Map<String, Object> response = new HashMap<>();

        if (isIndexingInProgress) {
            response.put("result", false);
            response.put("error", "Индексация уже запущена");
            return ResponseEntity.badRequest().body(response);
        }

        indexingService.startIndexing();
        isIndexingInProgress = true;

        response.put("result", true);
        return ResponseEntity.ok().body(response);
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<Map<String, Object>> stopIndexing() {
        Map<String, Object> response = new HashMap<>();

        if (!isIndexingInProgress) {
            response.put("result", false);
            response.put("error", "Индексация не запущена");
            return ResponseEntity.badRequest().body(response);
        }

        indexingService.stopIndexing();
        isIndexingInProgress = true;

        response.put("result", true);
        return ResponseEntity.ok().body(response);
    }
    @PostMapping("/indexPage")
    public ResponseEntity<Map<String, Object>> indexPage(@RequestBody String url) {
        Map<String, Object> response = new HashMap<>();
        String subUrl = url.substring(4);
        boolean isSuccess = indexingService.indexPage(subUrl);
        if (isSuccess) {
            response.put("result", true);
            return ResponseEntity.ok().body(response);
        } else {
            response.put("result", false);
            response.put("error", "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchPages(String query, String site, int offset, int limit) {
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
                    responseResults = searchingService.searchPagesOnAllSites(query);
                } else {
                    responseResults = searchingService.searchPagesOnOneSite(query, site);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            response.put("count", responseResults.size());
            response.put("data", responseResults);

            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
    }
}
