const ROLE_HOME_BY_ROLE = Object.freeze({
  USER: 'home',
  ARTIST: 'artist-dashboard',
  ADMIN: 'admin-dashboard'
});

export function getRoleHomeRouteName(role) {
  return ROLE_HOME_BY_ROLE[role] || ROLE_HOME_BY_ROLE.USER;
}

export function canAccessRoute(role, allowedRoles) {
  return !allowedRoles?.length || allowedRoles.includes(role);
}
