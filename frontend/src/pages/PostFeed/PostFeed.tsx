import React, { useState, useEffect } from "react";
import { useNavigate, useLocation, Outlet } from "react-router-dom";
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
  const location = useLocation();
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [activeCategory, setActiveCategory] = useState<CategoryType>("All");

  const [posts, setPosts] = useState<Post[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // Debounce search input
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchQuery);
    }, 300);
    return () => clearTimeout(handler);
  }, [searchQuery]);

  // Reset page when search or category changes
  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, activeCategory]);

  // Fetch posts from API
  useEffect(() => {
    const fetchPosts = async () => {
      setLoading(true);
      try {
        const params: any = { page, size: 10 };
        if (activeCategory !== "All") params.category = activeCategory;
        if (debouncedSearch) params.keyword = debouncedSearch;

        // Use the default sorted endpoint if no filters are applied
        const endpoint =
          activeCategory === "All" && !debouncedSearch
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
  }, [page, debouncedSearch, activeCategory, location.key]);

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

            <button className="search-submit-btn" onClick={handleSearchSubmit}>
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

        {/* Posts List */}
        <div className="posts-list">
          {loading ? (
            <p
              style={{
                textAlign: "center",
                color: "var(--body-text-tertiary)",
              }}
            >
              Loading posts...
            </p>
          ) : error ? (
            <p
              style={{
                textAlign: "center",
                color: "var(--state-error, #b91c1c)",
              }}
            >
              {error}
            </p>
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
      <Outlet />
    </div>
  );
}
