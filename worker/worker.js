const { createClient } = require('redis');
const Docker = require('dockerode');
const fs = require('fs/promises');
const path = require('path');
const { v4: uuid } = require('uuid');

const docker = new Docker();
const redisClient = createClient({ url: 'redis://redis:6379' });

const HOST_BASE_DIR = process.env.HOST_TMP_DIR; 
if (!HOST_BASE_DIR) {
    console.error("FATAL: HOST_TMP_DIR environment variable is not set.");
    process.exit(1);
}

// This is the path where the volume is mounted INSIDE THIS worker container.
const CONTAINER_BASE_DIR = "/worker-tmp";         // Path inside worker container

const LANG_CONFIG = {
    javascript: {
        image: 'node:18-alpine',
        fileName: 'index.js',
        command: (filePath) => ['node', filePath],
    },
    python: {
        image: 'python:3.9-slim',
        fileName: 'script.py',
        command: (filePath) => ['python', filePath],
    },
    java: {
        image: 'openjdk:17-jdk-slim',
        fileName: 'Main.java',
        command: (filePath) => ['sh', '-c', `javac -d /app ${filePath} && java Main`],
    },
    cpp: {
        image: 'gcc:latest',
        fileName: 'main.cpp',
        command: (filePath) => ['sh', '-c', `g++ ${filePath} -o /app/main && /app/main`],
    },
    c: {
        image: 'gcc:latest',
        fileName: 'main.c',
        command: (filePath) => ['sh', '-c', `gcc ${filePath} -o /app/main && /app/main`],
    },
    php: {
        image: 'php:8-cli-alpine',
        fileName: 'index.php',
        command: (filePath) => ['php', filePath],
    }
};

async function processJob(jobString) {
    const job = JSON.parse(jobString);
    const { jobId, code, language } = job;
    console.log(`Processing job ${jobId} for language ${language}...`);

    await redisClient.hSet(`job:${jobId}`, 'status', 'running');

    const config = LANG_CONFIG[language];
    if (!config) {
        await redisClient.hSet(`job:${jobId}`, {
            status: 'error',
            output: `Language '${language}' is not supported.`,
        });
        return;
    }

    // Create a unique directory ID
    const dirId = uuid();
    
    // This is the path the Node.js 'fs' module will use inside this container
    const containerDir = path.join(CONTAINER_BASE_DIR, dirId);

    // This is the path the HOST Docker daemon will use
    const hostDir = path.join(HOST_BASE_DIR, dirId);


    await fs.mkdir(containerDir, { recursive: true });
    const sourceFilePath = path.join(containerDir, config.fileName);
    await fs.writeFile(sourceFilePath, code);

    // Small delay to ensure file is flushed
    await new Promise(resolve => setTimeout(resolve, 100));

    try {
        let output = '';
        const containerFilePath = `/app/${config.fileName}`;

        console.log("File exists before container:", await fs.access(sourceFilePath).then(() => true).catch(() => false));
        console.log("Mounting host path:", hostDir); 

        const container = await docker.createContainer({
            Image: config.image,
            Cmd: config.command(containerFilePath),
            WorkingDir: "/app",
            AttachStdout: true,
            AttachStderr: true,
            HostConfig: {
                AutoRemove: true,
                Memory: 256 * 1024 * 1024,
                NetworkMode: 'none',
                Mounts: [
                    {
                        Target: "/app",
                        Source: hostDir, // Correct host-visible path
                        Type: "bind",
                        ReadOnly: false
                    }
                ]
            }
        });

        await container.start();

        const logStream = await container.logs({ follow: true, stdout: true, stderr: true });
        logStream.on('data', chunk => {
            output += chunk.toString('utf8');
        });

        await container.wait({ timeout: 30000 });

        console.log(`Job ${jobId} completed.`);
        await redisClient.hSet(`job:${jobId}`, {
            status: 'completed',
            output: output,
        });

    } catch (err) {
        console.error(`Error processing job ${jobId}:`, err);
        await redisClient.hSet(`job:${jobId}`, {
            status: 'error',
            output: err.message || 'An execution error occurred.',
        });
    } finally {
        await fs.rm(containerDir, { recursive: true, force: true });
    }
}

async function startWorker() {
    await redisClient.connect();
    console.log("Worker connected to Redis, waiting for jobs...");
    while (true) {
        try {
            const job = await redisClient.brPop('code-queue', 0);
            console.log("Raw job from Redis:", job);

            let jobString;
            if (Array.isArray(job)) {
                jobString = job[1];
            } else {
                jobString = job.element;
            }

            if (jobString) {
                await processJob(jobString);
            }
        } catch (err) {
            console.error("Worker loop error:", err);
            await new Promise(resolve => setTimeout(resolve, 1000));
        }
    }
}

startWorker();
