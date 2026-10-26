import { streamUrl } from '../api';

/**
 * Player interface implemented by every adapter:
 *   load(song)  start playing a song
 *   play(), pause(), seek(sec), setVolume(0..1), mute(bool)
 *   getCurrentTime(), getDuration()
 *   onEnded(cb), onError(cb(code)), onState(cb(playing)), onTime(cb(sec))
 */
export function createAudioAdapter() {
  const el = document.createElement('audio');
  el.preload = 'auto';
  const cbs = { ended: () => {}, error: () => {}, state: () => {}, time: () => {} };

  el.addEventListener('play', () => cbs.state(true));
  el.addEventListener('playing', () => cbs.state(true));
  el.addEventListener('pause', () => cbs.state(false));
  el.addEventListener('timeupdate', () => cbs.time(el.currentTime));
  el.addEventListener('loadedmetadata', () => cbs.time(el.currentTime));
  el.addEventListener('ended', () => cbs.ended());
  el.addEventListener('error', () => cbs.error('audio'));

  return {
    load(song) {
      el.src = streamUrl(song.id);
      el.play().catch((e) => {
        if (e.name !== 'AbortError') cbs.error('audio');
      });
    },
    play: () => el.play().catch(() => {}),
    pause: () => el.pause(),
    seek: (sec) => {
      el.currentTime = sec;
    },
    setVolume: (v) => {
      el.volume = v;
    },
    mute: (m) => {
      el.muted = m;
    },
    getCurrentTime: () => el.currentTime || 0,
    getDuration: () => (isFinite(el.duration) ? el.duration : 0),
    onEnded: (cb) => (cbs.ended = cb),
    onError: (cb) => (cbs.error = cb),
    onState: (cb) => (cbs.state = cb),
    onTime: (cb) => (cbs.time = cb),
  };
}
