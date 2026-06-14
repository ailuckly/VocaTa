#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$REPO_DIR"

echo "==> workspace diff whitespace"
git diff --check

echo "==> docs trailing whitespace"
python3 - <<'PY'
from pathlib import Path

paths = []
root_files = [
    "AGENTS.md",
    "CHANGELOG.md",
    "CODE_OF_CONDUCT.md",
    "CODE_STYLE.md",
    "CONTRIBUTING.md",
    "MAINTAINERS.md",
    "README.md",
    "SECURITY.md",
    "SUPPORT.md",
]

for name in root_files:
    path = Path(name)
    if path.exists():
        paths.append(path)

for pattern in [
    "docs/**/*.md",
    ".github/**/*.md",
    ".github/ISSUE_TEMPLATE/*.yml",
    ".gitattributes",
    ".env.example",
]:
    paths.extend(sorted(Path(".").glob(pattern)))

seen = set()
failures = []
for path in paths:
    if path in seen or not path.is_file():
        continue
    seen.add(path)
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        if line.endswith((" ", "\t")):
            failures.append(f"{path}:{line_number}")

if failures:
    print("Trailing whitespace found:")
    for failure in failures:
        print(failure)
    raise SystemExit(1)

print("Docs trailing whitespace OK")
PY

echo "==> markdown local links"
python3 - <<'PY'
import re
from pathlib import Path

paths = [p for p in Path(".").glob("*.md")]
paths += sorted(Path("docs").rglob("*.md"))
paths += sorted(Path(".github/ISSUE_TEMPLATE").glob("*.yml"))

missing = []
for path in paths:
    text = path.read_text(encoding="utf-8")
    for match in re.finditer(r"\[[^\]]+\]\(([^)]+)\)", text):
        target = match.group(1).split("#", 1)[0]
        if not target or "://" in target or target.startswith("mailto:"):
            continue
        if not (path.parent / target).resolve().exists():
            missing.append((str(path), target))

if missing:
    for source, target in missing:
        print(f"MISSING {source} -> {target}")
    raise SystemExit(1)

print("Markdown local links OK")
PY

echo "==> tech debt backlog contract"
python3 - <<'PY'
from pathlib import Path

backlog_path = Path("docs/技术债与后续工作.md")
required_sections = [
    "# 技术债与后续工作",
    "## 记录规则",
    "## 当前事项",
    "## 相关入口",
]
required_links = [
    "docs/开源成熟度路线图.md",
    "docs/开源成熟度审计.md",
]
referencing_files = [
    Path("AGENTS.md"),
    Path("README.md"),
    Path("docs/维护与分流指南.md"),
    Path("docs/AI审查清单.md"),
    Path("docs/开源成熟度路线图.md"),
    Path("docs/开源成熟度审计.md"),
]
failures = []

if not backlog_path.exists():
    failures.append(f"{backlog_path}: missing dedicated tech debt backlog")
else:
    text = backlog_path.read_text(encoding="utf-8")
    for section in required_sections:
        if section not in text:
            failures.append(f"{backlog_path}: missing section {section!r}")
    for link in required_links:
        if link not in text:
            failures.append(f"{backlog_path}: missing related link {link}")

for path in referencing_files:
    text = path.read_text(encoding="utf-8")
    if str(backlog_path) not in text:
        failures.append(f"{path}: must reference {backlog_path}")

if failures:
    print("Tech debt backlog contract mismatch:")
    for failure in failures:
        print(failure)
    raise SystemExit(1)

print("Tech debt backlog contract OK")
PY

echo "==> pre-pr risk hint contract"
python3 - <<'PY'
import json
import subprocess
from pathlib import Path

script_path = Path("scripts/pre-pr-check.sh")
doc_path = Path("docs/验证清单.md")
pr_template_path = Path(".github/pull_request_template.md")
script_text = script_path.read_text(encoding="utf-8")
doc_text = doc_path.read_text(encoding="utf-8")
pr_template_text = pr_template_path.read_text(encoding="utf-8")
required_script_terms = [
    "Risk review hints:",
    "auth/permission",
    "database/schema",
    "ai-streaming/provider",
    "api-contract",
    "dependency/tooling",
    "./scripts/report-frontend-assets.sh",
    "--json",
    "OUTPUT_JSON",
    "risk_review_hints",
    "recommended_commands",
    "changed_file_sample",
]
required_doc_terms = [
    "风险提示",
    "Risk review hints",
    "不会替代人工确认",
    "./scripts/pre-pr-check.sh --json",
    "机器可读 JSON",
    "risk_review_hints",
    "./scripts/report-frontend-assets.sh",
]
required_pr_template_terms = [
    "./scripts/pre-pr-check.sh",
    "Risk review hints",
    "风险提示处理",
]
failures = []

