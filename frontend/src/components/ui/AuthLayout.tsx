import React from 'react';
import './AuthLayout.css';
import logoRect from '../../assets/logo-rect.svg';
import logoBell from '../../assets/logo-bell.svg';

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
              <span className="logo-text-p">P</span>
              <div className="logo-bell-wrapper">
                <img src={logoRect} alt="" className="logo-rect" />
                <img src={logoBell} alt="Bell Icon" className="logo-bell" />
              </div>
              <span className="logo-text-ng">ng</span>
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
