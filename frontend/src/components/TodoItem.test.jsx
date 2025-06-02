import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import TodoItem from './TodoItem';
import { useTodos } from '../contexts/TodoContext';

// Mock the useTodos hook
vi.mock('../contexts/TodoContext');

const mockUseTodos = useTodos;

describe('TodoItem Component', () => {
  let mockUpdateTodo;
  let mockDeleteTodo;
  let mockToggleComplete;
  let mockTodo;

  const setupMocks = (initialTodoState = {}) => {
    mockUpdateTodo = vi.fn();
    mockDeleteTodo = vi.fn();
    mockToggleComplete = vi.fn();

    mockUseTodos.mockReturnValue({
      updateTodo: mockUpdateTodo,
      deleteTodo: mockDeleteTodo,
      toggleComplete: mockToggleComplete,
    });

    mockTodo = {
      id: '1',
      title: 'Test Todo',
      description: 'Test Description',
      completed: false,
      ...initialTodoState,
    };
  };

  const renderTodoItem = (todoProps) => {
    render(<TodoItem todo={todoProps || mockTodo} />);
  };

  beforeEach(() => {
    setupMocks(); // Reset mocks for each test
    vi.spyOn(window, 'confirm').mockImplementation(vi.fn(() => true)); // Default confirm to true
  });

  afterEach(() => {
    vi.restoreAllMocks(); // Restore window.confirm and any other spies
  });

  test('renders todo item correctly with title and description', () => {
    renderTodoItem();
    expect(screen.getByText(mockTodo.title)).toBeInTheDocument();
    expect(screen.getByText(mockTodo.description)).toBeInTheDocument();
  });

  test('renders correctly without description if not provided', () => {
    setupMocks({ description: '' });
    renderTodoItem();
    expect(screen.getByText(mockTodo.title)).toBeInTheDocument();
    // Check that no <p> tag (which is used for description) exists within the main text container
    const textContainer = screen.getByText(mockTodo.title).closest('div');
    expect(textContainer.querySelector('p')).not.toBeInTheDocument();
  });

  test('calls toggleComplete when the item text area is clicked', () => {
    renderTodoItem();
    // The div containing title and description is clickable
    fireEvent.click(screen.getByText(mockTodo.title).closest('div'));
    expect(mockToggleComplete).toHaveBeenCalledTimes(1);
    expect(mockToggleComplete).toHaveBeenCalledWith(mockTodo.id, mockTodo.completed);
  });

  test('applies line-through style when todo is completed', () => {
    setupMocks({ completed: true });
    renderTodoItem();
    const itemDiv = screen.getByText(mockTodo.title).closest('.flex.justify-between');
    expect(itemDiv).toHaveClass('line-through');
    expect(screen.getByText(mockTodo.title)).toHaveClass('text-gray-500');
    if (mockTodo.description) {
      expect(screen.getByText(mockTodo.description)).toHaveClass('text-gray-400');
    }
  });

  test('Delete button calls deleteTodo after confirmation', async () => {
    renderTodoItem();
    fireEvent.click(screen.getByRole('button', { name: /delete/i }));
    expect(window.confirm).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(mockDeleteTodo).toHaveBeenCalledWith(mockTodo.id));
  });

  test('Delete button does not call deleteTodo if confirmation is cancelled', () => {
    window.confirm.mockImplementationOnce(vi.fn(() => false));
    renderTodoItem();
    fireEvent.click(screen.getByRole('button', { name: /delete/i }));
    expect(window.confirm).toHaveBeenCalledTimes(1);
    expect(mockDeleteTodo).not.toHaveBeenCalled();
  });

  test('Edit button toggles edit mode', () => {
    renderTodoItem();
    fireEvent.click(screen.getByRole('button', { name: /edit/i }));
    expect(screen.getByRole('button', { name: /save/i })).toBeInTheDocument();
    expect(screen.getByDisplayValue(mockTodo.title)).toBeInTheDocument();
    expect(screen.getByDisplayValue(mockTodo.description)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument();
  });

  test('Cancel button in edit mode returns to view mode', () => {
    renderTodoItem();
    fireEvent.click(screen.getByRole('button', { name: /edit/i })); // Enter edit mode
    fireEvent.click(screen.getByRole('button', { name: /cancel/i })); // Click cancel
    expect(screen.getByText(mockTodo.title)).toBeInTheDocument(); // Back to view mode
    expect(screen.queryByRole('button', { name: /save/i })).not.toBeInTheDocument();
  });

  test('Saving an edited todo calls updateTodo and exits edit mode', async () => {
    renderTodoItem();
    fireEvent.click(screen.getByRole('button', { name: /edit/i })); // Enter edit mode

    const newTitle = 'Updated Title';
    const newDescription = 'Updated Description';
    fireEvent.change(screen.getByDisplayValue(mockTodo.title), { target: { value: newTitle } });
    fireEvent.change(screen.getByDisplayValue(mockTodo.description), { target: { value: newDescription } });

    mockUpdateTodo.mockResolvedValueOnce(); // Ensure updateTodo resolves successfully
    fireEvent.click(screen.getByRole('button', { name: /save/i }));

    await waitFor(() => {
      expect(mockUpdateTodo).toHaveBeenCalledWith(mockTodo.id, {
        title: newTitle,
        description: newDescription,
        completed: mockTodo.completed,
      });
    });

    // Simulate prop update by re-rendering with the new title in the todo prop
    // and ensure it exits edit mode.
    // First, ensure edit mode is exited
    await waitFor(() => {
        expect(screen.queryByRole('button', { name: /save/i })).not.toBeInTheDocument();
    });

    // Now re-render with the updated todo data to check the displayed title
    const updatedTodo = { ...mockTodo, title: newTitle, description: newDescription };
    renderTodoItem(updatedTodo); // Assumes renderTodoItem can take a specific todo

    expect(screen.getByText(newTitle)).toBeInTheDocument();
    expect(screen.getByText(newDescription)).toBeInTheDocument(); // Also check description
  });

  test('Does not call updateTodo if edited title is empty', () => {
    renderTodoItem();
    fireEvent.click(screen.getByRole('button', { name: /edit/i }));
    fireEvent.change(screen.getByDisplayValue(mockTodo.title), { target: { value: '  ' } }); // Empty title
    fireEvent.click(screen.getByRole('button', { name: /save/i }));
    expect(mockUpdateTodo).not.toHaveBeenCalled();
    // Stays in edit mode
    expect(screen.getByRole('button', { name: /save/i })).toBeInTheDocument();
  });

  test('logs error if updateTodo fails', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    mockUpdateTodo.mockRejectedValueOnce(new Error('Update failed'));
    renderTodoItem();

    fireEvent.click(screen.getByRole('button', { name: /edit/i }));
    fireEvent.change(screen.getByDisplayValue(mockTodo.title), { target: { value: 'New Title' } });
    fireEvent.click(screen.getByRole('button', { name: /save/i }));

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith("Update failed for item:", expect.any(Error));
    });
    consoleErrorSpy.mockRestore();
  });

  test('logs error if deleteTodo fails', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    mockDeleteTodo.mockRejectedValueOnce(new Error('Delete failed'));
    renderTodoItem();

    fireEvent.click(screen.getByRole('button', { name: /delete/i }));

    await waitFor(() => {
        expect(window.confirm).toHaveBeenCalled(); // Ensure confirm was called
    });
    await waitFor(() => {
        expect(consoleErrorSpy).toHaveBeenCalledWith("Delete failed for item:", expect.any(Error));
    });
    consoleErrorSpy.mockRestore();
  });
});
