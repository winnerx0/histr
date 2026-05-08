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
import { AuthProvider, useAuth } from "./auth";
import { Landing } from "./Landing";
import { Login } from "./Login";
import { Register } from "./Register";
import "./styles.css";

const queryClient = new QueryClient();

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { token, bootstrapping } = useAuth();
  if (bootstrapping) return <div className="auth-page" aria-busy="true" />;
  if (!token) return <Navigate to="/" replace />;
  return <>{children}</>;
}

function PublicOnlyRoute({ children }: { children: React.ReactNode }) {
  const { token, bootstrapping } = useAuth();
  if (bootstrapping) return <div className="auth-page" aria-busy="true" />;
  if (token) return <Navigate to="/dashboard" replace />;
  return <>{children}</>;
}

function LandingRoute() {
  const navigate = useNavigate();
  return (
    <Landing
      onSignIn={() => navigate("/login")}
      onGetStarted={() => navigate("/register")}
    />
  );
}

function LoginRoute() {
  const navigate = useNavigate();
  return (
    <Login
      onSwitch={() => navigate("/register")}
      onBack={() => navigate("/")}
    />
  );
}

function RegisterRoute() {
  const navigate = useNavigate();
  return (
    <Register
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
            <Route
              path="/register"
              element={
                <PublicOnlyRoute>
                  <RegisterRoute />
                </PublicOnlyRoute>
              }
            />
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
