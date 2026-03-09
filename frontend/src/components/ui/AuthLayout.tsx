import React from 'react';
import './AuthLayout.css';

interface AuthLayoutProps {
  heading: React.ReactNode;
  subtitle: React.ReactNode;
  children: React.ReactNode;
}

export function AuthLayout({ heading, subtitle, children }: AuthLayoutProps) {
  return (
    <div className="auth-page-container">
      <div className="auth-card">
        <div className="auth-content">
          <div className="auth-header">
            <div className="auth-logo-container" data-testid="auth-logo">
              <img src="/assets/Logo.svg" alt="Ping Logo" className="logo-main" />
            </div>
            
            <div className="auth-title-container">
              <h1 className="auth-heading">{heading}</h1>
              <p className="auth-subtitle">{subtitle}</p>
            </div>
          </div>
          
          <div className="auth-body">
            {children}
          </div>
        </div>
      </div>
    </div>
  );
}
