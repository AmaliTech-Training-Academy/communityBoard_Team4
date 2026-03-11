import { render, screen, act } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { AuthProvider, useAuth } from "./AuthContext";

// Mock api module
vi.mock("../services/api", () => ({
  default: {
    post: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  },
}));

import api from "../services/api";
const mockPost = vi.mocked(api.post);

// Test component that exposes auth context
function TestConsumer() {
  const { user, token, login, register, logout } = useAuth();
  return (
    <div>
      <span data-testid="user">{user ? user.name : "null"}</span>
      <span data-testid="token">{token || "null"}</span>
      <span data-testid="role">{user?.role || "null"}</span>
      <button
        data-testid="login-btn"
        onClick={() => login("test@test.com", "pass123")}
      >
        Login
      </button>
      <button
        data-testid="register-btn"
        onClick={() => register("Test User", "test@test.com", "pass123")}
      >
        Register
      </button>
      <button data-testid="logout-btn" onClick={() => logout()}>
        Logout
      </button>
    </div>
  );
}

describe("AuthContext", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it("provides null user when no token in localStorage", () => {
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>,
    );
    expect(screen.getByTestId("user").textContent).toBe("null");
    expect(screen.getByTestId("token").textContent).toBe("null");
  });

  it("restores user from localStorage", () => {
    localStorage.setItem("token", "stored-token");
    localStorage.setItem("role", "USER");
    localStorage.setItem("name", "Stored User");
    localStorage.setItem("email", "stored@test.com");

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>,
    );
    expect(screen.getByTestId("user").textContent).toBe("Stored User");
    expect(screen.getByTestId("token").textContent).toBe("stored-token");
    expect(screen.getByTestId("role").textContent).toBe("USER");
  });

  it("login sets user and stores in localStorage", async () => {
    mockPost.mockResolvedValueOnce({
      data: {
        token: "jwt-token-123",
        role: "USER",
        name: "John Doe",
        email: "john@test.com",
      },
    } as any);

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>,
    );

    await act(async () => {
      await userEvent.click(screen.getByTestId("login-btn"));
    });

    expect(mockPost).toHaveBeenCalledWith("/auth/login", {
      email: "test@test.com",
      password: "pass123", // pragma: allowlist secret
    });
    expect(screen.getByTestId("user").textContent).toBe("John Doe");
    expect(screen.getByTestId("token").textContent).toBe("jwt-token-123");
    expect(localStorage.getItem("token")).toBe("jwt-token-123");
    expect(localStorage.getItem("name")).toBe("John Doe");
  });

  it("register sets user and stores in localStorage", async () => {
    mockPost.mockResolvedValueOnce({
      data: {
        token: "reg-token-456",
        role: "USER",
        name: "Test User",
        email: "test@test.com",
      },
    } as any);

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>,
    );

    await act(async () => {
      await userEvent.click(screen.getByTestId("register-btn"));
    });

    expect(mockPost).toHaveBeenCalledWith("/auth/register", {
      name: "Test User",
      email: "test@test.com",
      password: "pass123", // pragma: allowlist secret
    });
    expect(screen.getByTestId("user").textContent).toBe("Test User");
    expect(localStorage.getItem("token")).toBe("reg-token-456");
  });

  it("logout clears user and localStorage", async () => {
    localStorage.setItem("token", "old-token");
    localStorage.setItem("role", "ADMIN");
    localStorage.setItem("name", "Admin");
    localStorage.setItem("email", "admin@test.com");

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>,
    );

    expect(screen.getByTestId("user").textContent).toBe("Admin");

    await act(async () => {
      await userEvent.click(screen.getByTestId("logout-btn"));
    });

    expect(screen.getByTestId("user").textContent).toBe("null");
    expect(screen.getByTestId("token").textContent).toBe("null");
    expect(localStorage.getItem("token")).toBeNull();
    expect(localStorage.getItem("role")).toBeNull();
    expect(localStorage.getItem("name")).toBeNull();
  });

  it("useAuth throws when used outside AuthProvider", () => {
    const consoleError = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});
    expect(() => render(<TestConsumer />)).toThrow(
      "useAuth must be used within an AuthProvider",
    );
    consoleError.mockRestore();
  });
});
