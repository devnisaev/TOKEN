import { execSync } from 'node:child_process';
import { mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = dirname(fileURLToPath(import.meta.url));
const outDir = join(root, '../shared-api-types');
mkdirSync(outDir, { recursive: true });

const specs = [
  ['auth', 'specs/auth.yaml'],
  ['registry', 'specs/registry.yaml'],
  ['compliance', 'specs/compliance.yaml'],
  ['marketplace', 'specs/marketplace.yaml'],
  ['rental', 'specs/rental.yaml'],
  ['gateway', 'specs/gateway.yaml'],
];

for (const [name, specPath] of specs) {
  const out = join(outDir, `${name}.ts`);
  execSync(
    `npx openapi-typescript ${join(root, specPath)} -o ${out}`,
    { stdio: 'inherit', cwd: root },
  );
}

execSync(`npx prettier --write "${outDir}/*.ts" 2>/dev/null || true`, {
  stdio: 'inherit',
  shell: true,
});

console.log('Generated types in frontend/shared-api-types/');
