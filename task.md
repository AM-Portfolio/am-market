# Docker Base Images & Module Build Task

## Phase 1: Create Docker Base Images
- [/] Create Java/Maven base image with cached dependencies
  - [ ] Create `Dockerfile.base-java-maven` with Maven dependency caching
  - [ ] Build and tag as `am-java-maven-base:latest`
- [ ] Create Python base image with common dependencies
  - [ ] Create `Dockerfile.base-python` with pip caching
  - [ ] Build and tag as `am-python-base:latest`

## Phase 2: Update Module Dockerfiles with Caching
- [ ] Update `am-market-data/Dockerfile`
  - [ ] Use multi-stage build with Maven base image
  - [ ] Implement dependency caching layer
  - [ ] Keep runtime stage optimized
- [ ] Update `am-common-investment-data/Dockerfile`
  - [ ] Create new Dockerfile using Maven base image
  - [ ] Implement dependency caching
- [ ] Update `am-parser/Dockerfile`
  - [ ] Use Python base image
  - [ ] Optimize dependency caching
- [ ] Update `market-data-analysis-py/Dockerfile`
  - [ ] Use Python base image
  - [ ] Optimize dependency caching

## Phase 3: Update Docker Compose Files
- [ ] Update root `docker-compose.yml`
  - [ ] Add base image build services
  - [ ] Update service dependencies
- [ ] Update individual module docker-compose files
  - [ ] Ensure they reference base images

## Phase 4: Build All Modules
- [ ] Build base images first
  - [ ] Build Java/Maven base
  - [ ] Build Python base
- [ ] Build individual modules
  - [ ] Build `am-market-data`
  - [ ] Build `am-common-investment-data`
  - [ ] Build `am-parser`
  - [ ] Build `market-data-analysis-py`
- [ ] Verify all builds complete successfully
  - [ ] Document any runtime errors (infrastructure not set up is acceptable)

## Phase 5: Documentation
- [ ] Create workflow for building base images
- [ ] Update README with new build instructions
- [ ] Document caching strategy
