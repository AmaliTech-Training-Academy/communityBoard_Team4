import React, { useState, useEffect, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { Navbar } from "../../components/layout/Navbar";
import api from "../../services/api";
import "./Analytics.css";

interface PostResponse {
  id: number;
  title: string;
  body: string;
  category: string;
  authorName: string;
  authorEmail: string;
  createdAt: string;
  updatedAt: string;
  commentCount: number;
}

interface AnalyticsData {
  totalPosts: number;
  totalComments: number;
  postsByCategory: { label: string; count: number }[];
  postsByDayOfWeek: { label: string; count: number }[];
  topContributors: { name: string; posts: number }[];
}

const DAYS_OF_WEEK = ["Mon", "Tues", "Wed", "Thurs", "Fri", "Sat", "Sun"];

const CATEGORY_LABELS: Record<string, string> = {
  EVENT: "Events",
  NEWS: "News",
  DISCUSSION: "Discussion",
  ALERT: "Alerts",
};

function computeAnalytics(posts: PostResponse[]): AnalyticsData {
  const totalPosts = posts.length;
  const totalComments = posts.reduce((sum, p) => sum + p.commentCount, 0);

  // Posts by category
  const categoryMap: Record<string, number> = {};
  posts.forEach((p) => {
    categoryMap[p.category] = (categoryMap[p.category] || 0) + 1;
  });
  const postsByCategory = Object.entries(categoryMap)
    .map(([cat, count]) => ({
      label: CATEGORY_LABELS[cat] || cat,
      count,
    }))
    .sort((a, b) => b.count - a.count);

  // Posts by day of week
  const dayMap: Record<number, number> = {};
  posts.forEach((p) => {
    const day = new Date(p.createdAt).getDay(); // 0=Sun, 1=Mon, ...
    const adjustedDay = day === 0 ? 6 : day - 1; // 0=Mon, 6=Sun
    dayMap[adjustedDay] = (dayMap[adjustedDay] || 0) + 1;
  });
  const postsByDayOfWeek = DAYS_OF_WEEK.map((label, i) => ({
    label,
    count: dayMap[i] || 0,
  }));

  // Top 10 contributors
  const authorMap: Record<string, number> = {};
  posts.forEach((p) => {
    authorMap[p.authorName] = (authorMap[p.authorName] || 0) + 1;
  });
  const topContributors = Object.entries(authorMap)
    .map(([name, postCount]) => ({ name, posts: postCount }))
    .sort((a, b) => b.posts - a.posts)
    .slice(0, 10);

  return {
    totalPosts,
    totalComments,
    postsByCategory,
    postsByDayOfWeek,
    topContributors,
  };
}

function BarChart({
  data,
  title,
}: {
  data: { label: string; count: number }[];
  title: string;
}) {
  const maxValue = Math.max(...data.map((d) => d.count), 1);
  // Round up to nearest nice number for y-axis
  const yMax = Math.ceil(maxValue / 10) * 10 || 10;
  const yTicks = [
    yMax,
    Math.round((yMax * 3) / 4),
    Math.round(yMax / 2),
    Math.round(yMax / 4),
    0,
  ];

  return (
    <div className="analytics-chart-card">
      <h3 className="analytics-chart-title">{title}</h3>
      <div className="analytics-chart-container">
        <div className="analytics-chart-y-axis">
          {yTicks.map((tick) => (
            <span key={tick} className="analytics-chart-y-label">
              {tick}
            </span>
          ))}
        </div>
        <div className="analytics-chart-body">
          <div className="analytics-chart-grid">
            {yTicks.map((tick) => (
              <div key={tick} className="analytics-chart-grid-line" />
            ))}
          </div>
          <div className="analytics-chart-bars">
            {data.map((d) => (
              <div key={d.label} className="analytics-bar-wrapper">
                <div
                  className="analytics-bar"
                  style={{
                    height: `${yMax > 0 ? (d.count / yMax) * 100 : 0}%`,
                  }}
                  title={`${d.label}: ${d.count}`}
                />
              </div>
            ))}
          </div>
          <div className="analytics-chart-x-axis">
            {data.map((d) => (
              <span key={d.label} className="analytics-chart-x-label">
                {d.label}
              </span>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

export function Analytics() {
  const navigate = useNavigate();
  const [posts, setPosts] = useState<PostResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchAllPosts = async () => {
      setLoading(true);
      try {
        const { data } = await api.get("/posts", {
          params: { page: 0, size: 1000 },
        });
        setPosts(data.content || []);
        setError("");
      } catch (err) {
        console.error("Failed to load analytics data", err);
        setError("Failed to load analytics data. Please try again.");
      } finally {
        setLoading(false);
      }
    };
    fetchAllPosts();
  }, []);

  const analytics = useMemo(() => computeAnalytics(posts), [posts]);

  return (
    <div className="analytics-page-container">
      <Navbar />

      <main className="analytics-main-content">
        {/* Breadcrumb */}
        <nav className="analytics-breadcrumb" aria-label="Breadcrumb">
          <span
            className="analytics-breadcrumb-link"
            onClick={() => navigate("/")}
          >
            Home
          </span>
          <span className="analytics-breadcrumb-chevron">&gt;</span>
          <span className="analytics-breadcrumb-current">Analytics</span>
        </nav>

        {loading && <p className="analytics-loading">Loading analytics...</p>}
        {error && <p className="analytics-error">{error}</p>}

        {!loading && !error && (
          <div className="analytics-content">
            {/* Stat Cards */}
            <div className="analytics-stat-cards">
              <div className="analytics-stat-card">
                <div className="analytics-stat-header">
                  <h3 className="analytics-stat-title">Total Posts</h3>
                  <div className="analytics-stat-icon analytics-stat-icon-chart">
                    <svg
                      width="20"
                      height="20"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="#395362"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <polyline points="22 7 13.5 15.5 8.5 10.5 2 17" />
                      <polyline points="16 7 22 7 22 13" />
                    </svg>
                  </div>
                </div>
                <p className="analytics-stat-value">{analytics.totalPosts}</p>
              </div>
              <div className="analytics-stat-card">
                <div className="analytics-stat-header">
                  <h3 className="analytics-stat-title">Total Comments</h3>
                  <div className="analytics-stat-icon analytics-stat-icon-comment">
                    <svg
                      width="20"
                      height="20"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="#395362"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z" />
                      <path d="M8 12h.01" />
                      <path d="M12 12h.01" />
                      <path d="M16 12h.01" />
                    </svg>
                  </div>
                </div>
                <p className="analytics-stat-value">
                  {analytics.totalComments}
                </p>
              </div>
            </div>

            {/* Charts */}
            <div className="analytics-charts">
              <BarChart
                data={analytics.postsByCategory}
                title="Posts by Category"
              />
              <BarChart
                data={analytics.postsByDayOfWeek}
                title="Posts Day of Week"
              />
            </div>

            {/* Top Contributors Table */}
            <div className="analytics-contributors">
              <h3 className="analytics-contributors-title">
                Top 10 Contributors
              </h3>
              <div className="analytics-table-wrapper">
                <table className="analytics-table">
                  <thead>
                    <tr>
                      <th className="analytics-th analytics-th-rank">Rank</th>
                      <th className="analytics-th analytics-th-name">Name</th>
                      <th className="analytics-th analytics-th-posts">Posts</th>
                    </tr>
                  </thead>
                  <tbody>
                    {analytics.topContributors.length === 0 ? (
                      <tr>
                        <td
                          colSpan={3}
                          className="analytics-td analytics-empty-row"
                        >
                          No contributors yet
                        </td>
                      </tr>
                    ) : (
                      analytics.topContributors.map((contributor, index) => (
                        <tr key={contributor.name}>
                          <td className="analytics-td analytics-td-rank">
                            {index + 1}
                          </td>
                          <td className="analytics-td analytics-td-name">
                            {contributor.name}
                          </td>
                          <td className="analytics-td analytics-td-posts">
                            {contributor.posts}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
