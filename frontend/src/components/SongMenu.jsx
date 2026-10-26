import { useEffect, useRef, useState } from 'react';
import { api, resolveSong } from '../api';
import { useLibrary } from '../context/LibraryContext';
import { usePlayer } from '../context/PlayerContext';

export default function SongMenu({ song, playlistId, onRemoved }) {
  const [open, setOpen] = useState(false);
  const [picking, setPicking] = useState(false);
  const [msg, setMsg] = useState('');
  const ref = useRef(null);
  const { playlists, refreshPlaylists } = useLibrary();
  const player = usePlayer();

  useEffect(() => {
    if (!open) return;
    const close = (e) => {
      if (!ref.current?.contains(e.target)) {
        setOpen(false);
        setPicking(false);
      }
    };
    const esc = (e) => e.key === 'Escape' && setOpen(false);
    document.addEventListener('mousedown', close);
    document.addEventListener('keydown', esc);
    return () => {
      document.removeEventListener('mousedown', close);
      document.removeEventListener('keydown', esc);
    };
  }, [open]);

  const run = async (fn, okMsg) => {
    try {
      await fn();
      setMsg(okMsg);
    } catch (e) {
      setMsg(e.message);
    }
    setOpen(false);
    setPicking(false);
    setTimeout(() => setMsg(''), 2500);
  };

  return (
    <div className="menu-wrap" ref={ref}>
      <button className="icon-btn" aria-label={`More options for ${song.title}`} aria-haspopup="menu" aria-expanded={open} onClick={() => setOpen((o) => !o)}>⋯</button>
      {msg && <span className="toast">{msg}</span>}
      {open && (
        <ul className="menu" role="menu">
          {!picking ? (
            <>
              <li><button role="menuitem" onClick={() => run(async () => player.playNext(song), 'Playing next')}>Play next</button></li>
              <li><button role="menuitem" onClick={() => run(async () => player.addToQueue(song), 'Added to queue')}>Add to queue</button></li>
              <li><button role="menuitem" onClick={() => setPicking(true)}>Add to playlist ›</button></li>
              {playlistId && (
                <li>
                  <button
                    role="menuitem"
                    onClick={() => run(async () => {
                      await api.removeFromPlaylist(playlistId, (await resolveSong(song)).id);
                      onRemoved?.(song.id);
                      refreshPlaylists();
                    }, 'Removed from playlist')}
                  >
                    Remove from this playlist
                  </button>
                </li>
              )}
            </>
          ) : (
            <>
              <li className="menu-title">Choose playlist</li>
              {playlists.length === 0 && <li className="menu-title">No playlists yet</li>}
              {playlists.map((p) => (
                <li key={p.id}>
                  <button
                    role="menuitem"
                    onClick={() => run(async () => {
                      await api.addToPlaylist(p.id, (await resolveSong(song)).id);
                      refreshPlaylists();
                    }, `Added to ${p.name}`)}
                  >
                    {p.name}
                  </button>
                </li>
              ))}
            </>
          )}
        </ul>
      )}
    </div>
  );
}
