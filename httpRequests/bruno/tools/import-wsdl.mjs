#!/usr/bin/env node
/**
 * Imports the WSDL contract snapshots (wsdl/*.wsdl) as Bruno requests.
 *
 * Wraps `bru import wsdl` and fixes the two things the importer gets wrong for
 * this service:
 *   1. the URL is only the path from soap:address (/services) - make it absolute
 *      via {{application-port}}
 *   2. the generated body is missing the targetNamespace on the request element
 *      - without it the service answers 404
 *
 * The .wsdl files stay the source of truth; the generated requests are written
 * to wsdl/<ServiceName>/.
 *
 * Usage (from httpRequests/bruno):
 *   node tools/import-wsdl.mjs
 */

import { execFileSync } from 'node:child_process';
import { mkdir, mkdtemp, readFile, readdir, rm, writeFile } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const COLLECTION_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const WSDL_DIR = path.join(COLLECTION_ROOT, 'wsdl');
const BASE_URL = 'http://localhost:{{application-port}}/services';

async function collectYmlFiles(directory) {
  const result = [];
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const entryPath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      result.push(...(await collectYmlFiles(entryPath)));
    } else if (entry.isFile() && entry.name.endsWith('.yml')) {
      result.push(entryPath);
    }
  }
  return result;
}

function rewriteRequest(content, targetNamespace) {
  return content
    .split('\n')
    .map((line) => {
      if (/^\s+url: \/services\s*$/.test(line)) {
        return line.replace('/services', BASE_URL);
      }
      if (line.trimStart().startsWith('data: ')) {
        return line.replace(/(<soap:Body>\s*<)([A-Za-z0-9_.:-]+)(\s*>)/,
          `$1$2 xmlns="${targetNamespace}"$3`);
      }
      return line;
    })
    .join('\n');
}

async function main() {
  const wsdlFiles = (await readdir(WSDL_DIR)).filter((name) => name.endsWith('.wsdl')).sort();
  if (wsdlFiles.length === 0) {
    throw new Error(`No .wsdl files found in ${WSDL_DIR}`);
  }

  for (const wsdlFile of wsdlFiles) {
    const wsdlPath = path.join(WSDL_DIR, wsdlFile);
    const wsdl = await readFile(wsdlPath, 'utf8');
    const targetNamespace = wsdl.match(/targetNamespace="([^"]+)"/)?.[1];
    const serviceName = wsdl.match(/<wsdl:service name="([^"]+)"/)?.[1];
    if (!targetNamespace || !serviceName) {
      throw new Error(`Cannot read targetNamespace/service name from ${wsdlFile}`);
    }

    const tempDir = await mkdtemp(path.join(os.tmpdir(), 'bruno-wsdl-'));
    try {
      execFileSync('bru', ['import', 'wsdl', '-s', wsdlPath, '-o', tempDir, '-n', wsdlFile.replace(/\.wsdl$/, '')], {
        stdio: 'pipe',
      });
      const imported = await collectYmlFiles(tempDir);
      const targetDir = path.join(WSDL_DIR, serviceName);
      await mkdir(targetDir, { recursive: true });
      for (const file of imported) {
        const content = rewriteRequest(await readFile(file, 'utf8'), targetNamespace);
        await writeFile(path.join(targetDir, path.basename(file)), content);
        console.log(`generated wsdl/${serviceName}/${path.basename(file)}`);
      }
    } finally {
      await rm(tempDir, { recursive: true, force: true });
    }
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
