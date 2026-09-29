from sfx import *

s = load_sounds()
for k in list(s):
    if k.startswith('police.'): del s[k]

for i in range(3):
    d = 0.55
    n = noise(d)
    crack = n * env(len(n), 0.0005, 0.5) * np.exp(-t(d) * (38 + i * 6))
    body = bp(n, 90, 900) * np.exp(-t(d) * 14) * 1.4
    thump = np.sin(2 * np.pi * (70 - 30 * t(d)) * t(d)) * np.exp(-t(d) * 20) * 0.9
    tail = lp_fast(noise(d), 500) * np.exp(-t(d) * 6) * 0.35
    save('police/shot%d' % i, norm(hp_fast(crack, 700) * 1.2 + body + thump + tail), s, 'police.shot')

d = 1.4
tt = t(d)
charge = np.sin(2 * np.pi * (200 + 1400 * np.clip(tt / 0.6, 0, 1) ** 2) * tt) * np.clip(tt / 0.6, 0, 1) * (tt < 0.62)
blast_t = np.clip(tt - 0.62, 0, None)
blast = (np.sin(2 * np.pi * 55 * blast_t) * 1.2 + bp(noise(d), 200, 6000) * 0.8 + np.sin(2 * np.pi * 1800 * blast_t) * 0.3 * np.exp(-blast_t * 9)) * np.exp(-blast_t * 3.2) * (tt >= 0.62)
save('police/rail', norm(charge * 0.5 + blast), s, 'police.rail')

d = 3.2
tt = t(d)
riser = sum(np.sin(2 * np.pi * f * (1 + tt ** 2 * 0.9) * tt) for f in (110, 165, 220, 330)) * (tt / d) ** 1.6
shimmer = bp(noise(d), 3000, 9000) * (tt / d) ** 2 * 0.6
hit_t = np.clip(tt - 2.6, 0, None)
boom = (np.sin(2 * np.pi * 45 * hit_t) * 1.5 + lp_fast(noise(d), 900) * 1.2) * np.exp(-hit_t * 4) * (tt >= 2.6)
save('police/ult', norm(riser * 0.35 + shimmer + boom), s, 'police.ult', stream=True)

d = 2.6
tt = t(d)
whoosh = bp(noise(d), 300, 4000) * np.sin(np.pi * np.clip(tt / 1.2, 0, 1)) * (tt < 1.2) * 0.6
ht = np.clip(tt - 1.2, 0, None)
impact = (np.sin(2 * np.pi * (60 - 25 * ht) * ht) * 1.8 + lp_fast(noise(d), 1500) * 1.4 + np.sin(2 * np.pi * 440 * ht) * 0.25 * np.exp(-ht * 2)) * np.exp(-ht * 2.2) * (tt >= 1.2)
save('police/finisher', norm(whoosh + impact), s, 'police.finisher', stream=True)

d = 1.1
tt = t(d)
f = 700 + 500 * (0.5 + 0.5 * np.sin(2 * np.pi * 6 * tt))
chirp = np.sign(np.sin(2 * np.pi * np.cumsum(f) / SR)) * 0.25 * env(len(tt), 0.01, 0.2)
static = bp(noise(d), 1500, 5000) * 0.15 * ((tt < 0.12) | (tt > 0.95))
save('police/radio', norm(lp_fast(chirp, 3500) + static, 0.7), s, 'police.radio')

write_sounds(s)
print('ok', [k for k in s if k.startswith('police')])
