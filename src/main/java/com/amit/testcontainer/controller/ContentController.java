package com.amit.testcontainer.controller;

import com.amit.testcontainer.bean.ContentVo;
import com.amit.testcontainer.service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contents")
public class ContentController {

    private final ContentService contentService;

    @GetMapping
    public List<ContentVo> getContents(@RequestParam(name = "yearOfRelease", required = false) Long yearOfRelease){

        if(yearOfRelease != null){
            return contentService.getLatestContents(yearOfRelease);
        } else {
            List<ContentVo> contents = contentService.getContents();
            return contents;
        }
    }

    @GetMapping("/{id}")
    public ContentVo getContent(@PathVariable("id") Long id){
        return contentService.getContent(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContentVo addContent(@RequestBody ContentVo content){
        return contentService.addContent(content);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateContent(@PathVariable Long id, @RequestBody ContentVo content){
        contentService.updateContent(id, content);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteContent(@PathVariable  Long id){
        contentService.deleteContent(id);
    }

}
