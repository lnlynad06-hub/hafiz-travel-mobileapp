const fs = require('fs');
const basePath = 'C:/Users/dania/Downloads/HAFIZ TRAVEL & TOUR';
const blade = fs.readFileSync(basePath + '/resources/views/company/packages/index.blade.php', 'utf8');

const start = blade.indexOf('id="tab-hotels"');
const end = blade.indexOf('id="tab-pricing"');
console.log(blade.substring(start, end));
