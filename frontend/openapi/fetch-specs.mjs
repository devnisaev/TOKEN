#!/usr/bin/env node
/**
 * Refresh OpenAPI specs from running services (dev only).
 * Usage: AUTH_URL=http://localhost:8083/api node fetch-specs.mjs
 */
import { writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = dirname(fileURLToPath(import.meta.url));
const specsDir = join(root, 'specs');

const services = [
  { name: 'auth', url: process.env.AUTH_URL ?? 'http://localhost:8083/api' },
  { name: 'registry', url: process.env.REGISTRY_URL ?? 'http://localhost:8081/api' },
  { name: 'compliance', url: process.env.COMPLIANCE_URL ?? 'http://localhost:8087/api' },
  { name: 'marketplace', url: process.env.MARKETPLACE_URL ?? 'http://localhost:8084/api' },
  { name: 'payment', url: process.env.PAYMENT_URL ?? 'http://localhost:8085/api' },
  { name: 'wallet', url: process.env.WALLET_URL ?? 'http://localhost:8090/api' },
  { name: 'issuance', url: process.env.ISSUANCE_URL ?? 'http://localhost:8082/api' },
  { name: 'notification', url: process.env.NOTIFICATION_URL ?? 'http://localhost:8089/api' },
  { name: 'document', url: process.env.DOCUMENT_URL ?? 'http://localhost:8088/api' },
  { name: 'rental', url: process.env.RENTAL_URL ?? 'http://localhost:8086/api' },
];

for (const { name, url } of services) {
  const res = await fetch(`${url}/v3/api-docs`);
  if (!res.ok) {
    console.warn(`Skip ${name}: ${res.status} ${url}`);
    continue;
  }
  const json = await res.json();
  writeFileSync(join(specsDir, `${name}.yaml`), JSON.stringify(json, null, 2));
  console.log(`Fetched ${name} → specs/${name}.yaml`);
}