for term in required_script_terms:
    if term not in script_text:
        failures.append(f"{script_path}: missing risk hint term {term!r}")

for term in required_doc_terms:
    if term not in doc_text:
        failures.append(f"{doc_path}: missing pre-pr risk documentation term {term!r}")

for term in required_pr_template_terms:
    if term not in pr_template_text:
        failures.append(f"{pr_template_path}: missing PR risk template term {term!r}")

try:
    json_output = subprocess.check_output(["./scripts/pre-pr-check.sh", "--json"], text=True)
    summary = json.loads(json_output)
except Exception as exc:
    failures.append(f"{script_path}: --json output must be valid JSON ({exc})")
else:
    required_json_keys = [
        "branch",
        "base_ref",
        "changed_file_count",
        "areas",
        "risk_flags",
        "recommended_commands",
        "risk_review_hints",
        "changed_file_sample",
        "changed_file_sample_limit",
        "has_more_changed_files",
    ]
    for key in required_json_keys:
        if key not in summary:
            failures.append(f"{script_path}: --json output missing key {key!r}")
    if not isinstance(summary.get("areas"), dict):
        failures.append(f"{script_path}: --json areas must be an object")
    if not isinstance(summary.get("risk_flags"), dict):
        failures.append(f"{script_path}: --json risk_flags must be an object")
    if not isinstance(summary.get("recommended_commands"), list):
        failures.append(f"{script_path}: --json recommended_commands must be a list")
    if not isinstance(summary.get("risk_review_hints"), list):
        failures.append(f"{script_path}: --json risk_review_hints must be a list")
    elif any(not item for item in summary["risk_review_hints"]):
        failures.append(f"{script_path}: --json risk_review_hints must not contain empty items")
    if not isinstance(summary.get("changed_file_sample"), list):
        failures.append(f"{script_path}: --json changed_file_sample must be a list")
    elif any(not item for item in summary["changed_file_sample"]):
        failures.append(f"{script_path}: --json changed_file_sample must not contain empty items")

if failures:
    print("Pre-PR risk hint contract mismatch:")
    for failure in failures:
        print(failure)
    raise SystemExit(1)

print("Pre-PR risk hint contract OK")
PY

echo "==> validation entrypoint documentation contract"
python3 - <<'PY'
from pathlib import Path

required_entries = [
    "./scripts/doctor.sh",
    "./scripts/check.sh",
    "./scripts/pre-pr-check.sh",
    "./scripts/report-frontend-assets.sh",
    "./scripts/validate-docs.sh",
    "./scripts/validate-backend.sh",
    "./scripts/validate-web.sh",
    "./scripts/validate-admin.sh",
    "./scripts/validate-docker.sh",
]
docs = {
    Path("README.md"): [
        *required_entries,
        "./scripts/doctor.sh --json",
        "production build asset",
    ],
    Path("docs/开发工作流.md"): [
        "./scripts/pre-pr-check.sh",
        "./scripts/report-frontend-assets.sh",
        "前端产物体积",
    ],
    Path("docs/AI协作流程.md"): [
        "./scripts/report-frontend-assets.sh",
        "前端产物体积",
    ],
}
failures = []

for path, terms in docs.items():
    text = path.read_text(encoding="utf-8")
    for term in terms:
        if term not in text:
            failures.append(f"{path}: missing validation entrypoint documentation term {term!r}")

if failures:
    print("Validation entrypoint documentation mismatch:")
    for failure in failures:
        print(failure)
    raise SystemExit(1)

print("Validation entrypoint documentation OK")
PY

echo "==> sensitive ignore contract"
python3 - <<'PY'
from pathlib import Path

gitignore_path = Path(".gitignore")
text = gitignore_path.read_text(encoding="utf-8")
required_patterns = [
    ".env",
    ".env.*",
    "!.env.example",
    "**/.env",
    "**/.env.*",
    "!**/.env.example",
    "**/*.pem",
    "**/*.key",
    "**/*.crt",
    "**/*.p12",
    "**/*.jks",
]
failures = []

