import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import AuthCallbackPage from './AuthCallbackPage';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';

// Mock useAuth
vi.mock('../contexts/AuthContext');
const mockUseAuth = useAuth;

// Mock useNavigate
const mockNavigateFn = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigateFn,
  };
});

describe('AuthCallbackPage Component', () => {
  let mockCheckAuthentication;

  beforeEach(() => {
    mockCheckAuthentication = vi.fn();
    mockUseAuth.mockReturnValue({
      checkAuthentication: mockCheckAuthentication,
      // Provide other defaults if AuthCallbackPage ever uses them directly,
      // for now, it only calls checkAuthentication.
    });
    mockNavigateFn.mockReset();
  });

  test('displays "Processing authentication..." message initially', () => {
    render(<AuthCallbackPage />);
    expect(screen.getByText('Processing authentication...')).toBeInTheDocument();
  });

  test('calls checkAuthentication on mount', () => {
    render(<AuthCallbackPage />);
    expect(mockCheckAuthentication).toHaveBeenCalledTimes(1);
  });

  test('redirects to "/" on successful authentication', async () => {
    mockCheckAuthentication.mockResolvedValueOnce({ id: 'user1', name: 'Test User' });
    render(<AuthCallbackPage />);

    await waitFor(() => {
      expect(mockNavigateFn).toHaveBeenCalledWith('/');
    });
  });

  test('redirects to "/login?error=auth_failed" if authentication fails (checkAuthentication returns null)', async () => {
    mockCheckAuthentication.mockResolvedValueOnce(null);
    render(<AuthCallbackPage />);

    await waitFor(() => {
      expect(mockNavigateFn).toHaveBeenCalledWith('/login?error=auth_failed');
    });
  });

  test('redirects to "/login?error=auth_failed_critical" if checkAuthentication throws an error', async () => {
    // This tests how the current component handles an error from checkAuthentication.
    // If specific error UI on this page was desired, the component would need changes.
    const authError = new Error('Network issue during auth');
    mockCheckAuthentication.mockRejectedValueOnce(authError);

    // Optional: Spy on console.error if we expect the component or hook to log it
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    render(<AuthCallbackPage />);

    // Because the promise from checkAuthentication is not caught inside handleAuthCallback,
    // it will cause an unhandled promise rejection. In a real app, this might bubble up.
    // For this test, we're checking the navigation which happens in the 'finally' equivalent
    // or if the error is caught and then navigates.
    // Given the code: `const user = await checkAuthentication(); if (user) ... else ...`
    // an error from checkAuthentication will skip the `if (user)` block.
    // The effect hook's error handling (or lack thereof for the promise) determines the outcome.
    // Let's assume for now the test environment or useEffect handles it such that it proceeds to the 'else'.
    // More robustly, the component should catch errors from checkAuthentication.

    // The component now navigates to a specific error path on critical failure
    await waitFor(() => {
      expect(mockNavigateFn).toHaveBeenCalledWith('/login?error=auth_failed_critical');
    });
    expect(consoleErrorSpy).toHaveBeenCalledWith("Authentication callback failed:", authError);
    consoleErrorSpy.mockRestore(); // Clean up spy
  });
});
