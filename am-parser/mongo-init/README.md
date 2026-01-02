# MongoDB Docker Environment

This directory contains MongoDB setup for AM Parser using Docker Compose.

## Services

### MongoDB (Port 27017)
- **Image**: MongoDB 7.0
- **Username**: admin
- **Password**: password123
- **Database**: mutual_funds
- **Data Persistence**: Yes (Docker volumes)

### Mongo Express (Port 8081)
- **Web UI**: http://localhost:8081
- **Username**: webadmin  
- **Password**: webpass123
- **Purpose**: MongoDB web interface for data visualization

## Quick Start

```bash
# Start MongoDB services
docker-compose up -d

# Check if services are running
docker-compose ps

# View logs
docker-compose logs mongodb
docker-compose logs mongo-express

# Stop services
docker-compose down

# Stop and remove volumes (⚠️ deletes all data)
docker-compose down -v
```

## Connection Strings

### For Python Applications
```python
# Default connection (no auth for local development)
mongo_uri = "mongodb://localhost:27017"

# With authentication
mongo_uri = "mongodb://admin:password123@localhost:27017"
```

### For AM Parser Service
```python
from am_persistence import create_mutual_fund_service

# Default local connection
service = create_mutual_fund_service()

# Custom connection
service = create_mutual_fund_service(
    mongo_uri="mongodb://admin:password123@localhost:27017",
    db_name="mutual_funds"
)
```

## Database Schema

### Collections Created
- **portfolios** - Main mutual fund portfolio data with schema validation
- **fund_summaries** - Aggregated fund statistics

### Indexes
- `mutual_fund_name + portfolio_date` (unique)
- `mutual_fund_name` 
- `portfolio_date`
- `portfolio_holdings.isin_code`
- `updated_at`

## Development Workflow

1. **Start MongoDB**:
   ```bash
   docker-compose up -d mongodb
   ```

2. **Test Connection**:
   ```python
   python -c "
   import asyncio
   from am_persistence import create_mutual_fund_service
   
   async def test():
       service = create_mutual_fund_service()
       await service._get_collection()
       print('✅ MongoDB connection successful!')
       await service.close()
   
   asyncio.run(test())
   "
   ```

3. **Save Portfolio Data**:
   ```bash
   python -m am_app save-portfolio --input data/mfextractedholdings/motilaloswalmf.json
   ```

4. **View Data in Web UI**:
   - Start Mongo Express: `docker-compose up -d mongo-express`
   - Open: http://localhost:8081
   - Login: webadmin / webpass123

## Troubleshooting

### Port Already in Use
```bash
# Check what's using port 27017
netstat -ano | findstr 27017

# Kill MongoDB processes
taskkill /f /im mongod.exe
```

### Reset Database
```bash
# Stop and remove all data
docker-compose down -v

# Start fresh
docker-compose up -d
```

### View Container Logs
```bash
docker-compose logs -f mongodb
```
