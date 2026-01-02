#!/bin/bash
# Quick start script for AM Parser MongoDB environment

echo "🚀 Starting AM Parser MongoDB Environment"
echo "=" x 40

# Start Docker Compose services
echo "🐳 Starting MongoDB and Mongo Express..."
docker-compose up -d

# Wait a moment for services to initialize
echo "⏳ Waiting for services to start..."
sleep 5

# Check service status
echo "📋 Service Status:"
docker-compose ps

# Show connection info
echo ""
echo "✅ Environment Ready!"
echo "🗄️  MongoDB: mongodb://admin:password123@localhost:27017"
echo "🌐 Web UI: http://localhost:8081"
echo "   Username: webadmin"
echo "   Password: webpass123"
echo ""
echo "💡 Test connection:"
echo "   python test_docker_setup.py"
echo ""
echo "📊 Save portfolio data:"
echo "   python -m am_app save-portfolio --input data/mfextractedholdings/motilaloswalmf.json"
