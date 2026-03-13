import React, { useEffect, useState } from "react";
import { Navbar } from "../../components/layout/Navbar";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import api from "../../services/api";
import "./Profile.css";

export function Profile() {
  const { user, updateUserName } = useAuth();
  const { showToast } = useToast();

  const [name, setName] = useState(user?.name || "");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [saving, setSaving] = useState(false);
  const [savingSubscriptions, setSavingSubscriptions] = useState(false);
  const [showPasswordSection, setShowPasswordSection] = useState(false);
  const [availableCategories, setAvailableCategories] = useState<string[]>([]);
  const [subscribedCategories, setSubscribedCategories] = useState<string[]>([]);

  useEffect(() => {
    const loadSubscriptionSettings = async () => {
      try {
        const [{ data: categories }, { data: subscriptions }] = await Promise.all([
          api.get("/categories"),
          api.get("/subscriptions/categories"),
        ]);
        setAvailableCategories(categories || []);
        setSubscribedCategories(subscriptions?.categories || []);
      } catch {
        setAvailableCategories(["NEWS", "EVENT", "DISCUSSION", "ALERT"]);
      }
    };

    loadSubscriptionSettings();
  }, []);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      const body: Record<string, string> = { name };
      if (showPasswordSection && currentPassword && newPassword) {
        body.currentPassword = currentPassword;
        body.newPassword = newPassword;
      }
      await api.put("/users/me", body);
      updateUserName(name);
      setCurrentPassword("");
      setNewPassword("");
      setShowPasswordSection(false);
      showToast("Profile updated successfully");
    } catch (err: any) {
      const msg = err?.response?.data?.message || "Failed to update profile";
      showToast(msg, "error");
    } finally {
      setSaving(false);
    }
  };

  const initials = user?.name
    ? user.name
        .split(" ")
        .map((n: string) => n[0])
        .join("")
        .substring(0, 2)
        .toUpperCase()
    : "U";

  const toggleCategory = (category: string) => {
    setSubscribedCategories((prev) =>
      prev.includes(category)
        ? prev.filter((c) => c !== category)
        : [...prev, category],
    );
  };

  const handleSaveSubscriptions = async () => {
    setSavingSubscriptions(true);
    try {
      await api.put("/subscriptions/categories", {
        categories: subscribedCategories,
      });
      showToast("Email notification subscriptions updated");
    } catch (err: any) {
      const msg =
        err?.response?.data?.message ||
        "Failed to update notification subscriptions";
      showToast(msg, "error");
    } finally {
      setSavingSubscriptions(false);
    }
  };

  return (
    <div className="profile-page">
      <Navbar />
      <main className="profile-main">
        <div className="profile-card">
          {/* Avatar */}
          <div className="profile-avatar-lg">
            <span>{initials}</span>
          </div>

          {/* Read-only meta */}
          <div className="profile-meta">
            <span className="profile-role-badge">{user?.role}</span>
            <p className="profile-email-display">{user?.email}</p>
          </div>

          <form className="profile-form" onSubmit={handleSave}>
            <div className="profile-field">
              <label htmlFor="profile-name">Display Name</label>
              <input
                id="profile-name"
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                maxLength={100}
                data-testid="profile-name-input"
              />
            </div>

            <div className="profile-password-toggle">
              <button
                type="button"
                className="toggle-password-btn"
                onClick={() => setShowPasswordSection(!showPasswordSection)}
              >
                {showPasswordSection ? "Cancel password change" : "Change password"}
              </button>
            </div>

            {showPasswordSection && (
              <div className="profile-password-section">
                <div className="profile-field">
                  <label htmlFor="current-password">Current Password</label>
                  <input
                    id="current-password"
                    type="password"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    autoComplete="current-password"
                  />
                </div>
                <div className="profile-field">
                  <label htmlFor="new-password">New Password</label>
                  <input
                    id="new-password"
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    minLength={8}
                    autoComplete="new-password"
                  />
                </div>
              </div>
            )}

            <button
              type="submit"
              className="profile-save-btn"
              disabled={saving}
              data-testid="profile-save-btn"
            >
              {saving ? "Saving…" : "Save Changes"}
            </button>
          </form>

          <section className="subscription-section">
            <h3 className="subscription-title">Email Notifications</h3>
            <p className="subscription-description">
              Receive email alerts when a new post is published in selected
              categories.
            </p>

            <div className="subscription-grid">
              {availableCategories.map((category) => {
                const checked = subscribedCategories.includes(category);
                return (
                  <label key={category} className="subscription-option">
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={() => toggleCategory(category)}
                    />
                    <span>{category}</span>
                  </label>
                );
              })}
            </div>

            <button
              type="button"
              className="profile-save-btn"
              onClick={handleSaveSubscriptions}
              disabled={savingSubscriptions}
              data-testid="subscription-save-btn"
            >
              {savingSubscriptions ? "Saving…" : "Save Notification Preferences"}
            </button>
          </section>
        </div>
      </main>
    </div>
  );
}
