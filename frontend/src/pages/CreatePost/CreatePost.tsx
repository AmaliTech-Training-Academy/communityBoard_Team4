import React, { useState } from "react";
import { useNavigate, useOutletContext } from "react-router-dom";
import api from "../../services/api";
import { useToast } from "../../context/ToastContext";
import {
  CategoryType,
  CATEGORY_DISPLAY_NAMES,
} from "../../components/ui/Badge";
import "./CreatePost.css";

const CATEGORIES: CategoryType[] = ["EVENT", "NEWS", "DISCUSSION", "ALERT"];

export function CreatePost() {
  const navigate = useNavigate();
  const { onPostCreated } = useOutletContext<{ onPostCreated: () => void }>();
  const { showToast } = useToast();
  const [form, setForm] = useState({
    title: "",
    body: "",
    category: "" as CategoryType,
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!form.title.trim() || !form.body.trim()) {
      setError("Title and body are required.");
      return;
    }

    setError("");
    setLoading(true);

    try {
      await api.post("/posts", form);
      onPostCreated();
      showToast("Post created successfully");
      navigate("/");
    } catch (err: any) {
      const msg = err.response?.data?.message || "Failed to create post";
      setError(msg);
      showToast(msg, "error");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="create-post-overlay create-post-page"
      onClick={() => navigate("/")}
    >
      <div
        className="create-post-breadcrumb"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          className="breadcrumb-link"
          onClick={() => navigate("/")}
          data-testid="breadcrumb-home"
        >
          <svg
            width="20"
            height="20"
            viewBox="0 0 20 20"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M3.33334 10L10 3.33334L16.6667 10M5 8.33334V15.8333C5 16.2754 5.17559 16.6993 5.48816 17.0118C5.80072 17.3244 6.22464 17.5 6.66667 17.5H13.3333C13.7754 17.5 14.1993 17.3244 14.5118 17.0118C14.8244 16.6993 15 16.2754 15 15.8333V8.33334"
              stroke="currentColor"
              strokeWidth="1.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
          Home
        </button>
        <svg
          className="breadcrumb-chevron"
          width="20"
          height="20"
          viewBox="0 0 20 20"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
        >
          <path
            d="M7.5 5L12.5 10L7.5 15"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
        <span className="breadcrumb-current">Create Post</span>
      </div>

      <div className="create-post-modal" onClick={(e) => e.stopPropagation()}>
        <div className="create-post-header">
          <button
            className="close-btn"
            onClick={() => navigate("/")}
            aria-label="Close"
            data-testid="close-create-post-btn"
          >
            <svg
              width="20"
              height="20"
              viewBox="0 0 20 20"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="M15 5L5 15M5 5L15 15"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </button>
        </div>

        <div className="create-post-body">
          <h2>Create New Post</h2>

          <form onSubmit={handleSubmit} className="create-post-form">
            {error && <div className="create-post-error">{error}</div>}

            <div className="form-group">
              <label>Post Title</label>
              <input
                type="text"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                placeholder="Enter a clear, descriptive title"
                maxLength={200}
                required
                data-testid="post-title-input"
              />
            </div>

            <div className="form-group">
              <label htmlFor="category-select">Category</label>
              <div className="select-wrapper">
                <select
                  id="category-select"
                  value={form.category}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      category: e.target.value as CategoryType,
                    })
                  }
                  required
                  data-testid="category-select"
                >
                  <option value="" disabled hidden>
                    Select
                  </option>
                  {CATEGORIES.map((c) => (
                    <option key={c} value={c}>
                      {CATEGORY_DISPLAY_NAMES[c] || c}
                    </option>
                  ))}
                </select>
                <div className="select-arrow">
                  <svg
                    width="12"
                    height="8"
                    viewBox="0 0 12 8"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                  >
                    <path
                      d="M1 1.5L6 6.5L11 1.5"
                      stroke="#395362"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                  </svg>
                </div>
              </div>
            </div>

            <div className="form-group textarea-group">
              <textarea
                value={form.body}
                onChange={(e) => setForm({ ...form, body: e.target.value })}
                placeholder="Share the details of your post..."
                required
                data-testid="post-body-textarea"
              />
            </div>

            <div className="create-post-actions">
              <button
                type="button"
                className="cancel-btn"
                onClick={() => navigate("/")}
                disabled={loading}
                data-testid="cancel-create-post-btn"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="submit-btn"
                disabled={loading}
                data-testid="submit-create-post-btn"
              >
                {loading ? "Publishing..." : "Create Post"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
