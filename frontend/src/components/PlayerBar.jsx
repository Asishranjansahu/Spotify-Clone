import { Link } from 'react-router-dom';
import { assetUrl } from '../api';
import { usePlayer } from '../context/PlayerContext';
import { useLibrary } from '../context/LibraryContext';
import { fmtTime } from '../utils';

export default function PlayerBar() {
  const p = usePlayer();
  const { isLiked, toggleLike } = useLibrary();
  const song = p.current;
  const max = p.duration || 0;

  return (
    <footer className="player" aria-label="Player">
      {p.error && <div className="player-error" role="alert">{p.error}</div>}
      <div className="player-song">
        {song ? (
          <>
            {song.coverUrl ? (
              <img className="cover sm" src={assetUrl(song.coverUrl)} alt="" />
            ) : (
              <div className="cover sm placeholder">♪</div>
            )}
            <div className="meta">
              <div className="title">{song.title}</div>
              {song.artistId ? (
                <Link className="sub" to={`/artist/${song.artistId}`}>{song.artistName}</Link>
              ) : (
                <span className="sub">{song.artistName}</span>
              )}
            </div>
            <button
              className={`icon-btn ${isLiked(song) ? 'on' : ''}`}
              aria-label={isLiked(song) ? 'Unlike song' : 'Like song'}
              aria-pressed={isLiked(song)}
              onClick={() => toggleLike(song)}
            >
              {isLiked(song) ? '♥' : '♡'}
            </button>
          </>
        ) : (
          <div className="sub">Pick a song to start listening</div>
        )}
      </div>

      <div className="player-center">
        <div className="controls">
          <button className={`icon-btn ${p.shuffle ? 'on' : ''}`} aria-label="Shuffle" aria-pressed={p.shuffle} onClick={p.toggleShuffle}>⤮</button>
          <button className="icon-btn" aria-label="Previous song" onClick={p.previous} disabled={!song}>⏮</button>
          <button className="play-btn" aria-label={p.playing ? 'Pause' : 'Play'} onClick={p.togglePlay} disabled={!song}>
            {p.playing ? '⏸' : '▶'}
          </button>
          <button className="icon-btn" aria-label="Next song" onClick={p.next} disabled={!song}>⏭</button>
          <button
            className={`icon-btn ${p.repeat !== 'off' ? 'on' : ''}`}
            aria-label={`Repeat: ${p.repeat}`}
            onClick={p.cycleRepeat}
          >
            {p.repeat === 'one' ? '🔂' : '🔁'}
          </button>
        </div>
        <div className="progress">
          <span className="time">{fmtTime(p.time)}</span>
          <input
            type="range"
            min={0}
            max={max || 1}
            step={0.1}
            value={Math.min(p.time, max || 1)}
            disabled={!song}
            aria-label="Seek"
            aria-valuetext={`${fmtTime(p.time)} of ${fmtTime(max)}`}
            onChange={(e) => p.seek(Number(e.target.value))}
            style={{ '--pct': `${max ? (p.time / max) * 100 : 0}%` }}
          />
          <span className="time">{fmtTime(max)}</span>
        </div>
      </div>

      <div className="player-right">
        <button className="icon-btn" aria-label="Show queue" onClick={p.toggleQueue}>☰</button>
        <button className="icon-btn" aria-label={p.muted ? 'Unmute' : 'Mute'} aria-pressed={p.muted} onClick={p.toggleMute}>
          {p.muted || p.volume === 0 ? '🔇' : p.volume < 0.5 ? '🔉' : '🔊'}
        </button>
        <input
          type="range"
          min={0}
          max={1}
          step={0.01}
          value={p.muted ? 0 : p.volume}
          aria-label="Volume"
          onChange={(e) => p.setVolume(Number(e.target.value))}
          style={{ '--pct': `${(p.muted ? 0 : p.volume) * 100}%` }}
        />
      </div>
    </footer>
  );
}
