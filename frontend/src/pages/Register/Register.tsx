import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { AuthLayout } from "../../components/ui/AuthLayout";
import { AuthInput } from "../../components/ui/AuthInput";
import { AuthButton } from "../../components/ui/AuthButton";
import "../Login/Login.css"; // Reuse Login styles

export function Register() {
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    password: "",
    confirmPassword: "",
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isLoading, setIsLoading] = useState(false);
  const { register } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const validate = () => {
    const newErrors: Record<string, string> = {};
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    const nameRegex = /^[A-Za-z\s]+$/;

    if (!formData.name.trim()) {
      newErrors.name = "Name is required";
    } else if (!nameRegex.test(formData.name.trim())) {
      newErrors.name = "Name can only contain letters and spaces";
    }

    if (!formData.email.trim()) {
      newErrors.email = "Email is required";
    } else if (!emailRegex.test(formData.email.trim())) {
      newErrors.email = "Invalid email format";
    }
    if (formData.password.trim().length < 6)
      newErrors.password = "Minimum of 6 characters required"; // pragma: allowlist secret
    if (formData.password !== formData.confirmPassword)
      newErrors.confirmPassword = "Passwords do not match"; // pragma: allowlist secret
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!validate()) return;
    setIsLoading(true);
    try {
      await register(
        formData.name.trim(),
        formData.email.trim(),
        formData.password,
      );
      showToast("Registration successful!");
      navigate("/");
    } catch (err: any) {
      const msg = err.response?.data?.message || "Registration failed";
      setErrors({ root: msg });
      showToast(msg, "error");
    } finally {
      setIsLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }));
    if (errors[e.target.name])
      setErrors((prev) => ({ ...prev, [e.target.name]: "" }));
  };

  return (
    <AuthLayout
      heading={
        <>
          Join the
          <br />
          Community
        </>
      }
      subtitle="Create an account to get started"
    >
      <form onSubmit={handleSubmit} className="auth-form-container" noValidate>
        <div className="auth-inputs-group">
          {errors.root && (
            <p className="auth-helper-text error-text">{errors.root}</p>
          )}
          <AuthInput
            name="name"
            label="Full Name"
            placeholder="e.g., John Doe"
            value={formData.name}
            onChange={handleChange}
            error={errors.name}
            iconSrc="/assets/mail.svg"
            dataTestId="name-input"
          />
          <AuthInput
            name="email"
            label="Email"
            type="email"
            placeholder="your@example.com"
            value={formData.email}
            onChange={handleChange}
            error={errors.email}
            iconSrc="/assets/mail.svg"
            dataTestId="email-input"
          />
          <AuthInput
            name="password"
            label="Password"
            type="password"
            placeholder="Enter password"
            value={formData.password}
            onChange={handleChange}
            error={errors.password}
            helperText="Minimum of 6 characters including special characters"
            iconSrc="/assets/icon-lock.svg"
            dataTestId="password-input"
          />
          <AuthInput
            name="confirmPassword"
            label="Confirm Password"
            type="password"
            placeholder="Enter password"
            value={formData.confirmPassword}
            onChange={handleChange}
            error={errors.confirmPassword}
            iconSrc="/assets/icon-lock.svg"
            dataTestId="confirm-password-input"
          />
        </div>
        <div className="auth-actions-group">
          <AuthButton
            type="submit"
            isLoading={isLoading}
            data-testid="submit-button"
          >
            Register
          </AuthButton>
          <div className="auth-footer-text">
            <span>Already have an account? </span>
            <Link to="/login" className="auth-link">
              Log in
            </Link>
          </div>
        </div>
      </form>
    </AuthLayout>
  );
}