for pattern in required_patterns:
    if pattern not in text:
        failures.append(f"{gitignore_path}: missing sensitive ignore pattern {pattern!r}")

if failures:
    print("Sensitive ignore contract mismatch:")
    for failure in failures:
        print(failure)
    raise SystemExit(1)

print("Sensitive ignore contract OK")
PY

echo "==> doctor script contract"
python3 - <<'PY'
import json
import subprocess
from pathlib import Path

doctor_path = Path("scripts/doctor.sh")
doctor_text = doctor_path.read_text(encoding="utf-8")
required_doctor_terms = [
    "--json",
    "OUTPUT_JSON",
    "sensitive ignore rules",
    "git check-ignore",
    ".env.prod",
    "test-private-key.pem",
    "scripts/check.sh",
    "scripts/pre-pr-check.sh",
    "scripts/report-frontend-assets.sh",
    "scripts/validate-docs.sh",
    "Failures:",
    "Warnings:",
]
failures = []

for term in required_doctor_terms:
    if term not in doctor_text:
        failures.append(f"{doctor_path}: missing doctor contract term {term!r}")

try:
    json_output = subprocess.check_output(["./scripts/doctor.sh", "--json"], text=True)
    summary = json.loads(json_output)
except Exception as exc:
    failures.append(f"{doctor_path}: --json output must be valid JSON ({exc})")
else:
    required_json_keys = [
        "failures",
        "warnings",
        "checks",
        "env_key_drift",
    ]
    for key in required_json_keys:
        if key not in summary:
            failures.append(f"{doctor_path}: --json output missing key {key!r}")
    if not isinstance(summary.get("checks"), list):
        failures.append(f"{doctor_path}: --json checks must be a list")
    elif any("status" not in item or "message" not in item for item in summary["checks"]):
        failures.append(f"{doctor_path}: --json checks items must include status and message")
    if not isinstance(summary.get("env_key_drift"), dict):
        failures.append(f"{doctor_path}: --json env_key_drift must be an object")

if failures:
    print("Doctor script contract mismatch:")
    for failure in failures:
        print(failure)
    raise SystemExit(1)

print("Doctor script contract OK")
PY

echo "==> editorconfig contract"
python3 - <<'PY'
from pathlib import Path

editorconfig_path = Path(".editorconfig")
code_style_path = Path("CODE_STYLE.md")
editorconfig_text = editorconfig_path.read_text(encoding="utf-8")
code_style_text = code_style_path.read_text(encoding="utf-8")
required_editorconfig_terms = [
    "root = true",
    "charset = utf-8",
    "end_of_line = lf",
    "insert_final_newline = true",
    "trim_trailing_whitespace = true",
    "indent_style = space",
    "[*.java]",
    "indent_size = 4",
]
required_code_style_terms = [
    ".editorconfig",
    "UTF-8",
    "LF",
    "Java 使用 4 空格缩进",
    "前端、文档和配置文件使用 2 空格缩进",
]
failures = []

for term in required_editorconfig_terms:
    if term not in editorconfig_text:
        failures.append(f"{editorconfig_path}: missing formatting baseline {term!r}")

for term in required_code_style_terms:
    if term not in code_style_text:
        failures.append(f"{code_style_path}: missing editorconfig documentation term {term!r}")

if failures:
    print("EditorConfig contract mismatch:")
    for failure in failures:
        print(failure)
    raise SystemExit(1)

print("EditorConfig contract OK")
PY

echo "==> gitattributes contract"
python3 - <<'PY'
from pathlib import Path

gitattributes_path = Path(".gitattributes")
code_style_path = Path("CODE_STYLE.md")
gitattributes_text = gitattributes_path.read_text(encoding="utf-8")
code_style_text = code_style_path.read_text(encoding="utf-8")
required_gitattributes_terms = [
    "* text=auto eol=lf",
    "*.bat text eol=crlf",
    "*.cmd text eol=crlf",
    "*.png binary",
    "*.jpg binary",
    "*.pdf binary",
    "*.woff2 binary",
    "*.mp4 binary",
]
required_code_style_terms = [
    ".gitattributes",
    "Git 文本规范",
    "脚本、源码、文档和配置文件默认按 LF 归一化",
    "Windows 批处理文件保留 CRLF",
    "图片、字体、音频、视频和 PDF 按二进制处理",
]
failures = []

