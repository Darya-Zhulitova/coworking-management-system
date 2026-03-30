'use client';

import { FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';

interface LoginState {
  email: string;
  password: string;
}

export function LoginForm() {
  const router = useRouter();
  const [formState, setFormState] = useState<LoginState>({ email: '', password: '' });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify(formState),
      });
      const data = (await response.json().catch(() => null)) as { message?: string } | null;
      if (!response.ok) {
        throw new Error(data?.message || 'Login failed. Please try again.');
      }
      router.replace('/dashboard');
      router.refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Login failed. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="auth-card">
      <h1>Admin Login</h1>
      <p className="auth-subtitle">Sign in with an administrator account.</p>
      <form className="auth-form" onSubmit={handleSubmit}>
        <label className="field">
          <span>Email</span>
          <input
            autoComplete="email"
            className="input"
            name="email"
            onChange={(event) => setFormState((state) => ({ ...state, email: event.target.value }))}
            placeholder="admin@test.test"
            required
            type="email"
            value={formState.email}
          />
        </label>
        <label className="field">
          <span>Password</span>
          <input
            autoComplete="current-password"
            className="input"
            name="password"
            onChange={(event) => setFormState((state) => ({ ...state, password: event.target.value }))}
            placeholder="Enter password"
            required
            type="password"
            value={formState.password}
          />
        </label>
        {errorMessage ? <p className="error-text">{errorMessage}</p> : null}
        <button className="primary-button" disabled={isSubmitting} type="submit">
          {isSubmitting ? 'Signing in...' : 'Sign in'}
        </button>
      </form>
    </section>
  );
}
