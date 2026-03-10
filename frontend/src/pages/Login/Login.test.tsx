import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from '../../context/AuthContext';
import { Login } from './Login';
import { describe, it, expect, vi } from 'vitest';

// Mock the AuthContext login function
const mockLogin = vi.fn();
vi.mock('../../context/AuthContext', async () => {
  const actual = await vi.importActual('../../context/AuthContext');
  return {
    ...actual as any,
    useAuth: () => ({
      login: mockLogin,
    })
  };
});

describe('Login Component', () => {
  const renderLogin = () => {
    return render(
      <BrowserRouter>
        <AuthProvider>
          <Login />
        </AuthProvider>
      </BrowserRouter>
    );
  };

  it('renders login form correctly', () => {
    renderLogin();
    expect(screen.getByText('Welcome back')).toBeInTheDocument();
    expect(screen.getByTestId('email-input')).toBeInTheDocument();
    expect(screen.getByTestId('password-input')).toBeInTheDocument();
    expect(screen.getByTestId('submit-button')).toBeInTheDocument();
  });

  it('shows validation errors when submitting empty form', async () => {
    renderLogin();
    fireEvent.click(screen.getByTestId('submit-button'));
    
    expect(await screen.findByText('Email is required')).toBeInTheDocument();
    expect(await screen.findByText('Password is required')).toBeInTheDocument();
  });

  it('shows error for incorrectly formatted email', async () => {
    const user = userEvent.setup();
    renderLogin();
    
    await user.type(screen.getByTestId('email-input'), 'invalid-email');
    await user.click(screen.getByTestId('submit-button'));
    
    expect(await screen.findByText('Incorrect email')).toBeInTheDocument();
  });

  it('calls login function with correct credentials', async () => {
    const user = userEvent.setup();
    renderLogin();
    
    await user.type(screen.getByTestId('email-input'), 'test@example.com');
    await user.type(screen.getByTestId('password-input'), 'password123');
    await user.click(screen.getByTestId('submit-button'));
    
    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('test@example.com', 'password123');
    });
  });
});
