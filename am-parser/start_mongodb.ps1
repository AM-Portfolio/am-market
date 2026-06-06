# Quick start script for AM Parser MongoDB environment

Write-Host "🚀 Starting AM Parser MongoDB Environment" -ForegroundColor Green
Write-Host "=" * 40

# Start Docker Compose services
Write-Host "🐳 Starting MongoDB and Mongo Express..." -ForegroundColor Yellow
docker-compose up -d

# Wait a moment for services to initialize
Write-Host "⏳ Waiting for services to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Check service status
Write-Host "📋 Service Status:" -ForegroundColor Cyan
docker-compose ps

# Show connection info
Write-Host ""
Write-Host "✅ Environment Ready!" -ForegroundColor Green
Write-Host "🗄️  MongoDB: mongodb://admin:<REDACTED_PASSWORD>@localhost:27017" -ForegroundColor White
Write-Host "🌐 Web UI: http://localhost:8081" -ForegroundColor White
Write-Host "   Username: webadmin" -ForegroundColor Gray
Write-Host "   Password: webpass123" -ForegroundColor Gray
Write-Host ""
Write-Host "💡 Test connection:" -ForegroundColor Yellow
Write-Host "   python test_docker_setup.py" -ForegroundColor White
Write-Host ""
Write-Host "📊 Save portfolio data:" -ForegroundColor Yellow
Write-Host "   python -m am_app save-portfolio --input data/mfextractedholdings/motilaloswalmf.json" -ForegroundColor White