for term in required_gitattributes_terms:
    if term not in gitattributes_text:
        failures.append(f"{gitattributes_path}: missing Git attribute rule {term!r}")

for term in required_code_style_terms:
    if term not in code_style_text:
        failures.append(f"{code_style_path}: missing gitattributes documentation term {term!r}")

if failures:
    print("Git attributes contract mismatch:")
    for failure in failures:
        print(failure)
    raise SystemExit(1)

print("Git attributes contract OK")
PY

echo "==> frontend asset report contract"
python3 - <<'PY'
import json
import subprocess
from pathlib import Path

script_path = Path("scripts/report-frontend-assets.sh")
docs_path = Path("docs/验证清单.md")
backlog_path = Path("docs/技术债与后续工作.md")
script_text = script_path.read_text(encoding="utf-8")
docs_text = docs_path.read_text(encoding="utf-8")
backlog_text = backlog_path.read_text(encoding="utf-8")
help_text = subprocess.check_output([str(script_path), "--help"], text=True)
required_script_terms = [
    "WARN_KB",
    "Largest assets:",
    "Assets over threshold:",
    "Run ./scripts/validate-$name.sh first",
    "OUTPUT_JSON",
]
required_docs_terms = [
    "./scripts/report-frontend-assets.sh",
    "WARN_KB",
    "production build asset",
    "--json",
]
required_backlog_terms = [
    "./scripts/report-frontend-assets.sh",
    "前端静态资产体积",
]
required_help_terms = [
    "Usage: ./scripts/report-frontend-assets.sh [--json] [web|admin|all]",
    "WARN_KB",
    "--json",
]
failures = []

if not script_path.exists():
    failures.append(f"{script_path}: missing frontend asset report script")
elif not script_path.stat().st_mode & 0o111:
    failures.append(f"{script_path}: must be executable")

for term in required_script_terms:
    if term not in script_text:
        failures.append(f"{script_path}: missing asset report term {term!r}")

for term in required_docs_terms:
    if term not in docs_text:
        failures.append(f"{docs_path}: missing asset report documentation term {term!r}")

for term in required_backlog_terms:
    if term not in backlog_text:
        failures.append(f"{backlog_path}: missing asset report backlog term {term!r}")

for term in required_help_terms:
    if term not in help_text:
        failures.append(f"{script_path}: --help missing term {term!r}")

try:
    json_output = subprocess.check_output([str(script_path), "--json", "web"], text=True)
    summary = json.loads(json_output)
except Exception as exc:
    failures.append(f"{script_path}: --json output must be valid JSON ({exc})")
else:
    required_json_keys = [
        "target",
        "warn_kb",
        "modules",
    ]
    for key in required_json_keys:
        if key not in summary:
            failures.append(f"{script_path}: --json output missing key {key!r}")
    if summary.get("target") != "web":
        failures.append(f"{script_path}: --json web output target must be 'web'")
    if not isinstance(summary.get("modules"), list):
        failures.append(f"{script_path}: --json modules must be a list")
    elif not summary["modules"]:
        failures.append(f"{script_path}: --json modules must not be empty for web target")
    else:
        module = summary["modules"][0]
        for key in [
            "name",
            "dist_dir",
            "exists",
            "file_count",
            "total_size_kib",
            "warn_threshold_kib",
            "assets_over_threshold",
            "largest_assets",
        ]:
            if key not in module:
                failures.append(f"{script_path}: --json module output missing key {key!r}")

if failures:
    print("Frontend asset report contract mismatch:")
    for failure in failures:
        print(failure)
    raise SystemExit(1)

print("Frontend asset report contract OK")
PY

echo "==> issue template yaml parse"
python3 - <<'PY'
from pathlib import Path

try:
    import yaml
except ModuleNotFoundError:
    yaml = None

required_keys = ["name:", "description:", "title:", "labels:", "body:"]

