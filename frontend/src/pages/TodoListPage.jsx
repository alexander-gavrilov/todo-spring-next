import React from 'react';
import TodoForm from '../components/TodoForm';
import TodoList from '../components/TodoList';
import { TodoProvider } from '../contexts/TodoContext';

const TodoListPageContent = () => {
    return (
        <div className="container mx-auto p-4 max-w-2xl">
            <header className="text-center my-6">
                <h1 className="text-4xl font-bold text-gray-800">My TODOs</h1>
            </header>
            <main>
                <TodoForm />
                <TodoList />
            </main>
        </div>
    );
};

const TodoListPage = () => {
  return (
    <TodoProvider>
      <TodoListPageContent />
    </TodoProvider>
  );
};

export default TodoListPage;
