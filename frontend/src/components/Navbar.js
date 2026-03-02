import React from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const Navbar = () => {
  const { user, logout } = useAuth();

  return (
    <nav className="navbar">
      <Link to="/" style={{ fontSize: "20px", fontWeight: "bold" }}>
        CommunityBoard
      </Link>
      <div>
        <Link to="/">Home</Link>
        {user ? (
          <>
            <Link to="/create-post">New Post</Link>
            <span style={{ marginLeft: 20 }}>Hi, {user.name}</span>
            <a href="#" onClick={(e) => { e.preventDefault(); logout(); }} style={{ marginLeft: 20 }}>
              Logout
            </a>
          </>
        ) : (
          <>
            <Link to="/login">Login</Link>
            <Link to="/register">Register</Link>
          </>
        )}
      </div>
    </nav>
  );
};

export default Navbar;
