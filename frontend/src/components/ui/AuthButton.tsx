import React from 'react';
import './AuthButton.css';

interface AuthButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  children: React.ReactNode;
  isLoading?: boolean;
}

export function AuthButton({ children, isLoading, className = '', ...props }: AuthButtonProps) {
  return (
    <button 
      className={`auth-btn-primary ${className} ${isLoading ? 'loading' : ''}`}
      disabled={isLoading || props.disabled}
      {...props}
    >
      {isLoading ? (
        <span className="auth-btn-spinner"></span>
      ) : (
        children
      )}
    </button>
  );
}
