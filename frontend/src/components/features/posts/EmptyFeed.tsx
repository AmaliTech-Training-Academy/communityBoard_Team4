import React from 'react';

export function EmptyFeed() {
  return (
    <div className="empty-feed-container">
      <img src="/assets/Group.svg" alt="Empty States" className="empty-state-img" />
      <p className="empty-state-text">No posts have been made yet</p>
    </div>
  );
}
