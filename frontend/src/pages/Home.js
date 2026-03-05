import React, { useEffect, useState } from "react";
import { postAPI } from "../services/api";

const Home = () => {
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    postAPI.getAll()
      .then((res) => setPosts(res.data.content || []))
      .catch((err) => console.error("Failed to load posts", err))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p style={{ marginTop: 30 }}>Loading posts...</p>;

  return (
    <div style={{ marginTop: 30 }}>
      <h1>Recent Posts</h1>
      {posts.length === 0 ? (
        <p>No posts yet. Be the first to post!</p>
      ) : (
        posts.map((post) => (
          <div key={post.id} className="card">
            <h2>{post.title}</h2>
            <p style={{ color: "#636e72", fontSize: 13 }}>
              by {post.authorName} • {new Date(post.createdAt).toLocaleDateString()}
              {post.categoryName && ` • ${post.categoryName}`}
            </p>
            <p style={{ marginTop: 10 }}>{post.content}</p>
            <p style={{ marginTop: 8, color: "#0984e3", fontSize: 13 }}>
              {post.commentCount} comments
            </p>
          </div>
        ))
      )}
    </div>
  );
};

export default Home;
