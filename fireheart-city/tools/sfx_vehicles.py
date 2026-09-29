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
write_sounds(s)
print('ok')
