import { describe, it, expect, vi, beforeEach } from "vitest";
import axios from "axios";

// We need to test the interceptors. Import the configured instance.
// Since api.ts runs interceptors on import, we test the behavior.

describe("API Service", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("creates axios instance with /api baseURL", async () => {
    const api = (await import("../services/api")).default;
    expect(api.defaults.baseURL).toBe("/api");
  });

  it("attaches token to request headers when present", async () => {
    localStorage.setItem("token", "test-jwt-token");

    // Re-import to get a fresh module
    const api = (await import("../services/api")).default;

    // The request interceptor is registered, test it by inspecting
    // the interceptor handlers
    const requestInterceptors = (api.interceptors.request as any).handlers;
    expect(requestInterceptors.length).toBeGreaterThan(0);

    // Simulate the interceptor
    const config = { headers: {} } as any;
    const fulfilled = requestInterceptors[0].fulfilled;
    const result = fulfilled(config);
    expect(result.headers.Authorization).toBe("Bearer test-jwt-token");
  });

  it("does not attach Authorization header when no token", async () => {
    const api = (await import("../services/api")).default;

    const requestInterceptors = (api.interceptors.request as any).handlers;
    const config = { headers: {} } as any;
    const fulfilled = requestInterceptors[0].fulfilled;
    const result = fulfilled(config);
    expect(result.headers.Authorization).toBeUndefined();
  });

  it("clears localStorage and redirects on 401 response", async () => {
    localStorage.setItem("token", "expired-token");
    localStorage.setItem("role", "USER");
    localStorage.setItem("name", "Test");

    const api = (await import("../services/api")).default;

    const responseInterceptors = (api.interceptors.response as any).handlers;
    const rejected = responseInterceptors[0].rejected;

    // Mock window.location.href
    const originalLocation = window.location;
    Object.defineProperty(window, "location", {
      writable: true,
      value: { ...originalLocation, href: "" },
    });

    const error = { response: { status: 401 } };
    await expect(rejected(error)).rejects.toEqual(error);

    expect(localStorage.getItem("token")).toBeNull();
    expect(localStorage.getItem("role")).toBeNull();
    expect(window.location.href).toBe("/login");

    // Restore
    Object.defineProperty(window, "location", {
      writable: true,
      value: originalLocation,
    });
  });

  it("passes through non-401 errors without redirect", async () => {
    const api = (await import("../services/api")).default;

    const responseInterceptors = (api.interceptors.response as any).handlers;
    const rejected = responseInterceptors[0].rejected;

    const error = { response: { status: 500 } };
    await expect(rejected(error)).rejects.toEqual(error);

    // Should not clear localStorage for non-401
    localStorage.setItem("token", "still-here");
    const error404 = { response: { status: 404 } };
    await expect(rejected(error404)).rejects.toEqual(error404);
    expect(localStorage.getItem("token")).toBe("still-here");
  });
});
