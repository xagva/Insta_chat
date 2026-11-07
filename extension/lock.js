// Instagram is a single-page app: in-app clicks change the URL without a page load,
// so the network rule alone isn't enough. Watch the path and bounce anything that isn't DMs/login.
const ALLOWED = /^\/(direct\/|accounts\/|challenge\/|two_factor|auth_platform\/|oauth\/)/;
const INBOX = "/direct/inbox/";

function enforce() {
  if (!ALLOWED.test(location.pathname)) location.replace(INBOX);
}

enforce();
let last = location.href;
setInterval(() => {
  if (location.href !== last) {
    last = location.href;
    enforce();
  }
}, 250);
