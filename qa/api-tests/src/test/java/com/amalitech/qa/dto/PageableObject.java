package com.amalitech.qa.dto;

import com.amalitech.qa.tests.search.SortObject;

public class PageableObject {
    private Boolean unpaged;
    private Boolean paged;
    private Integer pageNumber;
    private Integer pageSize;
    private Long offset;
    private SortObject sort;

    public PageableObject() {
    }

    public Boolean getUnpaged() {
        return unpaged;
    }

    public void setUnpaged(Boolean unpaged) {
        this.unpaged = unpaged;
    }

    public Boolean getPaged() {
        return paged;
    }

    public void setPaged(Boolean paged) {
        this.paged = paged;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public SortObject getSort() {
        return sort;
    }

    public void setSort(SortObject sort) {
        this.sort = sort;
    }
}
