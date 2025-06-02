import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import LoginPage from './LoginPage';
import { useAuth } from '../contexts/AuthContext';

// Mock the useAuth hook
vi.mock('../contexts/AuthContext');
const mockUseAuth = useAuth;

// Mock Navigate for checking redirection
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    Navigate: (props) => {
      mockNavigate(props.to);
      return <div data-testid="mock-navigate" data-to={props.to}>Navigating to {props.to}</div>;
    },
  };
});

describe('LoginPage Component', () => {
  let currentMockLogin; // Holds the mock for the current test's login function

  beforeEach(() => {
    currentMockLogin = vi.fn();
    mockUseAuth.mockReset();
    mockNavigate.mockReset();

    // Default mock setup for useAuth
    mockUseAuth.mockReturnValue({
      login: currentMockLogin,
      isAuthenticated: false,
      loading: false,
    });
  });

  test('renders correctly with login buttons when not authenticated and not loading', () => {
    // uses default mock from beforeEach
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { name: /login/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /login with google/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /login with facebook/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /login with microsoft/i })).toBeInTheDocument();
  });

  test('calls login with "google" when Google login button is clicked', () => {
    // uses default mock from beforeEach, which includes currentMockLogin
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );
    fireEvent.click(screen.getByRole('button', { name: /login with google/i }));
    expect(currentMockLogin).toHaveBeenCalledWith('google');
  });

  test('calls login with "facebook" when Facebook login button is clicked', () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );
    fireEvent.click(screen.getByRole('button', { name: /login with facebook/i }));
    expect(currentMockLogin).toHaveBeenCalledWith('facebook');
  });

  test('calls login with "microsoft" when Microsoft login button is clicked', () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );
    fireEvent.click(screen.getByRole('button', { name: /login with microsoft/i }));
    expect(currentMockLogin).toHaveBeenCalledWith('microsoft');
  });

  test('displays "Loading..." when loading is true and not authenticated', () => {
    mockUseAuth.mockReturnValueOnce({ // Override default for this test
      login: currentMockLogin,
      isAuthenticated: false,
      loading: true,
    });
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );
    expect(screen.getByText('Loading...')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /login with google/i })).not.toBeInTheDocument();
  });

  test('redirects to "/" if authenticated and not loading', () => {
    mockUseAuth.mockReturnValueOnce({ // Override default for this test
      login: currentMockLogin,
      isAuthenticated: true,
      loading: false,
    });
    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<div>Home Page</div>} />
        </Routes>
      </MemoryRouter>
    );
    expect(mockNavigate).toHaveBeenCalledWith('/');
    expect(screen.getByTestId('mock-navigate')).toHaveAttribute('data-to', '/');
    expect(screen.getByText('Navigating to /')).toBeInTheDocument();
  });

  test('does not redirect if authenticated but loading is true (waits for loading to finish)', () => {
    mockUseAuth.mockReturnValueOnce({ // Override default for this test
      login: currentMockLogin,
      isAuthenticated: true,
      loading: true,
    });
    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<div>Home Page</div>} />
        </Routes>
      </MemoryRouter>
    );
    // Should show Loading... because loading takes precedence over isAuthenticated for redirection
    expect(screen.getByText('Loading...')).toBeInTheDocument();
    expect(mockNavigate).not.toHaveBeenCalled();
  });
});
