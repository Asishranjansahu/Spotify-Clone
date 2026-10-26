// Official YouTube IFrame Player API only; no audio is downloaded or proxied.
let apiPromise = null;

// The IFrame API script is injected exactly once for the whole app.
function loadIframeApi() {
  if (window.YT?.Player) return Promise.resolve(window.YT);
  if (!apiPromise) {
    apiPromise = new Promise((resolve) => {
      const prev = window.onYouTubeIframeAPIReady;
      window.onYouTubeIframeAPIReady = () => {
        prev?.();
        resolve(window.YT);
      };
      const tag = document.createElement('script');
      tag.src = 'https://www.youtube.com/iframe_api';
      tag.async = true;
      document.head.appendChild(tag);
    });
  }
  return apiPromise;
}

const POLL_MS = 500;
const STATE = { ENDED: 0, PLAYING: 1, PAUSED: 2 };

/** Same interface as the audio adapter; `host` is a React-owned element that stays mounted. */
export function createYouTubeAdapter(host) {
  const cbs = { ended: () => {}, error: () => {}, state: () => {}, time: () => {} };
  let player = null;
  let volume = 80;
  let muted = false;
  let timer = null;

  const startPolling = () => {
    if (timer) return;
    timer = setInterval(() => player && cbs.time(player.getCurrentTime() || 0), POLL_MS);
  };
  const stopPolling = () => {
    clearInterval(timer);
    timer = null;
  };

  const applyAudio = () => {
    if (!player) return;
    player.setVolume(volume);
    if (muted) player.mute();
    else player.unMute();
  };

  let ready = null;
  const ensure = (firstVideoId) => {
    if (!ready) {
      ready = loadIframeApi().then(
        (YT) =>
          new Promise((resolve) => {
            const mount = document.createElement('div');
            host.appendChild(mount);
            // eslint-disable-next-line no-new
            new YT.Player(mount, {
              width: '100%',
              height: '100%',
              videoId: firstVideoId,
              playerVars: { autoplay: 1, playsinline: 1, rel: 0, origin: window.location.origin },
              events: {
                onReady: (e) => {
                  player = e.target;
                  applyAudio();
                  resolve(player);
                },
                onStateChange: (e) => {
                  if (e.data === STATE.PLAYING) {
                    cbs.state(true);
                    startPolling();
                  } else if (e.data === STATE.PAUSED) {
                    cbs.state(false);
                    stopPolling();
                  } else if (e.data === STATE.ENDED) {
                    cbs.state(false);
                    stopPolling();
                    cbs.ended();
                  }
                },
                onError: (e) => {
                  stopPolling();
                  cbs.error(e.data);
                },
              },
            });
          }),
      );
      return ready;
    }
    return ready;
  };

  let firstLoad = true;
  return {
    load(song) {
      if (firstLoad) {
        firstLoad = false;
        ensure(song.externalId);
        return;
      }
      ensure().then((p) => {
        applyAudio();
        p.loadVideoById(song.externalId);
      });
    },
    play: () => player?.playVideo(),
    pause: () => player?.pauseVideo(),
    seek: (sec) => player?.seekTo(sec, true),
    setVolume: (v) => {
      volume = Math.round(v * 100);
      applyAudio();
    },
    mute: (m) => {
      muted = m;
      applyAudio();
    },
    getCurrentTime: () => (player ? player.getCurrentTime() || 0 : 0),
    getDuration: () => (player ? player.getDuration() || 0 : 0),
    onEnded: (cb) => (cbs.ended = cb),
    onError: (cb) => (cbs.error = cb),
    onState: (cb) => (cbs.state = cb),
    onTime: (cb) => (cbs.time = cb),
  };
}
