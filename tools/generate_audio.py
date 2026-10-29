"""Generate original seamless offline chiptune and courtroom ambience. Python stdlib only."""
from pathlib import Path
import math
import random
import wave
from array import array

RATE = 22050
DURATION = 16
OUT = Path(__file__).resolve().parents[1] / 'app/src/main/res/raw'
OUT.mkdir(parents=True, exist_ok=True)

def write(name, values):
    peak = max(abs(x) for x in values) or 1
    pcm = array('h', (int(x / max(peak, 1) * 23000) for x in values))
    import sys
    if sys.byteorder != 'little':
        pcm.byteswap()
    with wave.open(str(OUT / name), 'wb') as wav:
        wav.setparams((1, 2, RATE, 0, 'NONE', 'not compressed'))
        wav.writeframes(pcm.tobytes())

music = [0.0] * (RATE * DURATION)

def note(start, duration, midi, volume, bass=False):
    frequency = 440 * 2 ** ((midi - 69) / 12)
    for i in range(int(duration * RATE)):
        t = i / RATE
        # Band-limited, soft pulse: three harmonics, no sharp digital clicks.
        wave_value = math.sin(2 * math.pi * frequency * t)
        if not bass:
            wave_value += .25 * math.sin(6 * math.pi * frequency * t) + .12 * math.sin(10 * math.pi * frequency * t)
        envelope = min(1, t / .018) * min(1, (duration - t) / .09)
        index = int(start * RATE) + i
        if index < len(music):
            music[index] += wave_value * volume * envelope

# A curious minor-key courtroom waltz-like motif, resolving back to its first bar.
melodies = [[69, 72, 76, 72, 71, 69, 64, 67], [65, 69, 72, 69, 67, 65, 64, 60],
            [67, 71, 74, 71, 72, 74, 71, 67], [64, 68, 71, 76, 74, 71, 68, 64]]
for bar in range(8):
    melody = melodies[bar % 4]
    for beat, pitch in enumerate(melody):
        note(bar * 2 + beat * .25, .19, pitch + (12 if bar >= 4 and beat % 3 == 0 else 0), .16)
    root = [45, 41, 43, 40][bar % 4]
    for beat in range(4):
        note(bar * 2 + beat * .5, .35, root if beat % 2 == 0 else root + 7, .22, bass=True)
write('court_music.wav', music)

rng = random.Random(814)
ambience = [0.0] * len(music)
filtered = 0.0
for i in range(len(ambience)):
    t = i / RATE
    filtered = filtered * .985 + rng.uniform(-1, 1) * .015
    edge = min(1, t / .4, (DURATION - t) / .4)
    ambience[i] = (filtered * .24 + math.sin(2 * math.pi * 110 * t) * .008) * edge
for start in [1.2, 4.7, 8.1, 12.8]:
    for i in range(int(.4 * RATE)):
        t = i / RATE
        envelope = math.sin(math.pi * t / .4) ** 2
        ambience[int(start * RATE) + i] += rng.uniform(-1, 1) * .024 * envelope
for start in [3.3, 9.5, 14.2]:
    for i in range(int(.16 * RATE)):
        t = i / RATE
        ambience[int(start * RATE) + i] += math.sin(2 * math.pi * 260 * t) * math.exp(-t * 35) * .1 * min(1, t / .003)
write('court_ambience.wav', ambience)
print('Generated two 16-second mono PCM loops:', OUT)
