import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { AuthLayout } from '../../components/ui/AuthLayout';
import { AuthInput } from '../../components/ui/AuthInput';
import { AuthButton } from '../../components/ui/AuthButton';
import './Login.css';

export function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isLoading, setIsLoading] = useState(false);
  
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setErrors({});
    
    const newErrors: Record<string, string> = {};
    
    if (!email) {
      newErrors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      newErrors.email = 'Incorrect email';
    }
    
    if (!password) {
      newErrors.password = 'Password is required';
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setIsLoading(true);
    try {
      await login(email, password);
      navigate('/');
    } catch (err: any) {
      setErrors({ email: 'Your email or password is incorrect' });
    } finally {
      setIsLoading(false);
    }
  };
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      // To mimic form submit event without extensive mocking, simply dispatch an event
      // However the simplest way in React is to just construct a synthetic event or extract the logic
      const syntheticEvent = { preventDefault: () => {} } as React.FormEvent<HTMLFormElement>;
      handleSubmit(syntheticEvent);
    }
  };

  return (
    <AuthLayout heading="Welcome back" subtitle="Sign in to your neighborhood community">
      <form onSubmit={handleSubmit} className="auth-form-container" noValidate>
        <div className="auth-inputs-group">
          <AuthInput
            id="email"
            label="Email"
            type="email"
            placeholder="your@example.com"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value);
              if (errors.email) setErrors(prev => ({ ...prev, email: '' }));
            }}
            onKeyDown={handleKeyDown}
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
              if (errors.password) setErrors(prev => ({ ...prev, password: '' }));
            }}
            onKeyDown={handleKeyDown}
            iconSrc="/assets/icon-lock.svg"
            error={errors.password}
            dataTestId="password-input"
          />
        </div>

        <div className="auth-actions-group">
          <AuthButton type="submit" isLoading={isLoading} data-testid="submit-button">Log In</AuthButton>
          
          <div className="auth-footer-text">
            <span>Don't have an account? </span>
            <Link to="/register" className="auth-link">Create one now</Link>
          </div>
        </div>
      </form>
    </AuthLayout>
  );
}
