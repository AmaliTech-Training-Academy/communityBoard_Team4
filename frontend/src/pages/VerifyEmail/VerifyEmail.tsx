import React, { useEffect, useMemo, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { AuthLayout } from "../../components/ui/AuthLayout";
import "../Login/Login.css";

export function VerifyEmail() {
  const location = useLocation();
  const token = useMemo(() => new URLSearchParams(location.search).get("token"), [location.search]);

  const [status, setStatus] = useState<"loading" | "success" | "error">("loading");
  const [message, setMessage] = useState("Verifying your email...");

  useEffect(() => {
    const verify = async () => {
      if (!token) {
        setStatus("error");
        setMessage("Verification token is missing");
        return;
      }

      try {
        const response = await fetch(`/api/auth/verify-email?token=${encodeURIComponent(token)}`, {
          method: "POST",
        });
        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
          setStatus("error");
          setMessage(data?.message || "Verification failed");
          return;
        }
        setStatus("success");
        setMessage(data?.message || "Email verified successfully");
      } catch {
        setStatus("error");
        setMessage("Verification failed. Please try again.");
      }
    };

    verify();
  }, [token]);

  return (
    <AuthLayout heading="Verify email" subtitle="Confirm your account to continue">
      <div className="auth-form-container">
        <div className="auth-inputs-group">
          <p className={`auth-helper-text ${status === "error" ? "error-text" : ""}`}>
            {message}
          </p>
        </div>
        <div className="auth-actions-group">
          <Link to="/login" className="auth-link">
            Back to login
          </Link>
        </div>
      </div>
    </AuthLayout>
  );
}
