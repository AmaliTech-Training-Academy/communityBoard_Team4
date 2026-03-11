
import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Navbar } from '../../components/layout/Navbar';
import { useAuth } from '../../context/AuthContext';
import { Badge, CategoryType } from '../../components/ui/Badge';
import './PostDetails.css';

// Dummy Data
interface Author {
  id: string;
  name: string;
  initials: string;
}

interface Comment {
  id: string;
  author: Author;
  content: string;
  createdAt: string;
}

const POST = {
  id: '1',
  title: 'Community Garden Workday This Saturday',
  category: 'Events' as CategoryType,
  content: "Join us this Saturday at 9 AM for our monthly community garden workday! We'll be planting spring vegetables and need volunteers. Bring gloves and water. Coffee and donuts provided!",
  author: { id: 'u1', name: 'Sarah Johnson' },
  createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(), // 2 hours ago
};

const INITIAL_COMMENTS = [
  {
    id: 'c1',
    author: { id: '1', name: 'John Doe', initials: 'JD' }, // matches mock admin user '1'
    content: "Looking forward to it! Should I bring anything else?",
    createdAt: new Date(Date.now() - 30 * 60 * 1000).toISOString(), // 30 mins ago
  },
  {
    id: 'c2',
    author: { id: 'u3', name: 'Esther Smith', initials: 'ES' },
    content: "Great initiative! Will be there with my family.",
    createdAt: new Date(Date.now() - 60 * 60 * 1000).toISOString(), // 1 hr ago
  },
  {
    id: 'c3',
    author: { id: 'u4', name: 'John Smith', initials: 'JS' },
    content: "Count me in! I'll bring some extra tools.",
    createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(), // 2 hrs ago
  }
];

// Helper to format "time ago"
const timeAgo = (dateString: string) => {
  const date = new Date(dateString);
  const now = new Date();
  if (isNaN(date.getTime())) return dateString;

  const seconds = Math.floor((now.getTime() - date.getTime()) / 1000);

  let interval = seconds / 31536000;
  if (interval > 1) return Math.floor(interval) + ' years ago';
  interval = seconds / 2592000;
  if (interval > 1) return Math.floor(interval) + ' months ago';
  interval = seconds / 86400;
  if (interval >= 1) return Math.floor(interval) + ' days ago';
  interval = seconds / 3600;
  if (interval >= 1) {
    const h = Math.floor(interval);
    return h === 1 ? 'about 1 hour ago' : 'About ' + h + ' hrs Ago';
  }
  interval = seconds / 60;
  if (interval >= 1) {
    const m = Math.floor(interval);
    return m === 1 ? '1 min Ago' : m + ' mins Ago';
  }
  return 'Just now';
};


export function PostDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [commentText, setCommentText] = useState('');
  const [comments, setComments] = useState(INITIAL_COMMENTS);
  const [editingCommentId, setEditingCommentId] = useState<string | null>(null);
  const [editCommentText, setEditCommentText] = useState('');

  const handleAddComment = () => {
    if (!commentText.trim() || !user) return;

    const newComment: Comment = {
      id: Date.now().toString(),
      content: commentText,
      author: {
        id: user.id || 'u2',
        name: user.name || 'Admin User',
        initials: user.name ? user.name.slice(0, 2).toUpperCase() : 'AU'
      },
      createdAt: new Date().toISOString()
    };

    setComments([newComment, ...comments]);
    setCommentText('');
  };

  const handleEditStart = (comment: Comment) => {
    setEditingCommentId(comment.id);
    setEditCommentText(comment.content);
  };

  const handleEditSave = () => {
    if (!editCommentText.trim()) return;
    setComments(comments.map(c => c.id === editingCommentId ? { ...c, content: editCommentText } : c));
    setEditingCommentId(null);
    setEditCommentText('');
  };

  const handleEditCancel = () => {
    setEditingCommentId(null);
    setEditCommentText('');
  };

  const handleDeleteComment = (commentId: string) => {
    setComments(comments.filter(c => c.id !== commentId));
  };

  return (
    <div className="details-page-container">
      <Navbar />

      <main className="details-main-content">

        {/* Breadcrumb */}
        <div className="breadcrumb-container">
          <img src="/assets/house.svg" alt="Home" className="breadcrumb-icon-img" onClick={() => navigate('/')} style={{ cursor: 'pointer' }} />
          <span className="breadcrumb-text" onClick={() => navigate('/')} style={{ cursor: 'pointer' }}>Home</span>
          <img src="/assets/chevron-right.svg" alt="Arrow" className="breadcrumb-icon-img" />
          <span className="breadcrumb-text breadcrumb-active">Post Details</span>
        </div>

        <div className="post-center-column">
          {/* Post Content Box */}
          <section className="post-details-card">

          <div className="post-header-row">
            <h1 className="post-title">{POST.title}</h1>
            <Badge category={POST.category} className="post-badge-large" />
          </div>

          <p className="post-body-text">{POST.content}</p>

          <div className="post-meta-row">
            <span className="post-author-name">{POST.author.name}</span>
            <div className="post-time-container">
              <img src="/assets/clock.svg" alt="Clock" className="clock-icon-img" />
              <span className="post-time-text">about 2 hours ago</span>
            </div>
          </div>

          <hr className="details-divider" />
        </section>

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
            <button className="add-comment-btn" onClick={handleAddComment} data-testid="submit-comment-button">
              Add comment
            </button>
          </div>

          <div className="comments-header">
            <h2>Comments <span>({comments.length})</span></h2>
          </div>

          {comments.length > 0 ? (
            <div className="comments-list">
              {comments.map((comment, index) => (
                <React.Fragment key={comment.id}>
                  <div className="comment-item">
                    <div className="comment-header-row">
                      <div className="comment-meta-row">
                        <div className="comment-avatar">{comment.author.initials}</div>
                        <div className="comment-author-info">
                          <span className="comment-author-name">{comment.author.name}</span>
                          <span className="comment-time">{timeAgo(comment.createdAt)}</span>
                        </div>
                      </div>

                        <div className="comment-actions">
                          <button
                            className="action-icon-btn"
                            onClick={() => handleEditStart(comment)}
                            data-testid={`edit-comment-${comment.id}`}
                          >
                            <img src="/assets/pen.svg" alt="Edit" className="action-icon-img" />
                          </button>
                          <button
                            className="action-icon-btn"
                            onClick={() => handleDeleteComment(comment.id)}
                            data-testid={`delete-comment-${comment.id}`}
                          >
                            <img src="/assets/trash-2.svg" alt="Delete" className="action-icon-img" />
                          </button>
                        </div>
                    </div>

                    {editingCommentId === comment.id ? (
                      <div className="edit-comment-container">
                        <textarea
                          className="comment-textarea edit-textarea"
                          value={editCommentText}
                          onChange={(e) => setEditCommentText(e.target.value)}
                          data-testid={`edit-textarea-${comment.id}`}
                        />
                        <button className="add-comment-btn save-changes-btn" onClick={handleEditSave} data-testid={`save-edit-${comment.id}`}>
                          Save Changes
                        </button>
                      </div>
                    ) : (
                      <p className="comment-content" data-testid={`comment-content-${comment.id}`}>{comment.content}</p>
                    )}
                  </div>
                  {index < comments.length - 1 && <hr className="details-divider" />}
                </React.Fragment>
              ))}
            </div>
          ) : (
            <div className="empty-comments-state">
              <img src="/assets/Group.svg" alt="No Comments" className="empty-comments-img" />
              <span className="empty-comments-text">No Comments yet</span>
            </div>
          )}

        </section>
        </div>
      </main>
    </div>
  );
}
