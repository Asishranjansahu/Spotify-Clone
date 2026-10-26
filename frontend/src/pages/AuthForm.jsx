import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function AuthForm({ mode }) {
  const { login, register } = useAuth();
  const [form, setForm] = useState({ username: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const isLogin = mode === 'login';
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      if (isLogin) await login(form.email, form.password);
      else await register(form);
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  };

  return (
    <div className="center-screen">
      <form className="card auth" onSubmit={submit}>
        <h1>♫ Spotify Clone</h1>
        <h2>{isLogin ? 'Log in' : 'Create account'}</h2>
        {!isLogin && (
          <label>Username
            <input value={form.username} onChange={set('username')} required minLength={3} maxLength={30} pattern="[A-Za-z0-9_.\-]+" title="Letters, numbers, . _ -" autoComplete="username" />
          </label>
        )}
        <label>Email
          <input type="email" value={form.email} onChange={set('email')} required autoComplete="email" />
        </label>
        <label>Password
          <input type="password" value={form.password} onChange={set('password')} required minLength={isLogin ? 1 : 8} autoComplete={isLogin ? 'current-password' : 'new-password'} />
        </label>
        {error && <div className="error" role="alert">{error}</div>}
        <button className="btn primary" disabled={busy}>{isLogin ? 'Log in' : 'Sign up'}</button>
        <p className="sub">
          {isLogin ? <>No account? <Link to="/register">Sign up</Link></> : <>Have an account? <Link to="/login">Log in</Link></>}
        </p>
      </form>
    </div>
  );
}
