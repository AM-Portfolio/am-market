import time
from app.core.logging.am_logging_client import AMLogger

app = FastAPI(title="Market Data Analysis Service", version="1.0.0")

am_logger = AMLogger(service_name="market-data-analysis-py")

# Middleware for structured logging
@app.middleware("http")
async def log_requests(request, call_next):
    start_time = time.time()
    response = await call_next(request)
    duration = (time.time() - start_time) * 1000 # to ms
    
    context = {
        "method": request.method,
        "path": request.url.path,
        "status": response.status_code,
        "latency_ms": round(duration, 2),
        "client_ip": request.client.host if request.client else "unknown"
    }
    
    am_logger.log("INFO", "API Request Processed", context)
    return response

# Allow all origins for dev simplicity
app.add_middleware(
    CORSMiddleware,
    allow_origin_regex="https?://.*",
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router, prefix="/v1/market/analysis")

@app.get("/health")
def health_check():
    return {"status": "ok", "service": "market-data-analysis-py"}
