import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, assetUrl } from '../api';
import { usePlayer } from '../context/PlayerContext';
import SongList from '../components/SongList';

export default function Album() {
  const { id } = useParams();
  const player = usePlayer();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    setData(null);
    api.album(id).then(setData).catch((e) => setError(e.message));
  }, [id]);

  if (error) return <div className="error" role="alert">{error}</div>;
  if (!data) return <p>Loading…</p>;
  const { album, songs } = data;

  return (
    <>
      <div className="hero">
        {album.coverUrl ? <img className="cover lg" src={assetUrl(album.coverUrl)} alt="" /> : <div className="cover lg placeholder">♪</div>}
        <div>
          <p className="sub">Album</p>
          <h1>{album.title}</h1>
          <p className="sub"><Link to={`/artist/${album.artistId}`}>{album.artistName}</Link>{album.releaseDate && ` · ${album.releaseDate.slice(0, 4)}`} · {songs.length} songs</p>
        </div>
      </div>
      <div className="actions">
        <button className="btn primary" disabled={!songs.length} onClick={() => player.playList(songs, 0)}>▶ Play</button>
      </div>
      <SongList songs={songs} />
    </>
  );
}
