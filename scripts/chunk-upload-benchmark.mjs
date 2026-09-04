import { createHash, randomBytes } from "node:crypto";
import { performance } from "node:perf_hooks";

const config = {
  baseUrl: process.env.BASE_URL ?? "http://localhost:8080",
  username: process.env.ACCOUNT_USERNAME ?? "user001",
  password: process.env.ACCOUNT_PASSWORD ?? "password123",
  fileMb: numberEnv("FILE_MB", 20),
  chunkMb: numberEnv("CHUNK_MB", 1),
  concurrent: numberEnv("CONCURRENT", 3),
  iterations: numberEnv("ITERATIONS", 1),
};

function numberEnv(name, fallback) {
  const value = Number(process.env[name]);
  return Number.isFinite(value) && value > 0 ? value : fallback;
}

function md5(buffer) {
  return createHash("md5").update(buffer).digest("hex");
}

function ms(value) {
  return Math.round(value);
}

function percent(value) {
  return `${value.toFixed(2)}%`;
}

async function requestJson(path, token, body) {
  const response = await fetch(`${config.baseUrl}${path}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(body),
  });
  const text = await response.text();
  let json = {};
  if (text) {
    try {
      json = JSON.parse(text);
    } catch {
      json = { raw: text };
    }
  }
  if (!response.ok) {
    throw new Error(`${path} failed: HTTP ${response.status} ${text}`);
  }
  return json;
}

async function login() {
  const response = await fetch(`${config.baseUrl}/account/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify({ username: config.username, password: config.password }),
  });
  const text = await response.text();
  if (!response.ok) {
    throw new Error(`/account/login failed: HTTP ${response.status} ${text}`);
  }
  return JSON.parse(text);
}

async function uploadChunk(token, uploadId, chunkIndex, chunk) {
  const form = new FormData();
  form.append("upload_id", uploadId);
  form.append("chunk_index", String(chunkIndex));
  form.append("chunk_hash", md5(chunk));
  form.append("file", new Blob([chunk], { type: "application/octet-stream" }), `${chunkIndex}.part`);

  const response = await fetch(`${config.baseUrl}/video/chunk/upload`, {
    method: "POST",
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: form,
  });
  const text = await response.text();
  if (!response.ok) {
    throw new Error(`/video/chunk/upload chunk=${chunkIndex} failed: HTTP ${response.status} ${text}`);
  }
  return text ? JSON.parse(text) : {};
}

async function mapWithConcurrency(items, concurrency, worker) {
  let cursor = 0;
  const errors = [];
  async function runWorker() {
    while (cursor < items.length) {
      const current = cursor++;
      try {
        await worker(items[current], current);
      } catch (error) {
        errors.push(error);
      }
    }
  }
  await Promise.all(Array.from({ length: Math.min(concurrency, items.length) }, runWorker));
  if (errors.length) {
    throw new Error(errors.map((error) => error.message).join("\n"));
  }
}

function buildChunks(fileBuffer, chunkSize) {
  const chunks = [];
  for (let offset = 0; offset < fileBuffer.length; offset += chunkSize) {
    chunks.push(fileBuffer.subarray(offset, Math.min(offset + chunkSize, fileBuffer.length)));
  }
  return chunks;
}

