import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import { Link } from 'react-router-dom';

const Navbar = () => {
    const { isAuthenticated, user, logout, loading } = useAuth();

    return (
        <nav className="bg-blue-500 p-4 text-white flex justify-between items-center">
            <Link to="/" className="text-xl font-bold">TODO App</Link>
            <div>
                {loading ? (
                    <span>Loading...</span>
                ) : isAuthenticated && user ? (
                    <div className="flex items-center space-x-4">
                        <span>Welcome, {user.name || user.preferred_username || user.login || 'User'}!</span>
                        <button
                            onClick={logout}
                            className="bg-red-500 hover:bg-red-700 text-white font-bold py-2 px-4 rounded"
                        >
                            Logout
                        </button>
                    </div>
                ) : (
                    <Link to="/login" className="hover:underline">Login</Link>
                )}
            </div>
        </nav>
    );
};

export default Navbar;
