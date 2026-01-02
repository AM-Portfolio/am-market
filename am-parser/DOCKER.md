# AM Parser - Docker Setup

## Complete Docker Setup

This project includes a complete Docker setup with the Python API server and MongoDB using Docker Compose.

### Quick Start

```bash
# Build and start all services (API + MongoDB + Mongo Express)
docker-compose up -d --build

# Check services status
docker-compose ps

# View API logs
docker-compose logs -f am-api

# Test API endpoint
curl http://localhost:8000/docs

# View data in MongoDB web interface
# Open: http://localhost:8081 (webadmin / webpass123)
```

### Services Included

| Service | Port | Purpose | Credentials |
|---------|------|---------|-------------|
| AM API | 8000 | FastAPI Server | - |
| MongoDB | 27017 | Database | admin / password123 |
| Mongo Express | 8081 | Web UI | webadmin / webpass123 |

### API Usage

**Access Points**:
- API Server: http://localhost:8000
- Interactive Docs (Swagger): http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

**Example API Calls**:
```bash
# Save portfolio via API
curl -X POST "http://localhost:8000/portfolios" \
     -H "Content-Type: application/json" \
     -d @data/mfextractedholdings/motilaloswalmf.json

# List portfolios
curl http://localhost:8000/portfolios

# Get specific portfolio
curl "http://localhost:8000/portfolios/{fund_name}/{date}"
```

**Using Python Client from Host**:
```python
import httpx

# Connect to dockerized API
client = httpx.Client(base_url="http://localhost:8000")
response = client.post("/portfolios", json=portfolio_data)
print(response.json())
```

### Data Persistence

- MongoDB data is persisted in Docker volumes
- Data directory (`./data`) mounted to API container
- Survives container restarts
- To reset: `docker-compose down -v` (⚠️ deletes all data)

### Development Workflow

1. **Start Services**: `docker-compose up -d --build`
2. **Check Logs**: `docker-compose logs -f am-api`
3. **Test API**: Open http://localhost:8000/docs
4. **Upload Data**: Use API endpoints at http://localhost:8000/portfolios
5. **View Data**: http://localhost:8081 (Mongo Express)
6. **Stop Services**: `docker-compose down`

### Docker Commands Reference

```bash
# Build and start services
docker-compose up -d --build

# View all logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f am-api
docker-compose logs -f mongodb

# Restart a service
docker-compose restart am-api

# Rebuild after code changes
docker-compose up -d --build am-api

# Stop all services
docker-compose down

# Stop and remove volumes (⚠️ deletes data)
docker-compose down -v

# Execute commands in running container
docker-compose exec am-api python -m am_app list-portfolios
```

### Environment Variables

Create a `.env` file in the project root for custom configuration:

```env
# MongoDB Connection (optional - defaults work with docker-compose)
MONGO_URI=mongodb://admin:password123@mongodb:27017
MONGO_DB=mutual_funds

# LLM Configuration (if using LLM parsing)
LLM_PROVIDER=openai
OPENAI_API_KEY=your_api_key_here
```

### Troubleshooting

**API not starting:**
```bash
# Check logs
docker-compose logs am-api

# Rebuild from scratch
docker-compose down
docker-compose up -d --build
```

**MongoDB connection issues:**
```bash
# Verify MongoDB is healthy
docker-compose ps

# Test MongoDB connection
docker-compose exec mongodb mongosh -u admin -p password123
```

**Port conflicts:**
If ports 8000, 8081, or 27017 are already in use, modify the port mappings in `docker-compose.yml`:
```yaml
ports:
  - "8001:8000"  # Change host port (left side)
```

See `mongo-init/README.md` for additional MongoDB configuration.
