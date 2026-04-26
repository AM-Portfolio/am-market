# AM Parser - Mutual Fund Portfolio Management API
# Using Python base image for faster builds
ARG BASE_REGISTRY=""
FROM ${BASE_REGISTRY}am-python-base:latest

# Set working directory
WORKDIR /app

# Copy requirements first for better caching
COPY requirements.txt .

# Install only additional dependencies not in base image
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Set environment variables
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

# Expose API port
EXPOSE 8000

# Health check
# Force Trigger: 2026-04-11T14:31:00 (Credential Alignment)
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD python -c "import httpx; httpx.get('http://localhost:8000/docs')" || exit 1

# Run the API server
CMD ["uvicorn", "am_api.api:app", "--host", "0.0.0.0", "--port", "8000"]
