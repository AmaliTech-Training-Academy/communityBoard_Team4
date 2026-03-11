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
    <div className="create-post-overlay" onClick={() => navigate("/")}>
      <div className="create-post-modal" onClick={(e) => e.stopPropagation()}>
        <div className="create-post-header">
          <button
            className="close-btn"
            onClick={() => navigate("/")}
            aria-label="Close"
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
              />
            </div>

            <div className="create-post-actions">
              <button
                type="button"
                className="cancel-btn"
                onClick={() => navigate("/")}
                disabled={loading}
              >
                Cancel
              </button>
              <button type="submit" className="submit-btn" disabled={loading}>
                {loading ? "Publishing..." : "Create Post"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
