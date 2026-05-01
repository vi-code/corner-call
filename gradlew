#!/usr/bin/env sh
ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
LOCAL_GRADLE="$ROOT_DIR/.gradle-local/gradle-8.2/bin/gradle"
CACHED_GRADLE="$HOME/.gradle/wrapper/dists/gradle-8.2-bin/bbg7u40eoinfdyxsxr3z4i7ta/gradle-8.2/bin/gradle"
export GRADLE_USER_HOME="$ROOT_DIR/.gradle-local/user-home"

if [ -x "$LOCAL_GRADLE" ]; then
  exec "$LOCAL_GRADLE" "$@"
fi

if [ ! -x "$CACHED_GRADLE" ]; then
  echo "Gradle 8.2 is not available in .gradle-local or at $CACHED_GRADLE." >&2
  echo "Install Gradle or run Android Studio once to populate the Gradle wrapper cache." >&2
  exit 1
fi

exec "$CACHED_GRADLE" "$@"
