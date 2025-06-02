import React from 'react';
import { render, screen } from '@testing-library/react';
import TodoListPage from './TodoListPage'; // This component includes TodoProvider
import { useTodos } from '../contexts/TodoContext';
import { useAuth } from '../contexts/AuthContext'; // Import useAuth to mock it

// Mock child components
vi.mock('../components/TodoForm', () => ({
  default: () => <div data-testid="mock-todo-form">Mocked TodoForm</div>,
}));

vi.mock('../components/TodoList', () => ({
  // The TodoList mock can also "use" the useTodos mock to show it's receiving context data
  default: () => {
    const { todos, loading, error } = useTodos(); // use the mocked useTodos
    if (error) return <div data-testid="mock-todo-list-error">{`Error: ${error}`}</div>;
    if (loading && !todos.length) return <div data-testid="mock-todo-list-loading">Loading...</div>;
    if (!todos.length) return <div data-testid="mock-todo-list-empty">No todos</div>;
    return <div data-testid="mock-todo-list">{`Todos count: ${todos.length}`}</div>;
  }
}));

// Mock the useAuth hook from AuthContext
vi.mock('../contexts/AuthContext');
const mockUseAuth = useAuth;

// Mock the useTodos hook from TodoContext
vi.mock('../contexts/TodoContext', async () => {
  const actual = await vi.importActual('../contexts/TodoContext');
  return {
    ...actual, // Import and retain TodoProvider
    useTodos: vi.fn(), // Mock useTodos
  };
});

const mockUseTodos = useTodos;

describe('TodoListPage Component', () => {
  const setupMockUseTodos = (state) => {
    mockUseTodos.mockReturnValue({
      todos: [],
      loading: false,
      error: null,
      addTodo: vi.fn(),
      updateTodo: vi.fn(),
      deleteTodo: vi.fn(),
      toggleComplete: vi.fn(),
      ...state,
    });
  };

  beforeEach(() => {
    mockUseTodos.mockReset();
    // Setup a default mock for useAuth for all tests in this suite
    mockUseAuth.mockReturnValue({
      isAuthenticated: true, // Or false, depending on typical unauthenticated state for TodoList
      user: { id: 'test-user' }, // Provide a user object if TodoProvider might use it
      loading: false,
      login: vi.fn(),
      logout: vi.fn(),
    });
  });

  test('renders the main heading, TodoForm, and TodoList', () => {
    setupMockUseTodos({ todos: [] }); // Default empty state
    render(<TodoListPage />);

    expect(screen.getByRole('heading', { name: /my todos/i })).toBeInTheDocument();
    expect(screen.getByTestId('mock-todo-form')).toBeInTheDocument();
    // When todos are empty, the mock shows the "empty" state.
    expect(screen.getByTestId('mock-todo-list-empty')).toBeInTheDocument();
  });

  test('TodoList (mock) displays loading state from context', () => {
    setupMockUseTodos({ loading: true, todos: [] });
    render(<TodoListPage />);
    expect(screen.getByTestId('mock-todo-list-loading')).toBeInTheDocument();
  });

  test('TodoList (mock) displays error state from context', () => {
    const errorMessage = "Failed to load";
    setupMockUseTodos({ error: errorMessage, todos: [] });
    render(<TodoListPage />);
    expect(screen.getByTestId('mock-todo-list-error')).toHaveTextContent(`Error: ${errorMessage}`);
  });

  test('TodoList (mock) displays empty message from context', () => {
    setupMockUseTodos({ todos: [] });
    render(<TodoListPage />);
    expect(screen.getByTestId('mock-todo-list-empty')).toBeInTheDocument();
  });

  test('TodoList (mock) receives and can react to todos from context', () => {
    const todos = [{ id: '1', title: 'Test' }];
    setupMockUseTodos({ todos });
    render(<TodoListPage />);
    expect(screen.getByTestId('mock-todo-list')).toHaveTextContent(`Todos count: ${todos.length}`);
  });
});
