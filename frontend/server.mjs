import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { extname, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('.', import.meta.url));
const port = Number(process.env.WEB_PORT || 5173);
const apiUrl = process.env.API_URL || `http://localhost:${process.env.API_PORT || 3001}/api`;
const types = { '.html': 'text/html', '.css': 'text/css', '.js': 'text/javascript', '.png': 'image/png', '.webp': 'image/webp', '.jpg': 'image/jpeg', '.woff2': 'font/woff2' };
const server = createServer(async (request, response) => {
  response.setHeader('X-Content-Type-Options', 'nosniff');
  response.setHeader('Cache-Control', 'no-cache');
  response.setHeader('Content-Security-Policy', `default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self' ${new URL(apiUrl).origin}; base-uri 'self'; frame-ancestors 'none'; form-action 'self'`);
  try {
    if (!['GET', 'HEAD'].includes(request.method)) { response.writeHead(405); response.end(); return; }
    const pathname = decodeURIComponent(new URL(request.url, 'http://localhost').pathname);
    if (pathname === '/config.js') {
      response.setHeader('Content-Type', 'text/javascript');
      response.end(`window.WORKBENCH_CONFIG = ${JSON.stringify({ apiUrl })};`);
      return;
    }
    const file = resolve(root, `.${pathname === '/' ? '/index.html' : pathname}`);
    if (!file.startsWith(root.endsWith(sep) ? root : root + sep) || !types[extname(file)]) { response.writeHead(404); response.end('Not found'); return; }
    const contents = await readFile(file);
    response.setHeader('Content-Type', `${types[extname(file)]}${['.html', '.css', '.js'].includes(extname(file)) ? '; charset=utf-8' : ''}`);
    response.writeHead(200);
    response.end(request.method === 'HEAD' ? undefined : contents);
  } catch {
    response.writeHead(404);
    response.end('Not found');
  }
});
server.listen(port, '127.0.0.1', () => console.log(`Workbench storefront: http://localhost:${port}`));
process.on('SIGINT', () => server.close(() => process.exit(0)));
process.on('SIGTERM', () => server.close(() => process.exit(0)));