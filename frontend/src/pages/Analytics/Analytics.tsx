import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Navbar } from "../../components/layout/Navbar";
import api from "../../services/api";
import "./Analytics.css";
import "../PostDetails/PostDetails.css";

// --- API response shapes (matching what the backend ACTUALLY returns) ---
// Note: The backend entities use Lombok @Getter which serializes Java field names
// directly. These differ from Percy's api_contract.md in some cases.

interface SummaryResponse {
  totalPosts: number;
  totalComments: number;
}

interface CategoryResponse {
  categoryName: string;  // backend field: categoryName (contract says "category")
  postCount: number;     // backend field: postCount (contract says "count")
}

interface DayResponse {
  dayName: string;       // backend field: dayName (contract says "day")
  dayOrder: number;      // 0=Sun, 1=Mon, ..., 6=Sat
  postCount: number;     // backend field: postCount (contract says "count")
}

interface ContributorResponse {
  etlRank: number;           // unique row number (used internally)
  trueRank: number;          // dense rank — tied users share the same value
  userId: number;
  contributorName: string;   // backend field: contributorName (contract says "name")
  contributorEmail: string;  // backend field: contributorEmail (contract says "email")
  postCount: number;
}

// Display labels for the category bar chart
const CATEGORY_LABELS: Record<string, string> = {
  EVENT: "Events",
  NEWS: "News",
  DISCUSSION: "Discussion",
  ALERT: "Alerts",
};

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
    ...new Set([
      yMax,
      Math.round((yMax * 3) / 4),
      Math.round(yMax / 2),
      Math.round(yMax / 4),
      0,
    ]),
  ];
  const avg =
    data.length > 0
      ? data.reduce((sum, d) => sum + d.count, 0) / data.length
      : 0;
  const avgPosition = yMax > 0 ? (avg / yMax) * 100 : 0;

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
            <div
              className="analytics-chart-avg-line"
              style={{ bottom: `${avgPosition}%` }}
            />
            {data.map((d) => (
              <div key={d.label} className="analytics-bar-wrapper">
                <div
                  className="analytics-bar"
                  style={{
                    height: `${yMax > 0 ? (d.count / yMax) * 100 : 0}%`,
                  }}
                >
                  <span className="analytics-bar-tooltip">
                    Count: {d.count}
                  </span>
                </div>
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
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [summary, setSummary] = useState<SummaryResponse>({ totalPosts: 0, totalComments: 0 });
  const [categoryData, setCategoryData] = useState<{ label: string; count: number }[]>([]);
  const [dayData, setDayData] = useState<{ label: string; count: number }[]>([]);
  const [contributors, setContributors] = useState<ContributorResponse[]>([]);

  useEffect(() => {
    const fetchAnalytics = async () => {
      setLoading(true);
      try {
        // Fire all 4 requests simultaneously — no reason to wait for one before the next
        const [summaryRes, categoryRes, dayRes, contributorsRes] = await Promise.all([
          api.get<SummaryResponse>("/analytics/summary"),
          api.get<CategoryResponse[]>("/analytics/posts-by-category"),
          api.get<DayResponse[]>("/analytics/posts-by-day"),
          api.get<ContributorResponse[]>("/analytics/contributors/top"),
        ]);

        setSummary(summaryRes.data);

        // Map API categoryName keys to display labels for the chart
        setCategoryData(
          categoryRes.data.map((d) => ({
            label: CATEGORY_LABELS[d.categoryName] || d.categoryName,
            count: d.postCount,
          })),
        );

        // Sort by dayOrder (0=Sun…6=Sat) so the chart axis is always in week order
        setDayData(
          [...dayRes.data]
            .sort((a, b) => a.dayOrder - b.dayOrder)
            .map((d) => ({ label: d.dayName, count: d.postCount })),
        );

        setContributors(contributorsRes.data);
        setError("");
      } catch (err) {
        console.error("Failed to load analytics data", err);
        setError("Failed to load analytics data. Please try again.");
      } finally {
        setLoading(false);
      }
    };
    fetchAnalytics();
  }, []);

  return (
    <div className="analytics-page-container">
      <Navbar />

      <main className="analytics-main-content">
        {/* Breadcrumb */}
        <div className="breadcrumb-container">
          <img
            src="/assets/house.svg"
            alt="Home"
            className="breadcrumb-icon-img breadcrumb-clickable"
            onClick={() => navigate("/")}
          />
          <span
            className="breadcrumb-text breadcrumb-clickable"
            onClick={() => navigate("/")}
          >
            Home
          </span>
          <img
            src="/assets/chevron-right.svg"
            alt="Arrow"
            className="breadcrumb-icon-img"
          />
          <span className="breadcrumb-text breadcrumb-active">Analytics</span>
        </div>

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
                    <img
                      src="/assets/trending-up.svg"
                      alt="Trending up"
                      width="20"
                      height="20"
                    />
                  </div>
                </div>
                <p className="analytics-stat-value">{summary.totalPosts}</p>
              </div>
              <div className="analytics-stat-card">
                <div className="analytics-stat-header">
                  <h3 className="analytics-stat-title">Total Comments</h3>
                  <div className="analytics-stat-icon analytics-stat-icon-comment">
                    <img
                      src="/assets/total-comments.svg"
                      alt="Comments"
                      width="20"
                      height="20"
                    />
                  </div>
                </div>
                <p className="analytics-stat-value">
                  {summary.totalComments}
                </p>
              </div>
            </div>

            {/* Charts */}
            <div className="analytics-charts">
              <BarChart
                data={categoryData}
                title="Posts by Category"
              />
              <BarChart
                data={dayData}
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
                      <th className="analytics-th analytics-th-rank">Ranks</th>
                      <th className="analytics-th analytics-th-name">Name</th>
                      <th className="analytics-th analytics-th-posts">Posts</th>
                    </tr>
                  </thead>
                  <tbody>
                    {contributors.length === 0 ? (
                      <tr>
                        <td
                          colSpan={3}
                          className="analytics-td analytics-empty-row"
                        >
                          No contributors yet
                        </td>
                      </tr>
                    ) : (
                      contributors.map((contributor) => (
                        <tr key={`${contributor.etlRank}-${contributor.contributorName}`}>
                          <td className="analytics-td analytics-td-rank">
                            {contributor.trueRank}
                          </td>
                          <td className="analytics-td analytics-td-name">
                            {contributor.contributorName}
                          </td>
                          <td className="analytics-td analytics-td-posts">
                            {contributor.postCount}
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
