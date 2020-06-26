package com.amit.testcontainer.controller;

import com.amit.testcontainer.bean.ContentVo;
import com.amit.testcontainer.entity.Content;
import com.amit.testcontainer.repository.ContentRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@SqlGroup({
        @Sql(value = "classpath:createContent.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
        @Sql(value = "classpath:deleteContent.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
})
@ActiveProfiles("test")
@Import(RedisConfiguration.class)
public class ContentControllerTest {

    private static int EXPECTED_QUERY_COUNT = 0;
    @LocalServerPort
    private Long port;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private ContentRepository contentRepository;

    @BeforeAll
    public static void init() {
        RedisContainer.REDIS_CONTAINER.start();
    }

    @AfterAll
    public static void finish() {
        RedisContainer.REDIS_CONTAINER.stop();
    }

    @BeforeEach
    public void doSanity() {
        assertThat(RedisContainer.REDIS_CONTAINER.isRunning()).isTrue();
    }

    @Test
    public void WhenGetAllContentsThenGetContentFromDBAndFromCache() {

        String addContentUrl = "http://localhost:" + port + "/api/contents";
        ResponseEntity<List<ContentVo>> responseEntity = restTemplate.exchange(addContentUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<ContentVo>>() {
        });

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody().size()).isEqualTo(3);

        Iterable<Content> dbEntity = contentRepository.findAll();
        List<Content> dbContents = new ArrayList<>();
        dbEntity.forEach(dbContents::add);
        assertThat(dbContents.size()).isEqualTo(responseEntity.getBody().size());

        Cache.ValueWrapper contentWapper = cacheManager.getCache("all.content.cache").get("getContents");
        List<ContentVo> cachedVo = (List<ContentVo>) contentWapper.get();
        assertThat(cachedVo.size()).isEqualTo(dbContents.size());

    }

    @Test
    public void givenContentWhenAddedThenAddedInDBAndCache() {

        String getContentsUrl = "http://localhost:" + port + "/api/contents";
        ContentVo contentVo = ContentVo.builder().title("MI-4").yearOfRelease(2008L).build();
        HttpEntity<ContentVo> entity = new HttpEntity<>(contentVo);
        ResponseEntity<ContentVo> responseEntity = restTemplate.exchange(getContentsUrl, HttpMethod.POST, entity, ContentVo.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(responseEntity.getBody().getTitle()).isEqualTo("MI-4");
        assertThat(responseEntity.getBody().getYearOfRelease()).isEqualTo(2008L);

        Long id = responseEntity.getBody().getId();

        Optional<Content> dbEntity = contentRepository.findById(id);
        assertThat(dbEntity.get().getTitle()).isEqualTo("MI-4");
        assertThat(dbEntity.get().getYearOfRelease()).isEqualTo(2008L);
        EXPECTED_QUERY_COUNT++;

        Cache.ValueWrapper contentWapper = cacheManager.getCache("content.cache").get(id);
        ContentVo cachedVo = (ContentVo) contentWapper.get();
        assertThat(cachedVo.getTitle()).isEqualTo("MI-4");
        assertThat(cachedVo.getYearOfRelease()).isEqualTo(2008L);

        final SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);

    }

    @Test
    public void givenIdWhenGetContentThenGetContentFromDBAndInCacheAndNextTimeFromCache() {

        final SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();

        String getContentUrl = "http://localhost:" + port + "/api/contents/1";

        ResponseEntity<ContentVo> responseEntity = restTemplate.exchange(getContentUrl, HttpMethod.GET, null, ContentVo.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody().getTitle()).isEqualTo("MI-1");
        assertThat(responseEntity.getBody().getYearOfRelease()).isEqualTo(2002L);
        EXPECTED_QUERY_COUNT++;
        assertEquals(EXPECTED_QUERY_COUNT, statistics.getEntityLoadCount());

        Optional<Content> dbEntity = contentRepository.findById(1L);
        assertThat(dbEntity.get().getTitle()).isEqualTo("MI-1");
        assertThat(dbEntity.get().getYearOfRelease()).isEqualTo(2002L);
        EXPECTED_QUERY_COUNT++;
        assertEquals(EXPECTED_QUERY_COUNT, statistics.getEntityLoadCount());

        Cache.ValueWrapper contentWapper = cacheManager.getCache("content.cache").get(1L);
        ContentVo cachedVo = (ContentVo) contentWapper.get();
        assertThat(cachedVo.getTitle()).isEqualTo("MI-1");
        assertThat(cachedVo.getYearOfRelease()).isEqualTo(2002L);

        responseEntity = restTemplate.exchange(getContentUrl, HttpMethod.GET, null, ContentVo.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody().getTitle()).isEqualTo("MI-1");
        assertThat(responseEntity.getBody().getYearOfRelease()).isEqualTo(2002L);
        assertEquals(EXPECTED_QUERY_COUNT, statistics.getEntityLoadCount());

    }

    @Test
    public void givenIdWhenUpdateContentThenContentUpdatedInDBAndInCache() {

        String getContentUrl = "http://localhost:" + port + "/api/contents/1";

        ContentVo contentVo = ContentVo.builder().title("MI-4").yearOfRelease(2008L).id(1L).build();
        HttpEntity<ContentVo> entity = new HttpEntity<>(contentVo);
        ResponseEntity<Void> responseEntity = restTemplate.exchange(getContentUrl, HttpMethod.PUT, entity, Void.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Optional<Content> dbEntity = contentRepository.findById(1L);
        assertThat(dbEntity.get().getTitle()).isEqualTo("MI-4");
        assertThat(dbEntity.get().getYearOfRelease()).isEqualTo(2008L);
        EXPECTED_QUERY_COUNT++;

        Cache.ValueWrapper contentWapper = cacheManager.getCache("content.cache").get(1L);
        ContentVo cachedVo = (ContentVo) contentWapper.get();
        assertThat(cachedVo.getTitle()).isEqualTo("MI-4");
        assertThat(cachedVo.getYearOfRelease()).isEqualTo(2008L);

    }

    @Test
    public void givenIdWhenDeleteContentThenContentDeletedInDBAndInCache() {

        String getContentUrl = "http://localhost:" + port + "/api/contents/1";

        ResponseEntity<ContentVo> responseEntity = restTemplate.exchange(getContentUrl, HttpMethod.GET, null, ContentVo.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody().getTitle()).isEqualTo("MI-1");
        assertThat(responseEntity.getBody().getYearOfRelease()).isEqualTo(2002L);
        EXPECTED_QUERY_COUNT++;

        Cache.ValueWrapper contentWapper = cacheManager.getCache("content.cache").get(1L);
        ContentVo cachedVo = (ContentVo) contentWapper.get();
        assertThat(cachedVo.getTitle()).isEqualTo("MI-1");
        assertThat(cachedVo.getYearOfRelease()).isEqualTo(2002L);

        ResponseEntity<Void> voidResponseEntity = restTemplate.exchange(getContentUrl, HttpMethod.DELETE, null, Void.class);

        assertThat(voidResponseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Optional<Content> dbEntity = contentRepository.findById(1L);
        assertThat(dbEntity.isPresent()).isFalse();
        EXPECTED_QUERY_COUNT++;

        contentWapper = cacheManager.getCache("content.cache").get(1L);
        assertThat(contentWapper).isNull();

    }


}
