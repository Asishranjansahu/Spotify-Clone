import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, assetUrl } from '../api';
import { usePlayer } from '../context/PlayerContext';
import SongList from '../components/SongList';

export default function Artist() {
  const { id } = useParams();
  const player = usePlayer();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    setData(null);
    api.artist(id).then(setData).catch((e) => setError(e.message));
  }, [id]);

  if (error) return <div className="error" role="alert">{error}</div>;
  if (!data) return <p>Loading…</p>;
  const { artist, albums, songs } = data;

  return (
    <>
      <div className="hero">
        {artist.imageUrl ? <img className="cover lg round" src={assetUrl(artist.imageUrl)} alt="" /> : <div className="cover lg round placeholder">♪</div>}
        <div>
          <p className="sub">Artist</p>
          <h1>{artist.name}</h1>
          {artist.bio && <p className="sub">{artist.bio}</p>}
        </div>
      </div>
      <div className="actions">
        <button className="btn primary" disabled={!songs.length} onClick={() => player.playList(songs, 0)}>▶ Play</button>
      </div>
      {albums.length > 0 && (
        <section>
          <h2>Albums</h2>
          <div className="grid">
            {albums.map((a) => (
              <Link key={a.id} to={`/album/${a.id}`} className="card tile">
                {a.coverUrl ? <img className="cover md" src={assetUrl(a.coverUrl)} alt="" /> : <div className="cover md placeholder">♪</div>}
                <div className="title">{a.title}</div>
                <div className="sub">{a.releaseDate?.slice(0, 4)}</div>
              </Link>
            ))}
          </div>
        </section>
      )}
      <section>
        <h2>Songs</h2>
        <SongList songs={songs} />
      </section>
    </>
  );
}
