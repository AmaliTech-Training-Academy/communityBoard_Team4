import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Navbar } from "../../components/layout/Navbar";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { Badge, CategoryType } from "../../components/ui/Badge";
import { ConfirmDialog } from "../../components/ui/ConfirmDialog";
import api from "../../services/api";
import "./PostDetails.css";

interface Comment {
  id: string;
  content: string;
  authorName: string;
  authorEmail?: string;
  createdAt: string;
}

// Helper to format "time ago"
const timeAgo = (dateString: string) => {
  const date = new Date(dateString);
  const now = new Date();
  if (isNaN(date.getTime())) return dateString;

  const seconds = Math.floor((now.getTime() - date.getTime()) / 1000);

  let interval = seconds / 31536000;
  if (interval > 1) return Math.floor(interval) + " years ago";
  interval = seconds / 2592000;
  if (interval > 1) return Math.floor(interval) + " months ago";
  interval = seconds / 86400;
  if (interval >= 1) return Math.floor(interval) + " days ago";
  interval = seconds / 3600;
  if (interval >= 1) {
    const h = Math.floor(interval);
    return h === 1 ? "about 1 hour ago" : "About " + h + " hrs Ago";
  }
  interval = seconds / 60;
  if (interval >= 1) {
    const m = Math.floor(interval);
    return m === 1 ? "1 min Ago" : m + " mins Ago";
  }
  return "Just now";
};

