import { useCallback, useEffect, useState } from 'react';
import { api } from '../api';

const MAX_AUDIO = 25 * 1024 * 1024;
const MAX_IMAGE = 5 * 1024 * 1024;

const toForm = (obj) => {
  const f = new FormData();
  Object.entries(obj).forEach(([k, v]) => v !== undefined && v !== null && v !== '' && f.append(k, v));
  return f;
};

function readDuration(file) {
  return new Promise((resolve) => {
    const a = new Audio();
    const url = URL.createObjectURL(file);
    a.preload = 'metadata';
    a.onloadedmetadata = () => {
      URL.revokeObjectURL(url);
      resolve(Math.round(a.duration) || 0);
    };
    a.onerror = () => {
      URL.revokeObjectURL(url);
      resolve(0);
    };
    a.src = url;
  });
}

function checkFile(file, max, label) {
  if (file && file.size > max) throw new Error(`${label} must be under ${Math.round(max / 1048576)} MB`);
}

export default function Admin() {
  const [artists, setArtists] = useState([]);
  const [albums, setAlbums] = useState([]);
  const [songs, setSongs] = useState([]);
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    const [a, al, s] = await Promise.all([api.artists(), api.albums(), api.songs({ size: 100 })]);
    setArtists(a);
    setAlbums(al);
    setSongs(s.content);
  }, []);
  useEffect(() => {
    load().catch((e) => setError(e.message));
  }, [load]);

  const act = async (fn, ok) => {
    setMsg('');
    setError('');
    try {
      await fn();
      await load();
      setMsg(ok);
      return true;
    } catch (e) {
      setError(e.message);
      return false;
    }
  };

  const submitArtist = (e) => {
    e.preventDefault();
    const form = e.currentTarget;
    const d = new FormData(form);
    act(async () => {
      checkFile(d.get('image'), MAX_IMAGE, 'Image');
      await api.createArtist(d);
      form.reset();
    }, 'Artist created');
  };

  const submitAlbum = (e) => {
    e.preventDefault();
    const form = e.currentTarget;
    const d = new FormData(form);
    act(async () => {
      checkFile(d.get('cover'), MAX_IMAGE, 'Cover');
      await api.createAlbum(d);
      form.reset();
    }, 'Album created');
  };

  const submitSong = (e) => {
    e.preventDefault();
    const form = e.currentTarget;
    const d = new FormData(form);
    act(async () => {
      const audio = d.get('audio');
      checkFile(audio, MAX_AUDIO, 'Audio file');
      checkFile(d.get('cover'), MAX_IMAGE, 'Cover');
      d.set('durationSec', String(await readDuration(audio)));
      if (!d.get('albumId')) d.delete('albumId');
      if (!d.get('cover')?.size) d.delete('cover');
      await api.createSong(d);
      form.reset();
    }, 'Song uploaded');
  };

  const editSong = (s) => {
    const title = window.prompt('Song title', s.title);
    if (!title?.trim()) return;
    act(() => api.updateSong(s.id, { title: title.trim(), artistId: s.artistId, albumId: s.albumId, durationSec: s.durationSec }), 'Song updated');
  };
  const editArtist = (a) => {
    const name = window.prompt('Artist name', a.name);
    if (!name?.trim()) return;
    act(() => api.updateArtist(a.id, toForm({ name: name.trim(), bio: a.bio })), 'Artist updated');
  };
  const editAlbum = (a) => {
    const title = window.prompt('Album title', a.title);
    if (!title?.trim()) return;
    act(() => api.updateAlbum(a.id, toForm({ title: title.trim(), artistId: a.artistId, releaseDate: a.releaseDate })), 'Album updated');
  };
  const del = (label, fn) => window.confirm(`Delete ${label}? This cannot be undone.`) && act(fn, 'Deleted');

  return (
    <>
      <h1>Admin</h1>
      {msg && <div className="ok" role="status">{msg}</div>}
      {error && <div className="error" role="alert">{error}</div>}

      <div className="admin-grid">
        <form className="card form" onSubmit={submitArtist}>
          <h2>New artist</h2>
          <label>Name<input name="name" required maxLength={150} /></label>
          <label>Bio<textarea name="bio" maxLength={2000} rows={2} /></label>
          <label>Image<input type="file" name="image" accept=".jpg,.jpeg,.png,.webp" /></label>
          <button className="btn primary">Create artist</button>
        </form>

        <form className="card form" onSubmit={submitAlbum}>
          <h2>New album</h2>
          <label>Title<input name="title" required maxLength={200} /></label>
          <label>Artist
            <select name="artistId" required defaultValue="">
              <option value="" disabled>Select…</option>
              {artists.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
            </select>
          </label>
          <label>Release date<input type="date" name="releaseDate" /></label>
          <label>Cover<input type="file" name="cover" accept=".jpg,.jpeg,.png,.webp" /></label>
          <button className="btn primary">Create album</button>
        </form>

        <form className="card form" onSubmit={submitSong}>
          <h2>Upload song</h2>
          <label>Title<input name="title" required maxLength={200} /></label>
          <label>Artist
            <select name="artistId" required defaultValue="">
              <option value="" disabled>Select…</option>
              {artists.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
            </select>
          </label>
          <label>Album (optional)
            <select name="albumId" defaultValue="">
              <option value="">None</option>
              {albums.map((a) => <option key={a.id} value={a.id}>{a.title} — {a.artistName}</option>)}
            </select>
          </label>
          <label>Audio (mp3/ogg/m4a/aac, max 25 MB)<input type="file" name="audio" accept=".mp3,.ogg,.m4a,.aac" required /></label>
          <label>Cover (optional)<input type="file" name="cover" accept=".jpg,.jpeg,.png,.webp" /></label>
          <button className="btn primary">Upload</button>
        </form>
      </div>

      <h2>Songs</h2>
      <ul className="admin-list">
        {songs.map((s) => (
          <li key={s.id}>
            <span>{s.title} <span className="sub">— {s.artistName}</span></span>
            <span>
              <button className="btn" onClick={() => editSong(s)}>Edit</button>
              <button className="btn danger" onClick={() => del(`"${s.title}"`, () => api.deleteSong(s.id))}>Delete</button>
            </span>
          </li>
        ))}
      </ul>
      <h2>Albums</h2>
      <ul className="admin-list">
        {albums.map((a) => (
          <li key={a.id}>
            <span>{a.title} <span className="sub">— {a.artistName}</span></span>
            <span>
              <button className="btn" onClick={() => editAlbum(a)}>Edit</button>
              <button className="btn danger" onClick={() => del(`album "${a.title}"`, () => api.deleteAlbum(a.id))}>Delete</button>
            </span>
          </li>
        ))}
      </ul>
      <h2>Artists</h2>
      <ul className="admin-list">
        {artists.map((a) => (
          <li key={a.id}>
            <span>{a.name}</span>
            <span>
              <button className="btn" onClick={() => editArtist(a)}>Edit</button>
              <button className="btn danger" onClick={() => del(`artist "${a.name}" and all their songs and albums`, () => api.deleteArtist(a.id))}>Delete</button>
            </span>
          </li>
        ))}
      </ul>
    </>
  );
}
