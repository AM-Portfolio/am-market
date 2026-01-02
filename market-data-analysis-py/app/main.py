from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api.routes import router

app = FastAPI(title="Market Data Analysis Service", version="1.0.0")

# Allow all origins for dev simplicity
app.add_middleware(
    CORSMiddleware,
    allow_origin_regex="https?://.*",
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router, prefix="/api/market/analysis/v1")

@app.get("/health")
def health_check():
    return {"status": "ok", "service": "market-data-analysis-py"}
