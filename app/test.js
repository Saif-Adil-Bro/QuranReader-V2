const https = require('https');
https.get('https://api.quran.com/api/v4/verses/by_chapter/1?words=true&word_fields=text_uthmani,location&translations=161&language=bn', (res) => {
  let data = '';
  res.on('data', (chunk) => { data += chunk; });
  res.on('end', () => { console.log(data); });
}).on('error', (err) => { console.log('Error: ', err.message); });
