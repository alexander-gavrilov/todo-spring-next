import React from 'react';
import { render, screen, act, waitFor } from '@testing-library/react';
import { AuthProvider, useAuth } from './AuthContext';
import * as apiService from '../services/apiService'; // Import all as a namespace

// Mock apiService
vi.mock('../services/apiService');

// Helper component to consume the context
const AuthTestConsumer = ({ action, actionArgs = [] }) => {
  const auth = useAuth();
  if (!auth) return <div>No Auth Context</div>;

  return (
    <div>
      <div data-testid="user">{JSON.stringify(auth.user)}</div>
      <div data-testid="loading">{auth.loading.toString()}</div>
      <div data-testid="isAuthenticated">{auth.isAuthenticated.toString()}</div>
      {action && <button onClick={() => auth[action](...actionArgs)}>Perform Action</button>}
    </div>
  );
};

describe('AuthContext', () => {
  let windowLocationHrefSpy;

  beforeEach(() => {
    // Reset mocks
    vi.clearAllMocks(); // Clears all mocks, including apiService if it was auto-mocked with spies

    // Re-establish default mocks for apiService functions for clarity per test suite if needed
    // For now, individual tests will set up their specific mockResolvedValue/mockRejectedValue

    // Mock window.location.href
    // Store original
    const originalLocation = window.location;
    // Delete to allow re-configure
    delete window.location;
    window.location = { ...originalLocation, href: '' }; // Initialize href
    windowLocationHrefSpy = vi.spyOn(window.location, 'href', 'set');
  });

  afterEach(() => {
    vi.restoreAllMocks(); // This will also restore window.location if needed by spyOn or if we restore it manually
  });

  describe('Initial State and useEffect fetchUser', () => {
    test('initial state: loading is true, then user is fetched (success)', async () => {
      const mockUser = { id: '1', name: 'Test User' };
      apiService.getCurrentUser.mockResolvedValueOnce(mockUser);

      render(
        <AuthProvider>
          <AuthTestConsumer />
        </AuthProvider>
      );

      // Initially loading should be true from useState, then useEffect kicks in
      // The immediate state before useEffect's async operation might be hard to catch without flushPromises or specific waits.
      // We'll check the state after the async operation in useEffect completes.
      expect(screen.getByTestId('loading')).toHaveTextContent('true'); // Initial render by AuthProvider

      await waitFor(() => {
        expect(screen.getByTestId('user')).toHaveTextContent(JSON.stringify(mockUser));
        expect(screen.getByTestId('loading')).toHaveTextContent('false');
        expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true');
      });
      expect(apiService.getCurrentUser).toHaveBeenCalledTimes(1);
    });

    test('initial state: loading is true, then no user is fetched (null)', async () => {
      apiService.getCurrentUser.mockResolvedValueOnce(null);
      render(
        <AuthProvider>
          <AuthTestConsumer />
        </AuthProvider>
      );
      expect(screen.getByTestId('loading')).toHaveTextContent('true');

      await waitFor(() => {
        expect(screen.getByTestId('user')).toHaveTextContent('null');
        expect(screen.getByTestId('loading')).toHaveTextContent('false');
        expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('false');
      });
      expect(apiService.getCurrentUser).toHaveBeenCalledTimes(1);
    });

    test('initial state: handles error during initial fetchUser', async () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      apiService.getCurrentUser.mockRejectedValueOnce(new Error('API Down'));
      render(
        <AuthProvider>
          <AuthTestConsumer />
        </AuthProvider>
      );
      expect(screen.getByTestId('loading')).toHaveTextContent('true');

      await waitFor(() => {
        expect(screen.getByTestId('user')).toHaveTextContent('null');
        expect(screen.getByTestId('loading')).toHaveTextContent('false');
        expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('false');
      });
      expect(apiService.getCurrentUser).toHaveBeenCalledTimes(1);
      expect(consoleErrorSpy).toHaveBeenCalledWith("Failed to fetch current user:", expect.any(Error));
      consoleErrorSpy.mockRestore();
    });
  });

  describe('login function', () => {
    test('redirects to the correct provider URL', async () => {
      render(
        <AuthProvider>
          <AuthTestConsumer action="login" actionArgs={['google']} />
        </AuthProvider>
      );

      // Wait for initial loading to complete from useEffect
      await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'));

      act(() => {
        screen.getByRole('button', { name: 'Perform Action' }).click();
      });

      expect(windowLocationHrefSpy).toHaveBeenCalledWith('http://localhost:8080/oauth2/authorization/google');
    });
  });

  describe('logout function', () => {
    test('calls apiLogout, clears user, and redirects on success', async () => {
      apiService.getCurrentUser.mockResolvedValueOnce({ id: '1', name: 'Initial User' }); // Start authenticated
      apiService.logoutUser.mockResolvedValueOnce({}); // Corrected: use original export name 'logoutUser'

      render(
        <AuthProvider>
          <AuthTestConsumer action="logout" />
        </AuthProvider>
      );

      await waitFor(() => expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true'));

      act(() => {
        screen.getByRole('button', { name: 'Perform Action' }).click();
      });

      await waitFor(() => {
        expect(apiService.logoutUser).toHaveBeenCalledTimes(1); // Corrected
        expect(screen.getByTestId('user')).toHaveTextContent('null');
        expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('false');
      });
      expect(windowLocationHrefSpy).toHaveBeenCalledWith('/login');
    });

    test('logs error if apiLogout fails, user is not cleared, no redirect', async () => {
      const mockUser = { id: '1', name: 'Initial User' };
      apiService.getCurrentUser.mockResolvedValueOnce(mockUser); // Start authenticated
      const logoutError = new Error('Logout API Failed');
      apiService.logoutUser.mockRejectedValueOnce(logoutError); // Corrected
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      render(
        <AuthProvider>
          <AuthTestConsumer action="logout" />
        </AuthProvider>
      );

      await waitFor(() => expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true'));

      act(() => {
        screen.getByRole('button', { name: 'Perform Action' }).click();
      });

      await waitFor(() => {
        expect(apiService.logoutUser).toHaveBeenCalledTimes(1); // Corrected
      });
      // User and isAuthenticated should NOT change because apiLogout failed before setUser(null)
      expect(screen.getByTestId('user')).toHaveTextContent(JSON.stringify(mockUser));
      expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true');
      expect(consoleErrorSpy).toHaveBeenCalledWith("Logout failed client-side:", logoutError);
      expect(windowLocationHrefSpy).not.toHaveBeenCalled(); // No redirect on failure
      consoleErrorSpy.mockRestore();
    });
  });

  describe('checkAuthentication function', () => {
    test('sets user if authenticated, loading states managed', async () => {
      const mockUser = { id: 'user123', name: 'Checked User' };
      apiService.getCurrentUser.mockResolvedValueOnce(null); // Initial load = no user
      apiService.getCurrentUser.mockResolvedValueOnce(mockUser); // Called by checkAuthentication

      render(
        <AuthProvider>
          <AuthTestConsumer action="checkAuthentication" />
        </AuthProvider>
      );
      await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false')); // After initial load

      act(() => {
        screen.getByRole('button', { name: 'Perform Action' }).click();
      });

      expect(screen.getByTestId('loading')).toHaveTextContent('true'); // Set by checkAuthentication start

      await waitFor(() => {
        expect(screen.getByTestId('user')).toHaveTextContent(JSON.stringify(mockUser));
        expect(screen.getByTestId('loading')).toHaveTextContent('false');
        expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true');
      });
      expect(apiService.getCurrentUser).toHaveBeenCalledTimes(2); // Initial + checkAuth
    });

    test('clears user if not authenticated, loading states managed', async () => {
      apiService.getCurrentUser.mockResolvedValueOnce({ id: '1', name: 'Initial User' }); // Start authenticated
      apiService.getCurrentUser.mockResolvedValueOnce(null); // Called by checkAuthentication

      render(
        <AuthProvider>
          <AuthTestConsumer action="checkAuthentication" />
        </AuthProvider>
      );
      await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'));

      act(() => {
        screen.getByRole('button', { name: 'Perform Action' }).click();
      });
      expect(screen.getByTestId('loading')).toHaveTextContent('true');

      await waitFor(() => {
        expect(screen.getByTestId('user')).toHaveTextContent('null');
        expect(screen.getByTestId('loading')).toHaveTextContent('false');
        expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('false');
      });
    });

    test('handles API error, clears user, loading states managed', async () => {
      apiService.getCurrentUser.mockResolvedValueOnce({ id: '1', name: 'Initial User' }); // Start authenticated
      apiService.getCurrentUser.mockRejectedValueOnce(new Error('API Error')); // Error in checkAuthentication

      render(
        <AuthProvider>
          <AuthTestConsumer action="checkAuthentication" />
        </AuthProvider>
      );
      await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'));

      act(() => {
        screen.getByRole('button', { name: 'Perform Action' }).click();
      });
      expect(screen.getByTestId('loading')).toHaveTextContent('true');

      await waitFor(() => {
        expect(screen.getByTestId('user')).toHaveTextContent('null');
        expect(screen.getByTestId('loading')).toHaveTextContent('false');
        expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('false');
      });
    });
  });
});
