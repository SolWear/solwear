import { NextResponse } from "next/server";
import fs from "fs/promises";
import path from "path";

export async function POST(request: Request) {
  try {
    const { email } = await request.json();

    if (!email || !email.includes("@")) {
      return NextResponse.json({ error: "Invalid email" }, { status: 400 });
    }

    // Save to a local file/database inside the container
    const dbDir = path.join(process.cwd(), "db");
    const filePath = path.join(dbDir, "emails.txt");

    // Ensure the directory exists
    await fs.mkdir(dbDir, { recursive: true });

    // Append the email and timestamp
    const entry = `${new Date().toISOString()} - ${email}\n`;
    await fs.appendFile(filePath, entry);

    return NextResponse.json({ success: true });
  } catch (error) {
    console.error("Error saving email:", error);
    return NextResponse.json({ error: "Internal server error" }, { status: 500 });
  }
}
