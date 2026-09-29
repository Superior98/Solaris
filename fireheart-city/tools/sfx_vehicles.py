from sfx import *

s = load_sounds()
for k in list(s):
    if k.startswith('vehicle.'): del s[k]


def engine(f0, dur, rasp, noise_amt, name):
    n = int(SR * dur)
    tt = np.arange(n) / SR
    cyc = int(round(f0 * dur))
    f = cyc / dur
    x = np.zeros(n)
    for k, a in ((1, 1.0), (2, 0.6), (3, 0.35), (4, 0.25), (6, 0.12), (8, 0.06)):
        x += a * np.sin(2 * np.pi * f * k * tt + k * 0.7)
    fire = (np.sin(2 * np.pi * f * tt) > 0.2).astype(float)
    x += rasp * (fire - fire.mean())
    nz = rng.standard_normal(n)
    nz = lp_fast(nz, 1200)
    x += noise_amt * nz * (0.6 + 0.4 * fire)
    x = lp_fast(x, 3000)
    fade = int(SR * 0.01)
    x[:fade] = x[:fade] * np.linspace(0, 1, fade) + x[-fade:] * np.linspace(1, 0, fade)
    save('vehicle/' + name, norm(x, 0.6), s, 'vehicle.' + name)


engine(46, 2.0, 0.25, 0.25, 'engine_car')
engine(72, 2.0, 0.55, 0.35, 'engine_bike')
engine(58, 2.0, 0.35, 0.6, 'engine_boat')
d = 0.9
tt = t(d)
horn = (np.sign(np.sin(2 * np.pi * 415 * tt)) * 0.5 + np.sign(np.sin(2 * np.pi * 523 * tt)) * 0.5) * env(len(tt), 0.01, 0.08)
save('vehicle/horn_car', norm(lp_fast(horn, 2500), 0.7), s, 'vehicle.horn_car')
d = 0.5
tt = t(d)
beep = np.sign(np.sin(2 * np.pi * 740 * tt)) * env(len(tt), 0.005, 0.05)
save('vehicle/horn_bike', norm(lp_fast(beep, 3000), 0.6), s, 'vehicle.horn_bike')
d = 1.6
tt = t(d)
crank = np.sin(2 * np.pi * 18 * tt) * bp(noise(d), 200, 1500) * (tt < 0.8)
catch = np.clip(tt - 0.8, 0, None)
roar = (np.sin(2 * np.pi * (40 + 60 * np.exp(-catch * 3)) * catch) + 0.4 * np.sin(2 * np.pi * (80 + 120 * np.exp(-catch * 3)) * catch)) * (tt >= 0.8) * np.exp(-catch * 1.2)
save('vehicle/start', norm(crank * 0.6 + roar, 0.7), s, 'vehicle.start')
d = 0.4
tt = t(d)
screech = bp(noise(d), 2000, 6000) * np.sin(np.pi * tt / d) + np.sin(2 * np.pi * 2600 * tt) * 0.2 * np.sin(np.pi * tt / d)
save('vehicle/skid', norm(screech, 0.5), s, 'vehicle.skid')
d = 2.0
tt = t(d)
sq = bp(noise(d), 1800, 5200)
tone = np.sin(2 * np.pi * (2300 + 120 * np.sin(2 * np.pi * 3 * tt)) * tt) * 0.35 + np.sin(2 * np.pi * (3100 + 90 * np.sin(2 * np.pi * 2.2 * tt)) * tt) * 0.18
x = sq * 0.6 + tone
fade = int(SR * 0.02)
x[:fade] = x[:fade] * np.linspace(0, 1, fade) + x[-fade:] * np.linspace(1, 0, fade)
save('vehicle/skid_loop', norm(x, 0.5), s, 'vehicle.skid_loop')
d = 0.9
tt = t(d)
thud = np.sin(2 * np.pi * (70 * np.exp(-tt * 3)) * tt) * np.exp(-tt * 7)
crunch = bp(noise(d), 400, 5000) * np.exp(-tt * 9)
glass = bp(noise(d), 4000, 9000) * np.exp(-np.clip(tt - 0.05, 0, None) * 5) * (tt > 0.05) * 0.5
tink = sum(np.sin(2 * np.pi * f * tt) * np.exp(-np.clip(tt - o, 0, None) * 18) * (tt > o) for f, o in ((3200, 0.08), (4100, 0.14), (2700, 0.22), (3600, 0.31))) * 0.15
save('vehicle/crash', norm(thud * 1.2 + crunch + glass + tink, 0.85), s, 'vehicle.crash')
d = 0.35
tt = t(d)
pop = np.zeros(len(tt))
for o in (0.0, 0.09, 0.16):
    m = tt >= o
    q = tt[m] - o
    pop[m] += bp(rng.standard_normal(m.sum()), 80, 2200) * np.exp(-q * 45) + np.sin(2 * np.pi * 90 * q) * np.exp(-q * 30)
save('vehicle/backfire', norm(pop, 0.75), s, 'vehicle.backfire')
d = 0.25
tt = t(d)
sh = bp(noise(d), 300, 2500) * np.exp(-tt * 14) * 0.6 + np.sin(2 * np.pi * 180 * tt) * np.exp(-tt * 25) * 0.4
save('vehicle/shift', norm(sh, 0.45), s, 'vehicle.shift')
d = 0.08
tt = t(d)
clk = bp(noise(d), 1500, 6000) * np.exp(-tt * 90) + np.sin(2 * np.pi * 1900 * tt) * np.exp(-tt * 120) * 0.5
save('vehicle/indicator', norm(clk, 0.35), s, 'vehicle.indicator')
d = 0.5
tt = t(d)
land = np.sin(2 * np.pi * (55 * np.exp(-tt * 2)) * tt) * np.exp(-tt * 9) + bp(noise(d), 100, 900) * np.exp(-tt * 14) * 0.5 + sum(np.sin(2 * np.pi * f * tt) * np.exp(-tt * 12) for f in (310, 460)) * 0.1
save('vehicle/land', norm(land, 0.7), s, 'vehicle.land')
write_sounds(s)
print('ok')
