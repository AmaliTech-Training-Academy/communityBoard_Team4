package com.amalitech.qa.dto;

import java.util.List;

import com.amalitech.qa.tests.post.PostResponse;
import com.amalitech.qa.tests.search.SortObject;

public class PagePostResponse {
    private Integer totalPages;
    private Long totalElements;
    private Boolean last;
    private PageableObject pageable;
    private Integer numberOfElements;
    private Boolean first;
    private Integer size;
    private List<PostResponse> content;
    private Integer number;
    private SortObject sort;
    private Boolean empty;

    public PagePostResponse() {
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(Long totalElements) {
        this.totalElements = totalElements;
    }

    public Boolean getLast() {
        return last;
    }

    public void setLast(Boolean last) {
        this.last = last;
    }

    public PageableObject getPageable() {
        return pageable;
    }

    public void setPageable(PageableObject pageable) {
        this.pageable = pageable;
    }

    public Integer getNumberOfElements() {
        return numberOfElements;
    }

    public void setNumberOfElements(Integer numberOfElements) {
        this.numberOfElements = numberOfElements;
    }

    public Boolean getFirst() {
        return first;
    }

    public void setFirst(Boolean first) {
        this.first = first;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public List<PostResponse> getContent() {
        return content;
    }

    public void setContent(List<PostResponse> content) {
        this.content = content;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public SortObject getSort() {
        return sort;
    }

    public void setSort(SortObject sort) {
        this.sort = sort;
    }

    public Boolean getEmpty() {
        return empty;
    }

    public void setEmpty(Boolean empty) {
        this.empty = empty;
    }
}
