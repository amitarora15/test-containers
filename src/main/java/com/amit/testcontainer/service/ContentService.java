package com.amit.testcontainer.service;

import com.amit.testcontainer.bean.ContentVo;
import com.amit.testcontainer.entity.Content;
import com.amit.testcontainer.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentService {

    private final ContentRepository contentRepository;

    private ContentVo getVo(Content entity) {
        return ContentVo.builder().id(entity.getId()).description(entity.getDescription()).title(entity.getTitle()).yearOfRelease(entity.getYearOfRelease()).build();
    }

    private Content getEntity(ContentVo vo){
        return Content.builder().description(vo.getDescription()).id(vo.getId()).title(vo.getTitle()).yearOfRelease(vo.getYearOfRelease()).build();
    }

    @Cacheable(value="all.content.cache", key = "#root.methodName", unless= "#result.size() == 0")
    public List<ContentVo> getContents() {
        Iterable<Content> contents = contentRepository.findAll();
        List<ContentVo> vos = new ArrayList<>();
        contents.forEach(c -> vos.add(getVo(c)));
        log.info("Getting content from DB : {} ", vos);
        return vos;
    }

    public List<ContentVo> getLatestContents(Long releaseYear){
        Optional<List<Content>> contents = contentRepository.findAllByYearOfReleaseAfter(releaseYear);
        if(contents.isPresent()){
            log.info("Got content from DB : {} ", contents);
            return contents.get().stream().map(content -> getVo(content)).collect(Collectors.toList());
        }
        return null;
    }

    @Cacheable(value="content.cache", key = "#id", unless="#result == null")
    public ContentVo getContent(Long id){
        Optional<Content> content = contentRepository.findById(id);
        if(content.isPresent()) {
            log.info("Got content from DB : {} ", content);
            return getVo(content.get());
        }
        return null;
    }

    @Caching(
            put = { @CachePut(value="content.cache", key = "#result.id")},
            evict = { @CacheEvict(value = "all.content.cache", key = "getContents", allEntries = true)}
    )
    public ContentVo addContent(ContentVo vo){
        Content entity = getEntity(vo);
        entity = contentRepository.save(entity);
        log.info("Added new content in DB : {} ", entity);
        return getVo(entity);
    }


    @Caching(
            put = { @CachePut(value="content.cache", key = "#id")},
            evict = { @CacheEvict(value="all.content.cache", key = "getContents", allEntries = true)}
    )
    public ContentVo updateContent(Long id, ContentVo vo){
        Optional<Content> content = contentRepository.findById(id);
        if(content.isPresent()){
            Content entity = getEntity(vo);
            log.info("Updating content in DB : {} ", entity);
            entity = contentRepository.save(entity);
            return getVo(entity);
        } else {
            throw new IllegalArgumentException("Content not present");
        }
    }

    @Caching(
            evict = { @CacheEvict(value="content.cache", key = "#id"), @CacheEvict(value="all.content.cache", key = "getContents", allEntries = true)}
    )
    public void deleteContent(Long id) {
        Optional<Content> content = contentRepository.findById(id);
        if(content.isPresent()){
            log.info("Deleting content in DB : {} ", content.get());
            contentRepository.delete(content.get());
        } else {
            throw new IllegalArgumentException("Content not present");
        }
    }

}
