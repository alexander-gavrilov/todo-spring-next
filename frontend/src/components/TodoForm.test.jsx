import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import TodoForm from './TodoForm';
import { useTodos } from '../contexts/TodoContext';

// Mock the useTodos hook
vi.mock('../contexts/TodoContext');

const mockUseTodos = useTodos;

describe('TodoForm Component', () => {
  let mockAddTodo;
  let mockLoading;
  let mockError;

  const renderTodoForm = () => {
    mockAddTodo = vi.fn();
    mockLoading = false;
    mockError = null;

    mockUseTodos.mockReturnValue({
      addTodo: mockAddTodo,
      loading: mockLoading,
      error: mockError,
    });

    render(<TodoForm />);
  };

  const renderTodoFormWithState = (initialState) => {
    mockAddTodo = initialState.addTodo || vi.fn();
    mockLoading = initialState.loading || false;
    mockError = initialState.error || null;

    mockUseTodos.mockReturnValue({
      addTodo: mockAddTodo,
      loading: mockLoading,
      error: mockError,
    });

    render(<TodoForm />);
  }

  beforeEach(() => {
    // Clear mocks before each test, but allow individual tests to set up specific mock return values
    if (mockAddTodo) mockAddTodo.mockClear();
    mockUseTodos.mockReset(); // Reset the mock itself
  });

  test('renders correctly with input fields and submit button', () => {
    renderTodoForm();
    expect(screen.getByLabelText(/title/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText('What needs to be done?')).toBeInTheDocument();
    expect(screen.getByLabelText(/description \(optional\)/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Add more details...')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /add todo/i })).toBeInTheDocument();
  });

  test('allows typing into title and description fields', () => {
    renderTodoForm();
    const titleInput = screen.getByLabelText(/title/i);
    const descriptionInput = screen.getByLabelText(/description \(optional\)/i);

    fireEvent.change(titleInput, { target: { value: 'Test Title' } });
    expect(titleInput.value).toBe('Test Title');

    fireEvent.change(descriptionInput, { target: { value: 'Test Description' } });
    expect(descriptionInput.value).toBe('Test Description');
  });

  test('calls addTodo with correct arguments and clears fields on successful submission', async () => {
    renderTodoFormWithState({ addTodo: mockAddTodo = vi.fn().mockResolvedValueOnce() });

    const titleInput = screen.getByLabelText(/title/i);
    const descriptionInput = screen.getByLabelText(/description \(optional\)/i);
    const submitButton = screen.getByRole('button', { name: /add todo/i });

    const testTitle = 'New Todo Title';
    const testDescription = 'New Todo Description';

    fireEvent.change(titleInput, { target: { value: testTitle } });
    fireEvent.change(descriptionInput, { target: { value: testDescription } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(mockAddTodo).toHaveBeenCalledTimes(1);
      expect(mockAddTodo).toHaveBeenCalledWith({
        title: testTitle,
        description: testDescription,
        completed: false,
      });
    });

    expect(titleInput.value).toBe('');
    expect(descriptionInput.value).toBe('');
  });

  test('shows error message if title is empty on submit and does not call addTodo', () => {
    renderTodoForm();
    const submitButton = screen.getByRole('button', { name: /add todo/i });

    fireEvent.click(submitButton);

    expect(screen.getByText('Title is required.')).toBeInTheDocument();
    expect(mockAddTodo).not.toHaveBeenCalled();
  });

  test('clears title error message after successful validation and submission', async () => {
    renderTodoFormWithState({ addTodo: mockAddTodo = vi.fn().mockResolvedValueOnce() });

    const titleInput = screen.getByLabelText(/title/i);
    const submitButton = screen.getByRole('button', { name: /add todo/i });

    // First, submit with empty title to show error
    fireEvent.click(submitButton);
    expect(screen.getByText('Title is required.')).toBeInTheDocument();

    // Then, type a title and submit again
    fireEvent.change(titleInput, { target: { value: 'Valid Title' } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(mockAddTodo).toHaveBeenCalledTimes(1);
    });
    expect(screen.queryByText('Title is required.')).not.toBeInTheDocument();
  });

  test('displays error message from context if contextError is present', () => {
    const contextErrorMessage = 'Network error from context';
    renderTodoFormWithState({ error: contextErrorMessage });

    expect(screen.getByText(`Context Error: ${contextErrorMessage}`)).toBeInTheDocument();
  });

  test('displays form error message if addTodo fails', async () => {
    const failureMessage = 'Failed to submit';
    renderTodoFormWithState({ addTodo: mockAddTodo = vi.fn().mockRejectedValueOnce(new Error(failureMessage)) });

    const titleInput = screen.getByLabelText(/title/i);
    fireEvent.change(titleInput, { target: { value: 'Test Title' } });
    fireEvent.click(screen.getByRole('button', { name: /add todo/i }));

    await waitFor(() => {
      expect(screen.getByText(failureMessage)).toBeInTheDocument();
    });
  });

  test('disables inputs and button and shows "Adding..." when loading', () => {
    renderTodoFormWithState({ loading: true });

    expect(screen.getByLabelText(/title/i)).toBeDisabled();
    expect(screen.getByLabelText(/description \(optional\)/i)).toBeDisabled();
    expect(screen.getByRole('button', { name: /adding.../i })).toBeDisabled();
  });
});
