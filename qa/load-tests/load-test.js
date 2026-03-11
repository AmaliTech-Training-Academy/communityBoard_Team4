/**
 * k6 Load Test — CommunityBoard API
 *
 * Tests the main API flows under realistic concurrent load.
 *
 * Scenarios:
 *   browse  — unauthenticated GET /api/posts  (80 % of VUs, read-heavy)
 *   auth    — POST /api/auth/login            (10 % of VUs)
 *   write   — authenticated POST + GET flow   (10 % of VUs)
 *
 * Load profile (ramped):
 *   0 →  2 min  ramp from 0 → 20 VUs
 *   2 →  6 min  hold at 20 VUs (steady state)
 *   6 →  8 min  ramp from 20 → 50 VUs (stress)
 *   8 → 10 min  ramp back down to 0
 *
 * Thresholds (hard SLOs — test fails if violated):
 *   http_req_duration p(95) < 1 s
 *   http_req_failed   rate  < 1 %
 *
 * Usage:
 *   k6 run --env TARGET_URL=http://your-alb-dns load-test.js
 *   k6 run --out json=results.json load-test.js
 */

import http from "k6/http";
import { check, group, sleep } from "k6";
import { Rate, Trend } from "k6/metrics";

// ── Configuration ─────────────────────────────────────────────────────────
const TARGET_URL = __ENV.TARGET_URL || "http://community-board-alb-1961944079.eu-west-1.elb.amazonaws.com";

// Custom metrics
const errorRate = new Rate("error_rate");
const loginDuration = new Trend("login_duration", true);
const postListDuration = new Trend("post_list_duration", true);

// ── Options ───────────────────────────────────────────────────────────────
export const options = {
  scenarios: {
    browse: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "2m", target: 16 }, // ramp up  (80% of 20)
        { duration: "4m", target: 16 }, // hold
        { duration: "2m", target: 40 }, // stress   (80% of 50)
        { duration: "2m", target: 0 },  // ramp down
      ],
      exec: "browsePosts",
    },
    auth: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "2m", target: 2 },
        { duration: "4m", target: 2 },
        { duration: "2m", target: 5 },
        { duration: "2m", target: 0 },
      ],
      exec: "loginFlow",
    },
    write: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "2m", target: 2 },
        { duration: "4m", target: 2 },
        { duration: "2m", target: 5 },
        { duration: "2m", target: 0 },
      ],
      exec: "writeFlow",
    },
  },

  thresholds: {
    // 95th-percentile response time under 1 second
    http_req_duration: ["p(95)<1000"],
    // Error rate under 1 %
    http_req_failed: ["rate<0.01"],
    // Per-endpoint thresholds
    post_list_duration: ["p(95)<800"],
    login_duration: ["p(95)<1200"],
  },
};

// ── Helpers ───────────────────────────────────────────────────────────────
const BASE = TARGET_URL.replace(/\/$/, "");
const JSON_HEADERS = { "Content-Type": "application/json" };

function getAuthToken() {
  const res = http.post(
    `${BASE}/api/auth/login`,
    JSON.stringify({ email: "testuser@example.com", password: "Test@1234!" }), // pragma: allowlist secret
    { headers: JSON_HEADERS, tags: { name: "login" } }
  );
  loginDuration.add(res.timings.duration);
  if (res.status === 200) {
    try {
      return res.json("token") || res.json("accessToken") || null;
    } catch (_) {
      return null;
    }
  }
  return null;
}

// ── Scenario: browse posts (unauthenticated) ──────────────────────────────
export function browsePosts() {
  group("Browse Posts", () => {
    // List posts
    const listRes = http.get(`${BASE}/api/posts`, {
      tags: { name: "list_posts" },
    });
    postListDuration.add(listRes.timings.duration);
    const listOk = check(listRes, {
      "list posts → 200": (r) => r.status === 200,
      "list posts → has body": (r) => r.body && r.body.length > 0,
    });
    errorRate.add(!listOk);

    // Fetch a single post if list returned results
    if (listRes.status === 200) {
      let posts = [];
      try {
        const data = listRes.json();
        // handle both array and page wrapper {content:[...]}
        posts = Array.isArray(data) ? data : data.content || [];
      } catch (_) {}

      if (posts.length > 0) {
        const post = posts[Math.floor(Math.random() * posts.length)];
        const detailRes = http.get(`${BASE}/api/posts/${post.id}`, {
          tags: { name: "get_post" },
        });
        const detailOk = check(detailRes, {
          "get post → 200": (r) => r.status === 200,
        });
        errorRate.add(!detailOk);

        // Fetch comments for the post
        const commRes = http.get(`${BASE}/api/posts/${post.id}/comments`, {
          tags: { name: "list_comments" },
        });
        check(commRes, { "list comments → 200": (r) => r.status === 200 });
      }
    }

    // Search posts
    const searchRes = http.get(`${BASE}/api/posts/search?keyword=community`, {
      tags: { name: "search_posts" },
    });
    check(searchRes, { "search posts → 200|404": (r) => [200, 404].includes(r.status) });
  });

  sleep(Math.random() * 2 + 1); // 1–3 s think time
}

// ── Scenario: login ───────────────────────────────────────────────────────
export function loginFlow() {
  group("Auth", () => {
    const token = getAuthToken();
    const ok = check({ token }, { "login → got token": (t) => t.token !== null });
    errorRate.add(!ok);
  });
  sleep(Math.random() * 3 + 2); // 2–5 s think time
}

// ── Scenario: authenticated write flow ───────────────────────────────────
export function writeFlow() {
  group("Authenticated Write", () => {
    const token = getAuthToken();
    if (!token) {
      errorRate.add(true);
      return;
    }

    const authHeaders = {
      ...JSON_HEADERS,
      Authorization: `Bearer ${token}`,
    };

    // Create a post
    const createRes = http.post(
      `${BASE}/api/posts`,
      JSON.stringify({
        title: `Load test post ${Date.now()}`,
        content: "Automated load test content. Please ignore.",
        categoryId: 1,
      }),
      { headers: authHeaders, tags: { name: "create_post" } }
    );
    const createOk = check(createRes, {
      "create post → 201|200": (r) => [200, 201].includes(r.status),
    });
    errorRate.add(!createOk);

    // Add a comment if the post was created
    if (createOk) {
      let postId;
      try {
        postId = createRes.json("id");
      } catch (_) {}

      if (postId) {
        const commentRes = http.post(
          `${BASE}/api/posts/${postId}/comments`,
          JSON.stringify({ content: "Load test comment" }),
          { headers: authHeaders, tags: { name: "create_comment" } }
        );
        check(commentRes, {
          "create comment → 201|200": (r) => [200, 201].includes(r.status),
        });
      }
    }
  });

  sleep(Math.random() * 4 + 3); // 3–7 s think time
}
