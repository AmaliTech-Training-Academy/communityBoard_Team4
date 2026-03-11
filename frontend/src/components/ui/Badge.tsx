import React from 'react';
import './Badge.css';

export type CategoryType =
  | 'All'
  | 'Events'
  | 'Lost & Found'
  | 'Recommendations'
  | 'Help Requests';

interface BadgeProps {
  category: CategoryType;
  /** If true, the badge acts as a filter tab. It will be styled differently if active or inactive */
  isFilter?: boolean;
  /** Used when isFilter is true to determine if this category is currently selected */
  isActive?: boolean;
  onClick?: () => void;
  className?: string;
}

export function Badge({
  category,
  isFilter = false,
  isActive = false,
  onClick,
  className = ''
}: BadgeProps) {

  const getBadgeClass = () => {
    if (isFilter) {
      if (isActive) {
        return 'badge-filter-active';
      }
      return 'badge-filter-inactive';
    }

    // Default colored badges for post cards and details
    switch (category) {
      case 'Events':
        return 'badge-events';
      case 'Lost & Found':
        return 'badge-lost-found';
      case 'Recommendations':
        return 'badge-recommendations';
      case 'Help Requests':
        return 'badge-help-requests';
      default:
        return 'badge-default';
    }
  };

  return (
    <div
      className={`badge ${getBadgeClass()} ${isFilter ? 'badge-clickable' : ''} ${className}`}
      onClick={isFilter ? onClick : undefined}
    >
      {category}
    </div>
  );
}
