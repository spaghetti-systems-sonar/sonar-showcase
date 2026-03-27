#!/bin/bash
# Setup script for local development with corporate Artifactory proxy
#
# This script configures Maven and npm to use Artifactory for artifact downloads.
# GitHub Actions will use public registries instead (no setup needed there).

set -e

echo "🔧 Setting up Artifactory configuration for local development..."

# Step 1: Set environment variable for Maven profile activation
echo ""
echo "📝 Step 1: Set USE_ARTIFACTORY environment variable"
echo "Add this to your shell profile (~/.zshrc, ~/.bashrc, or ~/.bash_profile):"
echo ""
echo "export USE_ARTIFACTORY=true"
echo ""

# Step 2: Configure Maven settings.xml
echo "📝 Step 2: Configure Maven settings.xml"
MAVEN_SETTINGS="$HOME/.m2/settings.xml"

if [ ! -f "$MAVEN_SETTINGS" ]; then
    mkdir -p "$HOME/.m2"
    echo "Creating $MAVEN_SETTINGS..."
    cat > "$MAVEN_SETTINGS" << 'EOF'
<settings>
  <servers>
    <server>
      <id>artifactory</id>
      <username>YOUR_ARTIFACTORY_USERNAME</username>
      <password>YOUR_ARTIFACTORY_PASSWORD</password>
    </server>
  </servers>
</settings>
EOF
    echo "✅ Created $MAVEN_SETTINGS"
    echo "⚠️  Please edit $MAVEN_SETTINGS and replace YOUR_ARTIFACTORY_USERNAME and YOUR_ARTIFACTORY_PASSWORD"
else
    echo "✅ $MAVEN_SETTINGS already exists"
    if ! grep -q "<id>artifactory</id>" "$MAVEN_SETTINGS"; then
        echo "⚠️  Warning: No <server> with <id>artifactory</id> found in settings.xml"
        echo "   Please add the artifactory server configuration manually"
    fi
fi

# Step 3: Configure npm .npmrc
echo ""
echo "📝 Step 3: Configure npm .npmrc for package downloads"
NPMRC_FILE="frontend/.npmrc"

if [ ! -f "$NPMRC_FILE" ]; then
    echo "Creating $NPMRC_FILE..."
    cat > "$NPMRC_FILE" << 'EOF'
registry=https://repox.jfrog.io/artifactory/api/npm/npm/
//repox.jfrog.io/artifactory/api/npm/npm/:_auth=YOUR_BASE64_ENCODED_CREDENTIALS
always-auth=true
EOF
    echo "✅ Created $NPMRC_FILE"
    echo "⚠️  Please edit $NPMRC_FILE and replace YOUR_BASE64_ENCODED_CREDENTIALS"
    echo "   Generate it with: echo -n 'username:password' | base64"
else
    echo "✅ $NPMRC_FILE already exists"
fi

# Summary
echo ""
echo "🎉 Setup complete! Next steps:"
echo ""
echo "1. Set USE_ARTIFACTORY=true in your shell profile and reload it:"
echo "   echo 'export USE_ARTIFACTORY=true' >> ~/.zshrc"
echo "   source ~/.zshrc"
echo ""
echo "2. Update credentials in $MAVEN_SETTINGS"
echo "3. Update credentials in $NPMRC_FILE"
echo ""
echo "Then run: mvn clean install"
echo ""
echo "Note: .npmrc is gitignored to prevent credential leaks"