for path in sorted(Path(".github/ISSUE_TEMPLATE").glob("*.yml")):
    text = path.read_text(encoding="utf-8")
    if yaml is not None:
        yaml.safe_load(text)
        print(f"YAML OK {path}")
        continue

    missing = [key for key in required_keys if key not in text]
    if missing:
        print(f"YAML STRUCTURE FAIL {path}: missing {', '.join(missing)}")
        raise SystemExit(1)
    if "\t" in text:
        print(f"YAML STRUCTURE FAIL {path}: tabs are not allowed")
        raise SystemExit(1)
    print(f"YAML STRUCTURE OK {path} (PyYAML not installed)")
PY

echo "==> package json and pom xml parse"
python3 - <<'PY'
import json
import xml.etree.ElementTree as ET
from pathlib import Path

for path in [Path("vocata-web/package.json"), Path("vocata-admin/package.json")]:
    json.loads(path.read_text(encoding="utf-8"))
    print(f"JSON OK {path}")

ET.parse("vocata-server/pom.xml")
print("XML OK vocata-server/pom.xml")
PY

echo "==> java version policy"
python3 - <<'PY'
import xml.etree.ElementTree as ET
from pathlib import Path

failures = []
pom_path = Path("vocata-server/pom.xml")
java_version_path = Path(".java-version")

root = ET.parse(pom_path).getroot()
namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
java_version = root.findtext("m:properties/m:java.version", namespaces=namespace)

if java_version is None:
    failures.append("vocata-server/pom.xml: missing properties/java.version")
elif java_version != "17":
    failures.append(f"vocata-server/pom.xml: java.version is {java_version!r}, expected '17'")

if not java_version_path.exists():
    failures.append(".java-version: missing Java runtime hint")
else:
    java_version_hint = java_version_path.read_text(encoding="utf-8").strip()
    if java_version_hint != java_version:
        failures.append(
            f".java-version: {java_version_hint!r} does not match vocata-server/pom.xml java.version {java_version!r}"
        )

if failures:
    print("Java version policy mismatch:")
    for failure in failures:
        print(failure)
    raise SystemExit(1)

print(f"Java version policy OK {java_version}")
PY

echo "==> node engine policy"
python3 - <<'PY'
import json
import re
from pathlib import Path

expected = "^20.19.0 || >=22.12.0"
paths = [Path("vocata-web/package.json"), Path("vocata-admin/package.json")]
failures = []

for path in paths:
    package = json.loads(path.read_text(encoding="utf-8"))
    actual = package.get("engines", {}).get("node")
    if actual != expected:
        failures.append(f"{path}: engines.node is {actual!r}, expected {expected!r}")

nvmrc = Path(".nvmrc")
if nvmrc.exists():
    nvm_version = nvmrc.read_text(encoding="utf-8").strip()
    match = re.fullmatch(r"v?(\d+)\.(\d+)\.(\d+)", nvm_version)
    if not match:
        failures.append(".nvmrc: expected an exact Node version like 22.12.0")
    else:
        major, minor, _patch = (int(part) for part in match.groups())
        supported = (major == 20 and minor >= 19) or (major == 22 and minor >= 12) or major > 22
        if not supported:
            failures.append(f".nvmrc: Node {nvm_version} is outside policy {expected}")

if failures:
    print("Node engine policy mismatch:")
    for failure in failures:
        print(failure)
    raise SystemExit(1)

print(f"Node engine policy OK {expected}")
PY

echo "==> docker frontend build modes"
python3 - <<'PY'
import json
from pathlib import Path

def env_value(path, key):
    prefix = f"{key}="
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("export "):
            line = line[len("export "):].lstrip()
        if line.startswith(prefix):
            return line[len(prefix):].strip().strip("'\"")
    return None

checks = [
    ("WEB_BUILD_MODE", Path("vocata-web/package.json")),
    ("ADMIN_BUILD_MODE", Path("vocata-admin/package.json")),
]

env_path = Path(".env.example")
failures = []

for key, package_path in checks:
    mode = env_value(env_path, key)
    package = json.loads(package_path.read_text(encoding="utf-8"))
    scripts = package.get("scripts", {})

    if not mode:
        failures.append(f".env.example: missing {key}")
        continue

    script_name = f"build:{mode}"
    if script_name not in scripts:
        failures.append(f"{key}={mode} but {package_path} has no {script_name} script")

if failures:
    print("Docker frontend build mode mismatch:")
    for failure in failures:
        print(failure)
    raise SystemExit(1)

print("Docker frontend build modes OK")
PY

