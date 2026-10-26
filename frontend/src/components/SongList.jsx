import { Link } from 'react-router-dom';
import { assetUrl, songKey } from '../api';
import { useLibrary } from '../context/LibraryContext';
import { usePlayer } from '../context/PlayerContext';
import { fmtTime } from '../utils';
import SongMenu from './SongMenu';

// Clicking play starts playback and makes the whole list the queue (FR-22).
export default function SongList({ songs, playlistId, onRemoved }) {
  const player = usePlayer();
  const { isLiked, toggleLike } = useLibrary();

  if (!songs.length) return <p className="sub">No songs here yet.</p>;

  return (
    <ul className="songs">
      {songs.map((s, i) => {
        const isCurrent = !!player.current && songKey(player.current) === songKey(s);
        const liked = isLiked(s);
        return (
          <li key={`${songKey(s)}-${i}`} className={isCurrent ? 'song active' : 'song'}>
            <button
              className="row-play"
              aria-label={isCurrent && player.playing ? `Pause ${s.title}` : `Play ${s.title}`}
              onClick={() => (isCurrent ? player.togglePlay() : player.playList(songs, i))}
            >
              {isCurrent && player.playing ? '⏸' : '▶'}
            </button>
            {s.coverUrl ? <img className="cover xs" src={assetUrl(s.coverUrl)} alt="" /> : <div className="cover xs placeholder">♪</div>}
            <div className="meta">
              <div className="title">{s.title}</div>
              <div className="sub">
                {s.artistId ? <Link to={`/artist/${s.artistId}`}>{s.artistName}</Link> : s.artistName}
                {s.albumId && <> · <Link to={`/album/${s.albumId}`}>{s.albumTitle}</Link></>}
              </div>
            </div>
            <button
              className={`icon-btn ${liked ? 'on' : ''}`}
              aria-label={liked ? `Unlike ${s.title}` : `Like ${s.title}`}
              aria-pressed={liked}
              onClick={() => toggleLike(s)}
            >
              {liked ? '♥' : '♡'}
            </button>
            <span className="time">{fmtTime(s.durationSec)}</span>
            <SongMenu song={s} playlistId={playlistId} onRemoved={onRemoved} />
          </li>
        );
      })}
    </ul>
  );
}
