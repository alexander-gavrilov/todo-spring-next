import React from 'react';
import { useTodos } from '../contexts/TodoContext';
import TodoItem from './TodoItem';

const TodoList = () => {
    const { todos, loading, error } = useTodos();

    if (error) return <p className="text-center text-red-500">Error: {error}</p>;
    if (loading && !todos.length) return <p className="text-center text-gray-500">Loading todos...</p>;
    if (!todos.length && !loading) return <p className="text-center text-gray-500">No todos yet. Add one above!</p>;

    return (
        <div className="mt-6">
            {todos.map(todo => (
                <TodoItem key={todo.id} todo={todo} />
            ))}
        </div>
    );
};

export default TodoList;
