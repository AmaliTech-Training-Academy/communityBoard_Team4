import React, { useState } from 'react';
import './AuthInput.css';

interface AuthInputProps {
  label: string;
  type?: 'text' | 'email' | 'password';
  placeholder: string;
  value: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  iconSrc?: string;
  error?: string | undefined;
  helperText?: string;
  id?: string;
  name?: string;
  dataTestId?: string;
}

export function AuthInput({
  label, type = 'text', placeholder, value, onChange,
  iconSrc, error, helperText, id, name, dataTestId
}: AuthInputProps) {
  const [showPassword, setShowPassword] = useState(false);
  const isPassword = type === 'password';
  const displayType = isPassword && showPassword ? 'text' : type;

  return (
    <div className="auth-field-wrapper">
      <label className="auth-label" htmlFor={id}>{label}</label>
      
      <div className={`auth-input-container ${error ? 'has-error' : ''}`}>
        <div className="auth-input-content">
          {iconSrc && (
            <img src={iconSrc} alt={`${label} icon`} className="auth-input-icon" />
          )}
          
          <input
            id={id}
            name={name}
            data-testid={dataTestId}
            type={displayType}
            className="auth-input-actual"
            placeholder={placeholder}
            value={value}
            onChange={onChange}
          />
          
          {isPassword && (
            <button
              type="button"
              className="password-toggle-btn"
              onClick={() => setShowPassword(!showPassword)}
            >
              <img src="/assets/icon-eye-off.svg" alt="Toggle password visibility" />
            </button>
          )}
        </div>
      </div>
      
      {(error || helperText) && (
        <p className={`auth-helper-text ${error ? 'error-text' : ''}`}>
          {error || helperText}
        </p>
      )}
    </div>
  );
}
