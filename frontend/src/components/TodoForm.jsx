import React, { useState } from 'react';
import { useTodos } from '../contexts/TodoContext';

const TodoForm = () => {
    const [title, setTitle] = useState('');
    const [description, setDescription] = useState('');
    const { addTodo, error: contextError, loading } = useTodos();
    const [formError, setFormError] = useState('');


    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!title.trim()) {
            setFormError('Title is required.');
            return;
        }
        setFormError('');
        try {
            await addTodo({ title, description, completed: false });
            setTitle('');
            setDescription('');
        } catch (err) {
            setFormError(err.message || 'Failed to add todo. Please try again.');
            console.error("Failed to add todo (form level):", err);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="p-6 mb-6 bg-white shadow-md rounded-lg space-y-4">
            <h2 className="text-xl font-semibold text-gray-700">Add New TODO</h2>
            {formError && <p className="text-red-500 text-sm">{formError}</p>}
            {contextError && !formError && <p className="text-red-500 text-sm">Context Error: {contextError}</p>}
            <div>
                <label htmlFor="title" className="block text-sm font-medium text-gray-700">Title</label>
                <input
                    id="title"
                    type="text"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    className="mt-1 block w-full p-2 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500"
                    placeholder="What needs to be done?"
                    disabled={loading}
                />
            </div>
            <div>
                <label htmlFor="description" className="block text-sm font-medium text-gray-700">Description (Optional)</label>
                <textarea
                    id="description"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    className="mt-1 block w-full p-2 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500"
                    rows="3"
                    placeholder="Add more details..."
                    disabled={loading}
                />
            </div>
            <button
                type="submit"
                className="w-full py-2 px-4 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-md shadow-sm disabled:bg-blue-300"
                disabled={loading}
            >
                {loading ? 'Adding...' : 'Add TODO'}
            </button>
        </form>
    );
};

export default TodoForm;
