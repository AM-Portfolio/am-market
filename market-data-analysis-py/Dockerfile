# Market Data Analysis Service
# Using Python base image for faster builds
FROM am-python-base:latest

WORKDIR /code

# Copy requirements first for better caching
COPY ./requirements.txt /code/requirements.txt

# Install only additional dependencies not in base image
RUN pip install --no-cache-dir --upgrade -r /code/requirements.txt

# Copy application code
COPY ./app /code/app

# Expose API port
EXPOSE 8010

# Run the API server
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8010"]