async function runOne(token, modeName, concurrency) {
  const fileSize = config.fileMb * 1024 * 1024;
  const chunkSize = config.chunkMb * 1024 * 1024;
  const fileBuffer = randomBytes(fileSize);
  const chunks = buildChunks(fileBuffer, chunkSize);
  const filename = `chunk-benchmark-${modeName}-${Date.now()}-${Math.random().toString(16).slice(2)}.mp4`;
  const fileHash = md5(fileBuffer);

  const totalStart = performance.now();
  const initStart = performance.now();
  const init = await requestJson("/video/chunk/init", token, {
    filename,
    file_size: fileSize,
    chunk_size: chunkSize,
    total_chunks: chunks.length,
    file_hash: fileHash,
  });
  const initMs = performance.now() - initStart;
  const uploadId = init.uploadId ?? init.upload_id;
  if (!uploadId) {
    throw new Error(`/video/chunk/init did not return uploadId: ${JSON.stringify(init)}`);
  }

  const uploadStart = performance.now();
  await mapWithConcurrency(chunks, concurrency, async (chunk, chunkIndex) => {
    await uploadChunk(token, uploadId, chunkIndex, chunk);
  });
  const uploadMs = performance.now() - uploadStart;

  const completeStart = performance.now();
  const complete = await requestJson("/video/chunk/complete", token, { upload_id: uploadId });
  const completeMs = performance.now() - completeStart;
  const totalMs = performance.now() - totalStart;

  if (!complete.videoId || !complete.videoUrl || complete.uploadStatus !== "COMPLETED") {
    throw new Error(`/video/chunk/complete did not return a completed video asset: ${JSON.stringify(complete)}`);
  }

  return {
    modeName,
    concurrency,
    uploadId,
    chunks: chunks.length,
    fileSize,
    initMs,
    uploadMs,
    completeMs,
    totalMs,
    videoId: complete.videoId,
    playUrl: complete.videoUrl,
  };
}

function average(results, field) {
  return results.reduce((sum, item) => sum + item[field], 0) / results.length;
}

function printResult(result) {
  console.log(`\n[${result.modeName}]`);
  console.log(`concurrency       : ${result.concurrency}`);
  console.log(`chunks            : ${result.chunks}`);
  console.log(`file size         : ${(result.fileSize / 1024 / 1024).toFixed(2)} MB`);
  console.log(`init              : ${ms(result.initMs)} ms`);
  console.log(`chunk upload      : ${ms(result.uploadMs)} ms`);
  console.log(`complete+MinIO    : ${ms(result.completeMs)} ms`);
  console.log(`total             : ${ms(result.totalMs)} ms`);
  console.log(`playUrl           : ${result.playUrl}`);
}

async function main() {
  console.log("Chunk upload benchmark");
  console.log(`baseUrl           : ${config.baseUrl}`);
  console.log(`username          : ${config.username}`);
  console.log(`fileMb            : ${config.fileMb}`);
  console.log(`chunkMb           : ${config.chunkMb}`);
  console.log(`concurrent        : ${config.concurrent}`);
  console.log(`iterations        : ${config.iterations}`);

  const loginResponse = await login();
  const token = loginResponse.token;
  if (!token) throw new Error("login did not return token");

  const serialResults = [];
  const concurrentResults = [];

  for (let i = 0; i < config.iterations; i++) {
    console.log(`\nIteration ${i + 1}/${config.iterations}`);
    const serial = await runOne(token, "serial", 1);
    printResult(serial);
    serialResults.push(serial);

    const concurrent = await runOne(token, `concurrent-${config.concurrent}`, config.concurrent);
    printResult(concurrent);
    concurrentResults.push(concurrent);
  }

  const serialAvg = average(serialResults, "totalMs");
  const concurrentAvg = average(concurrentResults, "totalMs");
  const uploadSerialAvg = average(serialResults, "uploadMs");
  const uploadConcurrentAvg = average(concurrentResults, "uploadMs");
  const totalImprovement = ((serialAvg - concurrentAvg) / serialAvg) * 100;
  const uploadImprovement = ((uploadSerialAvg - uploadConcurrentAvg) / uploadSerialAvg) * 100;

  console.log("\nSummary");
  console.log(`serial avg total             : ${ms(serialAvg)} ms`);
  console.log(`concurrent avg total         : ${ms(concurrentAvg)} ms`);
  console.log(`total duration improvement   : ${percent(totalImprovement)}`);
  console.log(`serial avg chunk upload      : ${ms(uploadSerialAvg)} ms`);
  console.log(`concurrent avg chunk upload  : ${ms(uploadConcurrentAvg)} ms`);
  console.log(`chunk upload improvement     : ${percent(uploadImprovement)}`);
  console.log("success rate                 : 100%");
}

main().catch((error) => {
  console.error("\nBenchmark failed");
  console.error(error);
  process.exit(1);
});
