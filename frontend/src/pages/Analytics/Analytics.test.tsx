import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
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

// Mock api
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

const mockPosts = [
  {
    id: 1,
    title: "Event Post",
    body: "Body 1",
    category: "EVENT",
    authorName: "Alice",
    authorEmail: "alice@example.com",
    createdAt: "2025-01-06T10:00:00", // Monday
    updatedAt: "2025-01-06T10:00:00",
    commentCount: 5,
  },
  {
    id: 2,
    title: "News Post",
    body: "Body 2",
    category: "NEWS",
    authorName: "Alice",
    authorEmail: "alice@example.com",
    createdAt: "2025-01-07T10:00:00", // Tuesday
    updatedAt: "2025-01-07T10:00:00",
    commentCount: 3,
  },
  {
    id: 3,
    title: "Discussion Post",
    body: "Body 3",
    category: "DISCUSSION",
    authorName: "Bob",
    authorEmail: "bob@example.com",
    createdAt: "2025-01-08T10:00:00", // Wednesday
    updatedAt: "2025-01-08T10:00:00",
    commentCount: 10,
  },
  {
    id: 4,
    title: "Alert Post",
    body: "Body 4",
    category: "ALERT",
    authorName: "Charlie",
    authorEmail: "charlie@example.com",
    createdAt: "2025-01-06T15:00:00", // Monday
    updatedAt: "2025-01-06T15:00:00",
    commentCount: 2,
  },
  {
    id: 5,
    title: "Another Event",
    body: "Body 5",
    category: "EVENT",
    authorName: "Alice",
    authorEmail: "alice@example.com",
    createdAt: "2025-01-10T10:00:00", // Friday
    updatedAt: "2025-01-10T10:00:00",
    commentCount: 7,
  },
];

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
    mockGet.mockResolvedValue({ data: { content: mockPosts } });
    renderAnalytics();

    await waitFor(() => {
      expect(screen.getByText("Home")).toBeInTheDocument();
      expect(
        screen.getByText("Analytics", {
          selector: ".analytics-breadcrumb-current",
        }),
      ).toBeInTheDocument();
    });
  });

  it("displays total posts count", async () => {
    mockGet.mockResolvedValue({ data: { content: mockPosts } });
    renderAnalytics();

    await waitFor(() => {
      expect(screen.getByText("Total Posts")).toBeInTheDocument();
      const statValues = screen.getAllByText("5");
      expect(statValues.length).toBeGreaterThanOrEqual(1);
    });
  });

  it("displays total comments count", async () => {
    mockGet.mockResolvedValue({ data: { content: mockPosts } });
    renderAnalytics();

    await waitFor(() => {
      expect(screen.getByText("Total Comments")).toBeInTheDocument();
      expect(screen.getByText("27")).toBeInTheDocument(); // 5+3+10+2+7
    });
  });

  it("renders Posts by Category chart", async () => {
    mockGet.mockResolvedValue({ data: { content: mockPosts } });
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
    mockGet.mockResolvedValue({ data: { content: mockPosts } });
    renderAnalytics();

    await waitFor(() => {
      expect(screen.getByText("Posts Day of Week")).toBeInTheDocument();
      expect(screen.getByText("Mon")).toBeInTheDocument();
      expect(screen.getByText("Fri")).toBeInTheDocument();
      expect(screen.getByText("Sun")).toBeInTheDocument();
    });
  });

  it("renders Top 10 Contributors table", async () => {
    mockGet.mockResolvedValue({ data: { content: mockPosts } });
    renderAnalytics();

    await waitFor(() => {
      expect(screen.getByText("Top 10 Contributors")).toBeInTheDocument();
      expect(screen.getByText("Alice")).toBeInTheDocument();
      expect(screen.getByText("Bob")).toBeInTheDocument();
      expect(screen.getByText("Charlie")).toBeInTheDocument();
    });
  });

  it("shows correct contributor rankings sorted by post count", async () => {
    mockGet.mockResolvedValue({ data: { content: mockPosts } });
    renderAnalytics();

    await waitFor(() => {
      const rows = screen.getAllByRole("row");
      // Header row + 3 data rows
      expect(rows).toHaveLength(4);

      // Alice has 3 posts (rank 1), Bob has 1 (rank 2), Charlie has 1 (rank 3)
      const cells = screen.getAllByRole("cell");
      // Row 1: rank 1, Alice, 3
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
    mockGet.mockResolvedValue({ data: { content: [] } });
    renderAnalytics();

    await waitFor(() => {
      expect(screen.getByText("No contributors yet")).toBeInTheDocument();
    });
  });

  it("fetches posts with correct API params", async () => {
    mockGet.mockResolvedValue({ data: { content: [] } });
    renderAnalytics();

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledWith("/posts", {
        params: { page: 0, size: 1000 },
      });
    });
  });
});
