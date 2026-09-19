const https = require('https');
const fs = require('fs');

const file = fs.createWriteStream("quran_font.ttf");
https.get("https://github.com/aliftype/amiri/raw/main/fonts/AmiriQuran-Regular.ttf", function(response) {
  response.pipe(file);
  file.on("finish", () => {
    file.close();
    console.log("Download Completed");
  });
});
