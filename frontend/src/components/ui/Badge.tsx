import React from "react";
import "./Badge.css";

export const CategoryType = {
  All: "All",
  NEWS: "NEWS",
  EVENT: "EVENT",
  DISCUSSION: "DISCUSSION",
  ALERT: "ALERT",
} as const;

// eslint-disable-next-line @typescript-eslint/no-redeclare
export type CategoryType = (typeof CategoryType)[keyof typeof CategoryType];

export const CATEGORY_DISPLAY_NAMES: Record<CategoryType, string> = {
  All: "All",
  NEWS: "News",
  EVENT: "Event",
  DISCUSSION: "Discussion",
  ALERT: "Alert",
};

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
  className = "",
}: BadgeProps) {
  const getBadgeClass = () => {
    if (isFilter) {
      if (isActive) {
        return "badge-filter-active";
      }
      return "badge-filter-inactive";
    }

    // Default colored badges for post cards and details
    switch (category) {
      case "EVENT":
        return "badge-event";
      case "NEWS":
        return "badge-news";
      case "DISCUSSION":
        return "badge-discussion";
      case "ALERT":
        return "badge-alert";
      default:
        return "badge-default";
    }
  };

  const displayCategory = CATEGORY_DISPLAY_NAMES[category] || category;

  return (
    <div
      className={`badge ${getBadgeClass()} ${isFilter ? "badge-clickable" : ""} ${className}`}
      onClick={isFilter ? onClick : undefined}
    >
      {displayCategory}
    </div>
  );
}
