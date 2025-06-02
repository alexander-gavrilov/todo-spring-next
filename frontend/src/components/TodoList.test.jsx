import React from 'react';
import { render, screen } from '@testing-library/react';
import TodoList from './TodoList';
import { useTodos } from '../contexts/TodoContext';

// Mock the useTodos hook
vi.mock('../contexts/TodoContext');
const mockUseTodos = useTodos;

// Mock the TodoItem component
vi.mock('./TodoItem', () => ({
  default: ({ todo }) => (
    <div data-testid={`todo-item-${todo.id}`}>
      <span>{todo.title}</span>
      {todo.description && <span>{todo.description}</span>}
    </div>
  ),
}));

describe('TodoList Component', () => {
  const setupMockUseTodos = (state) => {
    mockUseTodos.mockReturnValue({
      todos: [],
      loading: false,
      error: null,
      ...state, // Spread default state and override with provided state
    });
  };

  beforeEach(() => {
    mockUseTodos.mockReset();
  });

  test('displays "Loading todos..." when loading is true and no todos are present', () => {
    setupMockUseTodos({ loading: true, todos: [] });
    render(<TodoList />);
    expect(screen.getByText('Loading todos...')).toBeInTheDocument();
  });

  test('displays error message when error is present', () => {
    const errorMessage = 'Failed to fetch todos';
    setupMockUseTodos({ error: errorMessage });
    render(<TodoList />);
    expect(screen.getByText(`Error: ${errorMessage}`)).toBeInTheDocument();
  });

  test('displays "No todos yet. Add one above!" when todos array is empty and not loading/no error', () => {
    setupMockUseTodos({ todos: [] });
    render(<TodoList />);
    expect(screen.getByText('No todos yet. Add one above!')).toBeInTheDocument();
  });

  test('renders a list of todo items when todos are provided', () => {
    const todos = [
      { id: '1', title: 'First Todo', description: 'Desc 1', completed: false },
      { id: '2', title: 'Second Todo', description: '', completed: true },
    ];
    setupMockUseTodos({ todos });
    render(<TodoList />);

    expect(screen.getByTestId('todo-item-1')).toBeInTheDocument();
    expect(screen.getByText('First Todo')).toBeInTheDocument();
    expect(screen.getByText('Desc 1')).toBeInTheDocument();

    expect(screen.getByTestId('todo-item-2')).toBeInTheDocument();
    expect(screen.getByText('Second Todo')).toBeInTheDocument();
    expect(screen.queryByText('Desc 2')).not.toBeInTheDocument(); // Description is empty for second todo
  });

  test('renders todo items even when loading is true (e.g., during a refresh)', () => {
    const todos = [{ id: '1', title: 'Existing Todo', completed: false }];
    setupMockUseTodos({ todos, loading: true });
    render(<TodoList />);

    expect(screen.getByTestId('todo-item-1')).toBeInTheDocument();
    expect(screen.getByText('Existing Todo')).toBeInTheDocument();
    // "Loading todos..." should not be shown if there are already todos to display
    expect(screen.queryByText('Loading todos...')).not.toBeInTheDocument();
  });

  test('error message takes precedence over loading and no todos message', () => {
    const errorMessage = 'Critical Error!';
    setupMockUseTodos({
      todos: [],
      loading: true,
      error: errorMessage
    });
    render(<TodoList />);
    expect(screen.getByText(`Error: ${errorMessage}`)).toBeInTheDocument();
    expect(screen.queryByText('Loading todos...')).not.toBeInTheDocument();
    expect(screen.queryByText('No todos yet. Add one above!')).not.toBeInTheDocument();
  });

  test('no todos message takes precedence over loading message if todos are empty but loading is false', () => {
    setupMockUseTodos({
      todos: [],
      loading: false, // Explicitly false, but was true in a previous case
      error: null
    });
    render(<TodoList />);
    expect(screen.getByText('No todos yet. Add one above!')).toBeInTheDocument();
    expect(screen.queryByText('Loading todos...')).not.toBeInTheDocument();
  });

});
