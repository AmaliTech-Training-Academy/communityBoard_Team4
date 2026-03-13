import React, { useState, useEffect, useCallback } from "react";
import { useNavigate, Outlet } from "react-router-dom";
import { Badge, CategoryType } from "../../components/ui/Badge";
import { PostCard, Post } from "../../components/features/posts/PostCard";
import { EmptyFeed } from "../../components/features/posts/EmptyFeed";
import { Navbar } from "../../components/layout/Navbar";
import api from "../../services/api";
import "./PostFeed.css";

const CATEGORIES: CategoryType[] = [
  "All",
  "EVENT",
  "NEWS",
  "DISCUSSION",
  "ALERT",
];

export function PostFeed() {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [activeCategory, setActiveCategory] = useState<CategoryType>("All");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  const [posts, setPosts] = useState<Post[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);

  const onPostCreated = useCallback(() => {
    setSearchQuery("");
    setDebouncedSearch("");
    setActiveCategory("All");
    setStartDate("");
    setEndDate("");
    setPage(0);
    setRefreshKey((k) => k + 1);
  }, []);

  // Debounce search input
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchQuery);
    }, 300);
    return () => clearTimeout(handler);
  }, [searchQuery]);

  // Reset page when search, category, or date range changes
  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, activeCategory, startDate, endDate]);

  // Fetch posts from API
  useEffect(() => {
    const fetchPosts = async () => {
      setLoading(true);
      try {
        const params: any = { page, size: 10 };
        if (activeCategory !== "All") params.category = activeCategory;
        if (debouncedSearch) params.keyword = debouncedSearch;
        // Convert YYYY-MM-DD (HTML date input) to ISO-8601 datetime for the API
        if (startDate) params.startDate = `${startDate}T00:00:00`;
        if (endDate) params.endDate = `${endDate}T23:59:59`;

        // Use the search endpoint whenever any filter is active
        const endpoint =
          activeCategory === "All" && !debouncedSearch && !startDate && !endDate
            ? "/posts"
            : "/posts/search";

        const { data } = await api.get(endpoint, { params });
        setPosts(data.content || []);
        setTotalPages(data.totalPages || 0);
        setError("");
      } catch (err) {
        console.error("Failed to load posts", err);
        setPosts([]);
        setError("Failed to load posts. Please try again.");
      } finally {
        setLoading(false);
      }
    };
    fetchPosts();
  }, [page, debouncedSearch, activeCategory, startDate, endDate, refreshKey]);

  const handleSearchSubmit = () => {
    setDebouncedSearch(searchQuery);
    setPage(0);
  };

  return (
    <div className="feed-page-container">
      <Navbar />

      <main className="feed-main-content">
        {/* Top Actions Row: Search & Create */}
        <div className="feed-actions-row">
          <div className="search-and-submit">
            <div className="search-bar-container">
              <img
                src="/assets/search.svg"
                alt="Search"
                className="search-icon-img"
              />
              <input
                type="text"
                placeholder="Search by title of post..."
                className="search-input"
                data-testid="search-input"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleSearchSubmit()}
              />
              {searchQuery && (
                <button
                  className="clear-search-btn"
                  onClick={() => setSearchQuery("")}
                >
                  &times;
                </button>
              )}
            </div>

            <button
              className="search-submit-btn"
              onClick={handleSearchSubmit}
              data-testid="search-submit-btn"
            >
              <img
                src="/assets/search.svg"
                alt="Search"
                className="search-submit-icon"
              />
            </button>
          </div>

          <button
            className="create-post-btn"
            onClick={() => navigate("/create")}
            data-testid="create-post-btn"
          >
            <img src="/assets/plus.svg" alt="Plus" className="plus-icon-img" />
            <span>Create post</span>
          </button>
        </div>

        {/* Categories Row */}
        <div className="categories-row">
          <span className="categories-label">Categories:</span>
          <div className="categories-list">
            {CATEGORIES.map((category) => (
              <Badge
                key={category}
                category={category}
                isFilter={true}
                isActive={activeCategory === category}
                onClick={() => setActiveCategory(category)}
              />
            ))}
          </div>
        </div>

        {/* Date Range Filter Row */}
        <div className="date-filter-row">
          <span className="date-filter-label">Date range:</span>
          <div className="date-filter-inputs">
            <input
              type="date"
              className="date-filter-input"
              data-testid="start-date-input"
              value={startDate}
              max={endDate || undefined}
              onChange={(e) => setStartDate(e.target.value)}
            />
            <span className="date-filter-separator">to</span>
            <input
              type="date"
              className="date-filter-input"
              data-testid="end-date-input"
              value={endDate}
              min={startDate || undefined}
              onChange={(e) => setEndDate(e.target.value)}
            />
            {(startDate || endDate) && (
              <button
                className="date-filter-clear"
                onClick={() => { setStartDate(""); setEndDate(""); }}
              >
                Clear
              </button>
            )}
          </div>
        </div>

        {/* Posts List */}
        <div className="posts-list">
          {loading ? (
            <p className="posts-loading-text">Loading posts...</p>
          ) : error ? (
            <p className="posts-error-text">{error}</p>
          ) : posts.length > 0 ? (
            posts.map((post: any) => (
              <PostCard
                key={post.id}
                post={{
                  ...post,
                  author: { id: post.authorEmail, name: post.authorName },
                  commentCount: post.commentCount || 0,
                }}
                onClick={(id: string) => navigate(`/post/${id}`)}
              />
            ))
          ) : (
            <EmptyFeed />
          )}
        </div>

        {/* Pagination */}
        {totalPages > 0 && (
          <div className="pagination-container">
            <button
              className="pagination-btn"
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Previous
            </button>

            {Array.from({ length: totalPages }, (_, i) => (
              <button
                key={i}
                className={`pagination-btn ${page === i ? "pagination-active" : ""}`}
                onClick={() => setPage(i)}
              >
                {i + 1}
              </button>
            ))}

            <button
              className="pagination-btn"
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            >
              Next
            </button>
          </div>
        )}
      </main>

      {/* Renders CreatePost modal when on /create */}
      <Outlet context={{ onPostCreated }} />
    </div>
  );
}
