import urllib.request
import json
import sqlite3
import os

print('Downloading Bengali Quran...')
bn_req = urllib.request.Request('https://api.alquran.cloud/v1/quran/bn.bengali', headers={'User-Agent': 'Mozilla/5.0'})
bn_data = json.loads(urllib.request.urlopen(bn_req).read().decode('utf-8'))['data']

print('Downloading Arabic Quran...')
ar_req = urllib.request.Request('https://api.alquran.cloud/v1/quran/quran-uthmani', headers={'User-Agent': 'Mozilla/5.0'})
ar_data = json.loads(urllib.request.urlopen(ar_req).read().decode('utf-8'))['data']

print('Downloading English Quran...')
en_req = urllib.request.Request('https://api.alquran.cloud/v1/quran/en.sahih', headers={'User-Agent': 'Mozilla/5.0'})
en_data = json.loads(urllib.request.urlopen(en_req).read().decode('utf-8'))['data']

# Handle DB paths safely whether run in app project root or current directory
db_dir = 'app/src/main/assets/databases'
if not os.path.exists('app'):
    db_dir = '.'

os.makedirs(db_dir, exist_ok=True)

db_path = os.path.join(db_dir, 'quran.db')
tmp_db_path = os.path.join(db_dir, 'quran_tmp.db')

if os.path.exists(tmp_db_path):
    os.remove(tmp_db_path)

conn = sqlite3.connect(tmp_db_path)
c = conn.cursor()

c.execute("""
CREATE TABLE IF NOT EXISTS surah (
    number INTEGER NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    englishName TEXT NOT NULL,
    englishNameTranslation TEXT NOT NULL,
    numberOfAyahs INTEGER NOT NULL,
    revelationType TEXT NOT NULL
)
""")

c.execute("""
CREATE TABLE IF NOT EXISTS ayah (
    globalNumber INTEGER NOT NULL PRIMARY KEY,
    surahNumber INTEGER NOT NULL,
    numberInSurah INTEGER NOT NULL,
    juz INTEGER NOT NULL,
    page INTEGER NOT NULL,
    arabicText TEXT NOT NULL,
    englishText TEXT NOT NULL,
    bengaliText TEXT NOT NULL
)
""")

c.execute("""
CREATE TABLE IF NOT EXISTS room_master_table (
    id INTEGER PRIMARY KEY,
    identity_hash TEXT
)
""")

c.execute("""
INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, '40cac304f83466555049be1dd8630e8a')
""")

print('Inserting Surahs and Ayahs...')

for s_idx, surah in enumerate(bn_data['surahs']):
    surah_num = surah['number']
    s_ar = ar_data['surahs'][s_idx]
    s_en = en_data['surahs'][s_idx]
    
    num_ayahs = surah.get('numberOfAyahs', len(surah['ayahs']))
    
    c.execute("""
    INSERT INTO surah (number, name, englishName, englishNameTranslation, numberOfAyahs, revelationType)
    VALUES (?, ?, ?, ?, ?, ?)
    """, (
        surah_num,
        s_ar['name'],
        surah['englishName'],
        surah['englishNameTranslation'],
        num_ayahs,
        surah['revelationType']
    ))
    
    for a_idx, ayah_bn in enumerate(surah['ayahs']):
        ayah_ar = s_ar['ayahs'][a_idx]
        ayah_en = s_en['ayahs'][a_idx]
        
        c.execute("""
        INSERT INTO ayah (globalNumber, surahNumber, numberInSurah, juz, page, arabicText, englishText, bengaliText)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            ayah_bn['number'],
            surah_num,
            ayah_bn['numberInSurah'],
            ayah_bn['juz'],
            ayah_bn['page'],
            ayah_ar['text'],
            ayah_en['text'],
            ayah_bn['text']
        ))

conn.commit()
conn.close()

if os.path.exists(db_path):
    os.remove(db_path)
os.rename(tmp_db_path, db_path)
print(f'Successfully created Room-compliant quran.db at {db_path}!')


