import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import API, { GATEWAY_BASE_URL } from "../utils/api";
import toast from "react-hot-toast";
import "./Login.css";
import "./Register.css";

function Register() {
  // Hook to programmatically navigate between pages
  const navigate = useNavigate();
  // State object to manage input fields for the registration form
  const [form, setForm] = useState({ fullName: "", email: "", password: "", mobile: "" });
  // State to control the submit button loading and disabled states
  const [loading, setLoading] = useState(false);

  // Handle state changes for all input fields dynamically
  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  // Validate form inputs prior to sending request to backend
  function validate(data) {
    if (!data.fullName || !data.email || !data.password) {
      toast.error("Required fields must be completed.");
      return false;
    }
    // Ensure fullName only contains letters, single spaces, hyphens, and apostrophes
    const nameRegex = /^[a-zA-Z\-']+(?: [a-zA-Z\-']+)*$/;
    if (!nameRegex.test(data.fullName)) {
      toast.error("Full name can only contain letters, single spaces, hyphens, and apostrophes, and cannot start or end with a space.");
      return false;
    }
    // Ensure password has at least 8 characters, with letters and numbers
    const passRegex = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;
    if (!passRegex.test(data.password)) {
      toast.error("Password must contain at least 8 characters, including letters and numbers.");
      return false;
    }
    // Ensure mobile number is exactly 10 digits if provided
    if (data.mobile && !/^\d{10}$/.test(data.mobile)) {
      toast.error("Mobile number must be exactly 10 digits.");
      return false;
    }
    return true;
  }

  // Submit registration details to authentication API
  async function handleSubmit(e) {
    e.preventDefault();
    // Stop form submission if validation fails
    if (!validate(form)) return;

    try {
      setLoading(true);
      // Perform POST request to registration endpoint
      await API.post("/auth/register", form);
      toast.success("Account created. You may now login.");
      navigate("/login");
    } catch (error) {
      // Extract API error message if present, fallback to standard error text
      const msg = error.response?.data?.message || error.response?.data || "System registration error occurred.";
      toast.error(typeof msg === "string" ? msg : "Registration failed.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card register-card">
        <div className="auth-header">
          <h1 className="auth-title">Registration</h1>
          <p className="auth-subtitle">Fill in the fields below to create a new account</p>
        </div>

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label className="form-label">Full Name</label>
            <input
              type="text"
              name="fullName"
              className="form-input"
              placeholder="Full name"
              value={form.fullName}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Email Address</label>
            <input
              type="email"
              name="email"
              className="form-input"
              placeholder="Email address"
              value={form.email}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Password</label>
            <input
              type="password"
              name="password"
              className="form-input"
              placeholder="Minimum 8 characters with letters and numbers"
              value={form.password}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Mobile Number</label>
            <input
              type="tel"
              name="mobile"
              className="form-input"
              placeholder="10 digit number"
              value={form.mobile}
              onChange={handleChange}
            />
          </div>

          <button type="submit" className="auth-submit-btn" disabled={loading}>
            {loading ? "Processing..." : "Complete Registration"}
          </button>
        </form>

        <div className="auth-divider">
          <span>or</span>
        </div>

        <a href={`${GATEWAY_BASE_URL}/oauth2/authorization/google`} className="google-btn">
          <svg width="20" height="20" viewBox="0 0 24 24" style={{ marginRight: "10px" }}>
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
          </svg>
          Continue with Google
        </a>

        <div className="auth-footer">
          <p>
            Returning user?{" "}
            <Link to="/login" className="auth-switch-link">
              Log in
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}

export default Register;
