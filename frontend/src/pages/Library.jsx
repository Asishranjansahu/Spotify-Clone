import { Link } from 'react-router-dom';
import { useLibrary } from '../context/LibraryContext';

export default function Library() {
  const { playlists, likedIds } = useLibrary();
  return (
    <>
      <h1>Your Library</h1>
      <div className="grid">
        <Link to="/liked" className="card tile">
          <div className="cover md placeholder">♥</div>
          <div className="title">Liked Songs</div>
          <div className="sub">{likedIds.size} songs</div>
        </Link>
        {playlists.map((p) => (
          <Link key={p.id} to={`/playlist/${p.id}`} className="card tile">
            <div className="cover md placeholder">♫</div>
            <div className="title">{p.name}</div>
            <div className="sub">{p.songCount} songs</div>
          </Link>
        ))}
      </div>
      {playlists.length === 0 && <p className="sub">Use “Create Playlist” in the sidebar to make your first playlist.</p>}
    </>
  );
}
