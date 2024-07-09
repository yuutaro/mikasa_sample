FROM python:3

COPY requirements.txt ./
RUN apt-get update && \
  apt-get upgrade -y
RUN  pip install --no-cache-dir -r requirements.txt

WORKDIR /workspace