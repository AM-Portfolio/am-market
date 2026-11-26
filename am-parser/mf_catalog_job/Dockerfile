# am-parser MF catalog job: NSE MASTER_DOWNLOAD → SQLite → Postgres mf_catalog.
# Build from am-parser/: docker build -f mf_catalog_job/Dockerfile -t am-parser-mf-catalog-job .
FROM python:3.12-slim

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    TZ=Asia/Kolkata

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 1000 appgroup \
    && useradd --uid 1000 --gid appgroup --create-home --shell /usr/sbin/nologin appuser

WORKDIR /app

COPY mf_catalog_job/requirements-job.txt /app/mf_catalog_job/requirements-job.txt
RUN pip install --no-cache-dir -r /app/mf_catalog_job/requirements-job.txt

COPY mf_catalog_job/ /app/mf_catalog_job/

RUN mkdir -p /app/mf_catalog_job/data/masters \
    && chown -R appuser:appgroup /app

USER appuser
WORKDIR /app

# Non-secret JDBC URL shape (credentials from Vault / env at runtime).
ENV AM_MARKET_MF_JDBC_URL="jdbc:postgresql://postgres.asrax.in:5432/user_platform_dev?currentSchema=mf_catalog"

CMD ["python", "mf_catalog_job/daily_job.py", "--fail-on-stale"]
