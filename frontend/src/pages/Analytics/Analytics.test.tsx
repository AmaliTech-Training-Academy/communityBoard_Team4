import { render, screen, waitFor } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import { AuthProvider } from "../../context/AuthContext";
import { ToastProvider } from "../../context/ToastContext";
import { Analytics } from "./Analytics";
import { describe, it, expect, vi, beforeEach } from "vitest";

// Mock useAuth
vi.mock("../../context/AuthContext", async () => {
  const actual = await vi.importActual("../../context/AuthContext");
  return {
    ...(actual as any),
    useAuth: () => ({
      user: { name: "Test User", email: "test@example.com" },
      logout: vi.fn(),
    }),
  };
});

// Mock api — route responses based on the URL argument
const mockGet = vi.fn();
vi.mock("../../services/api", () => ({
  default: {
    get: (...args: any[]) => mockGet(...args),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  },
}));

// --- Mock data matching backend field names ---

const mockSummary = { totalPosts: 5, totalComments: 27 };

const mockCategories = [
  { categoryName: "EVENT", postCount: 2 },
  { categoryName: "NEWS", postCount: 1 },
  { categoryName: "DISCUSSION", postCount: 1 },
  { categoryName: "ALERT", postCount: 1 },
];

const mockDays = [
  { dayName: "Sun", dayOrder: 0, postCount: 0 },
  { dayName: "Mon", dayOrder: 1, postCount: 2 },
  { dayName: "Tue", dayOrder: 2, postCount: 1 },
  { dayName: "Wed", dayOrder: 3, postCount: 1 },
  { dayName: "Thu", dayOrder: 4, postCount: 0 },
  { dayName: "Fri", dayOrder: 5, postCount: 1 },
  { dayName: "Sat", dayOrder: 6, postCount: 0 },
];

const mockContributors = [
  {
    etlRank: 1,
    trueRank: 1,
    userId: 1,
    contributorName: "Alice",
    contributorEmail: "alice@example.com",
    postCount: 3,
  },
  {
    etlRank: 2,
    trueRank: 2,
    userId: 2,
    contributorName: "Bob",
    contributorEmail: "bob@example.com",
    postCount: 1,
  },
  {
    etlRank: 3,
    trueRank: 2,
    userId: 3,
    contributorName: "Charlie",
    contributorEmail: "charlie@example.com",
    postCount: 1,
  },
];

/** Route mockGet responses by URL path */
function setupMockEndpoints(overrides?: Partial<Record<string, any>>) {
  mockGet.mockImplementation((url: string) => {
    const responses: Record<string, any> = {
      "/analytics/summary": { data: mockSummary },
      "/analytics/posts-by-category": { data: mockCategories },
      "/analytics/posts-by-day": { data: mockDays },
      "/analytics/contributors/top": { data: mockContributors },
      ...overrides,
    };
    const match = responses[url];
    if (match instanceof Error) return Promise.reject(match);
    return Promise.resolve(match ?? { data: [] });
  });
}

describe("Analytics Component", () => {
  const renderAnalytics = () => {
    return render(
      <BrowserRouter>
        <ToastProvider>
          <AuthProvider>
            <Analytics />
          </AuthProvider>
        </ToastProvider>
      </BrowserRouter>,
    );
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders loading state initially", () => {
    mockGet.mockReturnValue(new Promise(() => {})); // never resolves
    renderAnalytics();
    expect(screen.getByText("Loading analytics...")).toBeInTheDocument();
  });

  it("renders breadcrumb navigation", async () => {
    setupMockEndpoints();
    renderAnalytics();

    await waitFor(() => {
      expect(screen.getByText("Home")).toBeInTheDocument();
      expect(
        screen.getByText("Analytics", {
          selector: ".breadcrumb-active",
        }),
      ).toBeInTheDocument();
    });
  });

  it("displays total posts count", async () => {
    setupMockEndpoints();
    renderAnalytics();

    await waitFor(() => {
      expect(screen.getByText("Total Posts")).toBeInTheDocument();
      const statValues = screen.getAllByText("5");
      expect(statValues.length).toBeGreaterThanOrEqual(1);
    });
  });

  it("displays total comments count", async () => {
    setupMockEndpoints();
    renderAnalytics();

    await waitFor(() => {
      expect(screen.getByText("Total Comments")).toBeInTheDocument();
      expect(screen.getByText("27")).toBeInTheDocument();
    });
  });

  it("renders Posts by Category chart", async () => {
    setupMockEndpoints();
    renderAnalytics();

    await waitFor(() => {
      expect(screen.getByText("Posts by Category")).toBeInTheDocument();
      expect(screen.getByText("Events")).toBeInTheDocument();
      expect(screen.getByText("News")).toBeInTheDocument();
      expect(screen.getByText("Discussion")).toBeInTheDocument();
      expect(screen.getByText("Alerts")).toBeInTheDocument();
    });
  });

  it("renders Posts Day of Week chart", async () => {
    setupMockEndpoints();
    renderAnalytics();

    await waitFor(() => {
      expect(screen.getByText("Posts Day of Week")).toBeInTheDocument();
      expect(screen.getByText("Mon")).toBeInTheDocument();
      expect(screen.getByText("Fri")).toBeInTheDocument();
      expect(screen.getByText("Sun")).toBeInTheDocument();
    });
  });

  it("renders Top 10 Contributors table", async () => {
    setupMockEndpoints();
    renderAnalytics();

    await waitFor(() => {
      expect(screen.getByText("Top 10 Contributors")).toBeInTheDocument();
      expect(screen.getByText("Alice")).toBeInTheDocument();
      expect(screen.getByText("Bob")).toBeInTheDocument();
      expect(screen.getByText("Charlie")).toBeInTheDocument();
    });
  });

  it("shows correct contributor rankings sorted by post count", async () => {
    setupMockEndpoints();
    renderAnalytics();

    await waitFor(() => {
      const rows = screen.getAllByRole("row");
      // Header row + 3 data rows
      expect(rows).toHaveLength(4);

      const cells = screen.getAllByRole("cell");
      // Row 1: trueRank 1, Alice, 3
      expect(cells[0]).toHaveTextContent("1");
      expect(cells[1]).toHaveTextContent("Alice");
      expect(cells[2]).toHaveTextContent("3");
    });
  });

  it("displays error message when API call fails", async () => {
    mockGet.mockRejectedValue(new Error("Network error"));
    renderAnalytics();

    await waitFor(() => {
      expect(
        screen.getByText("Failed to load analytics data. Please try again."),
      ).toBeInTheDocument();
    });
  });

  it("shows empty state for contributors when no posts", async () => {
    setupMockEndpoints({
      "/analytics/summary": { data: { totalPosts: 0, totalComments: 0 } },
      "/analytics/posts-by-category": { data: [] },
      "/analytics/posts-by-day": { data: [] },
      "/analytics/contributors/top": { data: [] },
    });
    renderAnalytics();

    await waitFor(() => {
      expect(screen.getByText("No contributors yet")).toBeInTheDocument();
    });
  });

  it("fetches all 4 analytics endpoints", async () => {
    setupMockEndpoints();
    renderAnalytics();

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledWith("/analytics/summary");
      expect(mockGet).toHaveBeenCalledWith("/analytics/posts-by-category");
      expect(mockGet).toHaveBeenCalledWith("/analytics/posts-by-day");
      expect(mockGet).toHaveBeenCalledWith("/analytics/contributors/top");
    });
  });
});
