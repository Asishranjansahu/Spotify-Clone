import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { api, resolveSong } from '../api';
import { createAudioAdapter } from '../player/audioAdapter';
import { createYouTubeAdapter } from '../player/youtubeAdapter';

const PlayerContext = createContext(null);
export const usePlayer = () => useContext(PlayerContext);

const LISTEN_THRESHOLD_SEC = 30;
const YT_UNPLAYABLE = [100, 101, 150]; // removed/private, embedding disabled
const sourceOf = (song) => (song?.source === 'YOUTUBE' ? 'YOUTUBE' : 'LOCAL');

export function PlayerProvider({ children }) {
  const ytHostRef = useRef(null);
  const adapters = useRef({ LOCAL: null, YOUTUBE: null });
  const activeSource = useRef('LOCAL');
  const handlers = useRef({});
  const volumeRef = useRef(0);
  const mutedRef = useRef(false);
  const [queue, setQueue] = useState([]);
  const [index, setIndex] = useState(-1);
  const [nonce, setNonce] = useState(0); // bumps on every "start this track" request
  const [playing, setPlaying] = useState(false);
  const [time, setTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolumeState] = useState(() => Number(localStorage.getItem('volume') ?? 0.8));
  const [muted, setMuted] = useState(false);
  const [shuffle, setShuffle] = useState(false);
  const [repeat, setRepeat] = useState('off'); // off | all | one
  const [error, setError] = useState('');
  const [queueOpen, setQueueOpen] = useState(false);

  const historyStack = useRef([]); // previously played indexes, for "previous" while shuffling
  const listened = useRef({ sec: 0, last: 0, counted: false });
  const errorStreak = useRef(0);

  const current = index >= 0 ? queue[index] : null;

  useEffect(() => {
    document.body.classList.toggle('yt-on', current?.source === 'YOUTUBE');
  }, [current]);

  const start = useCallback((i) => {
    setIndex(i);
    setNonce((n) => n + 1);
    setError('');
  }, []);

  // Lazily create the adapter for a source; its events are ignored unless it is the active one.
  const getAdapter = (source) => {
    if (adapters.current[source]) return adapters.current[source];
    const ad = source === 'YOUTUBE' ? createYouTubeAdapter(ytHostRef.current) : createAudioAdapter();
    const live = (fn) => (...args) => activeSource.current === source && handlers.current[fn](...args);
    ad.onEnded(live('ended'));
    ad.onError(live('error'));
    ad.onState(live('state'));
    ad.onTime(live('time'));
    ad.setVolume(volumeRef.current);
    ad.mute(mutedRef.current);
    adapters.current[source] = ad;
    return ad;
  };
  const adapter = () => adapters.current[activeSource.current];

  // pick the adapter from the song's source and start it whenever a start is requested
  useEffect(() => {
    if (nonce === 0 || index < 0 || !queue[index]) return;
    const song = queue[index];
    const source = sourceOf(song);
    adapters.current[activeSource.current]?.pause();
    activeSource.current = source;
    listened.current = { sec: 0, last: 0, counted: false };
    getAdapter(source).load(song);
    resolveSong(song)
      .then((saved) => api.recordPlay(saved.id))
      .catch(() => {});
    setTime(0);
    setDuration(song.durationSec || 0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [nonce]);

  useEffect(() => {
    volumeRef.current = volume;
    Object.values(adapters.current).forEach((a) => a?.setVolume(volume));
    localStorage.setItem('volume', String(volume));
  }, [volume]);

  useEffect(() => {
    mutedRef.current = muted;
    Object.values(adapters.current).forEach((a) => a?.mute(muted));
  }, [muted]);

  const playList = useCallback(
    (songs, startIndex = 0) => {
      if (!songs.length) return;
      historyStack.current = [];
      errorStreak.current = 0;
      setQueue(songs);
      start(startIndex);
    },
    [start],
  );

  const pickNext = (auto) => {
    if (!queue.length) return -1;
    if (auto && repeat === 'one') return index;
    if (shuffle && queue.length > 1) {
      let n;
      do n = Math.floor(Math.random() * queue.length);
      while (n === index);
      return n;
    }
    if (index + 1 < queue.length) return index + 1;
    return repeat === 'all' ? 0 : -1;
  };

  const next = (auto = false) => {
    const n = pickNext(auto);
    if (n < 0) {
      adapter()?.pause();
      adapter()?.seek(0);
      setPlaying(false);
      return;
    }
    if (n !== index) historyStack.current.push(index);
    start(n);
  };

  const previous = () => {
    const ad = adapter();
    if (ad && ad.getCurrentTime() > 3) {
      seek(0);
      return;
    }
    if (shuffle && historyStack.current.length) {
      start(historyStack.current.pop());
    } else if (index > 0) {
      start(index - 1);
    } else if (repeat === 'all' && queue.length) {
      start(queue.length - 1);
    } else {
      seek(0);
    }
  };

  function handleError(code) {
    if (!current) return;
    setPlaying(false);
    const reason = YT_UNPLAYABLE.includes(code)
      ? `"${current.title}" is unavailable or can't be embedded.`
      : `Could not play "${current.title}".`;
    setError(`${reason}${queue.length > 1 ? ' Skipping to the next song…' : ''}`);
    errorStreak.current += 1;
    if (queue.length > 1 && errorStreak.current < queue.length) {
      const failedNonce = nonce;
      setTimeout(() => {
        if (failedNonce === nonceRef.current) next(true);
      }, 1500);
    }
  }
  const nonceRef = useRef(nonce);
  nonceRef.current = nonce;

  const togglePlay = () => {
    const ad = adapter();
    if (!ad || !current) return;
    if (playing) ad.pause();
    else ad.play();
  };

  const seek = (sec) => {
    const ad = adapter();
    if (!ad || !current) return;
    const max = ad.getDuration() || duration;
    const t = Math.max(0, Math.min(sec, max || sec));
    ad.seek(t);
    setTime(t);
    listened.current.last = t;
  };

  const setVolume = (v) => {
    const clamped = Math.max(0, Math.min(1, v));
    setVolumeState(clamped);
    if (clamped > 0) setMuted(false);
  };

  const playNext = (song) => {
    if (!queue.length) return playList([song]);
    setQueue((q) => {
      const copy = [...q];
      copy.splice(index + 1, 0, song);
      return copy;
    });
  };

  const addToQueue = (song) => {
    if (!queue.length) return playList([song]);
    setQueue((q) => [...q, song]);
  };

  const removeFromQueue = (i) => {
    if (i === index) return;
    setQueue((q) => q.filter((_, k) => k !== i));
    if (i < index) setIndex((x) => x - 1);
    historyStack.current = historyStack.current.filter((k) => k !== i).map((k) => (k > i ? k - 1 : k));
  };

  const playFromQueue = (i) => start(i);

  // keyboard shortcuts
  useEffect(() => {
    const onKey = (e) => {
      const tag = e.target.tagName;
      if (['INPUT', 'TEXTAREA', 'SELECT', 'BUTTON', 'A'].includes(tag) || e.target.isContentEditable) return;
      if (e.metaKey || e.ctrlKey || e.altKey) return;
      if (e.code === 'Space') {
        e.preventDefault();
        togglePlay();
      } else if (e.code === 'ArrowRight') {
        e.preventDefault();
        seek((adapter()?.getCurrentTime() || 0) + 5);
      } else if (e.code === 'ArrowLeft') {
        e.preventDefault();
        seek((adapter()?.getCurrentTime() || 0) - 5);
      } else if (e.code === 'ArrowUp') {
        e.preventDefault();
        setVolume(volume + 0.05);
      } else if (e.code === 'ArrowDown') {
        e.preventDefault();
        setVolume(volume - 0.05);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  });

  const onTime = (t) => {
    setTime(t);
    const d = adapter()?.getDuration();
    if (d) setDuration(d);
    const l = listened.current;
    const delta = t - l.last;
    if (delta > 0 && delta < 1.5) l.sec += delta; // only count natural playback, not seeks
    l.last = t;
    if (!l.counted && l.sec >= LISTEN_THRESHOLD_SEC && current) {
      l.counted = true;
      resolveSong(current)
        .then((saved) => api.played(saved.id))
        .catch(() => {});
    }
  };

  handlers.current = {
    ended: () => next(true),
    error: handleError,
    time: onTime,
    state: (isPlaying) => {
      setPlaying(isPlaying);
      if (isPlaying) {
        errorStreak.current = 0;
        setError('');
      }
    },
  };

  const value = {
    queue, index, current, playing, time, duration, volume, muted, shuffle, repeat, error, queueOpen,
    playList, togglePlay, next: () => next(false), previous, seek, setVolume,
    toggleMute: () => setMuted((m) => !m),
    toggleShuffle: () => setShuffle((s) => !s),
    cycleRepeat: () => setRepeat((r) => (r === 'off' ? 'all' : r === 'all' ? 'one' : 'off')),
    playNext, addToQueue, removeFromQueue, playFromQueue,
    toggleQueue: () => setQueueOpen((o) => !o),
  };

  return (
    <PlayerContext.Provider value={value}>
      {children}
      <div className={`yt-box${current?.source === 'YOUTUBE' ? '' : ' yt-box-off'}`} aria-label="YouTube video player">
        <div ref={ytHostRef} className="yt-host" />
        <div className="yt-credit">Powered by YouTube</div>
      </div>
    </PlayerContext.Provider>
  );
}
