import { spawn } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const rootDir = path.resolve(__dirname);

const services = [
  { name: 'catalog-service', dir: 'catalog-service', jar: 'catalog-service-0.0.1-SNAPSHOT.jar', port: 8082 },
  { name: 'cart-service', dir: 'cart-service', jar: 'cart-service-0.0.1-SNAPSHOT.jar', port: 8083 },
  { name: 'auth-service', dir: 'auth-service', jar: 'auth-service-0.0.1-SNAPSHOT.jar', port: 8081 },
  { name: 'payment-service', dir: 'payment-service', jar: 'payment-service-0.0.1-SNAPSHOT.jar', port: 8085 },
  { name: 'order-service', dir: 'order-service', jar: 'order-service-0.0.1-SNAPSHOT.jar', port: 8084 },
  { name: 'api-gateway', dir: 'api-gateway', jar: 'api-gateway-0.0.1-SNAPSHOT.jar', port: 3001 },
];

console.log('Starting Microservices Architecture (JAR mode)...\n');

const processes = [];

for (const svc of services) {
  console.log(`[STARTING] ${svc.name} on port ${svc.port}...`);
  const svcPath = path.join(rootDir, svc.dir);
  const jarPath = path.join('target', svc.jar);

  const proc = spawn('java', ['-jar', jarPath], {
    cwd: svcPath,
    stdio: 'pipe'
  });

  proc.stdout.on('data', (data) => {
    const text = data.toString();
    if (text.includes('Started ') || text.includes('Tomcat started') || text.includes('Netty started')) {
      console.log(`[ONLINE] ${svc.name} is ready on port ${svc.port}`);
    }
  });

  proc.stderr.on('data', (data) => {
    // print errors if unexpected
  });

  proc.on('error', (err) => {
    console.error(`[ERROR] Failed to launch ${svc.name}:`, err.message);
  });

  proc.on('close', (code) => {
    if (code !== 0 && code !== null) {
      console.log(`[STOPPED] ${svc.name} exited with code ${code}`);
    }
  });

  processes.push(proc);
}

process.on('SIGINT', () => {
  console.log('\nShutting down all services...');
  processes.forEach((p) => p.kill());
  process.exit(0);
});
