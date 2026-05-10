import fs from "node:fs";
import path from "node:path";
import Database from "better-sqlite3";
import { dataDir } from "./env";

type GlobalWithDb = typeof globalThis & { __solwearDb?: Database.Database };

export function db(): Database.Database {
  const g = globalThis as GlobalWithDb;
  if (g.__solwearDb) return g.__solwearDb;

  const dir = dataDir();
  fs.mkdirSync(dir, { recursive: true });
  const database = new Database(path.join(dir, "solwear.db"));
  database.pragma("journal_mode = WAL");
  database.exec(`
    CREATE TABLE IF NOT EXISTS notify_emails (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      email TEXT NOT NULL,
      created_at TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS pinboard_ideas (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      x_user_id TEXT NOT NULL,
      username TEXT NOT NULL,
      idea TEXT NOT NULL,
      status TEXT NOT NULL DEFAULT 'pending',
      created_at TEXT NOT NULL,
      moderated_at TEXT
    );

    CREATE INDEX IF NOT EXISTS idx_pinboard_ideas_user ON pinboard_ideas (x_user_id);
    CREATE INDEX IF NOT EXISTS idx_pinboard_ideas_status_created ON pinboard_ideas (status, created_at);
  `);
  g.__solwearDb = database;
  return database;
}
