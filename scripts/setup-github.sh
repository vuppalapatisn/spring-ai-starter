#!/usr/bin/env bash
# ================================================================
# setup-github.sh — Initialize git repo and push to GitHub
#
# Prerequisites:
#   - git installed
#   - GitHub CLI (gh) installed: https://cli.github.com
#   - Docker Hub account
#
# Usage:
#   chmod +x scripts/setup-github.sh
#   ./scripts/setup-github.sh
# ================================================================

set -euo pipefail

# ── Configuration — edit these ────────────────────────────────────────────
GITHUB_USERNAME="${GITHUB_USERNAME:-your-github-username}"
REPO_NAME="${REPO_NAME:-spring-ai-starter}"
DOCKERHUB_USERNAME="${DOCKERHUB_USERNAME:-your-dockerhub-username}"
REPO_VISIBILITY="${REPO_VISIBILITY:-public}"     # public or private

echo "=================================================="
echo " Spring AI Starter — GitHub Setup"
echo "=================================================="
echo " GitHub user  : $GITHUB_USERNAME"
echo " Repo name    : $REPO_NAME"
echo " Docker Hub   : $DOCKERHUB_USERNAME"
echo " Visibility   : $REPO_VISIBILITY"
echo "=================================================="
echo ""

# ── Step 1: Init local git repo ───────────────────────────────────────────
echo "▶ Initialising git repository..."
git init
git add .
git commit -m "feat: initial Spring AI ecosystem starter

Includes:
- Multi-model chat (OpenAI, Anthropic, Mistral, Ollama)
- RAG pipeline with pgvector, Chroma, Qdrant, Redis
- Autonomous agent with tool calling
- Evaluation & guardrails
- Actuator + Swagger UI
- GitHub Actions CI/CD
- Kubernetes manifests + Helm chart
- Docker Compose full infra stack"

# ── Step 2: Create GitHub repo ────────────────────────────────────────────
echo ""
echo "▶ Creating GitHub repository..."
gh repo create "$GITHUB_USERNAME/$REPO_NAME" \
  --"$REPO_VISIBILITY" \
  --description "Production-ready Spring AI Ecosystem starter — chat, RAG, agents, K8s" \
  --push \
  --source=.

# ── Step 3: Set GitHub Secrets ────────────────────────────────────────────
echo ""
echo "▶ Setting GitHub repository secrets..."
echo "  (You'll be prompted if values aren't already set as env vars)"

set_secret() {
  local name=$1
  local value="${!name:-}"
  if [ -z "$value" ]; then
    echo -n "  Enter value for $name: "
    read -rs value
    echo ""
  fi
  gh secret set "$name" --body "$value" --repo "$GITHUB_USERNAME/$REPO_NAME"
  echo "  ✓ $name set"
}

set_secret "OPENAI_API_KEY"
set_secret "ANTHROPIC_API_KEY"
set_secret "MISTRAL_API_KEY"
set_secret "DOCKERHUB_USERNAME"
set_secret "DOCKERHUB_TOKEN"

# Optional secrets (skip if not configured)
echo ""
echo "▶ Optional secrets (press Enter to skip):"
for secret in KUBECONFIG_STAGING KUBECONFIG_PRODUCTION SLACK_WEBHOOK_URL CODECOV_TOKEN; do
  echo -n "  Enter value for $secret (or Enter to skip): "
  read -rs val
  echo ""
  if [ -n "$val" ]; then
    gh secret set "$secret" --body "$val" --repo "$GITHUB_USERNAME/$REPO_NAME"
    echo "  ✓ $secret set"
  else
    echo "  ⏭ $secret skipped"
  fi
done

# ── Step 4: Set GitHub Variables ──────────────────────────────────────────
echo ""
echo "▶ Setting GitHub variables..."
gh variable set DOCKERHUB_USERNAME --body "$DOCKERHUB_USERNAME" --repo "$GITHUB_USERNAME/$REPO_NAME"
echo "  ✓ DOCKERHUB_USERNAME variable set"

# ── Step 5: Create branch protection ─────────────────────────────────────
echo ""
echo "▶ Configuring branch protection for 'main'..."
gh api repos/"$GITHUB_USERNAME/$REPO_NAME"/branches/main/protection \
  --method PUT \
  --field required_status_checks='{"strict":true,"contexts":["Test"]}' \
  --field enforce_admins=false \
  --field required_pull_request_reviews='{"required_approving_review_count":1}' \
  --field restrictions=null 2>/dev/null || echo "  ⚠ Branch protection requires GitHub Pro for private repos"

echo ""
echo "=================================================="
echo "✅ Done!"
echo ""
echo "  Repository : https://github.com/$GITHUB_USERNAME/$REPO_NAME"
echo "  Actions    : https://github.com/$GITHUB_USERNAME/$REPO_NAME/actions"
echo "  Packages   : https://hub.docker.com/r/$DOCKERHUB_USERNAME/$REPO_NAME"
echo ""
echo "Next steps:"
echo "  1. Push a commit to 'main' to trigger CI/CD"
echo "  2. Tag a release:  git tag v1.0.0 && git push --tags"
echo "  3. Deploy to K8s:  helm upgrade --install ..."
echo "=================================================="
