// Rebuild the bundled PNG with Node.js only: node scripts/generate-default-avatar.cjs
const { mkdirSync, writeFileSync } = require('node:fs');
const path = require('node:path');
const { deflateSync } = require('node:zlib');

const size = 128;
const samples = 4;
const pixels = Buffer.alloc(size * (1 + size * 4));

function colorAt(x, y) {
  if ((x - 64) ** 2 + (y - 64) ** 2 > 63 ** 2) return [0, 0, 0, 0];
  const head = (x - 64) ** 2 + (y - 46) ** 2 <= 21 ** 2;
  const shoulders = ((x - 64) / 41) ** 2 + ((y - 106) / 35) ** 2 <= 1;
  return head || shoulders ? [113, 164, 139, 255] : [231, 244, 235, 255];
}

for (let y = 0; y < size; y++) {
  for (let x = 0; x < size; x++) {
    const sum = [0, 0, 0, 0];
    for (let sy = 0; sy < samples; sy++) {
      for (let sx = 0; sx < samples; sx++) {
        const rgba = colorAt(x + (sx + 0.5) / samples, y + (sy + 0.5) / samples);
        for (let channel = 0; channel < 4; channel++) sum[channel] += rgba[channel];
      }
    }
    const offset = y * (1 + size * 4) + 1 + x * 4;
    for (let channel = 0; channel < 4; channel++) pixels[offset + channel] = Math.round(sum[channel] / (samples * samples));
  }
}

function crc32(buffer) {
  let crc = 0xffffffff;
  for (const byte of buffer) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit++) crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0);
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
  const name = Buffer.from(type, 'ascii');
  const length = Buffer.alloc(4);
  length.writeUInt32BE(data.length);
  const checksum = Buffer.alloc(4);
  checksum.writeUInt32BE(crc32(Buffer.concat([name, data])));
  return Buffer.concat([length, name, data, checksum]);
}

const header = Buffer.alloc(13);
header.writeUInt32BE(size, 0);
header.writeUInt32BE(size, 4);
header[8] = 8;
header[9] = 6; // RGBA
const png = Buffer.concat([
  Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
  chunk('IHDR', header),
  chunk('IDAT', deflateSync(pixels)),
  chunk('IEND', Buffer.alloc(0))
]);
const output = path.resolve(__dirname, '../miniprogram/images/default-avatar.png');
mkdirSync(path.dirname(output), { recursive: true });
writeFileSync(output, png);
console.log('Generated 128 x 128 default-avatar.png (' + png.length + ' bytes)');
