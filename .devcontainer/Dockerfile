FROM mcr.microsoft.com/devcontainers/typescript-node:18

# Install orchent
ARG TARGETPLATFORM
ARG OS=${TARGETPLATFORM%/*}
ARG ARCH=${TARGETPLATFORM#*/}

ARG ORCHENT_VERSION=1.3.0.rc1

RUN if [ "${OS}" = "linux" ]; then \
    wget https://github.com/indigo-dc/orchent/releases/download/v${ORCHENT_VERSION}/orchent_${ORCHENT_VERSION}_${ARCH}.deb && \
    dpkg -i orchent_${ORCHENT_VERSION}_${ARCH}.deb && \
    rm -rf orchent_${ORCHENT_VERSION}_${ARCH}.deb; \
    fi
