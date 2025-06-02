import React, { createContext, useState, useContext, useEffect, useCallback } from 'react';
import apiClient from '../services/apiService'; // Using the configured axios instance
import { useAuth } from './AuthContext';

const TodoContext = createContext();

export const TodoProvider = ({ children }) => {
    const [todos, setTodos] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const { isAuthenticated } = useAuth();

    const fetchTodos = useCallback(async () => {
        if (!isAuthenticated) {
            setTodos([]); // Clear todos if not authenticated
            return;
        }
        setLoading(true);
        setError(null);
        try {
            const response = await apiClient.get('/api/todos');
            setTodos(response.data);
        } catch (err) {
            console.error("Failed to fetch todos:", err);
            setError(err.message || 'Failed to fetch todos');
            setTodos([]); // Clear todos on error
        } finally {
            setLoading(false);
        }
    }, [isAuthenticated]);

    useEffect(() => {
        fetchTodos();
    }, [fetchTodos]);

    const addTodo = async (todoData) => {
        setLoading(true);
        try {
            const response = await apiClient.post('/api/todos', todoData);
            setTodos(prevTodos => [...prevTodos, response.data]);
            setError(null);
        } catch (err) {
            console.error("Failed to add todo:", err);
            setError(err.message || 'Failed to add todo');
            throw err; // Re-throw to allow form to handle error
        } finally {
            setLoading(false);
        }
    };

    const updateTodo = async (id, updatedData) => {
        setLoading(true);
        try {
            const response = await apiClient.put(`/api/todos/${id}`, updatedData);
            setTodos(prevTodos => prevTodos.map(todo => (todo.id === id ? response.data : todo)));
            setError(null);
        } catch (err) {
            console.error("Failed to update todo:", err);
            setError(err.message || 'Failed to update todo');
            throw err; // Re-throw
        } finally {
            setLoading(false);
        }
    };

    const deleteTodo = async (id) => {
        setLoading(true);
        try {
            await apiClient.delete(`/api/todos/${id}`);
            setTodos(prevTodos => prevTodos.filter(todo => todo.id !== id));
            setError(null);
        } catch (err) {
            console.error("Failed to delete todo:", err);
            setError(err.message || 'Failed to delete todo');
            throw err; // Re-throw
        } finally {
            setLoading(false);
        }
    };

    const toggleComplete = async (id, currentStatus) => {
        const todo = todos.find(t => t.id === id);
        if (!todo) return;
        // Construct the DTO with all required fields for an update
        const updatedData = {
            title: todo.title,
            description: todo.description,
            completed: !currentStatus
        };
        await updateTodo(id, updatedData);
    };


    return (
        <TodoContext.Provider value={{ todos, loading, error, fetchTodos, addTodo, updateTodo, deleteTodo, toggleComplete }}>
            {children}
        </TodoContext.Provider>
    );
};

export const useTodos = () => useContext(TodoContext);
