#!/bin/bash
# Version bump script for Qon
# Usage: ./scripts/bump_version.sh [major|minor|patch|<version>]

set -e

BUILD_GRADLE="app/build.gradle"

if [ ! -f "$BUILD_GRADLE" ]; then
    echo "Error: $BUILD_GRADLE not found. Run from project root."
    exit 1
fi

# Extract current version
CURRENT_VERSION_NAME=$(grep -E 'versionName\s*=' "$BUILD_GRADLE" | head -1 | sed -E 's/.*versionName\s*=\s*"([^"]+)".*/\1/')
CURRENT_VERSION_CODE=$(grep -E 'versionCode\s*=' "$BUILD_GRADLE" | head -1 | sed -E 's/.*versionCode\s*=\s*([0-9]+).*/\1/')

if [ -z "$CURRENT_VERSION_NAME" ] || [ -z "$CURRENT_VERSION_CODE" ]; then
    echo "Error: Could not extract current version from $BUILD_GRADLE"
    exit 1
fi

echo "Current version: $CURRENT_VERSION_NAME (code: $CURRENT_VERSION_CODE)"

# Parse version
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION_NAME"

# Determine new version
if [ $# -eq 0 ]; then
    echo "Usage: $0 [major|minor|patch|<version>]"
    echo "Example: $0 patch"
    echo "Example: $0 1.7.0"
    exit 1
fi

case "$1" in
    major)
        NEW_MAJOR=$((MAJOR + 1))
        NEW_MINOR=0
        NEW_PATCH=0
        ;;
    minor)
        NEW_MAJOR=$MAJOR
        NEW_MINOR=$((MINOR + 1))
        NEW_PATCH=0
        ;;
    patch)
        NEW_MAJOR=$MAJOR
        NEW_MINOR=$MINOR
        NEW_PATCH=$((PATCH + 1))
        ;;
    *)
        # Assume it's a full version string
        if [[ "$1" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
            IFS='.' read -r NEW_MAJOR NEW_MINOR NEW_PATCH <<< "$1"
        else
            echo "Error: Invalid version format. Use major|minor|patch or X.Y.Z"
            exit 1
        fi
        ;;
esac

NEW_VERSION_NAME="${NEW_MAJOR}.${NEW_MINOR}.${NEW_PATCH}"
NEW_VERSION_CODE=$(date +%s)

echo "New version: $NEW_VERSION_NAME (code: $NEW_VERSION_CODE)"

# Update build.gradle
sed -i "s/versionName = \".*\"/versionName = \"$NEW_VERSION_NAME\"/" "$BUILD_GRADLE"
sed -i "s/versionCode = .*/versionCode = $NEW_VERSION_CODE/" "$BUILD_GRADLE"

echo "Updated $BUILD_GRADLE"

# Show diff
echo "---"
git diff "$BUILD_GRADLE"

echo ""
echo "Done! Commit the changes and tag with: git tag v$NEW_VERSION_NAME"