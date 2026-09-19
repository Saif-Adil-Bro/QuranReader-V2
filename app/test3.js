const https = require('https');
https.get('https://api.alquran.cloud/v1/surah/2/quran-uthmani', (res) => {
  let data = '';
  res.on('data', (chunk) => { data += chunk; });
  res.on('end', () => { 
    try {
      const parsed = JSON.parse(data);
      console.log("Verse 1 text:", parsed.data.ayahs[0].text);
      console.log("First 45 chars:", parsed.data.ayahs[0].text.substring(0, 45));
    } catch(e) { console.log(e); }
  });
}).on('error', (err) => { console.log('Error: ', err.message); });
