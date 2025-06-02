import React, { useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';

const AuthCallbackPage = () => {
    const { checkAuthentication } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        const handleAuthCallback = async () => {
            const user = await checkAuthentication();
            if (user) {
                navigate('/'); // Redirect to home/dashboard if login successful
            } else {
                navigate('/login?error=auth_failed'); // Redirect to login with error if failed
            }
        };

        handleAuthCallback();
    }, [checkAuthentication, navigate]);

    return (
        <div className="flex items-center justify-center min-h-screen">
            <p className="text-xl">Processing authentication...</p>
        </div>
    );
};

export default AuthCallbackPage;
