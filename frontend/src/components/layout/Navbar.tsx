import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import "./Navbar.css";

export function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const toggleMobileMenu = () => {
    setIsMobileMenuOpen(!isMobileMenuOpen);
  };

  // Extract initials for the avatar if user name exists
  const initials = user?.name
    ? user.name
        .split(" ")
        .map((n: string) => n[0])
        .join("")
        .substring(0, 2)
        .toUpperCase()
    : "U";

  return (
    <nav className="navbar-container border-bottom-light">
      <div className="navbar-content">
        {/* Logo Section */}
        <div
          className="navbar-logo"
          onClick={() => navigate("/")}
          data-testid="navbar-logo"
        >
          <img src="/assets/Logo.svg" alt="Ping Logo" className="logo-svg" />
        </div>

        {/* Desktop Navigation */}
        <div className="navbar-desktop-menu">
          {/* Analytics Button */}
          <button
            className="navbar-action-btn"
            onClick={() => navigate("/analytics")}
          >
            <img
              src="/assets/Analytics.svg"
              alt="Analytics"
              className="icon-dark svg-icon"
            />
            <span className="text-dark-medium">Analytics</span>
          </button>

          {/* User Profile */}
          <div className="navbar-profile">
            <div className="avatar">
              <span>{initials}</span>
            </div>
            <div className="profile-text">
              <span className="profile-name">{user?.name || "User"}</span>
              <span className="profile-email">
                {user?.email || "user@example.com"}
              </span>
            </div>
          </div>

          {/* Logout Button */}
          <button
            className="navbar-action-btn error-btn"
            onClick={handleLogout}
          >
            <img
              src="/assets/log-out.svg"
              alt="Log out"
              className="icon-error svg-icon"
            />
            <span className="text-error-medium">Log out</span>
          </button>
        </div>

        {/* Mobile Menu Toggle Button */}
        <div className="navbar-mobile-toggle">
          <button
            onClick={toggleMobileMenu}
            className="icon-button"
            data-testid="mobile-menu-toggle"
          >
            {isMobileMenuOpen ? (
              <span className="mobile-close-icon">&times;</span>
            ) : (
              <img src="/assets/menu.svg" alt="Menu" className="svg-icon" />
            )}
          </button>
        </div>
      </div>

      {/* Mobile Navigation Dropdown */}
      {isMobileMenuOpen && (
        <div className="navbar-mobile-menu border-bottom-light">
          <div className="mobile-profile-section">
            <div className="avatar">
              <span>{initials}</span>
            </div>
            <div className="profile-text">
              <span className="profile-name">{user?.name || "User"}</span>
              <span className="profile-email">
                {user?.email || "user@example.com"}
              </span>
            </div>
          </div>

          <button
            className="navbar-action-btn mobile-menu-item"
            onClick={() => {
              navigate("/analytics");
              setIsMobileMenuOpen(false);
            }}
          >
            <img
              src="/assets/Analytics.svg"
              alt="Analytics"
              className="icon-dark svg-icon"
            />
            <span className="text-dark-medium">Analytics</span>
          </button>

          <button
            className="navbar-action-btn error-btn mobile-menu-item"
            onClick={handleLogout}
          >
            <img
              src="/assets/log-out.svg"
              alt="Log out"
              className="icon-error svg-icon"
            />
            <span className="text-error-medium">Log out</span>
          </button>
        </div>
      )}
    </nav>
  );
}
