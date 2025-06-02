import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Navbar from './Navbar';
import { useAuth } from '../contexts/AuthContext';

// Mock the useAuth hook
vi.mock('../contexts/AuthContext');

const mockUseAuth = useAuth;

describe('Navbar Component', () => {
  const renderNavbar = (authValue) => {
    mockUseAuth.mockReturnValue(authValue);
    render(
      <MemoryRouter>
        <Navbar />
      </MemoryRouter>
    );
  };

  test('renders correctly', () => {
    renderNavbar({ isAuthenticated: false, user: null, logout: vi.fn(), loading: false });
    expect(screen.getByText('TODO App')).toBeInTheDocument();
  });

  test('displays the brand/logo', () => {
    renderNavbar({ isAuthenticated: false, user: null, logout: vi.fn(), loading: false });
    expect(screen.getByText('TODO App')).toBeInTheDocument();
    expect(screen.getByText('TODO App').closest('a')).toHaveAttribute('href', '/');
  });

  describe('when user is not authenticated', () => {
    test('displays Login link', () => {
      renderNavbar({ isAuthenticated: false, user: null, logout: vi.fn(), loading: false });
      expect(screen.getByText('Login')).toBeInTheDocument();
      expect(screen.getByText('Login').closest('a')).toHaveAttribute('href', '/login');
    });
  });

  describe('when user is authenticated', () => {
    const mockUser = { name: 'Test User' };
    const mockLogout = vi.fn();

    beforeEach(() => {
      renderNavbar({ isAuthenticated: true, user: mockUser, logout: mockLogout, loading: false });
    });

    test('displays welcome message with user name', () => {
      expect(screen.getByText(`Welcome, ${mockUser.name}!`)).toBeInTheDocument();
    });

    test('displays Logout button', () => {
      expect(screen.getByRole('button', { name: /logout/i })).toBeInTheDocument();
    });

    test('calls logout function when Logout button is clicked', () => {
      screen.getByRole('button', { name: /logout/i }).click();
      expect(mockLogout).toHaveBeenCalledTimes(1);
    });
  });

  describe('when auth state is loading', () => {
    test('displays Loading... message', () => {
      renderNavbar({ isAuthenticated: false, user: null, logout: vi.fn(), loading: true });
      expect(screen.getByText('Loading...')).toBeInTheDocument();
    });
  });

  test('navigation links are present', () => {
    // Test for brand link (already covered, but good for this specific requirement)
    renderNavbar({ isAuthenticated: false, user: null, logout: vi.fn(), loading: false });
    expect(screen.getByText('TODO App').closest('a')).toHaveAttribute('href', '/');

    // Test for Login link when not authenticated
    expect(screen.getByText('Login').closest('a')).toHaveAttribute('href', '/login');

    // Test for authenticated user (no specific nav links other than logout, which is a button)
    // If there were other links for authenticated users, they would be tested here.
    // For now, this confirms the structure for different states.
  });
});
