import axios from 'axios';
import apiClient, { getCurrentUser, logoutUser } from './apiService';

// Mock axios
// Remove vi.mock('axios')

// apiClient is the actual instance imported from apiService.js
// We will spy on its methods.
let getSpy;
let postSpy;
// Add putSpy, deleteSpy if those functions are implemented and tested later

describe('API Service', () => {
  beforeEach(() => {
    // Create spies on the methods of the imported apiClient instance
    getSpy = vi.spyOn(apiClient, 'get');
    postSpy = vi.spyOn(apiClient, 'post');
    // vi.spyOn(apiClient, 'put'); // if needed
    // vi.spyOn(apiClient, 'delete'); // if needed

    // Reset mocks is handled by spyOn resetting behavior or explicitly:
    getSpy.mockReset();
    postSpy.mockReset();
  });

  afterEach(() => {
    // Restore original methods after each test to avoid interference
    vi.restoreAllMocks();
  });

  describe('getCurrentUser', () => {
    test('successfully fetches current user data', async () => {
      const userData = { id: '1', name: 'Test User' };
      getSpy.mockResolvedValueOnce({ data: userData });

      const result = await getCurrentUser();

      expect(getSpy).toHaveBeenCalledWith('/api/user/me');
      expect(result).toEqual(userData);
    });

    test('returns null if API returns 401', async () => {
      // Simulate an Axios-like error object structure
      const error = new Error("Request failed with status code 401");
      error.response = { status: 401 };
      getSpy.mockRejectedValueOnce(error);

      const result = await getCurrentUser();

      expect(getSpy).toHaveBeenCalledWith('/api/user/me');
      expect(result).toBeNull();
    });

    test('returns null if API returns 403', async () => {
      const error = new Error("Request failed with status code 403");
      error.response = { status: 403 };
      getSpy.mockRejectedValueOnce(error);

      const result = await getCurrentUser();

      expect(getSpy).toHaveBeenCalledWith('/api/user/me');
      expect(result).toBeNull();
    });

    test('throws error for other API errors (e.g., 500)', async () => {
      const error = new Error("Request failed with status code 500");
      error.response = { status: 500, data: 'Server Error' };
      getSpy.mockRejectedValueOnce(error);

      await expect(getCurrentUser()).rejects.toThrow(error);
      expect(getSpy).toHaveBeenCalledWith('/api/user/me');
    });

    test('throws error for network errors (no response object)', async () => {
        const networkError = new Error('Network Error');
        // No .response for a pure network error
        getSpy.mockRejectedValueOnce(networkError);

        await expect(getCurrentUser()).rejects.toThrow(networkError);
        expect(getSpy).toHaveBeenCalledWith('/api/user/me');
      });
  });

  describe('logoutUser', () => {
    test('successfully logs out the user', async () => {
      postSpy.mockResolvedValueOnce({});

      await logoutUser();

      expect(postSpy).toHaveBeenCalledWith('/api/logout');
    });

    test('throws error if logout API call fails', async () => {
      const error = new Error("Request failed with status code 500");
      error.response = { status: 500, data: 'Logout Failed' };
      postSpy.mockRejectedValueOnce(error);
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      await expect(logoutUser()).rejects.toThrow(error);

      expect(postSpy).toHaveBeenCalledWith('/api/logout');
      expect(consoleErrorSpy).toHaveBeenCalledWith('Logout failed:', error);
      consoleErrorSpy.mockRestore();
    });
  });

  // Placeholders for tests of missing functions
  describe('loginUser (Function not found in apiService.js)', () => {
    test.skip('successfully logs in a user', () => {});
    test.skip('throws error on login failure', () => {});
  });

  describe('fetchTodos (Function not found in apiService.js)', () => {
    test.skip('successfully fetches todos', () => {});
    test.skip('throws error on fetchTodos failure', () => {});
  });

  describe('addTodo (Function not found in apiService.js)', () => {
    test.skip('successfully adds a todo', () => {});
    test.skip('throws error on addTodo failure', () => {});
  });

  describe('updateTodo (Function not found in apiService.js)', () => {
    test.skip('successfully updates a todo', () => {});
    test.skip('throws error on updateTodo failure', () => {});
  });

  describe('deleteTodo (Function not found in apiService.js)', () => {
    test.skip('successfully deletes a todo', () => {});
    test.skip('throws error on deleteTodo failure', () => {});
  });
});
