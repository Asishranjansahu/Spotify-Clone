import { NavLink, useNavigate } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../context/AuthContext';
import { useLibrary } from '../context/LibraryContext';

export default function Sidebar() {
  const { user, logout } = useAuth();
  const { playlists, refreshPlaylists } = useLibrary();
  const navigate = useNavigate();

  const createPlaylist = async () => {
    const name = window.prompt('Playlist name', `My Playlist #${playlists.length + 1}`);
    if (!name || !name.trim()) return;
    try {
      const p = await api.createPlaylist({ name: name.trim() });
      await refreshPlaylists();
      navigate(`/playlist/${p.id}`);
    } catch (e) {
      window.alert(e.message);
    }
  };

  return (
    <nav className="sidebar" aria-label="Main navigation">
      <div className="brand">♫ Spotify Clone</div>
      <ul className="nav">
        <li><NavLink to="/" end>Home</NavLink></li>
        <li><NavLink to="/search">Search</NavLink></li>
        <li><NavLink to="/library">Your Library</NavLink></li>
        <li><NavLink to="/liked">Liked Songs</NavLink></li>
        {user?.role === 'ADMIN' && <li><NavLink to="/admin">Admin</NavLink></li>}
      </ul>
      <button className="btn-link" onClick={createPlaylist}>+ Create Playlist</button>
      <ul className="nav playlists">
        {playlists.map((p) => (
          <li key={p.id}><NavLink to={`/playlist/${p.id}`}>{p.name}</NavLink></li>
        ))}
      </ul>
      <div className="sidebar-footer">
        <NavLink to="/profile">{user?.username}</NavLink>
        <button className="btn-link" onClick={logout}>Log out</button>
      </div>
    </nav>
  );
}
