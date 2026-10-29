"""Real Android smoke test. Requires a booted adb device and an installed debug APK."""
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = 'ru.absurd.carrotcourt'
OUT = Path('smoke-results')
OUT.mkdir(exist_ok=True)

def adb(*args):
    return subprocess.check_output(['adb', *args], timeout=45).decode('utf-8')

def screen():
    adb('shell', 'uiautomator', 'dump', '/sdcard/court.xml')
    return ET.fromstring(adb('shell', 'cat', '/sdcard/court.xml'))

def tap(text, scroll=False):
    if scroll:
        for _ in range(3):
            adb('shell', 'input', 'swipe', '350', '420', '350', '1250', '180')
        time.sleep(.3)
    for _ in range(10 if scroll else 1):
        root = screen()
        for node in root.iter('node'):
            if node.get('text', '').replace('▪ ', '') == text:
                x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
                if x2 > x1 and y2 > y1:
                    adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))
                    time.sleep(.8)
                    return
        if scroll:
            adb('shell', 'input', 'swipe', '350', '1100', '350', '420', '350')
            time.sleep(.5)
    raise AssertionError('Visible button not found: ' + text)

def state():
    raw = adb('shell', 'run-as', PACKAGE, 'cat', 'shared_prefs/carrot_court_v1.xml')
    return json.loads(ET.fromstring(raw).find("string[@name='state']").text)

def screenshot(name):
    with (OUT / name).open('wb') as f:
        subprocess.run(['adb', 'exec-out', 'screencap', '-p'], stdout=f, timeout=30, check=True)

def launch():
    output = adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
    assert 'Status: ok' in output, output
    time.sleep(3)
    assert adb('shell', 'pidof', PACKAGE).strip(), 'Application process missing'

try:
    adb('logcat', '-c')
    adb('shell', 'wm', 'size', '720x1520')
    adb('shell', 'wm', 'density', '300')
    adb('shell', 'settings', 'put', 'global', 'window_animation_scale', '0')
    adb('shell', 'settings', 'put', 'global', 'transition_animation_scale', '0')
    adb('shell', 'settings', 'put', 'global', 'animator_duration_scale', '0')
    adb('shell', 'svc', 'wifi', 'disable')
    adb('shell', 'svc', 'data', 'disable')
    launch()
    assert 'ПОЛОВНИКЕ' in ET.tostring(screen(), encoding='unicode')
    screenshot('01-story.png')
    for label in ('Версия обвинения', 'Доводы защиты', 'Допросить свидетеля'):
        tap(label, scroll=True)
    assert set(state()['story']['seen']) == {'charge', 'defense', 'witness'}
    tap('Оправдать Морковку', scroll=True)
    assert state()['story']['decisions'] == [0]
    tap('Следующая глава', scroll=True)
    assert state()['story']['chapter'] == 1
    screenshot('02-next-chapter.png')

    tap('СУД')
    tap('Обвинить', scroll=True)
    assert state()['speaker'] == 'Прокурор Брокколи'
    tap('Защитить', scroll=True)
    assert state()['speaker'] == 'Адвокат Майонез'
    tap('Допросить свидетеля', scroll=True)
    assert state()['speaker'].startswith('Свидетель:')
    tap('Вынести приговор', scroll=True)
    saved = state()
    assert saved['completed'] == 1 and saved['closed']
    assert saved['guilty'] + saved['acquitted'] == 1
    tap('АРХИВ')
    screenshot('03-archive.png')
    assert 'Рассмотрено дел' in ET.tostring(screen(), encoding='unicode')
    tap('АБСУРД')
    tap('Достижения')
    screenshot('04-achievements.png')

    adb('shell', 'am', 'force-stop', PACKAGE)
    launch()
    restored = state()
    assert restored['completed'] == 1
    assert restored['story']['chapter'] == 1
    assert restored['story']['decisions'] == [0]
    assert 'Посылка без адресата' in ET.tostring(screen(), encoding='unicode')
    assert 'FATAL EXCEPTION' not in adb('logcat', '-d', '-s', 'AndroidRuntime:E')
    (OUT / 'result.txt').write_text('PASS: offline launch, story investigation, decision, next chapter, free court, archive, achievements and restart persistence.\n')
    print((OUT / 'result.txt').read_text())
finally:
    (OUT / 'logcat.txt').write_text(adb('logcat', '-d'))
    screenshot('last-screen.png')
