import crypto from "node:crypto";
import { requiredEnv } from "./env";

export function randomToken(): string {
  return crypto.randomBytes(32).toString("base64url");
}

export function codeChallenge(verifier: string): string {
  return crypto.createHash("sha256").update(verifier).digest("base64url");
}

export function xAuthorizeUrl(state: string, verifier: string): string {
  const params = new URLSearchParams({
    response_type: "code",
    client_id: requiredEnv("X_CLIENT_ID"),
    redirect_uri: requiredEnv("X_CALLBACK_URL"),
    scope: "users.read follows.read offline.access",
    state,
    code_challenge: codeChallenge(verifier),
    code_challenge_method: "S256",
  });
  return `https://x.com/i/oauth2/authorize?${params.toString()}`;
}

export async function exchangeCode(code: string, verifier: string): Promise<string> {
  const clientId = requiredEnv("X_CLIENT_ID");
  const clientSecret = requiredEnv("X_CLIENT_SECRET");
  const body = new URLSearchParams({
    grant_type: "authorization_code",
    code,
    redirect_uri: requiredEnv("X_CALLBACK_URL"),
    code_verifier: verifier,
    client_id: clientId,
  });

  const res = await fetch("https://api.x.com/2/oauth2/token", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      Authorization: `Basic ${Buffer.from(`${clientId}:${clientSecret}`).toString("base64")}`,
    },
    body,
  });
  if (!res.ok) throw new Error(`X token exchange failed: ${res.status}`);
  const data = (await res.json()) as { access_token?: string };
  if (!data.access_token) throw new Error("X token exchange did not return an access token");
  return data.access_token;
}

export async function getMe(accessToken: string): Promise<{ id: string; username: string }> {
  const res = await fetch("https://api.x.com/2/users/me?user.fields=username", {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (!res.ok) throw new Error(`X user lookup failed: ${res.status}`);
  const data = (await res.json()) as { data?: { id: string; username: string } };
  if (!data.data?.id || !data.data.username) throw new Error("X user lookup returned no user");
  return data.data;
}

export async function followsSolWear(accessToken: string, userId: string): Promise<boolean> {
  const target = process.env.SOLWEAR_X_USER_ID;
  if (!target) return false;
  let token: string | undefined;
  for (let page = 0; page < 5; page++) {
    const params = new URLSearchParams({ max_results: "1000" });
    if (token) params.set("pagination_token", token);
    const res = await fetch(`https://api.x.com/2/users/${userId}/following?${params.toString()}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    if (!res.ok) throw new Error(`X following lookup failed: ${res.status}`);
    const data = (await res.json()) as {
      data?: Array<{ id: string }>;
      meta?: { next_token?: string };
    };
    if (data.data?.some((user) => user.id === target)) return true;
    token = data.meta?.next_token;
    if (!token) break;
  }
  return false;
}
