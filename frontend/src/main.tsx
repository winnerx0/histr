import React from "react";
import ReactDOM from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
  useNavigate,
} from "react-router-dom";
import { App } from "./App";
import { AuthCallback } from "./AuthCallback";
import { AuthProvider, useAuth } from "./auth";
import { Landing } from "./Landing";
import { Login } from "./Login";
import "./styles.css";

const queryClient = new QueryClient();

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { token, bootstrapping } = useAuth();
  if (bootstrapping)
    return (
      <div className="grid min-h-screen place-items-center" aria-busy="true" />
    );
  if (!token) return <Navigate to="/" replace />;
  return <>{children}</>;
}

function PublicOnlyRoute({ children }: { children: React.ReactNode }) {
  const { token, bootstrapping } = useAuth();
  if (bootstrapping)
    return (
      <div className="grid min-h-screen place-items-center" aria-busy="true" />
    );
  if (token) return <Navigate to="/dashboard" replace />;
  return <>{children}</>;
}

function LandingRoute() {
  const navigate = useNavigate();
  return (
    <Landing
      onSignIn={() => navigate("/login")}
      onGetStarted={() => navigate("/login")}
    />
  );
}

function LoginRoute() {
  const navigate = useNavigate();
  return (
    <Login
      onSwitch={() => navigate("/login")}
      onBack={() => navigate("/")}
    />
  );
}

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <BrowserRouter>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <Routes>
            <Route
              path="/"
              element={
                <PublicOnlyRoute>
                  <LandingRoute />
                </PublicOnlyRoute>
              }
            />
            <Route
              path="/login"
              element={
                <PublicOnlyRoute>
                  <LoginRoute />
                </PublicOnlyRoute>
              }
            />
            <Route path="/callback" element={<AuthCallback />} />
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <App />
                </ProtectedRoute>
              }
            />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AuthProvider>
      </QueryClientProvider>
    </BrowserRouter>
  </React.StrictMode>,
);
