-- Seed categories
INSERT INTO categories (id, name, description) VALUES
  (1, 'General', 'General discussions and announcements'),
  (2, 'Events', 'Upcoming community events'),
  (3, 'Tech', 'Technology related posts'),
  (4, 'Help', 'Questions and help requests')
ON CONFLICT (id) DO NOTHING;

-- Seed admin user (password: password123, BCrypt encoded)
INSERT INTO users (id, email, name, password, role, created_at) VALUES
  (1, 'admin@amalitech.com', 'Admin User', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', NOW()),
  (2, 'user@amalitech.com', 'Test User', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER', NOW())
ON CONFLICT (id) DO NOTHING;

-- Seed sample posts
INSERT INTO posts (id, title, content, category_id, author_id, created_at, updated_at) VALUES
  (1, 'Welcome to CommunityBoard!', 'This is our new community platform. Feel free to post announcements, share events, and discuss topics.', 1, 1, NOW(), NOW()),
  (2, 'Upcoming Team Building Event', 'We are organizing a cross-location team building event next Friday. Details to follow.', 2, 1, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
