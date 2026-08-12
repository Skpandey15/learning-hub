import { describe, expect, it } from "vitest";
import { isUsable, userManager } from "./oidc.ts";

describe("OIDC configuration", () => {
  it("requires a non-expired access token", () => {
    expect(isUsable(null)).toBe(false);
    expect(isUsable({ expired: true, access_token: "token" } as never)).toBe(false);
    expect(isUsable({ expired: false } as never)).toBe(false);
    expect(isUsable({ expired: false, access_token: "token" } as never)).toBe(true);
  });
  it("uses the learning hub public client", () => {
    expect(userManager.settings.client_id).toBe("learning-hub-web");
    expect(userManager.settings.response_type).toBe("code");
  });
});
