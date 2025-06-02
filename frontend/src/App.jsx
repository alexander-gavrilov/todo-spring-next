import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import LoginPage from './pages/LoginPage';
import Navbar from './components/Navbar';
import AuthCallbackPage from './pages/AuthCallbackPage';
import TodoListPage from './pages/TodoListPage';

const ProtectedRoute = ({ children }) => {
    const { isAuthenticated, loading } = useAuth();
    if (loading) {
        return (
            <div className="flex justify-center items-center min-h-screen">
                <p className="text-xl">Loading application...</p>
            </div>
        );
    }
    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }
    return children;
};

function AppContent() {
    return (
        <div className="flex flex-col min-h-screen bg-gray-50">
            <Navbar />
            <main className="flex-grow">
                <Routes>
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/auth/callback" element={<AuthCallbackPage />} />
                    <Route
                        path="/"
                        element={
                            <ProtectedRoute>
                                <TodoListPage />
                            </ProtectedRoute>
                        }
                    />
                    <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
            </main>
            <footer className="text-center p-4 bg-gray-100 text-sm text-gray-600">
                TODO App &copy; 2023
            </footer>
        </div>
    );
}

function App() {
    return (
        <Router>
            <AuthProvider>
                <AppContent />
            </AuthProvider>
        </Router>
    );
}

export default App;
