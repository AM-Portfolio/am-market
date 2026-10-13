ARG BASE_REGISTRY=""
FROM ${BASE_REGISTRY}am-python-base:latest

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD python -c "import httpx; httpx.get('http://localhost:8080/health')" || exit 1

CMD ["uvicorn", "am_news.main:app", "--host", "0.0.0.0", "--port", "8080"]
