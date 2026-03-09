import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { AuthLayout } from '../components/ui/AuthLayout';
import { AuthInput } from '../components/ui/AuthInput';
import { AuthButton } from '../components/ui/AuthButton';
import './Login.css'; // Reuse Login styles

import iconMail from '../assets/icon-mail.svg';
import iconLock from '../assets/icon-lock.svg';

export function Register() {
  const [formData, setFormData] = useState({ name: '', email: '', password: '', confirmPassword: '' });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isLoading, setIsLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const validate = () => {
    const newErrors: Record<string, string> = {};
    if (!formData.name) newErrors.name = 'Name is required';
    if (!formData.email) newErrors.email = 'Email is required';
    if (formData.password.length < 6) newErrors.password = 'Minimum of 6 characters required';
    if (formData.password !== formData.confirmPassword) newErrors.confirmPassword = 'Passwords do not match';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;
    setIsLoading(true);
    try {
      await register(formData.name, formData.email, formData.password);
      navigate('/login', { state: { message: 'Registration successful! Please log in.' } });
    } catch (err: any) {
      setErrors({ root: err.message || 'Registration failed' });
    } finally {
      setIsLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData(prev => ({ ...prev, [e.target.name]: e.target.value }));
    if (errors[e.target.name]) setErrors(prev => ({ ...prev, [e.target.name]: '' }));
  };

  return (
    <AuthLayout heading={<>Join the<br />Community</>} subtitle="Create an account to get started">
      <form onSubmit={handleSubmit} className="auth-form-container">
        <div className="auth-inputs-group">
          {errors.root && <p className="auth-helper-text error-text">{errors.root}</p>}
          <AuthInput
            name="name" label="Full Name" placeholder="e.g., John Doe"
            value={formData.name} onChange={handleChange} error={errors.name} iconSrc={iconMail}
            dataTestId="name-input"
          />
          <AuthInput
            name="email" label="Email" type="email" placeholder="your@example.com"
            value={formData.email} onChange={handleChange} error={errors.email} iconSrc={iconMail}
            dataTestId="email-input"
          />
          <AuthInput
            name="password" label="Password" type="password" placeholder="Enter password"
            value={formData.password} onChange={handleChange} error={errors.password}
            helperText="Minimum of 6 characters including special characters" iconSrc={iconLock}
            dataTestId="password-input"
          />
          <AuthInput
            name="confirmPassword" label="Confirm Password" type="password" placeholder="Enter password"
            value={formData.confirmPassword} onChange={handleChange} error={errors.confirmPassword} iconSrc={iconLock}
            dataTestId="confirm-password-input"
          />
        </div>
        <div className="auth-actions-group">
          <AuthButton type="submit" isLoading={isLoading} data-testid="submit-button">Register</AuthButton>
          <div className="auth-footer-text">
            <span>Already have an account? </span><Link to="/login" className="auth-link">Log in</Link>
          </div>
        </div>
      </form>
    </AuthLayout>
  );
}
