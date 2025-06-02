import React, { useState } from 'react';
import { useTodos } from '../contexts/TodoContext';

const TodoItem = ({ todo }) => {
    const { updateTodo, deleteTodo, toggleComplete } = useTodos();
    const [isEditing, setIsEditing] = useState(false);
    const [editedTitle, setEditedTitle] = useState(todo.title);
    const [editedDescription, setEditedDescription] = useState(todo.description || '');

    const handleUpdate = async (e) => {
        e.preventDefault();
        if (!editedTitle.trim()) return; // Basic validation
        try {
            await updateTodo(todo.id, { title: editedTitle, description: editedDescription, completed: todo.completed });
            setIsEditing(false);
        } catch (error) {
            // Error is logged in context, could show item-specific error here
            console.error("Update failed for item:", error);
        }
    };

    const handleDelete = async () => {
        if (window.confirm("Are you sure you want to delete this todo?")) {
            try {
                await deleteTodo(todo.id);
            } catch (error) {
                console.error("Delete failed for item:", error);
            }
        }
    };

    if (isEditing) {
        return (
            <form onSubmit={handleUpdate} className="p-4 mb-2 border rounded-lg bg-white shadow space-y-2">
                <input
                    type="text"
                    value={editedTitle}
                    onChange={(e) => setEditedTitle(e.target.value)}
                    className="w-full p-2 border rounded"
                    placeholder="Title"
                />
                <textarea
                    value={editedDescription}
                    onChange={(e) => setEditedDescription(e.target.value)}
                    className="w-full p-2 border rounded"
                    placeholder="Description (optional)"
                    rows="3"
                />
                <div className="flex space-x-2">
                    <button type="submit" className="bg-green-500 hover:bg-green-600 text-white py-1 px-3 rounded">Save</button>
                    <button type="button" onClick={() => setIsEditing(false)} className="bg-gray-300 hover:bg-gray-400 text-black py-1 px-3 rounded">Cancel</button>
                </div>
            </form>
        );
    }

    return (
        <div className={`p-4 mb-2 border rounded-lg shadow flex justify-between items-center ${todo.completed ? 'bg-green-50 line-through' : 'bg-white'}`}>
            <div onClick={() => toggleComplete(todo.id, todo.completed)} className="cursor-pointer flex-grow">
                <h3 className={`text-lg font-semibold ${todo.completed ? 'text-gray-500' : 'text-gray-800'}`}>{todo.title}</h3>
                {todo.description && <p className={`text-sm ${todo.completed ? 'text-gray-400' : 'text-gray-600'}`}>{todo.description}</p>}
            </div>
            <div className="flex space-x-2 ml-4">
                <button onClick={() => setIsEditing(true)} className="bg-yellow-400 hover:bg-yellow-500 text-white py-1 px-3 rounded text-sm">Edit</button>
                <button onClick={handleDelete} className="bg-red-500 hover:bg-red-600 text-white py-1 px-3 rounded text-sm">Delete</button>
            </div>
        </div>
    );
};

export default TodoItem;
