import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { AuthLayout } from "../../components/ui/AuthLayout";
import { AuthInput } from "../../components/ui/AuthInput";
import { AuthButton } from "../../components/ui/AuthButton";
import "./Login.css";

export function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isLoading, setIsLoading] = useState(false);
  const [isResending, setIsResending] = useState(false);

  const { login } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setErrors({});

    const newErrors: Record<string, string> = {};

    if (!email.trim()) {
      newErrors.email = "Email is required";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      newErrors.email = "Incorrect email";
    }

    if (!password.trim()) {
      newErrors.password = "Password is required"; // pragma: allowlist secret
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setIsLoading(true);
    try {
      await login(email.trim(), password);
      showToast("Authenticated successfully");
      navigate("/");
    } catch (err: any) {
      const msg = err.response?.data?.message || "Login failed";
      setErrors({ root: msg });
      showToast(msg, "error");
    } finally {
      setIsLoading(false);
    }
  };

  const handleResendVerification = async () => {
    if (!email.trim()) {
      setErrors((prev) => ({ ...prev, root: "Enter your email to resend verification" }));
      return;
    }

    setIsResending(true);
    try {
      await fetch("/api/auth/resend-verification", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: email.trim() }),
      });
      showToast("If your account is unverified, a new verification email has been sent");
    } catch {
      showToast("Unable to resend verification email", "error");
    } finally {
      setIsResending(false);
    }
  };

  return (
    <AuthLayout
      heading="Welcome back"
      subtitle="Sign in to your neighborhood community"
    >
      <form onSubmit={handleSubmit} className="auth-form-container" noValidate>
        <div className="auth-inputs-group">
          {errors.root && (
            <>
              <p className="auth-helper-text error-text">{errors.root}</p>
              {errors.root.toLowerCase().includes("not verified") && (
                <button
                  type="button"
                  className="auth-link"
                  onClick={handleResendVerification}
                  disabled={isResending}
                >
                  {isResending ? "Sending..." : "Resend verification email"}
                </button>
              )}
            </>
          )}
          <AuthInput
            id="email"
            label="Email"
            type="email"
            placeholder="your@example.com"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value);
              if (errors.email) setErrors((prev) => ({ ...prev, email: "" }));
            }}
            iconSrc="/assets/mail.svg"
            error={errors.email}
            dataTestId="email-input"
          />
          <AuthInput
            id="password"
            label="Password"
            type="password"
            placeholder="Enter password"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value);
              if (errors.password)
                setErrors((prev) => ({ ...prev, password: "" }));
            }}
            iconSrc="/assets/icon-lock.svg"
            error={errors.password}
            dataTestId="password-input"
          />
        </div>

        <div className="auth-actions-group">
          <AuthButton
            type="submit"
            isLoading={isLoading}
            data-testid="submit-button"
          >
            Log In
          </AuthButton>

          <div className="auth-footer-text">
            <span>Don't have an account? </span>
            <Link to="/register" className="auth-link">
              Create one now
            </Link>
          </div>
        </div>
      </form>
    </AuthLayout>
  );
}
