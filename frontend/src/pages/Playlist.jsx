import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api } from '../api';
import { useLibrary } from '../context/LibraryContext';
import { usePlayer } from '../context/PlayerContext';
import SongList from '../components/SongList';

export default function Playlist() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { refreshPlaylists } = useLibrary();
  const player = usePlayer();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  const load = () => api.playlist(id).then(setData).catch((e) => setError(e.message));
  useEffect(() => {
    setData(null);
    setError('');
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  if (error) return <div className="error" role="alert">{error}</div>;
  if (!data) return <p>Loading…</p>;

  const rename = async () => {
    const name = window.prompt('Rename playlist', data.playlist.name);
    if (!name || !name.trim()) return;
    try {
      await api.updatePlaylist(id, { name: name.trim(), description: data.playlist.description });
      await Promise.all([load(), refreshPlaylists()]);
    } catch (e) {
      window.alert(e.message);
    }
  };

  const remove = async () => {
    if (!window.confirm(`Delete playlist "${data.playlist.name}"?`)) return;
    await api.deletePlaylist(id);
    await refreshPlaylists();
    navigate('/library');
  };

  return (
    <>
      <h1>{data.playlist.name}</h1>
      <p className="sub">{data.songs.length} songs</p>
      <div className="actions">
        <button className="btn primary" disabled={!data.songs.length} onClick={() => player.playList(data.songs, 0)}>▶ Play</button>
        <button className="btn" onClick={rename}>Rename</button>
        <button className="btn danger" onClick={remove}>Delete</button>
      </div>
      <SongList
        songs={data.songs}
        playlistId={id}
        onRemoved={(songId) => setData({ ...data, songs: data.songs.filter((s) => s.id !== songId) })}
      />
    </>
  );
}
