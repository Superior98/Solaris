from sfx import *

s = load_sounds()
for k in list(s):
    if k.startswith('kitchen.'): del s[k]
d = 2.5
tt = t(d)
base = hp_fast(noise(d), 2500) * 0.25
crackle = np.zeros(len(tt))
for _ in range(900):
    i = rng.integers(0, len(tt) - 400)
    n = int(rng.integers(40, 300))
    crackle[i:i + n] += hp_fast(noise(n / SR + 0.01)[:n], 1500) * np.exp(-np.arange(n) / (n / 4)) * rng.uniform(0.3, 1.0)
x = (base + crackle * 0.5) * env(len(tt), 0.1, 0.4)
save('kitchen/sizzle', norm(x, 0.7), s, 'kitchen.sizzle')
d = 1.2
tt = t(d)
x = np.zeros(len(tt))
for c0 in (0.05, 0.3, 0.55, 0.8):
    i = int(c0 * SR)
    n = int(0.08 * SR)
    thunk = (np.sin(2 * np.pi * 180 * np.arange(n) / SR) * 0.8 + bp(noise(0.09)[:n], 800, 5000) * 0.6) * np.exp(-np.arange(n) / SR * 60)
    x[i:i + n] += thunk
save('kitchen/chop', norm(x, 0.8), s, 'kitchen.chop')
d = 2.0
tt = t(d)
x = np.zeros(len(tt))
for _ in range(40):
    i = int(rng.integers(0, len(tt) - 3000))
    n = 2500
    f0 = rng.uniform(250, 700)
    k = np.arange(n) / SR
    x[i:i + n] += np.sin(2 * np.pi * (f0 + 900 * k) * k) * np.exp(-k * 30) * rng.uniform(0.3, 1)
x += lp_fast(noise(d), 400) * 0.3
save('kitchen/bubble', norm(x * env(len(tt), 0.1, 0.3), 0.6), s, 'kitchen.bubble')
write_sounds(s)
print('ok')