export function PostDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { showToast } = useToast();

  const [post, setPost] = useState<any>(null);
  const [loadingPost, setLoadingPost] = useState(true);

  const [commentText, setCommentText] = useState("");
  const [comments, setComments] = useState<Comment[]>([]);
  const [commentPage, setCommentPage] = useState(0);
  const [totalCommentPages, setTotalCommentPages] = useState(0);
  const [loadingComments, setLoadingComments] = useState(false);

  const [confirmAction, setConfirmAction] = useState<{
    message: string;
    onConfirm: () => void;
  } | null>(null);

  const [editingCommentId, setEditingCommentId] = useState<string | null>(null);
  const [editingCommentContent, setEditingCommentContent] = useState("");

  useEffect(() => {
    const fetchPost = async () => {
      try {
        const { data } = await api.get(`/posts/${id}`);
        setPost(data);
      } catch (err) {
        console.error("Failed to load post", err);
      } finally {
        setLoadingPost(false);
      }
    };
    if (id) fetchPost();
  }, [id]);

  const fetchComments = async (pageToLoad: number) => {
    setLoadingComments(true);
    try {
      const { data } = await api.get(
        `/posts/${id}/comments?page=${pageToLoad}&size=10`,
      );
      setComments(data.content || []);
      setTotalCommentPages(data.totalPages || 0);
      setCommentPage(pageToLoad);
    } catch (err) {
      console.error("Failed to load comments", err);
    } finally {
      setLoadingComments(false);
    }
  };

  useEffect(() => {
    if (id) fetchComments(0);
  }, [id]);

  const handleAddComment = async () => {
    if (!commentText.trim() || !user) return;
    try {
      await api.post(`/posts/${id}/comments`, { content: commentText });
      setCommentText("");
      fetchComments(0);
      setPost((prev: any) => ({
        ...prev,
        commentCount: (prev.commentCount || 0) + 1,
      }));
      showToast("Comment added successfully");
    } catch (err) {
      console.error("Failed to add comment", err);
      showToast("Failed to add comment", "error");
    }
  };

  const handleDeleteComment = async (commentId: string) => {
    setConfirmAction({
      message: "Are you sure you want to delete this comment?",
      onConfirm: async () => {
        setConfirmAction(null);
        try {
          await api.delete(`/comments/${commentId}`);
          setComments(comments.filter((c) => c.id !== commentId));
          setPost((prev: any) => ({
            ...prev,
            commentCount: Math.max(0, (prev.commentCount || 1) - 1),
          }));
          showToast("Comment deleted successfully");
        } catch (err) {
          console.error("Failed to delete comment", err);
          showToast("Failed to delete comment", "error");
        }
      },
    });
  };

  const handleEditComment = (comment: Comment) => {
    setEditingCommentId(comment.id);
    setEditingCommentContent(comment.content);
  };

  const handleCancelEdit = () => {
    setEditingCommentId(null);
    setEditingCommentContent("");
  };

  const handleSaveEditComment = async () => {
    if (!editingCommentId || !editingCommentContent.trim()) return;
    try {
      const { data } = await api.put(`/comments/${editingCommentId}`, {
        content: editingCommentContent,
      });
      setComments(
        comments.map((c) =>
          c.id === editingCommentId ? { ...c, content: data.content } : c,
        ),
      );
      setEditingCommentId(null);
      setEditingCommentContent("");
      showToast("Comment updated successfully");
    } catch (err) {
      console.error("Failed to update comment", err);
      showToast("Failed to update comment", "error");
    }
  };

  const handleDeletePost = async () => {
    setConfirmAction({
      message: "Are you sure you want to delete this post?",
      onConfirm: async () => {
        setConfirmAction(null);
        try {
          await api.delete(`/posts/${id}`);
          showToast("Post deleted successfully");
          navigate("/");
        } catch (err) {
          console.error("Failed to delete post", err);
          showToast("Failed to delete post", "error");
        }
      },
    });
  };

  const canEditPost =
    user && post && (user.email === post.authorEmail || user.role === "ADMIN");

  return (
    <div className="details-page-container">
      <Navbar />

      <main className="details-main-content">
        {/* Breadcrumb */}
        <div className="breadcrumb-container">
          <img
            src="/assets/house.svg"
            alt="Home"
            className="breadcrumb-icon-img"
            onClick={() => navigate("/")}
            style={{ cursor: "pointer" }}
          />
          <span
            className="breadcrumb-text"
            onClick={() => navigate("/")}
            style={{ cursor: "pointer" }}
          >
            Home
          </span>
          <img
            src="/assets/chevron-right.svg"
            alt="Arrow"
            className="breadcrumb-icon-img"
          />
          <span className="breadcrumb-text breadcrumb-active">
            Post Details
          </span>
        </div>

        <div className="post-center-column">
          {/* Post Content Box */}
          {loadingPost ? (
            <p>Loading post...</p>
          ) : !post ? (
            <p>Post not found.</p>
          ) : (
            <section className="post-details-card">
              <div
                className="post-header-row"
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "flex-start",
                }}
              >
                <div style={{ flex: 1 }}>
                  <h1 className="post-title">{post.title}</h1>
                  <Badge
                    category={post.category as CategoryType}
                    className="post-badge-large"
                  />
                </div>
                {canEditPost && (
                  <div style={{ display: "flex", gap: "8px" }}>
                    <button
                      className="action-icon-btn"
                      onClick={() => alert("Edit post coming soon!")}
                      title="Edit Post"
                    >
                      <img
                        src="/assets/pen.svg"
                        alt="Edit"
                        className="action-icon-img"
                      />
                    </button>
                    <button
                      className="action-icon-btn"
                      onClick={handleDeletePost}
                      title="Delete Post"
                    >
                      <img
                        src="/assets/trash-2.svg"
                        alt="Delete"
                        className="action-icon-img"
                      />
                    </button>
                  </div>
                )}
              </div>

              <p className="post-body-text">{post.body}</p>

              <div className="post-meta-row">
                <span className="post-author-name">{post.authorName}</span>
                <div className="post-time-container">
                  <img
                    src="/assets/clock.svg"
                    alt="Clock"
                    className="clock-icon-img"
                  />
                  <span className="post-time-text">
                    {timeAgo(post.createdAt)}
                  </span>
                </div>
              </div>

              <hr className="details-divider" />
            </section>
          )}

          {/* Comments Section */}
          <section className="comments-section">
            {/* Add Comment Input */}
            <div className="add-comment-container">
              <textarea
                className="comment-textarea"
                placeholder="Share your thoughts..."
                value={commentText}
                onChange={(e) => setCommentText(e.target.value)}
                data-testid="add-comment-textarea"
              />
              <button
                className="add-comment-btn"
                onClick={handleAddComment}
                data-testid="submit-comment-button"
              >
                Add comment
              </button>
            </div>

            <div className="comments-header">
              <h2>
                Comments <span>({post?.commentCount || comments.length})</span>
              </h2>
            </div>

            {loadingComments ? (
              <p>Loading comments...</p>
            ) : comments.length > 0 ? (
              <div className="comments-list">
                {comments.map((comment, index) => {
                  // Admin or matching author name/email can delete comments
                  const canDeleteComment =
                    user &&
                    (user.role === "ADMIN" ||
                      user.name === comment.authorName ||
                      user.email === comment.authorEmail);
                  const initials = comment.authorName
                    ? comment.authorName.slice(0, 2).toUpperCase()
                    : "U";

                  return (
                    <React.Fragment key={comment.id}>
                      <div className="comment-item">
                        <div className="comment-header-row">
                          <div className="comment-meta-row">
                            <div className="comment-avatar">{initials}</div>
                            <div className="comment-author-info">
                              <span className="comment-author-name">
                                {comment.authorName}
                              </span>
                              <span className="comment-time">
                                {timeAgo(comment.createdAt)}
                              </span>
                            </div>
                          </div>

                          {canDeleteComment && (
                            <div className="comment-actions">
                              {editingCommentId === comment.id ? (
                                <>
                                  <button
                                    className="comment-edit-save-btn"
                                    onClick={handleSaveEditComment}
                                  >
                                    Save
                                  </button>
                                  <button
                                    className="comment-edit-cancel-btn"
                                    onClick={handleCancelEdit}
                                  >
                                    Cancel
                                  </button>
                                </>
                              ) : (
                                <>
                                  <button
                                    className="action-icon-btn"
                                    onClick={() => handleEditComment(comment)}
                                    data-testid={`edit-comment-${comment.id}`}
                                  >
                                    <img
                                      src="/assets/pen.svg"
                                      alt="Edit"
                                      className="action-icon-img"
                                    />
                                  </button>
                                  <button
                                    className="action-icon-btn"
                                    onClick={() =>
                                      handleDeleteComment(comment.id)
                                    }
                                    data-testid={`delete-comment-${comment.id}`}
                                  >
                                    <img
                                      src="/assets/trash-2.svg"
                                      alt="Delete"
                                      className="action-icon-img"
                                    />
                                  </button>
                                </>
                              )}
                            </div>
                          )}
                        </div>

                        {editingCommentId === comment.id ? (
                          <textarea
                            className="comment-edit-textarea"
                            value={editingCommentContent}
                            onChange={(e) =>
                              setEditingCommentContent(e.target.value)
                            }
                            autoFocus
                          />
                        ) : (
                          <p
                            className="comment-content"
                            data-testid={`comment-content-${comment.id}`}
                          >
                            {comment.content}
                          </p>
                        )}
                      </div>
                      {index < comments.length - 1 && (
                        <hr className="details-divider" />
                      )}
                    </React.Fragment>
                  );
                })}

                {/* Comment Pagination */}
                {totalCommentPages > 1 && (
                  <div
                    className="pagination-container"
                    style={{ marginTop: "20px" }}
                  >
                    <button
                      className="pagination-btn"
                      disabled={commentPage === 0}
                      onClick={() =>
                        fetchComments(Math.max(0, commentPage - 1))
                      }
                    >
                      Previous
                    </button>
                    {Array.from({ length: totalCommentPages }, (_, i) => (
                      <button
                        key={i}
                        className={`pagination-btn ${commentPage === i ? "pagination-active" : ""}`}
                        onClick={() => fetchComments(i)}
                      >
                        {i + 1}
                      </button>
                    ))}
                    <button
                      className="pagination-btn"
                      disabled={commentPage >= totalCommentPages - 1}
                      onClick={() =>
                        fetchComments(
                          Math.min(totalCommentPages - 1, commentPage + 1),
                        )
                      }
                    >
                      Next
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <div className="empty-comments-state">
                <img
                  src="/assets/Group.svg"
                  alt="No Comments"
                  className="empty-comments-img"
                />
                <span className="empty-comments-text">No Comments yet</span>
              </div>
            )}
          </section>
        </div>
      </main>

      {confirmAction && (
        <ConfirmDialog
          message={confirmAction.message}
          onConfirm={confirmAction.onConfirm}
          onCancel={() => setConfirmAction(null)}
        />
      )}
    </div>
  );
}
