"""
Import all certificates from the AWS RDS global CA bundle into the JVM truststore.
Run inside an eclipse-temurin container that has keytool on PATH.
"""
import os
import re
import subprocess
import sys
import tempfile

BUNDLE_PATH = "/tmp/rds-ca-bundle.pem"
TRUSTSTORE_PATH = "/opt/cacerts-with-rds"

with open(BUNDLE_PATH) as f:
    content = f.read()

certs = re.findall(
    r"-----BEGIN CERTIFICATE-----.*?-----END CERTIFICATE-----",
    content,
    re.DOTALL,
)

if not certs:
    print("ERROR: no certificates found in bundle", file=sys.stderr)
    sys.exit(1)

print(f"Importing {len(certs)} RDS CA certificates into {TRUSTSTORE_PATH} ...")

failed = 0
for i, cert in enumerate(certs):
    with tempfile.NamedTemporaryFile(suffix=".pem", delete=False, mode="w") as f:
        f.write(cert)
        fname = f.name

    result = subprocess.run(
        [
            "keytool", "-import", "-noprompt",
            "-alias", f"rds-ca-{i}",
            "-keystore", TRUSTSTORE_PATH,
            "-storepass", "changeit",
            "-file", fname,
        ],
        capture_output=True,
    )
    os.unlink(fname)

    # keytool exits 1 when the alias already exists — that is not an error here.
    if result.returncode != 0 and b"already exists" not in result.stderr:
        print(f"ERROR importing cert {i}: {result.stderr.decode()}", file=sys.stderr)
        failed += 1

if failed > 0:
    print(f"{failed} certificate import(s) failed — aborting", file=sys.stderr)
    sys.exit(1)

print("All RDS CA certificates imported successfully.")
