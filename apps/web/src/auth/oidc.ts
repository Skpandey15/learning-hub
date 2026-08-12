import { UserManager, WebStorageStateStore, type User } from "oidc-client-ts";

const origin = window.location.origin;

export const userManager = new UserManager({
  authority: `${origin}/auth/realms/learning-hub`,
  client_id: "learning-hub-web",
  redirect_uri: `${origin}/oidc/callback`,
  post_logout_redirect_uri: `${origin}/signed-out`,
  response_type: "code",
  scope: "openid profile email roles",
  loadUserInfo: false,
  automaticSilentRenew: false,
  monitorSession: true,
  userStore: new WebStorageStateStore({ store: window.sessionStorage }),
});

export function isUsable(user: User | null): user is User {
  return user !== null && !user.expired && typeof user.access_token === "string";
}
