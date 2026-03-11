import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import { AuthProvider } from "../../context/AuthContext";
import { ToastProvider } from "../../context/ToastContext";
import { Register } from "./Register";
import { describe, it, expect, vi } from "vitest";

const mockRegister = vi.fn();
vi.mock("../../context/AuthContext", async () => {
  const actual = await vi.importActual("../../context/AuthContext");
  return {
    ...(actual as any),
    useAuth: () => ({
      register: mockRegister,
    }),
  };
});

describe("Register Component", () => {
  const renderRegister = () => {
    return render(
      <BrowserRouter>
        <ToastProvider>
          <AuthProvider>
            <Register />
          </AuthProvider>
        </ToastProvider>
      </BrowserRouter>,
    );
  };

  it("renders register form correctly", () => {
    renderRegister();
    expect(screen.getByText(/Join the/i)).toBeInTheDocument();
    expect(screen.getByText(/Community/i)).toBeInTheDocument();

    expect(screen.getByTestId("name-input")).toBeInTheDocument();
    expect(screen.getByTestId("email-input")).toBeInTheDocument();
    expect(screen.getByTestId("password-input")).toBeInTheDocument();
    expect(screen.getByTestId("confirm-password-input")).toBeInTheDocument();
    expect(screen.getByTestId("submit-button")).toBeInTheDocument();
  });

  it("shows validation errors for empty fields", async () => {
    renderRegister();
    fireEvent.click(screen.getByTestId("submit-button"));

    await waitFor(() => {
      expect(screen.getByText("Name is required")).toBeInTheDocument();
      expect(screen.getByText("Email is required")).toBeInTheDocument();
    });
  });

  it("shows error for incorrectly formatted email", async () => {
    renderRegister();

    fireEvent.change(screen.getByTestId("name-input"), {
      target: { value: "Test User" },
    });
    fireEvent.change(screen.getByTestId("email-input"), {
      target: { value: "johndoe@" },
    });
    fireEvent.change(screen.getByTestId("password-input"), {
      target: { value: "password123" },
    });
    fireEvent.change(screen.getByTestId("confirm-password-input"), {
      target: { value: "password123" },
    });
    fireEvent.click(screen.getByTestId("submit-button"));

    await waitFor(() => {
      expect(screen.getByText("Invalid email format")).toBeInTheDocument();
    });
  });

  it("shows error for name containing numbers", async () => {
    renderRegister();

    fireEvent.change(screen.getByTestId("name-input"), {
      target: { value: "Jane1 Doe" },
    });
    fireEvent.change(screen.getByTestId("email-input"), {
      target: { value: "test@example.com" },
    });
    fireEvent.change(screen.getByTestId("password-input"), {
      target: { value: "password123" },
    });
    fireEvent.change(screen.getByTestId("confirm-password-input"), {
      target: { value: "password123" },
    });
    fireEvent.click(screen.getByTestId("submit-button"));

    await waitFor(() => {
      expect(
        screen.getByText("Name can only contain letters and spaces"),
      ).toBeInTheDocument();
    });
  });

  it("validates password length and matching correctly", async () => {
    renderRegister();

    // Fill out valid name/email but invalid passwords
    fireEvent.change(screen.getByTestId("name-input"), {
      target: { value: "Test User" },
    });
    fireEvent.change(screen.getByTestId("email-input"), {
      target: { value: "test@example.com" },
    });
    fireEvent.change(screen.getByTestId("password-input"), {
      target: { value: "123" },
    }); // Too short
    fireEvent.change(screen.getByTestId("confirm-password-input"), {
      target: { value: "1234" },
    }); // Mismatch
    fireEvent.click(screen.getByTestId("submit-button"));

    await waitFor(() => {
      expect(
        screen.getByText("Minimum of 6 characters required"),
      ).toBeInTheDocument();
      expect(screen.getByText("Passwords do not match")).toBeInTheDocument();
    });
  });

  it("calls register function with valid data", async () => {
    renderRegister();

    fireEvent.change(screen.getByTestId("name-input"), {
      target: { value: "Test User" },
    });
    fireEvent.change(screen.getByTestId("email-input"), {
      target: { value: "test@example.com" },
    });
    fireEvent.change(screen.getByTestId("password-input"), {
      target: { value: "password123" },
    });
    fireEvent.change(screen.getByTestId("confirm-password-input"), {
      target: { value: "password123" },
    });

    fireEvent.click(screen.getByTestId("submit-button"));

    await waitFor(() => {
      expect(mockRegister).toHaveBeenCalledWith(
        "Test User",
        "test@example.com",
        "password123",
      );
    });
  });
});