echo "==> env template duplicate keys"
python3 - <<'PY'
import re
from collections import defaultdict
from pathlib import Path

path = Path(".env.example")
seen = defaultdict(list)

for line_number, raw_line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
    line = raw_line.strip()
    if not line or line.startswith("#"):
        continue
    if line.startswith("export "):
        line = line[len("export "):].lstrip()
    if "=" not in line:
        continue
    key = line.split("=", 1)[0].strip()
    if re.match(r"^[A-Za-z_][A-Za-z0-9_]*$", key):
        seen[key].append(line_number)

duplicates = {key: lines for key, lines in seen.items() if len(lines) > 1}
if duplicates:
    print("Duplicate .env.example keys:")
    for key in sorted(duplicates):
        line_list = ", ".join(str(line) for line in duplicates[key])
        print(f"{key}: lines {line_list}")
    raise SystemExit(1)

print("Env template duplicate keys OK")
PY

echo "==> compose env template coverage"
python3 - <<'PY'
import re
from pathlib import Path

def env_keys(path):
    keys = set()
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("export "):
            line = line[len("export "):].lstrip()
        if "=" not in line:
            continue
        key = line.split("=", 1)[0].strip()
        if re.match(r"^[A-Za-z_][A-Za-z0-9_]*$", key):
            keys.add(key)
    return keys

compose_text = Path("docker-compose.yml").read_text(encoding="utf-8")
compose_keys = set(re.findall(r"\$\{([A-Za-z_][A-Za-z0-9_]*)(?::-[^}]*)?\}", compose_text))
template_keys = env_keys(Path(".env.example"))
missing = sorted(compose_keys - template_keys)

if missing:
    print("Compose env template coverage mismatch:")
    for key in missing:
        print(f"docker-compose.yml references {key}, but .env.example does not define it")
    raise SystemExit(1)

print("Compose env template coverage OK")
PY

echo "==> shell script syntax"
while IFS= read -r script; do
  bash -n "$script"
  echo "Shell syntax OK $script"
done < <(find scripts -maxdepth 1 -type f -name '*.sh' -print | sort)

echo "==> validation script executable bits"
missing_executable=0
for script in \
  scripts/check-node-version.sh \
  scripts/check.sh \
  scripts/doctor.sh \
  scripts/export-schema.sh \
  scripts/pre-pr-check.sh \
  scripts/report-frontend-assets.sh \
  scripts/validate-admin.sh \
  scripts/validate-backend.sh \
  scripts/validate-docker.sh \
  scripts/validate-docs.sh \
  scripts/validate-staging-host.sh \
  scripts/validate-web.sh; do
  if [[ ! -x "$script" ]]; then
    echo "NOT EXECUTABLE $script"
    missing_executable=1
  fi
done

if [[ "$missing_executable" -ne 0 ]]; then
  exit 1
fi

echo "Validation script executable bits OK"

echo "==> secret pattern scan"
python3 - <<'PY'
import re
import subprocess
from pathlib import Path

patterns = [
    re.compile(r"sk-[A-Za-z0-9_-]{20,}"),
    re.compile(r"ghp_[A-Za-z0-9_]{20,}"),
    re.compile(r"github_pat_[A-Za-z0-9_]{20,}"),
    re.compile(r"AIza[0-9A-Za-z_-]{20,}"),
    re.compile(r"xox[baprs]-[A-Za-z0-9-]{20,}"),
    re.compile(r"-----BEGIN (RSA |OPENSSH |EC |DSA )?PRIVATE KEY-----"),
]

excluded_dirs = {"node_modules", "target", "dist", ".idea", ".vscode"}
findings = []

git_files = subprocess.check_output(
    ["git", "ls-files", "--cached", "--others", "--exclude-standard"],
    text=True,
).splitlines()

for file_name in git_files:
    path = Path(file_name)
    if not path.is_file():
        continue
    if any(part in excluded_dirs for part in path.parts):
        continue
    try:
        text = path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        continue
    for line_number, line in enumerate(text.splitlines(), start=1):
        if any(pattern.search(line) for pattern in patterns):
            findings.append(f"{path}:{line_number}")

if findings:
    print("Potential secret patterns found:")
    for finding in findings:
        print(finding)
    raise SystemExit(1)

print("Secret pattern scan OK")
PY

echo "==> node version policy"
"$SCRIPT_DIR/check-node-version.sh"
