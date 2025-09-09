FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Copy data into container
COPY ./data /app

# Expose the port
EXPOSE 9099

# Start the HTTP server
CMD ["python", "-m", "http.server", "9099"]