import React from 'react';
import { Badge, CategoryType } from '../../ui/Badge';
import './PostCard.css';

export interface Post {
  id: string;
  title: string;
  content: string;
  category: CategoryType;
  author: {
    id: string;
    name: string;
  };
  createdAt: string; // ISO date string or formatted string
  commentCount: number;
}

interface PostCardProps {
  post: Post;
  onClick?: (postId: string) => void;
}

// Helper to format "time ago" relative date
const timeAgo = (dateString: string) => {
  const date = new Date(dateString);
  const now = new Date();
  if (isNaN(date.getTime())) return dateString; // fallback if already formatted

  const seconds = Math.floor((now.getTime() - date.getTime()) / 1000);
  
  let interval = seconds / 31536000;
  if (interval > 1) return Math.floor(interval) + ' years ago';
  interval = seconds / 2592000;
  if (interval > 1) return Math.floor(interval) + ' months ago';
  interval = seconds / 86400;
  if (interval >= 1) {
    const d = Math.floor(interval);
    return d === 1 ? '1 day ago' : d + ' days ago';
  }
  interval = seconds / 3600;
  if (interval >= 1) {
    const h = Math.floor(interval);
    return h === 1 ? 'about 1 hour ago' : 'about ' + h + ' hours ago';
  }
  interval = seconds / 60;
  if (interval >= 1) {
    const m = Math.floor(interval);
    return m === 1 ? '1 minute ago' : m + ' minutes ago';
  }
  return Math.floor(seconds) + ' seconds ago';
};

export function PostCard({ post, onClick }: PostCardProps) {
  return (
    <div className="post-card" onClick={() => onClick && onClick(post.id)}>
      <div className="post-card-header">
        <h3 className="post-card-title">{post.title}</h3>
        <Badge category={post.category} className="post-card-badge" />
      </div>
      
      <p className="post-card-content-snippet">
        {post.content}
      </p>
      
      <div className="post-card-footer">
        <div className="post-card-meta">
          <span className="post-card-author">{post.author.name}</span>
          <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            <img src="/assets/clock.svg" alt="Clock" className="clock-icon-img" />
            <span className="post-card-time">{timeAgo(post.createdAt)}</span>
          </div>
        </div>
        
        <div className="post-card-comments">
          <img src="/assets/message.svg" alt="Comments" className="comment-icon-img" />
          <span className="comment-count">{post.commentCount}</span>
        </div>
      </div>
    </div>
  );
}
