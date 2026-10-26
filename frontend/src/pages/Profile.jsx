import { useState } from 'react';
import { api } from '../api';
import { useAuth } from '../context/AuthContext';

export default function Profile() {
  const { user, setUser } = useAuth();
  const [form, setForm] = useState({ username: user.username, email: user.email, newPassword: '' });
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setMsg('');
    setError('');
    try {
      const body = { username: form.username, email: form.email };
      if (form.newPassword) body.newPassword = form.newPassword;
      setUser(await api.updateMe(body));
      setForm({ ...form, newPassword: '' });
      setMsg('Profile updated');
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <>
      <h1>Profile</h1>
      <form className="card form" onSubmit={submit}>
        <label>Username<input value={form.username} onChange={set('username')} required minLength={3} maxLength={30} /></label>
        <label>Email<input type="email" value={form.email} onChange={set('email')} required /></label>
        <label>New password (optional)<input type="password" value={form.newPassword} onChange={set('newPassword')} minLength={8} autoComplete="new-password" /></label>
        {error && <div className="error" role="alert">{error}</div>}
        {msg && <div className="ok" role="status">{msg}</div>}
        <button className="btn primary">Save</button>
      </form>
    </>
  );
}
