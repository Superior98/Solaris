from sfx import *

s = load_sounds()
for k in list(s):
    if k.startswith('stalker.'): del s[k]

d = 6.0
tt = t(d)
x = np.zeros(len(tt))
cycle = [(0.0, 1.5, 'in'), (1.7, 3.0, 'out'), (3.2, 4.6, 'in'), (4.8, 5.9, 'out')]
for a, b, kind in cycle:
    m = (tt >= a) & (tt < b)
    ph = (tt[m] - a) / (b - a)
    e = np.sin(np.pi * ph) ** (1.6 if kind == 'in' else 1.1)
    n = noise(b - a + 0.01)[:m.sum()]
    lo, hi = (500, 2600) if kind == 'in' else (220, 1500)
    seg = bp(n, lo, hi) * e
    rasp = np.sin(2 * np.pi * (48 if kind == 'out' else 62) * tt[m]) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 23 * tt[m]))) * e * 0.18
    wet = bp(noise(b - a + 0.01)[:m.sum()], 2500, 6000) * e ** 3 * 0.25
    x[m] += seg + rasp + wet
save('stalker/breath', norm(x, 0.8), s, 'stalker.breath', stream=True)

d = 0.5
tt = t(d)
clicks = np.zeros(len(tt))
for c0 in (0.0, 0.07, 0.11, 0.2, 0.26, 0.33):
    i = int(c0 * SR)
    burst = bp(noise(0.02), 1500, 7000) * np.exp(-np.arange(int(0.02 * SR)) / SR * 250)
    clicks[i:i + len(burst)] += burst * (1.0 - c0)
save('stalker/crack', norm(clicks, 0.9), s, 'stalker.crack')

d = 1.6
tt = t(d)
f0 = 380 + 900 * np.clip(tt / 0.25, 0, 1) - 300 * np.clip((tt - 0.25) / 1.3, 0, 1)
ph = 2 * np.pi * np.cumsum(f0) / SR
saw = sum(((k * ph / (2 * np.pi) + 0.37 * k) % 1 - 0.5) * (1 / k ** 0.6) for k in (1, 1.013, 0.497, 1.51, 2.02))
scream = np.tanh(saw * 3.5) * 0.7 + bp(noise(d), 800, 7000) * 0.6
env = np.clip(tt / 0.03, 0, 1) * np.exp(-np.clip(tt - 0.4, 0, None) * 3)
boom = np.sin(2 * np.pi * (60 - 30 * tt) * tt) * np.exp(-tt * 5) * 1.2
save('stalker/scream', norm(scream * env + boom, 0.98), s, 'stalker.scream')

d = 4.0
tt = t(d)
drone = sum(np.sin(2 * np.pi * f * tt + np.sin(2 * np.pi * 0.2 * tt) * k) for k, f in enumerate((41, 43.5, 82.2, 123.7))) * 0.25
drone += bp(noise(d), 60, 300) * 0.4
save('stalker/drone', norm(drone * env_fade(len(tt)) if False else drone, 0.5), s, 'stalker.drone', stream=True)

write_sounds(s)
print('ok')
