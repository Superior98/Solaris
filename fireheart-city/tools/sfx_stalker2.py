from sfx import *

s = load_sounds()
for k in ('stalker.step', 'stalker.heart', 'stalker.whisper', 'stalker.static', 'stalker.sting'):
    s.pop(k, None)
for i in range(3):
    d = 0.35
    tt = t(d)
    thud = np.sin(2 * np.pi * (70 - 30 * tt) * tt) * np.exp(-tt * 25)
    crunch = bp(noise(d), 300, 2500) * np.exp(-tt * (18 + i * 4)) * 0.6
    save('stalker/step%d' % i, norm(thud + crunch, 0.8), s, 'stalker.step')
d = 1.0
tt = t(d)
beat = np.zeros(len(tt))
for b0 in (0.0, 0.22):
    k = np.clip(tt - b0, 0, None)
    beat += np.sin(2 * np.pi * 48 * k) * np.exp(-k * 18) * (tt >= b0) * (1.0 if b0 == 0 else 0.7)
save('stalker/heart', norm(lp_fast(beat, 200), 0.9), s, 'stalker.heart')
d = 3.5
tt = t(d)
w = np.zeros(len(tt))
for _ in range(9):
    a = rng.uniform(0, d - 0.6)
    ln = rng.uniform(0.25, 0.6)
    m = (tt >= a) & (tt < a + ln)
    ph = (tt[m] - a) / ln
    seg = bp(noise(ln + 0.02)[:m.sum()], rng.uniform(1800, 2600), rng.uniform(4500, 7000)) * np.sin(np.pi * ph) ** 2
    w[m] += seg * rng.uniform(0.4, 1)
save('stalker/whisper', norm(w, 0.6), s, 'stalker.whisper', stream=True)
d = 1.2
tt = t(d)
st = noise(d) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 7 * tt)))
save('stalker/static', norm(hp_fast(st, 1000), 0.5), s, 'stalker.static')
d = 2.5
tt = t(d)
sting = sum(np.sin(2 * np.pi * f * tt) for f in (233, 247, 262, 277)) * np.exp(-tt * 1.2) * np.clip(tt / 0.02, 0, 1)
sting += np.sin(2 * np.pi * 35 * tt) * np.exp(-tt * 2) * 1.5
save('stalker/sting', norm(sting, 0.85), s, 'stalker.sting')
write_sounds(s)
print('ok')
