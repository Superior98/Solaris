import json
import os
import subprocess
import sys
import numpy as np

SR = 44100
ROOT = os.path.join(os.path.dirname(__file__), '..', 'res', 'assets', 'fireheartcity')
rng = np.random.default_rng(7)


def t(d):
    return np.arange(int(SR * d)) / SR


def env(n, a=0.005, r=0.2, sustain=1.0):
    x = np.ones(n) * sustain
    na, nr = int(SR * a), int(SR * r)
    if na: x[:na] = np.linspace(0, sustain, na)
    if nr: x[-nr:] *= np.linspace(1, 0, nr)
    return x


def lowpass(x, cutoff):
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = np.zeros_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc = (1 - a) * x[i] + a * acc
        y[i] = acc
    return y


def lp_fast(x, cutoff):
    from numpy.fft import rfft, irfft, rfftfreq
    f = rfftfreq(len(x), 1 / SR)
    X = rfft(x)
    X *= 1 / np.sqrt(1 + (f / cutoff) ** 4)
    return irfft(X, len(x))


def hp_fast(x, cutoff):
    from numpy.fft import rfft, irfft, rfftfreq
    f = rfftfreq(len(x), 1 / SR)
    X = rfft(x)
    X *= 1 / np.sqrt(1 + (cutoff / np.maximum(f, 1)) ** 4)
    return irfft(X, len(x))


def bp(x, lo, hi):
    return hp_fast(lp_fast(x, hi), lo)


def noise(d):
    return rng.standard_normal(int(SR * d))


def norm(x, peak=0.9):
    m = np.max(np.abs(x)) or 1
    return x / m * peak


def save(name, x, sounds, key, stream=False, variants=None):
    out = os.path.join(ROOT, 'sounds', *name.split('/')) + '.ogg'
    os.makedirs(os.path.dirname(out), exist_ok=True)
    wav = out[:-4] + '.wav'
    pcm = (np.clip(x, -1, 1) * 32767).astype('<i2')
    import wave
    with wave.open(wav, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '5', out], check=True)
    os.remove(wav)
    entry = sounds.setdefault(key, {'sounds': []})
    s = {'name': 'fireheartcity:' + name}
    if stream: s['stream'] = True
    entry['sounds'].append(s)


def load_sounds():
    p = os.path.join(ROOT, 'sounds.json')
    return json.load(open(p)) if os.path.exists(p) else {}


def write_sounds(s):
    json.dump(s, open(os.path.join(ROOT, 'sounds.json'), 'w'), indent=1)
